package uk.co.bithatch.drawzx.wizards;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import uk.co.bithatch.drawzx.palettes.AdobeColorTablePalette;
import uk.co.bithatch.drawzx.palettes.GIMPPalette;
import uk.co.bithatch.drawzx.palettes.JASCPalette;
import uk.co.bithatch.drawzx.palettes.PaintNETPalette;
import uk.co.bithatch.zyxy.graphics.Palette;

public class PaletteImportWizard extends Wizard implements IWorkbenchWizard {

    private PaletteImportWizardPage page;

    public PaletteImportWizard() {
        setWindowTitle("Import Palette File");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // Optional: pre-populate selection
    }

    @Override
    public void addPages() {
        page = new PaletteImportWizardPage("Import Palette File");
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        IFile destination = page.getTargetPath();
        IPath source = page.getSourcePath();

        try {
        	Palette pal = loadPalette(source.toPath());
        	
        	var path = destination.getLocation().toPath();
			try(var out = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
        		pal.save(out);	
        	}
			
			destination.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			
	        var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IDE.openEditor(page, destination);
            return true;
        } catch (Exception e) {
            page.setErrorMessage("Import failed: " + e.getMessage());
            return false;
        }
    }

	public static Palette loadPalette(Path source) throws IOException {
		Palette pal;
		if(source.toString().toLowerCase().endsWith(".act")) {
			pal = new AdobeColorTablePalette(source);
		}
		else if(source.toString().toLowerCase().endsWith(".pal")) {
			pal = new JASCPalette(source);
		}
		else if(source.toString().toLowerCase().endsWith(".gpl")) {
			pal = new GIMPPalette(source);
		}
		else if(source.toString().toLowerCase().endsWith(".txt")) {
			pal = new PaintNETPalette(source);
		}
		else {
			/* TODO magic? */
			throw new IOException("Could not determine palette type from file extension.");
		}
		return pal;
	}
}
