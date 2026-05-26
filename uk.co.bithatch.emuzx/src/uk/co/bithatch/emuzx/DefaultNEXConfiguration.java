package uk.co.bithatch.emuzx;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uk.co.bithatch.emuzx.NexConverter.LoadingScreen;
import uk.co.bithatch.emuzx.NexConverter.NexHeader;

public class DefaultNEXConfiguration extends AbstractNEXConfiguration {
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
	int lastBank = -1;
	private boolean fileAdded;

	@Override
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

	@Override
	public void sl2(Path file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nxi(Path file) {
		// TODO Auto-generated method stub
		
	}

	@Override
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

	@Override
	public void loading(int border, int bar1, int bar2, int delay1, int delay2) {
		header512.borderColor = border;
		header512.loadingBar = bar1;
		header512.loadingColor = bar2;
		header512.loadingBankDelay = delay1;
		header512.loadedDelay = delay2;
	}

	@Override
	public void bmp(Path file, boolean use_8bit_palette, boolean dont_save_palette) {

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
				int v = NexConverter.getPaletteValue(r, g, b);
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

		header512.loadingScreen |= LoadingScreen.LAYER2 + (128 * (dont_save_palette ? 1 : 0));
		fileAdded = true;
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

	@Override
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
				int v = NexConverter.getPaletteValue(r, g, b);
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

	@Override
	public void shr(Path file) {
		try (var fis = Files.newInputStream(file)) {
			fis.read(loadingHiRes, 0, 6144);
			header512.loadingScreen |= LoadingScreen.HI_RES;
			fileAdded = true;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void shc(Path file) {
		try (var fis = Files.newInputStream(file)) {
			fis.read(loadingHiCol, 0, 12288);
			header512.loadingScreen |= LoadingScreen.HI_COLOUR;
			fileAdded = true;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void mmu(Path file, Optional<Integer> bank8k, Optional<Integer> address8k) {
		if (bank8k.isPresent()) {
			currentBank = bank8k.get() >> 1;
		}

		if (address8k.isPresent()) {
			currentAddress = address8k.get();
			if (bank8k.get() != currentBank << 1) {
				currentAddress += 0x2000;
			}
		}

		addFile(file, Optional.empty(), Optional.empty());
	}

	@Override
	public void addFile(Path file, Optional<Integer> bank, Optional<Integer> address, int[] SNA_Bank) {

		var sna = file.getFileName().toString().endsWith(".sna");

		currentBank = bank.orElse(currentBank);
		currentAddress = address.orElse(currentAddress);

		byte[] SNA_Header = null;
		byte[] SNA128_Header = null;

		try (var fin = Files.newInputStream(file)) {

			if (sna) {
				SNA_Header = new byte[27];
				fin.readNBytes(SNA_Header, 0, SNA_Header.length);
				currentBank = 5;
				currentAddress = 0x400;
				for (int i = 0; i < SNA_Bank.length; i++) {
					SNA_Bank[i] = i;
				}
			}

//		    	var buf = new byte[0x8000];
			var buf = new byte[0x4000];

			while (true) {
				var realBank = NexConverter.getRealBank(currentBank);
				int maskedAddr = currentAddress & 0x3FFF;
				var offset = (realBank * 16384) + maskedAddr;
				var length = 0x4000 - maskedAddr;
				// HRM, im sure this is same as python ... but getting
				// different results, will have to debug the python
				// ' code a bit and try and work out what values these should be
//    	            var length = 0x8000 - maskedAddr;

				length = fin.read(buf, 0, length);
				if (length == -1)
					break;
				System.arraycopy(buf, 0, bigFile, offset, length);

				if (currentBank < 64 + 48) {
					header512.banks[currentBank] = 1;
				}

				if (sna && SNA_Bank[realBank] == 0) {
					header512.banks[currentBank] = 0;
				}

				if (realBank > lastBank) {
					lastBank = realBank;
				}

				if (sna) {
					if (currentBank == 0) {

						SNA128_Header = fin.readNBytes(4);

						var sp = NexConverter.makeNum(SNA128_Header, 23, 2);

						if (NexConverter.isEmptyBytes(SNA128_Header)) {
							SNA128_Header = new byte[4];
							var sp2 = sp;
							if (sp2 > 16384) {
								sp2 -= 16384;
							}
							SNA128_Header[0] = bigFile[sp2 + 16];
							SNA128_Header[1] = bigFile[sp2 + 17];
							SNA128_Header[2] = 0;
							SNA128_Header[3] = 0;
						}

						header512.SP = sp;
						header512.PC = NexConverter.makeNum(SNA128_Header, 0, 2);
					}
				}

				currentAddress = ((currentAddress & 0xC000) + 0x4000) & 0xC000;
				currentBank = NexConverter.getNextBank(currentBank);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void entryBank(int bank) {
		header512.entryBank = bank;
		if (header512.versionNumber[2] < '2') {
			header512.versionNumber = "V1.2".getBytes();
		}
	}

	@Override
	public void pcsp(int pc, Optional<Integer> sp, Optional<Integer> bank) {
		header512.PC = pc;
		sp.ifPresent(v -> header512.SP = v);
		bank.ifPresent(this::entryBank);
	}

	@Override
	public void core(int... core) {
		for (var i = 0; i < Math.min(core.length, 3); i++) {
			header512.coreRequired[i] = (byte) core[i];
		}
	}

	@Override
	public boolean hasContent() {
		return fileAdded;
	}
}