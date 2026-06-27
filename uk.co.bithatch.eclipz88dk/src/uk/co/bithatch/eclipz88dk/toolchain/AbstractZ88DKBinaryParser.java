package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.IAddressFactory;
import org.eclipse.cdt.core.IBinaryParser;
import org.eclipse.cdt.core.settings.model.ICConfigExtensionReference;
import org.eclipse.cdt.utils.Addr32Factory;
import org.eclipse.cdt.utils.BinaryFile;
import org.eclipse.cdt.utils.BinaryObjectAdapter;
import org.eclipse.cdt.utils.Symbol;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

public abstract class AbstractZ88DKBinaryParser extends AbstractCExtension implements IBinaryParser {

	private static final ILog LOG = ILog.of(AbstractZ88DKBinaryParser.class);
	private static final int PROCESS_TIMEOUT_SECONDS = 8;
	private static final String EXT_OPT_NM = "nm";
	private static final String EXT_OPT_NM_ARGS = "nmArgs";
	private static final String DEFAULT_NM_ARGS = "-a";

	private static final Pattern Z88DK_NM_PREFIX;
	
	static {
		try {
			Z88DK_NM_PREFIX = Pattern.compile("^\\s+([A-Z])\\s+([A-Z])\\s+(\\$[0-9A-Fa-f]+):\\s+(.*)\\s+\\(.*\\)\\s+\\(.*\\)$");
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static final IAddressFactory ADDRESS_FACTORY = new Addr32Factory();

	protected abstract Set<String> supportedExtensions();

	@Override
	public IBinaryFile getBinary(byte[] hints, IPath path) throws IOException {
		return getBinary(path);
	}

	@Override
	public IBinaryFile getBinary(IPath path) throws IOException {
		if (path == null) {
			throw new IOException("No input path provided.");
		}
		var ext = extension(path);
		if (!supportedExtensions().contains(ext)) {
			throw new IOException("Unsupported file extension for Z88DK parser: " + path.toOSString());
		}
		if ("lib".equals(ext)) {
			return new Z88DKBinaryArchive(this, path);
		}
		return new Z88DKBinaryObject(this, path, binaryType(ext));
	}

	@Override
	public String getFormat() {
		return "Z88DK (z88dk-z80nm)";
	}

	@Override
	public boolean isBinary(byte[] hints, IPath path) {
		return path != null && supportedExtensions().contains(extension(path));
	}

	@Override
	public int getHintBufferSize() {
		return 8;
	}

	protected int binaryType(String ext) {
		return switch (ext) {
		case "o" -> IBinaryFile.OBJECT;
		case "lib" -> IBinaryFile.ARCHIVE;
		default -> IBinaryFile.EXECUTABLE;
		};
	}

	private static String extension(IPath path) {
		var ext = path.getFileExtension();
		return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
	}

	private Optional<Z88DKSDK> sdk() {
		var pax = Z88DKPreferencesAccess.get();
		var project = getProject();
		var sdk = pax.getSDK(project);
		if (sdk.isPresent()) {
			return sdk;
		}
		return pax.getDefaultSDK();
	}

	private List<String> z80nmCommandLine(IPath path) {
		var opts = extensionData(EXT_OPT_NM_ARGS).orElse(DEFAULT_NM_ARGS);
		if (opts.isBlank()) {
			opts = DEFAULT_NM_ARGS;
		}

		var cmd = new ArrayList<String>();
		var configuredNm = extensionData(EXT_OPT_NM).orElse("").trim();
		if (!configuredNm.isBlank()) {
			cmd.add(configuredNm);
		} else {
			var z80nm = sdk().map(Z88DKSDK::z80nm).orElse(null);
			if (z80nm != null && z80nm.isFile()) {
				cmd.add(z80nm.getAbsolutePath());
			} else {
				cmd.add("z88dk-z80nm");
			}
		}
		cmd.addAll(Arrays.stream(opts.trim().split("\\s+")).filter(s -> !s.isBlank()).toList());
		cmd.add(path.toOSString());
		return cmd;
	}

	private Optional<String> extensionData(String key) {
		ICConfigExtensionReference cfgRef = getConfigExtensionReference();
		if (cfgRef != null) {
			var value = cfgRef.getExtensionData(key);
			if (value != null) {
				return Optional.of(value);
			}
		}
		var extRef = getExtensionReference();
		if (extRef != null) {
			var value = extRef.getExtensionData(key);
			if (value != null) {
				return Optional.of(value);
			}
		}
		return Optional.empty();
	}

	private List<String> runZ80nm(IPath path) {
		var cmd = z80nmCommandLine(path);
		try {
			var pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			var process = pb.start();
			var lines = new ArrayList<String>();
			try (var in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = in.readLine()) != null) {
					lines.add(line);
				}
			}
			var done = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!done) {
				process.destroyForcibly();
				LOG.warn("z80nm timed out for " + path.toOSString());
				return Collections.emptyList();
			}
			if (process.exitValue() != 0) {
				LOG.warn("z80nm returned exit code " + process.exitValue() + " for " + path.toOSString());
			}
			return lines;
		} catch (Exception e) {
			LOG.warn("Could not execute z80nm for " + path.toOSString(), e);
			return Collections.emptyList();
		}
	}

	private static int symbolTypeFromNm(char t) {
		return switch (Character.toLowerCase(t)) {
		case 't' -> ISymbol.FUNCTION;
		default -> ISymbol.VARIABLE;
		};
	}

	private static Long parseHex(String val) {
		try {
			if (val != null && val.startsWith("$")) {
				val = val.substring(1);
			}
			return Long.parseUnsignedLong(val, 16);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	private static void addParsedSymbol(BinaryObjectAdapter owner, Set<ISymbol> symbols, String name, long addr, int type) {
		if (name == null || name.isBlank()) {
			return;
		}
		var address = ADDRESS_FACTORY.createAddress(Long.toHexString(addr), 16);
		symbols.add(new Symbol(owner, name, type, address, 0));
	}

	private static void parseNmLine(BinaryObjectAdapter owner, Set<ISymbol> symbols, String line) {
		var nm = Z88DK_NM_PREFIX.matcher(line);
		if (nm.matches()) {
			var address = parseHex(nm.group(3));
			if (address != null) {
				addParsedSymbol(owner, symbols, nm.group(4), address, symbolTypeFromNm(nm.group(2).charAt(0)));
			}
			return;
		}

	}

	private ISymbol[] parseSymbols(BinaryObjectAdapter owner, IPath path) {
		var syms = new LinkedHashSet<ISymbol>();

		/* Primary source: z80nm output. */
		for (var line : runZ80nm(path)) {
			parseNmLine(owner, syms, line);
		}

		return syms.stream()
				.sorted(Comparator.comparing(ISymbol::getAddress).thenComparing(ISymbol::getName))
				.toArray(ISymbol[]::new);
	}

	private static final class Z88DKBinaryArchive extends BinaryFile implements IBinaryArchive {

		private final IBinaryObject[] objects;

		private Z88DKBinaryArchive(AbstractZ88DKBinaryParser parser, IPath path) {
			super(parser, path, IBinaryFile.ARCHIVE);
			this.objects = new IBinaryObject[] { new Z88DKBinaryObject(parser, path, IBinaryFile.OBJECT) };
		}

		@Override
		public IBinaryObject[] getObjects() {
			return objects;
		}
	}

	private static final class Z88DKBinaryObject extends BinaryObjectAdapter {

		private final IAddressFactory addressFactory = ADDRESS_FACTORY;
		private final BinaryObjectInfo info;
		private final ISymbol[] symbols;

		private Z88DKBinaryObject(AbstractZ88DKBinaryParser parser, IPath path, int type) {
			super(parser, path, type);
			this.info = new BinaryObjectInfo();
			this.info.cpu = "z80";
			this.info.isLittleEndian = true;
			this.info.hasDebug = false;
			this.info.text = 0;
			this.info.data = 0;
			this.info.bss = 0;
			this.symbols = parser.parseSymbols(this, path);
		}

		@Override
		public ISymbol[] getSymbols() {
			return symbols;
		}

		@Override
		public IAddressFactory getAddressFactory() {
			return addressFactory;
		}

		@Override
		protected BinaryObjectInfo getBinaryObjectInfo() {
			return info;
		}
	}
}
