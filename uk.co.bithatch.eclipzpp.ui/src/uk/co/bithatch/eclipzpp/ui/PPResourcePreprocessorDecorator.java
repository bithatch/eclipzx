package uk.co.bithatch.eclipzpp.ui;

import java.util.Optional;

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

	void decorate(GenericPreprocessor.Builder bldr);
}
