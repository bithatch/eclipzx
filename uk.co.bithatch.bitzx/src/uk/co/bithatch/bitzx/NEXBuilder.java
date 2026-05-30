package uk.co.bithatch.bitzx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;


public final class NEXBuilder {

	public static final int[] DEFAULT_CORE = new int[] { 3,0,0 };
	public static final String DEFAULT_CORE_STR = String.join(".", IntStream.of(DEFAULT_CORE).mapToObj(String::valueOf).toList());
	
	public static int parseInt(String str) {
		str = str.trim();
		if(str.startsWith("$")) {
			return  Integer.parseInt(str.substring(1), 16);
		}
		else if(str.toLowerCase().endsWith("h")) {
			return Integer.parseInt(str.substring(0, str.length() - 1), 16);
		}
		else {
			return Integer.decode(str);
		}
	}
	
	private NEXBuilder() {
	}

	public static class NexHeader {
		public byte[] next = "Next".getBytes();
		public byte[] versionNumber = "V1.1".getBytes();
		public int ramRequired = 0;
		public int numBanksToLoad = 0;
		public int loadingScreen = 0;
		public int borderColor = 0;
		public int SP = 0;
		public int PC = 0;
		public int numExtraFiles = 0;
		public byte[] banks = new byte[112];
		public int loadingBar = 0;
		public int loadingColor = 0;
		public int loadingBankDelay = 0;
		public int loadedDelay = 0;
		public int dontResetRegs = 0;
		public byte[] coreRequired = new byte[3];
		public int hiResColors = 0;
		public int entryBank = 0;

		public byte[] asBinary() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				out.write(pad(next, 4));
				out.write(pad(versionNumber, 4));
				out.write((byte) ramRequired);
				out.write((byte) numBanksToLoad);
				out.write((byte) loadingScreen);
				out.write((byte) borderColor);
				out.write(toShort(SP));
				out.write(toShort(PC));
				out.write(toShort(numExtraFiles));
				out.write(banks);
				out.write((byte) loadingBar);
				out.write((byte) loadingColor);
				out.write((byte) loadingBankDelay);
				out.write((byte) loadedDelay);
				out.write((byte) dontResetRegs);
				out.write(pad(coreRequired, 3));
				out.write((byte) hiResColors);
				out.write((byte) entryBank);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return pad(out.toByteArray(), 512);
		}

		private byte[] pad(byte[] data, int length) {
			return Arrays.copyOf(data, length);
		}

