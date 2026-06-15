package uk.co.bithatch.eclipzpp.ui;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipzpp.FileSystemResourceResolver;
import uk.co.bithatch.eclipzpp.GenericPreprocessor;
import uk.co.bithatch.eclipzpp.Mode;
import uk.co.bithatch.eclipzpp.SourceMap;

public final class PPPreprocessingSupport {

	private PPPreprocessingSupport() {
	}

	public static String preprocess(GenericPreprocessor.Builder bldr, PPResource resource, IFile file, SourceMap map,
			ILog log, String input) throws IOException {
		var configured = configure(bldr, resource, file, map, log);
		var pp = configured.build();
		log.info("Preprocessing " + file + "  [" + pp.mode() + "/" + pp.format() + "]");

		pp.resourceResolver().ifPresent(rr -> {
			((FileSystemResourceResolver) rr).includePaths().forEach(ip -> {
				log.info("  " + ip);
			});

			((FileSystemResourceResolver) rr).runtimedir().ifPresent(ip -> {
				log.info("  RT: " + ip);
			});
		});
		return pp.process(input);
	}

	public static GenericPreprocessor.Builder configure(GenericPreprocessor.Builder bldr, PPResource resource, IFile file,
			SourceMap map, ILog log) {
		/* Start in editor mode; decorators can still override this later. */
		bldr.withMode(Mode.EDITOR).withSourceMap(map).onWarning((wrn, ln, msg) -> {
			log.warn("[PP] " + wrn + " @ " + ln + " : " + msg);
		}).onError((err, ln, msg) -> {
			log.warn("[PP] " + err + " @ " + ln + " : " + msg);
		});

		PPResourcePreprocessorDecorator.Instance.get().ifPresent(dec -> {
			log.info("Build is adding custom preprocessor configuration");
			dec.decorate(bldr, resource, file);
		});

		PPResourcePreprocessorDecorator.decorators().forEach(dec -> {
			log.info("Extension " + dec.getClass().getName() + " is contributing to preprocesor configuration");
			dec.decorate(bldr, resource, file);
		});
		return bldr;
	}
}
