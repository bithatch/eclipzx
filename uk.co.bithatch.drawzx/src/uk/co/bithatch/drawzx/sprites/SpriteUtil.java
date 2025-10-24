package uk.co.bithatch.drawzx.sprites;

import java.nio.ByteBuffer;

public class SpriteUtil {

	public static int[] oneBitRowToCellRow(byte data) {
		return oneBitRowToCellRow(data, 0, new int[8]);
	}
	public static int[] oneBitRowToCellRow(byte data, int offset, int[] dest) {
		for(var i = 7 ; i >= 0 ; i--) {
			dest[offset + (7 - i)] = ( data >> i & 0x01 ) == 0 ? 0 : 1;  
		}
		return dest;
	}
	
	public static void byteRowToCellRow(ByteBuffer buf, int width, int offset, int[] data) {
		for(int i = 0 ; i < width; i++) {
			data[offset + i] = Byte.toUnsignedInt(buf.get());
		}
	}
	

	public static void nibbleRowToCellRow(ByteBuffer buf, int width, int offset, int[] data) {
		// TODO
		var v = 0;
		for(int i = 0 ; i < width; i++) {
			if(i % 2 == 0) {
				v = Byte.toUnsignedInt(buf.get());
				data[offset + i] = ( v >> 4 ) &0xf;
			}
			else
				data[offset + i] = v & 0x0f;
		}
	}
}
