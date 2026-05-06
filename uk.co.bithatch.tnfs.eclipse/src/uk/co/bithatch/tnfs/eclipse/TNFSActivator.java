package uk.co.bithatch.tnfs.eclipse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import uk.co.bithatch.tnfs.lib.Protocol;
import uk.co.bithatch.tnfs.server.TNFSMounts;
import uk.co.bithatch.tnfs.server.TNFSServer;
import uk.co.bithatch.tnfs.server.TNFSServer.Builder;

/**
 * The activator class controls the plug-in life cycle
 */
public class TNFSActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "uk.co.bithatch.tnfs.eclipse"; //$NON-NLS-1$

	public static final String TNFS_PATH = "icons/tnfs16.png";

	// The shared instance
	private static TNFSActivator plugin;

	private TNFSServer<?> tcpServer;
	private TNFSServer<?> udpServer;
	private TNFSMounts mounts;
	private List<IContainer> sharedContainers = new ArrayList<IContainer>();

	private boolean tcp;

	private boolean udp;

	private String hostname;

	private int port;

	private Object enabled;

	public final static ILog LOG = ILog.of(TNFSActivator.class);

	/**
	 * The constructor
	 */
	public TNFSActivator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		

//		try {
//			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, false, new UpdateSettingsOperation());
//		} catch (InvocationTargetException  |  InterruptedException e) {
//			throw new IllegalStateException(e);
//		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TNFSActivator getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(TNFS_PATH, imageDescriptorFromPlugin(PLUGIN_ID, TNFS_PATH));
	}

	public void setSharecContainers(List<IContainer> sharedContainers) {
		this.sharedContainers = sharedContainers;
	}

	public void updateState(IProgressMonitor sub) throws IOException {

		var prefs = TNFSPreferencesAccess.get();
		var enabled = prefs.isEnabled();
		var shouldRun = enabled && sharedContainers.size() > 0;
		var running = tcpServer != null || udpServer != null;
		var exceptions = new ArrayList<Exception>();

		var hostname = prefs.getAddress();
		var port = prefs.getPort();
		var tcp = prefs.isTCP();
		var udp = prefs.isUDP();

		var cfgchg = !Objects.equals(hostname, this.hostname) || 
				!Objects.equals(port, this.port) || 
				!Objects.equals(tcp, this.tcp) || 
				!Objects.equals(udp, this.udp) || 
				!Objects.equals(enabled, this.enabled);

		if ((!shouldRun || cfgchg) && running) {
			sub.subTask("Stopping TNFS server");
			try {
				if (tcpServer != null) {
					tcpServer.close();
				}
			} finally {
				try {
					if (udpServer != null) {
						udpServer.close();
					}
				} finally {
					tcpServer = null;
					this.sharedContainers.clear();
					running = false;
				}
			}
		}

		if (shouldRun && !running) {
			sub.subTask("Starting TNFS server");

			mounts = new TNFSMounts();
			sharedContainers.forEach(cntr -> {
				mountContainer(exceptions, cntr);
			});
			
			if (tcp) {
				tcpServer = createBuilder(prefs, Protocol.TCP, exceptions).build();
				new Thread(tcpServer::run, "TNFS-TCP").start();
			}
			if (udp) {
				udpServer = createBuilder(prefs, Protocol.UDP, exceptions).build();
				new Thread(udpServer::run, "TNFS-UDP").start();
			}
		} else if (shouldRun) {
			sub.subTask("Reconfiguring TNFS server");
			var mpaths = new ArrayList<String>();
			for (var mnt : mounts.mounts()) {

				IResource cntr = null;
				for (var c : sharedContainers) {
					var mpath = TNFSResourceProperties.getProperty(c, TNFSResourceProperties.MOUNT_PATH,
							c.getFullPath().toString());
					if (mpath.equals(mnt.fs().mountPath())) {
						cntr = c;
					}
				}

				if (cntr == null) {
					mounts.unmount(mnt.fs().mountPath());
					LOG.info("Unmounted  " + mnt.fs().mountPath());
				} else {
					mpaths.add(mnt.fs().mountPath());
				}
			}

			for (var cntr : sharedContainers) {
				var mountPath = TNFSResourceProperties.getProperty(cntr, TNFSResourceProperties.MOUNT_PATH,
						cntr.getFullPath().toString());
				if (!mpaths.contains(mountPath)) {
					mountContainer(exceptions, cntr);
				}
			}
		}

		this.tcp = tcp;
		this.udp = udp;
		this.hostname = hostname;
		this.port = port;
		this.enabled = enabled;
	}

	private Builder createBuilder(TNFSPreferencesAccess prefs, Protocol protocol, ArrayList<Exception> exceptions) {
		var bldr = new TNFSServer.Builder().withPort(prefs.getPort());

		bldr.withProtocol(protocol);
		bldr.withHostname(prefs.getAddress());
		bldr.withFileSystemFactory(mounts);
		return bldr;
	}

	private void mountContainer(ArrayList<Exception> exceptions, IContainer cntr) {
		var mountPath = TNFSResourceProperties.getProperty(cntr, TNFSResourceProperties.MOUNT_PATH,
				cntr.getFullPath().toString());
		var target = cntr.getLocation().toPath().toAbsolutePath();
		try {
			mounts.mount(mountPath, target);
			LOG.info("Mounted  " + mountPath + " to " + target);
		} catch (IOException ioe) {
			LOG.error("Failed to mount " + mountPath + " to " + target + ".", ioe);
			exceptions.add(ioe);
		}
	}
}
