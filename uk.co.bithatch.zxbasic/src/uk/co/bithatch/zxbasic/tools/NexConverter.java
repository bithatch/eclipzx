package uk.co.bithatch.zxbasic.tools;

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

public final class NexConverter {

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
	
	private NexConverter() {
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

	public static class NexConfiguration {
		public List<byte[]> extraFiles = new ArrayList<>();
		public NexHeader header512 = new NexHeader();
		public byte[] loadingULA = new byte[6912];
		public byte[] palette = new byte[512];
		public byte[] loading = new byte[49152];
		public byte[] paletteLoRes = new byte[512];
		public byte[] loadingLoRes = new byte[12288];
		public byte[] loadingHiRes = new byte[6144];
		public byte[] loadingHiCol = new byte[12288];
		public byte[] bigFile = new byte[1835008]; // 112 * 16KB
		

	    private int currentBank;
	    private int currentAddress;
	    private int lastBank = -1;
	    private boolean fileAdded;

		public void extraFile(Path file) {
			try (var fis = Files.newInputStream(file)) {
				var contents = fis.readAllBytes();
				if (extraFiles.size() >= 255) {
					throw new IllegalStateException("Too many extra files; max is 255");
				}
				extraFiles.add(contents);
				header512.numExtraFiles = extraFiles.size();
				fileAdded = true;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		public void scr(Path file) {
			try (var fis = Files.newInputStream(file)) {
				var screenData = new byte[6912];
				var read = fis.read(screenData);
				if (read == 6912) {
					System.arraycopy(screenData, 0, loadingULA, 0, 6912);
					header512.loadingScreen |= LoadingScreen.ULA_LOADING;
				} else {
					throw new IllegalArgumentException("Invalid SCR file size: " + file);
				}
				fileAdded = true;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		public void bmp(
			    Path file,
			    boolean use_8bit_palette,
			    boolean dont_save_palette,
			    int border,
			    int bar1,
			    int bar2,
			    int delay1,
			    int delay2) {
			
			
			try (var fis = Files.newInputStream(file)) {
				var header = new byte[54];
				if (fis.read(header) != 54)
					throw new IOException("Invalid BMP header");

				palette = new byte[1024]; // 256 * 4
				if (fis.read(palette) != 1024)
					throw new IOException("Invalid BMP palette");

				for (int i = 0; i < 256; i++) {
					int b = Byte.toUnsignedInt(palette[i * 4]);
					int g = Byte.toUnsignedInt(palette[i * 4 + 1]);
					int r = Byte.toUnsignedInt(palette[i * 4 + 2]);
					int v = getPaletteValue(r, g, b);
					palette[i * 2] = (byte) (v & 0xFF);
					palette[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
				}

				int dataOffset = ((header[10] & 0xFF) | ((header[11] & 0xFF) << 8));
				fis.skip(dataOffset - 54 - 1024);
				for (int i = 0; i < 192; i++) {
					int offset = 256 * (191 - i);
					fis.read(loading, offset, 256);
				}

				header512.loadingScreen |= LoadingScreen.LAYER2;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

		    header512.loadingScreen |= LoadingScreen.LAYER2 + ( 128 * ( dont_save_palette ? 1 : 0 ) );
		    fileAdded = true;

		    header512.borderColor = border;
		    header512.loadingBar = bar1;
		    header512.loadingColor = bar2;
		    header512.loadingBankDelay = delay1;
		    header512.loadedDelay = delay2;
		}


//		public void bmp(Path file) {
//			try (var fis = Files.newInputStream(file)) {
//				var header = new byte[54];
//				if (fis.read(header) != 54)
//					throw new IOException("Invalid BMP header");
//
//				var palette = new byte[1024]; // 256 * 4
//				if (fis.read(palette) != 1024)
//					throw new IOException("Invalid BMP palette");
//
//				for (int i = 0; i < 256; i++) {
//					int b = Byte.toUnsignedInt(palette[i * 4]);
//					int g = Byte.toUnsignedInt(palette[i * 4 + 1]);
//					int r = Byte.toUnsignedInt(palette[i * 4 + 2]);
//					int v = getPaletteValue(r, g, b);
//					palette[i * 2] = (byte) (v & 0xFF);
//					palette[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
//				}
//
//				int dataOffset = ((header[10] & 0xFF) | ((header[11] & 0xFF) << 8));
//				fis.skip(dataOffset - 54 - 1024);
//				for (int i = 0; i < 192; i++) {
//					int offset = 256 * (191 - i);
//					fis.read(loading, offset, 256);
//				}
//
//				header512.loadingScreen |= LoadingScreen.LAYER2;
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//		}

		public void slr(Path file) {
			try (var fis = Files.newInputStream(file)) {
				var header = new byte[54];
				if (fis.read(header) != 54)
					throw new IOException("Invalid SLR header");

				if (fis.read(paletteLoRes) != 512)
					throw new IOException("Invalid SLR palette");

				fis.read(loadingULA, 0, 6912);

				for (int i = 0; i < 256; i++) {
					int b = Byte.toUnsignedInt(paletteLoRes[i * 4]);
					int g = Byte.toUnsignedInt(paletteLoRes[i * 4 + 1]);
					int r = Byte.toUnsignedInt(paletteLoRes[i * 4 + 2]);
					int v = getPaletteValue(r, g, b);
					paletteLoRes[i * 2] = (byte) (v & 0xFF);
					paletteLoRes[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
				}

				for (int i = 0; i < 96; i++) {
					int offset = 128 * (95 - i);
					fis.read(loadingLoRes, offset, 128);
				}

				header512.loadingScreen |= LoadingScreen.LO_RES;
				fileAdded = true;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		public void shr(Path file) {
			try (var fis = Files.newInputStream(file)) {
				fis.read(loadingHiRes, 0, 6144);
				header512.loadingScreen |= LoadingScreen.HI_RES;
				fileAdded = true;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		public void shc(Path file) {
			try (var fis = Files.newInputStream(file)) {
				fis.read(loadingHiCol, 0, 12288);
				header512.loadingScreen |= LoadingScreen.HI_COLOUR;
				fileAdded = true;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		public void mmu(Path file, int bank) {
			mmu(file, Optional.of(bank), Optional.empty());
		}
		
		public void mmu(Path file, int bank, int address) {
			mmu(file, Optional.of(bank), Optional.of(address));
		}
		
		public void mmu(Path file, Optional<Integer> bank8k, Optional<Integer> address8k) {
			if(bank8k.isPresent()) {
				currentBank = bank8k.get() >> 1;
			}
			
			if(address8k.isPresent()) {
				currentAddress = address8k.get();
				if(bank8k.get() != currentBank << 1) {
					currentAddress += 0x2000;
				}
			}

		    addFile(file, Optional.empty(), Optional.empty());
		}


		public void addFile(Path file, Optional<Integer> bank, Optional<Integer> address) {
			addFile(file, bank, address, new int[256]);
		}

		public void addFile(Path file, Optional<Integer> bank, Optional<Integer> address, int[] SNA_Bank) {

			var sna = file.getFileName().toString().endsWith(".sna");
			
			currentBank = bank.orElse(currentBank);
		    currentAddress = address.orElse(currentAddress);
		    
		    byte[] SNA_Header = null;
		    byte[] SNA128_Header = null;
		    
		    try (var fin = Files.newInputStream(file)) {
		    	
		    	if(sna) {
		    		SNA_Header = new byte[27];
		    		fin.readNBytes(SNA_Header, 0, SNA_Header.length);
		    		currentBank = 5;
		    		currentAddress= 0x400;
		    		for(int i = 0 ; i < SNA_Bank.length; i++) {
		    			SNA_Bank[i] = i;
		    		}
		    	}
		    	
//		    	var buf = new byte[0x8000];
		    	var buf = new byte[0x4000];
		    	
		    	while(true) {
		    		var realBank = getRealBank(currentBank);
		    		int maskedAddr = currentAddress & 0x3FFF;
					var offset = ( realBank * 16384 ) + maskedAddr;
    	            var length = 0x4000 - maskedAddr;
					// HRM, im sure this is same as python ... but getting
					// different results, will have to debug the python
					//' code a bit and try and work out what values these should be
//    	            var length = 0x8000 - maskedAddr;
    	            
    	            length = fin.read(buf, 0, length);
    	            if(length == -1)
    	            	break;
    	            System.arraycopy(buf, 0, bigFile,offset, length);
    	            
    	            if(currentBank < 64 + 48) {
    	            	header512.banks[currentBank] = 1;
    	            }
    	            
    	            if(sna && SNA_Bank[realBank] == 0) {
    	            	header512.banks[currentBank] = 0;
    	            }
    	            
    	            if(realBank > lastBank) {
    	            	lastBank = realBank;
    	            }
    	            
    	            if(sna) {
    	            	if(currentBank == 0) {
    	            		
    	            		SNA128_Header = fin.readNBytes(4);
    	            		
    	            		var sp = makeNum(SNA128_Header, 23, 2);
    	            		
    	            		if(isEmptyBytes(SNA128_Header)) {
        	            		SNA128_Header = new byte[4];
        	            		var sp2 = sp;
        	            		if(sp2 > 16384) {
        	            			sp2 -= 16384;
        	            		}
        	            		SNA128_Header[0] = bigFile[sp2 + 16];
        	            		SNA128_Header[1] = bigFile[sp2 + 17];
        	            		SNA128_Header[2] = 0;
        	            		SNA128_Header[3] = 0;
    	            		}
    	            		
    	            		header512.SP = sp;
    	            		header512.PC = makeNum(SNA128_Header, 0, 2);
    	            	}
    	            }
    	            
    	            currentAddress = ((currentAddress & 0xC000) + 0x4000) & 0xC000;
    	            currentBank = getNextBank(currentBank);
		    	}
		    	
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		public void entryBank(int bank) {
			header512.entryBank = bank;
			if (header512.versionNumber[2] < '2') {
				header512.versionNumber = "V1.2".getBytes();
			}
		}

		public void pcsp(int pc) {
			pcsp(pc, Optional.empty());
		}

		public void pcsp(int pc, Optional<Integer> sp) {
			pcsp(pc, sp, Optional.empty());
		}

		public void pcsp(int pc, Optional<Integer> sp, Optional<Integer> bank) {
			header512.PC = pc;
			sp.ifPresent(v -> header512.SP = v);
			bank.ifPresent(v -> {
				header512.entryBank = v;
				if (header512.versionNumber[2] < '2') {
					header512.versionNumber = "V1.2".getBytes();
				}
			});
		}

		public void core(String core) {
			var a = core.split("\\.");
			var v = new int[a.length];
			for(var i = 0 ; i < v.length; i++) {
				v[i] = Integer.parseInt(a[i]);
			}
			core(v);
		}

		public void core(int... core) {
			for(var i = 0 ; i < Math.min(core.length, 3); i++) {
				header512.coreRequired[i] = (byte)core[i];
			}
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
			System.out.println("Usage: java NexConverter <FILEIN> <FILEOUT> [--ram-required MB]");
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

		var configuration = new NexConfiguration();

		var parser = new CfgFileParser(configuration);
		parser.parse(Paths.get(inputFile));

		var generator = new NexGenerator(configuration);
		if (ramRequired != null)
			generator.ramRequired(ramRequired);
		generator.generate(Paths.get(outputFile));
	}

	public static class NexGenerator {

		
		private NexConfiguration configuration;
		private int ramRequired;
		private Consumer<String> reporting = s -> {};

		public NexGenerator(NexConfiguration configuration) {
			this.configuration = configuration;
		}

		public Consumer<String> reporting() {
			return reporting;
		}

		public NexGenerator reporting(Consumer<String> reporting) {
			this.reporting = reporting;
			return this;
		}

		public NexConfiguration configuration() {
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

		    if(configuration.lastBank <= -1 && !configuration.fileAdded) {
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

		private NexConfiguration configuration;
		private Consumer<String> reporting = s -> System.out.println(s);

		public CfgFileParser(NexConfiguration configuration) {
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
							configuration.bmp(fname, dontSavePalette, use8BitPalette,
									nextArg(parts), nextArg(parts), nextArg(parts), nextArg(parts), nextArg(parts));
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

	private static int getPaletteValue(int r, int g, int b) {
		return ((convert8BitTo5Bit(r) >> 2) & 1) << 8 | (convert8BitTo5Bit(r) >> 1) | (convert8BitTo5Bit(g) << 2)
				| (convert8BitTo5Bit(b) << 5);
	}

	private static int convert8BitTo5Bit(int v) {
		return Math.min(255, v) >> 5;
	}
	
	private static boolean isEmptyBytes(byte... bytes) {
		for(var b : bytes) {
			if(b != 0)
				return false;
		}
		return true;
	}
	
	private static int makeNum(byte[]  nums, int offset, int len) {
	    var result = 0;
	    var acc = 1;

	    for(var n : nums) {
	        result += n * acc;
	        acc <<= 8;
	    }

	    return result;
	}

	private final static int[] NEXT_BANKS = {1, 3, 0, 4, 6, 2, 7, 8};
	
	private static int getNextBank(int bank) {
	    if(bank > 7)
	        return bank + 1;
		return NEXT_BANKS[bank];
	}

	private final static int[] REAL_BANKS = {2, 3, 1, 4, 5, 0, 6, 7};

	private static int getRealBank(int bank) {
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
