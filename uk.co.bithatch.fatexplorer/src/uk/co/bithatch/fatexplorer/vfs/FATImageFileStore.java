package uk.co.bithatch.fatexplorer.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsObject;
import de.waldheinz.fs.fat.FatFileSystem;
import uk.co.bithatch.fatexplorer.Activator;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.fatexplorer.preferences.PreferenceConstants;
import uk.co.bithatch.fatexplorer.util.FileNames;
import uk.co.bithatch.fatexplorer.util.Util;

public class FATImageFileStore extends FileStore {

	private final FATImageFileSystem fileSystem;
	private final String name;
	private final FatFileSystem fatFileSystem;
	private final FsObject dir;
	private final FATImageFileStore parent;
	private final String uuid;

	public FATImageFileStore(String uuid, FATImageFileSystem fileSystem, String path, FatFileSystem fatFileSystem) {
		this.fileSystem = fileSystem;
		this.fatFileSystem = fatFileSystem;
		this.parent = null;
		this.dir = fatFileSystem.getRoot();
		this.uuid = uuid;
		this.name = "/";
	}

	private FATImageFileStore(String name, FsObject dir, FATImageFileStore parent) {
		this.fileSystem = parent.fileSystem;
		this.fatFileSystem = parent.fatFileSystem;
		this.uuid = parent.uuid;
		this.dir = dir;
		this.parent = parent;
		this.name = name;
	}

