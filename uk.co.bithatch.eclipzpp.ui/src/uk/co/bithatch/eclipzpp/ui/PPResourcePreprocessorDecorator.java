package uk.co.bithatch.eclipzpp.ui;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.eclipzpp.GenericPreprocessor;

public interface PPResourcePreprocessorDecorator {
	
	public static class Instance {
		private static ThreadLocal<PPResourcePreprocessorDecorator> instance = new ThreadLocal<>();
		
		public static Optional<PPResourcePreprocessorDecorator> get() {
			return Optional.ofNullable(instance.get());
		}

		public static void remove() {
			instance.remove();
		}
		
		public static void set(PPResourcePreprocessorDecorator instance) {
			Instance.instance.set(instance);
		}
	}

	String DECORATOR_EXTENSION_POINT_ID = "uk.co.bithatch.eclipzpp.ui.preprocessorDecorator";

	void decorate(GenericPreprocessor.Builder bldr, PPResource resource, IFile file);

	public static Stream<PPResourcePreprocessorDecorator> decorators() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(DECORATOR_EXTENSION_POINT_ID);
		if (point == null)
			return Stream.empty();
		else {
			return Arrays.asList(point.getConfigurationElements()).stream()
				.filter(c -> c.getName().equals("preprocessorDecorator"))
				.map(c -> {
					try {
						return (PPResourcePreprocessorDecorator)(c.createExecutableExtension("class"));
					} catch (CoreException e) {
						throw new IllegalStateException("Failed to create a machine extension.", e);
					}
				});
		}
	}

}
