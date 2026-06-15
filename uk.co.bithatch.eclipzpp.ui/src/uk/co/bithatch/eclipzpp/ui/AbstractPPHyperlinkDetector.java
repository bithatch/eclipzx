package uk.co.bithatch.eclipzpp.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hyperlinking.DefaultHyperlinkDetector;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import uk.co.bithatch.bitzx.Strings;

public abstract class AbstractPPHyperlinkDetector extends DefaultHyperlinkDetector {
	private final static ILog LOG = ILog.of(AbstractPPHyperlinkDetector.class);
	
	public record Coords(String type, int line, String filename) {
	}

	@Override
	public final IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		var links = new ArrayList<IHyperlink>();
		
		/* Add default links */
		var linksArr = super.detectHyperlinks(textViewer, region, canShowMultipleHyperlinks);
		if(linksArr != null) {
			links.addAll(Arrays.asList(linksArr));
		}

		/* Give subclasses a go at adding links */
		if(canShowMultipleHyperlinks || links.isEmpty()) {
			IXtextDocument document = (IXtextDocument) textViewer.getDocument();
			if (document != null) {
				document.readOnly(new IUnitOfWork<Void, XtextResource>() {
					@Override
					public java.lang.Void exec(XtextResource resource) throws Exception {
						if (resource != null) {
							findHyperlinks(links, document, (PPResource)resource, region, canShowMultipleHyperlinks);
						}
						return null;
					}
				});
			}
		}
		
		if(canShowMultipleHyperlinks || links.isEmpty()) {
			return  links.toArray(new IHyperlink[0]);
		}
		else {
			return new IHyperlink[] { links.get(0) };
		}
	}
	
	protected void findIncludes(List<IHyperlink> links, IXtextDocument document, PPResource resource, IRegion region,
			boolean canShowMultipleHyperlinks) {
		try {
			var thisFile = resource.getFile().get();
			var thisUri = URI.createPlatformResourceURI(thisFile.getFullPath().toString(), true);
			var line = document.getLineOfOffset(region.getOffset());
			var lineText = document.get(document.getLineOffset(line), document.getLineLength(line));
			if (lineText.toLowerCase().startsWith("#include ")) {
				var filename = lineText.substring(9).trim();

				if (filename.startsWith("\"") && filename.endsWith("\"")) {
					/* Relative to this resource */
					filename = filename.substring(1, filename.length() - 1);
					var uri = thisUri.resolve(URI.createURI(filename, false));
					links.add(createHyperlink(region, resource, new Coords("include", 0, uri.toString())));
				} else if (filename.startsWith("<") && filename.endsWith(">")) {
					filename = filename.substring(1, filename.length() - 1);
					var path = findInclude(resource, filename);
					if (path != null && path.toFile().exists()) {
						links.add(createHyperlink(region, resource, new Coords("include", 0, path.toUri().toString())));
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to find hyperlinks.", e);
		}
	}

	protected abstract Path findInclude(PPResource resource, String filename);
	
	protected void findHyperlinks(List<IHyperlink> links, IXtextDocument document, PPResource resource, IRegion region, boolean canShowMultipleHyperlinks) {
		var hidden = resource.map().closestHiddenLine(region.getOffset());
		if (hidden == null)
			return;
		var line = hidden.trim();

		if (line.toLowerCase().startsWith("c_line ") || line.toLowerCase().startsWith("#line ")) {
			try {
				links.add(createHyperlink(region, resource, parseLineCoords(line)));
			} catch (Exception nfe) {
			}
		}
	}
	
	protected final Coords parseLineCoords(String line) {
		var idx = line.indexOf(' ');
		if (idx == -1) {
			idx = line.indexOf('\t');
		}
		if (idx == -1) {
			throw new IllegalArgumentException("No filename.");
		}
		var type = line.substring(0, idx).trim();
		var cline = line.substring(idx + 1).trim();
		try {
			idx = cline.indexOf(',');
			if (idx == -1) {
				idx = cline.indexOf(' ');
			}
			var lineNumber = Integer.parseInt(cline.substring(0, idx).trim());
			var filename = Strings.stripQuoted(cline.substring(idx + 1).trim()).split("::")[0].trim();
			return new Coords(type, lineNumber, filename);
		} catch (Exception nfe) {
			throw new IllegalArgumentException("Invalid line format: " + line, nfe);
		}
	}

	protected final IHyperlink createHyperlink(IRegion region, XtextResource resource, Coords coords) {
		var baseURI = resource.getURI();
		var resolvedURI = URI.createURI(coords.filename).resolve(baseURI);
		// Convert to a local file path
		File file = null;
		if (resolvedURI.isFile()) {
			file = new File(resolvedURI.toFileString());
		} else if (resolvedURI.isPlatformResource()) {
			// Handle platform:/resource/ URIs
			String platformPath = resolvedURI.toPlatformString(true);
			if (platformPath != null) {
				IFile wsFile = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot()
						.getFile(new org.eclipse.core.runtime.Path(platformPath));
				if (wsFile.exists()) {
					file = wsFile.getLocation().toFile();
				}
			}
		}
		else {
			file = new File(coords.filename);
		}
		if (file == null || !file.exists())
			return null;

		var targetFile = file;
		var targetLineNumber = coords.line;
		var linkRegion = new Region(region.getOffset(), Math.max(1, region.getLength()));

		return new IHyperlink() {
			@Override
			public IRegion getHyperlinkRegion() {
				return linkRegion;
			}

			@Override
			public String getTypeLabel() {
				return "Open File";
			}

			@Override
			public String getHyperlinkText() {
				return "Open " + coords.filename;
			}

			@Override
			public void open() {
				try {
					var fileStore = EFS.getLocalFileSystem().getStore(targetFile.toURI());
					var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					var editorPart = IDE.openEditorOnFileStore(page, fileStore);
					if (editorPart instanceof ITextEditor textEditor) {
						var doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
						if (doc != null) {
							var maxLine = doc.getNumberOfLines();
							var zeroBased = Math.max(0, Math.min(targetLineNumber - 1, Math.max(0, maxLine - 1)));
							var targetOffset = doc.getLineOffset(zeroBased);
							textEditor.selectAndReveal(targetOffset, 0);
						}
					}
				} catch (Exception e) {
					// ignore
				}
			}
		};
	}
}
