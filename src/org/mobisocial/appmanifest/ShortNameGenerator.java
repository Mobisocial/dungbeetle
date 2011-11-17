package org.mobisocial.appmanifest;

import java.nio.ByteBuffer;

public class ShortNameGenerator {

	public static void main(String[] args) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put((byte)'M');
		buffer.put((byte)'U');
		buffer.put((byte)'S');
		buffer.put((byte)'U');
		
		buffer.position(0);
		System.out.println("0x" + Integer.toHexString(buffer.getInt()));
	}
}