	@Override
	public URI toURI() {
		return URI.create(PreferenceConstants.SCHEME + "://" + uuid + path());
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		try {
			synchronized(fatFileSystem) {
				FileInfo info;
				if (parent == null) {
					info = new FileInfo(FileNames.getPathFileName(FATPreferencesAccess.getPathForUUID(uuid)));
					info.setExists(true);
					info.setDirectory(true);
				} else {
					info = new FileInfo(getName());
					var parentDir = (FsDirectory) parent.dir;
					var entry = parentDir == null ? null : parentDir.getEntry(name);
					if (entry == null) {
						info.setExists(false);
					} else {
						info.setExists(true);
						info.setDirectory(entry.isDirectory());
						if (entry.isFile())
							info.setLength(entry.getFile().getLength());
						info.setLastModified(entry.getLastModified());
					}
				}
				return info;
			}
		} catch (IOException ioe) {
			throw new CoreException(Status.error("Failed to fetch info.", ioe));
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public String getUuid() {
		return uuid;
	}

	@Override
	public IFileStore getParent() {
		return parent;
	}

	@Override
	public IFileStore getChild(String name) {
		if (dir instanceof FsDirectory fsDir) {
			synchronized(fatFileSystem) {
				try {
					var de = fsDir.getEntry(name);
					if(de == null) {
						return new FATImageFileStore(name, null, this);
					}
					else if (de.isDirectory()) {
						return new FATImageFileStore(name, de.getDirectory(), this);
					} else {
						return new FATImageFileStore(name, null, this);
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
		return new FATImageFileStore(name, null, this);
	}

	@Override
	public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		var info = fetchInfo();
		var destInfo = fetchInfo();
		if(info.isDirectory() == destInfo.isDirectory() && getParent().equals(destination.getParent())) {

			synchronized (fatFileSystem) {
				if(parent == null)
					throw new CoreException(Status.error("Cannot rename the root."));

				try {
					var thisEntry = ((FsDirectory)parent.dir).getEntry(name);
					thisEntry.setName(destination.getName());
					fatFileSystem.flush();
				}
				catch(IOException ioe) {
					throw new CoreException(Status.error("Failed to copy or  move file.", ioe));
				}
			}
		}
		else {
			super.move(destination, options, monitor);
		}
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		if (dir instanceof FsDirectory fsDir) {
			return Util.stream(fsDir).map(FsDirectoryEntry::getName).filter(n -> !n.equals(".") && !n.equals(".."))
					.toList().toArray(new String[0]);
		} else
			return new String[0];
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		if (dir == null) {
			synchronized(fatFileSystem) {
				try {
					var entry = ((FsDirectory) parent.dir).getEntry(name);
					if (entry == null) {
						throw new FileNotFoundException(path());
					} else {
						var file = entry.getFile();
						var buf = allocBuffer();
						return new InputStream() {
	
							private long pos;
	
							@Override
							public int read() throws IOException {
								synchronized(fatFileSystem) {
									if(pos >= file.getLength())
										return -1;
									buf.limit((int) Math.min(file.getLength(), 1));
									file.read(pos, buf);
									buf.flip();
									try {
										if (buf.hasRemaining()) {
											try {
												return buf.get();
											} finally {
												pos++;
											}
										} else
											return -1;
									} finally {
										buf.clear();
									}
								}
							}
	
							@Override
							public int read(byte[] b, int off, int len) throws IOException {
								synchronized(fatFileSystem) {
									if(pos >= file.getLength())
										return -1;
									buf.limit((int) Math.min(file.getLength() - pos, len));
									file.read(pos, buf);
									buf.flip();
									var rd = buf.remaining();
									try {
										if (rd == 0)
											return -1;
										else {
											buf.get(b, off, rd);
											pos += rd;
											return rd;
										}
									} finally {
										buf.clear();
									}
								}
							}
	
						};
					}
				} catch (IOException ioe) {
					throw new CoreException(Status.error("Failed to open input stream.", ioe));
				}
			}
		}

		throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Read not supported"));
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		if (dir == null) {
			synchronized(fatFileSystem) {
				try {
					var entry = ((FsDirectory) parent.dir).getEntry(name);
					if (entry == null) {
						entry = ((FsDirectory) parent.dir).addFile(name);
					}
					var file = entry.getFile();
					var buf = allocBuffer();
					var startPos = ( options & EFS.APPEND ) != 0 ? file.getLength() : 0;
					return new OutputStream() {

						private long pos = startPos;

						@Override
						public void write(int b) throws IOException {
							synchronized(fatFileSystem) {
								buf.put((byte)b);
								buf.flip();
								try {
									checkSpace(1);
									file.write(pos, buf);
									pos++;
								}
								finally {
									buf.clear();
								}
							}
						}

						@Override
						public void write(byte[] b, int off, int len) throws IOException {
							synchronized(fatFileSystem) {
								buf.put(b, off, len);
								buf.flip();
								try {
									var w = Math.min(b.length, len);
									checkSpace(w);
									buf.limit(w);
									file.write(pos, buf);
									pos += w;
								}
								finally {
									buf.clear();
								}
							}
						}

						
						@Override
						public void flush() throws IOException {
							synchronized(fatFileSystem) {
								file.flush();
							}
						}

						@Override
						public void close() throws IOException {
							synchronized(fatFileSystem) {
								file.setLength(pos);
								fatFileSystem.flush();
							}
						}

						private void checkSpace(int need) throws IOException {
							if(pos + need >= file.getLength()) {
//								System.out.println(pos + " + " + need + " >= " + file.getLength());
//								var extra = file.getLength() - ( pos + need ) + 1;
//								System.out.println("extra = " + extra + "( " + file.getLength() + " - (" + pos + " + " + need + ") + 1 )");
//								System.out.println("leng = " + file.getLength() + extra);
//								file.setLength(file.getLength() + extra);
								file.setLength(pos + need);
							}
						}
					};
				} catch (IOException ioe) {
					throw new CoreException(Status.error("Failed to open output stream.", ioe));
				}
			}
		}

		throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Read not supported"));
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FATImageFileStore other && Objects.equals(fileSystem, other.fileSystem)
				&& Objects.equals(path(), other.path());
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileSystem, path());
	}

	protected ByteBuffer allocBuffer() {
		return ByteBuffer.allocate(16384);
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		var parent = (FATImageFileStore)getParent();
		if(parent != null && !parent.fetchInfo().exists()) {
			parent.mkdir(options, monitor);
		}
		try {
			var dir = ((FsDirectory) parent.dir).addDirectory(getName());
			fatFileSystem.flush();
			return new FATImageFileStore(getName(), dir.getDirectory(), parent);
		}
		catch(IOException ioe) {
			throw new CoreException(Status.error("Failed to copy or  move file.", ioe));
		}
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		synchronized (fatFileSystem) {
			if(parent == null)
				throw new CoreException(Status.error("Cannot delete the root."));
			try {
				((FsDirectory)parent.dir).remove(name);
			} catch (IOException e) {
				throw new CoreException(Status.error("Failed to delete file or folder.", e));
			}
		}
	}

	public FatFileSystem nativeFileSystem() {
		return fatFileSystem;
	}

//	public boolean isDirectory() {
//		return dir instanceof FsDirectory;
//	}

	private String path() {
		return path(new StringBuilder());

	}

	private String path(StringBuilder buf) {
		if (parent != null) {
			parent.path(buf);
			buf.append("/");
		}
		try {
			if(name.equals("/")) {
				buf.append("/");
			}
			else {
				buf.append(URLEncoder.encode(name, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
		return buf.toString();
	}

}