		private byte[] toShort(int value) {
			return new byte[] { (byte) (value & 0xFF), (byte) ((value >> 8) & 0xFF) };
		}
	}

	public static class LoadingScreen {
		public static final int LAYER2 = 1;
		public static final int ULA_LOADING = 2;
		public static final int LO_RES = 4;
		public static final int HI_RES = 8;
		public static final int HI_COLOUR = 16;
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java NEXBuilder <FILEIN> <FILEOUT> [--ram-required MB]");
			return;
		}

		String inputFile = args[0];
		String outputFile = args[1];
		Integer ramRequired = null;

		if (args.length > 3 && args[2].equals("--ram-required")) {
			try {
				ramRequired = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				System.out.println("Invalid RAM value: " + args[3]);
				return;
			}
		}

		var configuration = new DefaultNEXConfiguration();

		var parser = new CfgFileParser(configuration);
		parser.parse(Paths.get(inputFile));

		var generator = new NexGenerator(configuration);
		if (ramRequired != null)
			generator.ramRequired(ramRequired);
		generator.generate(Paths.get(outputFile));
	}

	public static class NexGenerator {

		
		private DefaultNEXConfiguration configuration;
		private int ramRequired;
		private Consumer<String> reporting = s -> {};

		public NexGenerator(DefaultNEXConfiguration configuration) {
			this.configuration = configuration;
		}

		public Consumer<String> reporting() {
			return reporting;
		}

		public NexGenerator reporting(Consumer<String> reporting) {
			this.reporting = reporting;
			return this;
		}

		public INEXConfiguration configuration() {
			return configuration;
		}

		public int ramRequired() {
			return ramRequired;
		}

		public NexGenerator ramRequired(int ramRequired) {
			this.ramRequired = Math.max(ramRequired - 1, 0);
			return this;
		}

		public void generate(Path file) {

		    if(configuration.lastBank <= -1 && !configuration.hasContent()) {
		        return;
		    }
		        		
			try (var fos = Files.newOutputStream(file)) {
				var header = configuration.header512;
				header.ramRequired = ramRequired;

				header.numBanksToLoad = 0;
				for (byte b : header.banks) {
					if (b != 0)
						header.numBanksToLoad++;
				}

				reporting.accept(String.format("Generating NEX file in version %s", new String(header.versionNumber)));
				fos.write(header.asBinary());

				if ((header.loadingScreen & LoadingScreen.LAYER2) != 0) {
					fos.write(configuration.palette);
					fos.write(configuration.loading);
					reporting.accept("Adding Layer2 loading screen");
				}

				if ((header.loadingScreen & LoadingScreen.ULA_LOADING) != 0) {
					fos.write(configuration.loadingULA);
					reporting.accept("Adding ULA loading screen");
				}

				if ((header.loadingScreen & LoadingScreen.LO_RES) != 0) {
					fos.write(configuration.paletteLoRes);
					fos.write(configuration.loadingLoRes);
					reporting.accept("Adding Lo-Res loading screen");
				}

				if ((header.loadingScreen & LoadingScreen.HI_RES) != 0) {
					fos.write(configuration.loadingHiRes);
					reporting.accept("Adding Hi-Res loading screen");
				}

				if ((header.loadingScreen & LoadingScreen.HI_COLOUR) != 0) {
					fos.write(configuration.loadingHiCol);
					reporting.accept("Adding Hi-Colourloading screen");
				}

				for (var i = 0; i < 112; i++) {
					if (header.banks[getBankOrder(i)] != 0) {
						reporting.accept(String.format("Adding Bank %d", i));
						fos.write(configuration.bigFile, i * 16384, 16384);
					}
				}

				if(configuration.extraFiles.size() > 0) {
					var lookupTable = new ByteArrayOutputStream();
					var fileId = 0;
					var offset = 0;
	
					for (var extra : configuration.extraFiles) {
						reporting.accept(String.format("Adding extra file %s", extra));
//						var compressed = new ByteArrayOutputStream();
//						// TODO change to zx0
//						try (var gzip = new GZIPOutputStream(compressed)) {
//							gzip.write(extra);
//						}
//						var compressedData = compressed.toByteArray();
//						var length = compressedData.length;
						var length = extra.length;
	
						// Write ID
						fos.write(fileId++);
	
						// Write length (4 bytes LE)
						fos.write(length & 0xFF);
						fos.write((length >> 8) & 0xFF);
						fos.write((length >> 16) & 0xFF);
						fos.write((length >> 24) & 0xFF);
	
						// Write compressed data
//						fos.write(compressedData);
						fos.write(extra);
	
						// Append to lookup table (ID + offset)
						lookupTable.write(fileId - 1);
						lookupTable.write(offset & 0xFF);
						lookupTable.write((offset >> 8) & 0xFF);
						lookupTable.write((offset >> 16) & 0xFF);
						lookupTable.write((offset >> 24) & 0xFF);
	
						offset += 5 + length; // ID + len + data
					}

					reporting.accept("Adding lookup table.");
					fos.write(lookupTable.toByteArray());
					fos.write(0xFF); // End of table marker
				}

				reporting.accept(String.format("NEX file written to %s", file));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public static class CfgFileParser {

		private DefaultNEXConfiguration configuration;
		private Consumer<String> reporting = s -> System.out.println(s);

		public CfgFileParser(DefaultNEXConfiguration configuration) {
			this.configuration = configuration;
		}

		public Consumer<String> reporting() {
			return reporting;
		}

		public CfgFileParser reporting(Consumer<String> reporting) {
			this.reporting = reporting;
			return this;
		}

		public void parse(Path filePath) {
			try (var reader = Files.newBufferedReader(filePath)) {
				String line;
				int lineNum = 0;

				while ((line = reader.readLine()) != null) {
					lineNum++;
					line = line.trim();
					if (line.isEmpty() || line.startsWith(";"))
						continue;

					if (line.startsWith("!COR")) {
						var parts = line.substring(4).split(",");
						var v = new int[Math.min(parts.length, 3)];
						for (int i = 0; i < Math.min(parts.length, 3); i++) {
							v[i] = (byte) Integer.parseInt(parts[i].trim());
						}
						configuration.core(v);

						reporting.accept(String.format("Requires Core %d.%d.%d or greater", configuration.header512.coreRequired[0], configuration.header512.coreRequired[1],
								configuration.header512.coreRequired[2]));
						
					} else if (line.startsWith("!SCR")) {
						var scrFile = filePath.getParent().resolve(line.substring(4).trim());
						configuration.scr(scrFile);
						reporting.accept(String.format("Loaded SCR screen: %s", scrFile));
					} else if (line.startsWith("!BMP")) {
						/* TODO parameters ... see nextcreator.py */
						var spec = line.substring(4).trim();
						var dontSavePalette = false;
						var use8BitPalette = false;
						if(line.startsWith(",")) {
							// ?
						}
						else { 
							if(spec.startsWith("!")) {
								dontSavePalette = true;
								spec = spec.substring(1);
							}
							if(spec.startsWith("8")) {
								use8BitPalette = true;
								spec = spec.substring(1);
								if(spec.startsWith(","))
									spec = spec.substring(1);
							}
							var parts = new ArrayList<>(Arrays.asList(spec.split(",")));
							var fname = filePath.getParent().resolve(parts.remove(0));
							configuration.bmp(fname, dontSavePalette, use8BitPalette);
							
							// NOTE to remain compatible with NB, only BMP supports loading screen parameters
							configuration.loading(nextArg(parts), nextArg(parts), nextArg(parts), nextArg(parts), nextArg(parts));
							reporting.accept(String.format("Loaded BMP screen: %s", fname));
						}
					} else if (line.startsWith("!SLR")) {
						var slrFile = filePath.getParent().resolve(line.substring(4).trim());
						configuration.slr(slrFile);
						reporting.accept(String.format("Loaded SLR screen: %s", slrFile));
					} else if (line.startsWith("!SHR")) {
						var shrFile = filePath.getParent().resolve(line.substring(4).trim());
						configuration.shr(shrFile);
						reporting.accept(String.format("Loaded SHR screen: %s", shrFile));
					} else if (line.startsWith("!SHC")) {
						var shcFile = filePath.getParent().resolve(line.substring(4).trim());
						configuration.shc(shcFile);
						reporting.accept(String.format("Loaded SHC screen: %s", shcFile));
					} else if (line.startsWith("!MMU")) {
						var parts = line.split(",");
						var filename = parts[0].trim();
						
						Optional<Integer> bank = parts.length > 1 ? Optional.of(Integer.parseInt(parts[1].trim())) : Optional.empty();
						Optional<Integer> address = parts.length > 2 ? Optional.of(parseInt(parts[2].trim())) : Optional.empty();
						
						var addFile = filePath.getParent().resolve(filename);
						
						configuration.mmu(addFile, bank, address);
						
						reporting.accept(String.format("Added file '%s' to bank %d at address 0x%04X (%d bytes)", filename, bank,
								address, Files.size(addFile)));

					} else if (line.startsWith("!PCSP")) {

						var parts = line.substring(5).trim().split(",");
						if (parts.length < 1)
							return;

						var pc = parseInt(parts[0].trim());

						reporting.accept(String.format("Set PC to $%04X", configuration.header512.PC));

						Optional<Integer> sp;
						if (parts.length > 1) {
							sp = Optional.of(parseInt(parts[1].trim()));
							reporting.accept(String.format("Set SP to $%04X", configuration.header512.SP));
						} else {
							sp = Optional.empty();
						}

						Optional<Integer> bank;
						if (parts.length > 2) {
							bank = Optional.of(Integer.parseInt(parts[2].trim()));
							reporting.accept(String.format("Set entry bank to %d", configuration.header512.entryBank));
						} else {
							bank = Optional.empty();
						}

						configuration.pcsp(pc, sp, bank);

					} else if (line.startsWith("!BANK")) {
						var bank = Integer.parseInt(line.substring(5).trim());
						configuration.entryBank(bank);
						reporting.accept(String.format("Set entry bank to %d", bank));
					} else if (line.startsWith("!EXTRA")) {
						var extraFile = filePath.getParent().resolve(line.substring(7).trim());
						configuration.extraFile(extraFile);
						reporting.accept(String.format("Added extra file '%s' (%d bytes)", extraFile, Files.size(extraFile)));
					} else if(!line.startsWith("!")) {
						var parts = line.split(",");
						var filename = parts[0].trim();
						
						Optional<Integer> bank = parts.length > 1 ? Optional.of(Integer.parseInt(parts[1].trim())) : Optional.empty();
						Optional<Integer> address = parts.length > 2 ? Optional.of(parseInt(parts[2].trim())) : Optional.empty();
						
						var addFile = filePath.getParent().resolve(filename);
						
						configuration.addFile(addFile, bank, address);
						
						reporting.accept(String.format("Added file '%s' to bank %d at address 0x%04X (%d bytes)", filename, bank,
								address, Files.size(addFile)));
						
					} else {
						reporting.accept(String.format("Unrecognized or unsupported line @ %d: %s",lineNum, line));
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

	}
	
	private static int nextArg(List<String> remain) {
		if(remain.isEmpty())
			return 0;
		else
			return Integer.decode(remain.removeFirst());
	}

	public static int getPaletteValue(int r, int g, int b) {
		return ((convert8BitTo5Bit(r) >> 2) & 1) << 8 | (convert8BitTo5Bit(r) >> 1) | (convert8BitTo5Bit(g) << 2)
				| (convert8BitTo5Bit(b) << 5);
	}

	private static int convert8BitTo5Bit(int v) {
		return Math.min(255, v) >> 5;
	}
	
	public static boolean isEmptyBytes(byte... bytes) {
		for(var b : bytes) {
			if(b != 0)
				return false;
		}
		return true;
	}
	
	public static int makeNum(byte[]  nums, int offset, int len) {
	    var result = 0;
	    var acc = 1;

	    for(var n : nums) {
	        result += n * acc;
	        acc <<= 8;
	    }

	    return result;
	}

	private final static int[] NEXT_BANKS = {1, 3, 0, 4, 6, 2, 7, 8};
	
	public static int getNextBank(int bank) {
	    if(bank > 7)
	        return bank + 1;
		return NEXT_BANKS[bank];
	}

	private final static int[] REAL_BANKS = {2, 3, 1, 4, 5, 0, 6, 7};

	public static int getRealBank(int bank) {
	    if(bank > 7)
	        return bank;
		return REAL_BANKS[bank];
	}

	private final static int[] BANK_ORDER = {5, 2, 0, 1, 3, 4, 6, 7};

	private static int getBankOrder(int bank) {
	    if(bank > 7)
	        return bank;
		return BANK_ORDER[bank];
	}
}
