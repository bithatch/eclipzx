package uk.co.bithatch.zxbasic.interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Optional;


public final class Z80Memory {
	
	public final static class Default {
		final static Charset ENCODING;
		
		static {
			ENCODING = Charset.forName(System.getProperty("z80memory.stringEncoding", "US-ASCII"));
		}	
	}
	
	public enum BankSize {
		FULL, HALF;
		
		private final static long FULL_BYTES = 16384;
		private final static long HALF_BYTES = 8192;
		
		public long toBytes() {
			switch(this) {
			case FULL:
				return FULL_BYTES;
			default:
				return HALF_BYTES;
			}
		}
	}

	public final static class Builder {
		private Optional<Long> bytes = Optional.empty();
		private Optional<ByteBuffer> data = Optional.empty();
		private Optional<Integer> banks = Optional.empty();
		private BankSize bankSize = BankSize.HALF;
		private int addressableSize = 65536;
		
		public Builder withAddressableSize(int addressableSize) {
			this.addressableSize = addressableSize;
			return this;
		}
		
		public Builder withBankSize(BankSize bankSize) {
			this.bankSize = bankSize;
			return this;
		}
		
		public Builder withBanks(int banks) {
			this.banks = Optional.of(banks);
			return this;
		}
		
		public Builder withSize(long bytes) {
			this.bytes = Optional.of(bytes);
			return this;
		}
		
		public Builder withData(byte[] data) {
			return withData(ByteBuffer.wrap(data));
		}
		
		public Builder withData(ByteBuffer data) {
			this.data = Optional.of(data);
			return this;
		}
		
		public Z80Memory build() {
			return new Z80Memory(this);
		}		
	}
	
	private final ByteBuffer data;
	private final BankSize bankSize;
	private final int addressableSize;
	private final int[] map;
	private final int slots;
	
	private Z80Memory(Builder bldr) {
		var sz = bldr.data.map(d -> (long)d.capacity()).orElseGet(() -> {
			return bldr.bytes.orElseGet(() -> {
				return bldr.banks.orElse(8) * bldr.bankSize.toBytes();
			});
		}); 
		
		if(bldr.banks.isPresent() && bldr.bytes.isPresent()) {
			throw new IllegalStateException("If provided, only the number of banks or the total size is needed, not both.");
		}
		
		if(bldr.data.isPresent()) {
			if(sz == bldr.data.get().capacity()) {
				data = bldr.data.get();
			}
			else if(sz < bldr.data.get().capacity()) {
				data = bldr.data.get().slice(0, sz.intValue());
			}
			else {
				throw new IllegalStateException("If the data buffer and the size is provided, it must be at least as big as the size provided.");
			}
		}
		else {
			data = ByteBuffer.allocateDirect(sz.intValue());
		}
		data.order(ByteOrder.LITTLE_ENDIAN);
				
		this.bankSize = bldr.bankSize;
		this.addressableSize = bldr.addressableSize;

		slots = (int)(addressableSize / bankSize().toBytes());
		map = new int[slots];
		for(var i = 0 ; i < slots; i++) {
			map[i] = i;
		}
	}
	
	public int addressableSlots() {
		return slots;
	}
	
	public int map(int slot) {
		return map[slot];
	}
	
	public void map(int slot, int bank) {
		if(bank >= banks())
			throw new IllegalArgumentException("Bank beyond end of memory");
		map[slot] = bank;
	}
	
	public ByteBuffer bank(int bank) {
		var bsz = bankSize().toBytes();
		var mappedBank = map[bank];
		var addr = mappedBank * bsz;
		var buf = data.slice((int)addr, (int)bsz);
		return buf;
	}
	
	/**
	 * The entire memory as one contiguous buffer.
	 * 
	 * @return data
	 */
	public ByteBuffer data() {
		return data;
	}
	
	public BankSize bankSize() {
		return bankSize;
	}
	
	public int banks() {
		return (int)(data.capacity() / bankSize.toBytes());
	}

	public void poke(int address, int value) {
		poke(address, (byte)value);
	}

	public void poke(int address, byte... value) {
		data.put(address & 0xffff, value);
	}

	public void put(int address, ByteBuffer buf, int offset, int len) {
		data.put(address & 0xffff, buf, offset, len);
	}

	public Var peek(VarType type, int address) {
		switch(type) {
		case BYTE:
			return new Var(type, data.get());
		case FIXED:
			return new Var(type, data.getFloat());
		case FLOAT:
			return new Var(type, data.getDouble());
		case INTEGER:
			return new Var(type, data.getShort());
		case LONG:
			return new Var(type, data.getInt());
		case UBYTE:
			return new Var(type, (short)Byte.toUnsignedInt(data.get()));
		case UINTEGER:
			return new Var(type, (short)Short.toUnsignedInt(data.getShort()));
		case ULONG:
			return new Var(type, (short)Integer.toUnsignedLong(data.getInt()));
		case STRING:
			return new Var(type, cString(data));
		default:
			throw new UnsupportedOperationException("Unsupported PEEK type.");
		}
	}

	public int peek(int address) {
		return Byte.toUnsignedInt(data.get());
	}
	
	static String cString(ByteBuffer data) {
		var nextString = data.slice();
		int i;
		for (i = 0; data.hasRemaining() && data.get() != 0x00; i++) {
		}
		nextString.limit(i);
		return Default.ENCODING.decode(nextString).toString();
	}


	static ByteBuffer cString(String str, ByteBuffer buf) {
		buf.put(str.getBytes(Default.ENCODING));
		buf.put((byte)0);
		return buf;
	}
}
