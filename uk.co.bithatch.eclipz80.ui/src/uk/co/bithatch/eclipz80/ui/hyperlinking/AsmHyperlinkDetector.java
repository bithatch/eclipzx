package uk.co.bithatch.eclipz80.ui.hyperlinking;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import uk.co.bithatch.bitzx.Strings;
import uk.co.bithatch.eclipzpp.ui.PPResource;

public class AsmHyperlinkDetector extends AbstractHyperlinkDetector {

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

		IXtextDocument document = (IXtextDocument) textViewer.getDocument();
		if (document == null)
			return null;

		return document.readOnly(new IUnitOfWork<IHyperlink[], XtextResource>() {
			@Override
			public IHyperlink[] exec(XtextResource resource) throws Exception {
				if (resource == null)
					return null;
				var ppresource = (PPResource) resource;
				var hidden = ppresource.map().closestHiddenLine(region.getOffset());
				if (hidden == null)
					return null;
				var line = hidden.trim();

				if (line.toLowerCase().startsWith("c_line")) {
					var idx = line.indexOf(' ');
					if (idx == -1) {
						idx = line.indexOf('\t');
					}
					if (idx == -1) {
						return null;
					}
					var cline = line.substring(idx + 1).trim();
					try {
						idx = cline.indexOf(',');
						if (idx == -1) {
							idx = cline.indexOf(' ');
						}
						if (idx != -1) {
							var lineNumber = Integer.parseInt(cline.substring(0, idx).trim());
							var filename = Strings.stripQuoted(cline.substring(idx + 1).trim()).split("::")[0].trim();

							return createHyperlink(region, resource, lineNumber, filename);
						}
					} catch (Exception nfe) {
					}
				}

				// Give up
				return null;
			}

		});
	}

	private IHyperlink[] createHyperlink(IRegion region, XtextResource resource, int lineNumber, String filename) {
		var baseURI = resource.getURI();
		var resolvedURI = URI.createURI(filename).resolve(baseURI);
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
		if (file == null || !file.exists())
			return null;

		var targetFile = file;
		var targetLineNumber = lineNumber;
		var linkRegion = new Region(region.getOffset(), Math.max(1, region.getLength()));

		return new IHyperlink[] { new IHyperlink() {
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
				return "Open " + filename;
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
		} };
	}
}
