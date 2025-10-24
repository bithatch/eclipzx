package uk.co.bithatch.eclipz88dk.toolchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.language.ProjectLanguageConfiguration;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public final class Z88DK {
	

//	public static void ensureSources(ICProjectDescription projDesc, IProject project, IProgressMonitor pm)
//			throws CoreException {
//		if (pm == null)
//			pm = new NullProgressMonitor();
//
//		// 1) Create src/ folder if missing
//		IFolder src = project.getFolder("src");
//		if (!src.exists()) {
//			src.create(IResource.FORCE | IResource.DERIVED /* or 0 */, true, pm);
//		}
//
//		// 2) Make sure there is at least one C file (optional, just for quick sanity)
//		IFile main = src.getFile("main.c");
//		if (!main.exists()) {
//			String stub = "int main(void){return 0;}\\n";
//			main.create(new java.io.ByteArrayInputStream(stub.getBytes(java.nio.charset.StandardCharsets.UTF_8)), true,
//					pm);
//		}
//
//		// 3) Add Source Entries for ALL configurations
//		for (ICConfigurationDescription cfg : projDesc.getConfigurations()) {
//			// Point source entry at /<project>/src (recommended)
//			ICSourceEntry[] entries = new ICSourceEntry[] {
//					new CSourceEntry(src.getFullPath(), null /* exclusions */, 0 /* flags */) };
//			cfg.setSourceEntries(entries);
//		}
//
//		// 4) Persist .cproject changes
//		CoreModel.getDefault().setProjectDescription(project, projDesc);
//
//		// 5) Refresh + reindex so the indexer picks up sources
//		project.refreshLocal(IResource.DEPTH_INFINITE, pm);
//		ICProject cproj = CoreModel.getDefault().create(project); // project -> ICProject
//		if (cproj != null) {
//		    CCorePlugin.getIndexManager().reindex(cproj);
//		}
//	}

}
