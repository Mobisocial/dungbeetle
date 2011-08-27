package org.mobisocial.appmanifest;

import java.nio.ByteBuffer;

public class ShortNameGenerator {

	public static void main(String[] args) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put((byte)'J');
		buffer.put((byte)'A');
		buffer.put((byte)'V');
		buffer.put((byte)0x00);
		
		buffer.position(0);
		System.out.println("0x" + Integer.toHexString(buffer.getInt()));
	}
}
