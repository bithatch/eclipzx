package uk.co.bithatch.eclipzpp.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.linking.lazy.LazyLinkingResource;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.util.TextRegion;

import com.google.common.io.CharStreams;

import uk.co.bithatch.eclipzpp.GenericPreprocessor;
import uk.co.bithatch.eclipzpp.IMappedResource;
import uk.co.bithatch.eclipzpp.Mode;
import uk.co.bithatch.eclipzpp.SourceMap;
import uk.co.bithatch.eclipzpp.SourceMapRegistry;

public abstract class PPResource extends LazyLinkingResource implements IMappedResource {
	private final static ILog LOG = ILog.of(PPResource.class);

	public static final String PROJECT_KEY = "zxbasic.project";

	private SourceMap map = new SourceMap();

	/** Keep the ORIGINAL (unprocessed) text for incremental updates. */
	private static final class OriginalTextAdapter extends AdapterImpl {
		String text;

		OriginalTextAdapter(String t) {
			this.text = t;
		}

		@Override
		public boolean isAdapterForType(Object type) {
			return type == OriginalTextAdapter.class;
		}
	}

	private String getOriginal() {
		for (Adapter a : eAdapters())
			if (a instanceof OriginalTextAdapter ota)
				return ota.text;
		return "";
	}

	private void setOriginal(String s) {
		OriginalTextAdapter ota = null;
		for (Adapter a : eAdapters())
			if (a instanceof OriginalTextAdapter x) {
				ota = x;
				break;
			}
		if (ota == null) {
			ota = new OriginalTextAdapter(s);
			eAdapters().add(ota);
		} else
			ota.text = s;
	}

	@Override
	protected final void doLoad(InputStream inputStream, Map<?, ?> options) throws IOException {
		try {
			SourceMapRegistry.set(map);
			super.doLoad(inputStream, options);
		} finally {
			SourceMapRegistry.set(null);
		}
	}

	protected abstract Optional<IFile> getFile(Resource resource);

	@Override
	public SourceMap map() {
		return map;
	}

	@Override
	public final void update(int offset, int replacedTextLength, String newText) {
		if (!isLoaded())
			throw new IllegalStateException("You can't update an unloaded resource.");

		var file = getFile(this);
		if (file.isEmpty()) {
			super.update(offset, replacedTextLength, newText);
			return;
		}

		try {
			SourceMapRegistry.set(map);

			// preserve stock semantics
			isUpdating = true;

			// --- 1) Build updated ORIGINAL text (editor delta is in original coordinates)
			var oldParse = getParseResult();
			var entryPoint = getEntryPoint();

			var replace = new ReplaceRegion(new TextRegion(offset, replacedTextLength), newText);
			var originalBuf = new StringBuilder(getOriginal());
			replace.applyTo(originalBuf);

			var updatedOriginal = originalBuf.toString();
			setOriginal(updatedOriginal);

			// --- 2) Preprocess the UPDATED original
			var pr = preprocess(file.get(), updatedOriginal);

			// --- 3) Choose entry rule like the stock method does
			var oldEntry = NodeModelUtils.getEntryParserRule(oldParse.getRootNode());
			var entry = (entryPoint == null || entryPoint == oldEntry) ? oldEntry : entryPoint;

			// IMPORTANT: do a FULL parse of the preprocessed text (delta reparse is invalid
			// now)
			var newParse = getParser().parse(entry, new StringReader(pr));

			// --- 4) Install new state (use the TWO-arg overload like the stock method)
			updateInternalState(oldParse, newParse);

		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		} finally {
			SourceMapRegistry.set(null);
			isUpdating = false;
		}
	}

	@Override
	protected final Reader createReader(InputStream inputStream) throws IOException {
		var fileOr = getFile(this);
		if (fileOr.isPresent()) {

			var file = fileOr.get();
			var instr = CharStreams.toString(new InputStreamReader(inputStream));
			
			setOriginal(instr);

			var ppd = preprocess(file, instr);

			SourceMapRegistry.attach(this, map);
			return new StringReader(ppd);
		} else {
			return super.createReader(inputStream);
		}
	}

	protected final String preprocess(IFile file, String instr) throws IOException {
		map.clear();
		
//		return Files.readString(file.getLocation().toPath());
		
		var bldr = builder(file.getProject()).
				withSourceMap(map).
				onWarning((wrn, ln, msg) -> {
					LOG.warn("[PP] " + wrn+ " @ " + ln + " : " + msg);
				}).
				onError((err, ln, msg) -> {
					LOG.warn("[PP] " + err+ " @ " + ln + " : " + msg);
				});
		
		
		PPResourcePreprocessorDecorator.Instance.get().
			ifPresentOrElse(dec -> dec.decorate(bldr), () -> {
				bldr.withMode(Mode.EDITOR);
			});
		
		var pp = bldr.build();
		LOG.info("Preprocessing " +  file + "  [" + pp.mode() + "]");

		var ppd = pp.process(instr);

		System.out.println("------------------->");
		var i = 1;
		for(var ln : ppd.split(System.lineSeparator())) {
			System.out.println(String.format("%03d : %s",  i++, ln));
		}
		System.out.println("<-------------------");
		
		pp.defines().forEach((k,v) -> {
			System.out.println("DEFINES: "+ k + "=" + v);
		});
		
		map.hiddenLines().forEach((k,v) -> {
			System.out.println("HIDDEN: " + k + "=" + v);
		});
		
		map.segments().forEach(seg -> {
			System.out.println("Seg " + seg.getUri() + " : " + seg.getOriginalLine() + " (" + seg.getOriginalLines()
					+ ") -> " + seg.getPreprocessedLine() + " (" + seg.getPreprocessedLines() + ")");
		});
		return ppd;
	}

	public abstract GenericPreprocessor.Builder builder(IProject project);

}
