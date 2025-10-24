package uk.co.bithatch.bitzx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

public class LanguageSystem {

	public static final String EXTENSION_POINT_ID = "uk.co.bithatch.bitzx.languageSystemProvider";

	public static Optional<IArchitecture> architecture(WellKnownArchitecture architecture) {
		for (var arch : architectures()) {
			if (architecture.equals(arch.wellKnown().orElse(null))) {
				return Optional.of(arch);
			}
		}
		return Optional.empty();
	}

	public static Optional<IOutputFormat> architecture(WellKnownOutputFormat wellKnown) {
		for (var fmt : outputFormats()) {
			if (wellKnown.equals(fmt.wellKnown().orElse(null))) {
				return Optional.of(fmt);
			}
		}
		return Optional.empty();
	}

	public static IArchitecture architectureOrDefault(IProject project, String archName) {
		var hndlrs = new LinkedHashSet<ILanguageSystemProvider>();
		for (var d : descriptors()) {
			var dsc = createHandler(d);
			for (var a : dsc.architectures(project)) {
				if (a.name().equals(archName))
					return a;
			}
			hndlrs.add(dsc);
		}

		for (var hndlr : hndlrs) {
			var archs = hndlr.architectures(project);
			if (!archs.isEmpty()) {
				return archs.get(0);
			}
		}
		throw new IllegalStateException("Could not get any architectures for this project!");
	}

	public static List<? extends IArchitecture> architectures() {
		return architecturesFor(null);
	}

	public static List<? extends IArchitecture> architecturesFor(IResource resource) {
		return streamHandlers().flatMap(hndlr -> hndlr.architectures(resource).stream()).toList();
	}

	public static List<LanguageSystemProviderDescriptor> descriptorsFor(IResource resource) {
		return descriptors().stream().filter(d -> resource == null || createHandler(d).isCompatible(resource)).toList();
	}
	
	public static boolean isCompatible(IResource resource) {
		try {
			languageSystem(resource);
			return true;
		}
		catch(IllegalArgumentException iae) {
			return false;
		}
	}

	public static ILanguageSystemProvider languageSystem(IResource resource) {
		for(var desc : descriptors()) {
			var hndlr = createHandler(desc);
			if(hndlr.isCompatible(resource)) {
				return hndlr;
			}
		}
		throw new IllegalArgumentException("This resource has no compatible language system provider installed (e.g. ZX Basic or Z88DK");
	}

	@SuppressWarnings("unchecked")
	public static <T extends ILanguageSystemProvider> T languageSystem(Class<T> sys) {
		for(var desc : descriptors()) {
			var hndlr = createHandler(desc);
			if(hndlr.getClass().equals(sys)) {
				return (T)hndlr;
			}
		}
		throw new IllegalArgumentException("No language system provider of this class.");
	}

	public static IOutputFormat outputFormatOrDefault(IProject project, String fmtName) {
		var hndlrs = new LinkedHashSet<ILanguageSystemProvider>();
		for (var d : descriptors()) {
			var dsc = createHandler(d);
			for (var a : dsc.outputFormats(project)) {
				if (a.name().equals(fmtName))
					return a;
			}
			hndlrs.add(dsc);
		}

		for (var hndlr : hndlrs) {
			var archs = hndlr.outputFormats(project);
			if (!archs.isEmpty()) {
				return archs.get(0);
			}
		}
		throw new IllegalStateException("Could not get any output format for this project!");

	}

	public static List<IOutputFormat> outputFormats() {
		return outputFormatsFor(null);
	}

	public static List<IOutputFormat> outputFormatsFor(IResource resource) {
		return streamHandlers().flatMap(hndlr -> hndlr.outputFormats(resource).stream()).distinct().sorted(IOutputFormat.comparator()).toList();
	}

	protected static Stream<ILanguageSystemProvider> streamHandlers() {
		return descriptors().stream().map(LanguageSystem::createHandler);
	}

	private static ILanguageSystemProvider createHandler(LanguageSystemProviderDescriptor desc) {
		try {
			return desc.createHandler();
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<LanguageSystemProviderDescriptor> descriptors() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else
			return Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("languageSystemProvider") && c.getAttribute("id") != null)
					.map(c -> new LanguageSystemProviderDescriptor(c, point)).toList();
	}

	public static Optional<ILanguageSystemProvider> languageSystemByName(String name) {
		return  descriptors().stream().filter(desc -> desc.name().equals(name)).map(LanguageSystem::createHandler).findFirst();
	}

}
