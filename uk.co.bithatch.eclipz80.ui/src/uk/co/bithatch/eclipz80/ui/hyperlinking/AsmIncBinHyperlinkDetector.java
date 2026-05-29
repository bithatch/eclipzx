package uk.co.bithatch.eclipz80.ui.hyperlinking;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

/**
 * Provides F3 (Open Declaration) hyperlinking for INCBIN / BINARY file path
 * strings. These are plain string attributes (not cross-references), so Xtext
 * won't provide navigation automatically.
 */
public class AsmIncBinHyperlinkDetector extends AbstractHyperlinkDetector {

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		IXtextDocument document = (IXtextDocument) textViewer.getDocument();
		if (document == null) return null;

		return document.readOnly(new IUnitOfWork<IHyperlink[], XtextResource>() {
			@Override
			public IHyperlink[] exec(XtextResource resource) throws Exception {
				if (resource == null) return null;
				IParseResult parseResult = resource.getParseResult();
				if (parseResult == null) return null;

				INode node = NodeModelUtils.findLeafNodeAtOffset(parseResult.getRootNode(), region.getOffset());
				if (node == null) return null;

				EObject semantic = NodeModelUtils.findActualSemanticObjectFor(node);
				if (!(semantic instanceof uk.co.bithatch.eclipz80.asm.IncBin)) return null;

				uk.co.bithatch.eclipz80.asm.IncBin incBin = (uk.co.bithatch.eclipz80.asm.IncBin) semantic;
				if (incBin.getFile() == null) return null;

				String filePath = incBin.getFile();
				if (filePath == null || filePath.isEmpty()) return null;

				// Resolve relative to the containing resource
				URI baseURI = resource.getURI();
				URI resolvedURI = URI.createURI(filePath).resolve(baseURI);

				// Convert to a local file path
				File file = null;
				if (resolvedURI.isFile()) {
					file = new File(resolvedURI.toFileString());
				} else if (resolvedURI.isPlatformResource()) {
					// Handle platform:/resource/ URIs
					String platformPath = resolvedURI.toPlatformString(true);
					if (platformPath != null) {
						org.eclipse.core.resources.IFile wsFile =
							org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
								.getRoot().getFile(new org.eclipse.core.runtime.Path(platformPath));
						if (wsFile.exists()) {
							file = wsFile.getLocation().toFile();
						}
					}
				}
				if (file == null || !file.exists()) return null;

				final File targetFile = file;
				IRegion linkRegion = new Region(node.getOffset(), node.getLength());

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
						return "Open " + filePath;
					}

					@Override
					public void open() {
						try {
							IFileStore fileStore = EFS.getLocalFileSystem().getStore(targetFile.toURI());
							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
							IDE.openEditorOnFileStore(page, fileStore);
						} catch (Exception e) {
							// ignore
						}
					}
				}};
			}
		});
	}
}
