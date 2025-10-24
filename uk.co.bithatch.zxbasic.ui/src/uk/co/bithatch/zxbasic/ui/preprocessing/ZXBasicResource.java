package uk.co.bithatch.zxbasic.ui.preprocessing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.xtext.linking.lazy.LazyLinkingResource;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.util.TextRegion;

import com.google.common.io.CharStreams;

import uk.co.bithatch.zxbasic.preprocessor.SourceMap;
import uk.co.bithatch.zxbasic.preprocessor.ZXPreprocessor;
import uk.co.bithatch.zxbasic.preprocessor.ZXPreprocessor.Mode;
import uk.co.bithatch.zxbasic.scoping.SourceMapRegistry;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXBasicResource extends LazyLinkingResource {

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
	protected void doLoad(InputStream inputStream, Map<?, ?> options) throws IOException {
		try {
			SourceMapRegistry.set(map);
			super.doLoad(inputStream, options);
		}
		finally {
			SourceMapRegistry.set(null);
		}
	}

	@Override
	public void update(int offset, int replacedTextLength, String newText) {
		if (!isLoaded())
			throw new IllegalStateException("You can't update an unloaded resource.");

		var file = ZXBasicResourceUtil.getFile(this);
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

		} catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		} finally {
			SourceMapRegistry.set(null);
			isUpdating = false;
		}
	}

	@Override
	protected Reader createReader(InputStream inputStream) throws IOException {
		var fileOr = ZXBasicResourceUtil.getFile(this);
		if (fileOr.isPresent()) {

			var file = fileOr.get();

			System.out.println("createReader - " + file);

			var instr = CharStreams.toString(new InputStreamReader(inputStream));
			setOriginal(instr);

			var ppd = preprocess(file, instr);

			SourceMapRegistry.attach(this, map);
			return new StringReader(ppd);
		} else {
			return super.createReader(inputStream);
		}
	}

	protected String preprocess(IFile file, String instr) throws IOException {
		map.clear();
		System.out.println("Preprocess:");
		var pp = builderForProject(file.getProject()).withSourceMap(map).withMode(Mode.EDITOR).onError((err, msg) -> {
			/* TODO move to log or somehow get to Problems? */
			System.out.println("[PP] " + err + " : " + msg);
		}).build();

		var ppd = pp.process(instr);

		System.out.println("------------------->");
		System.out.println(ppd);
		System.out.println("<-------------------");
		return ppd;
	}

	public static ZXPreprocessor.Builder builderForProject(IProject project) {
		var pax = ZXBasicPreferencesAccess.get();
		var fs = new ZXPreprocessor.FileSystemResourceResolver.Builder().withIncludeDirs(pax.getAllLibs(project))
				.withWorkingDir(project.getLocation().toFile())
				.withRuntimeDir(pax.getSDK(project).runtime(pax.getArchitecture(project))).build();

		return new ZXPreprocessor.Builder().withResourceResolver(fs).withDefines(pax.getDefines(project));
	}
}
