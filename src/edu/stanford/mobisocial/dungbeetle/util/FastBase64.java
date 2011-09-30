package edu.stanford.mobisocial.dungbeetle.util;

import android.os.Build;
import android.util.Base64;

public class FastBase64 {
	public static byte[] encode(byte[] data) {
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.encode(data, Base64.DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.encodeToByte(data, false);
	}
	public static String encodeToString(byte[] data) {
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.encodeToString(data, Base64.DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.encodeToString(data, false);
	}
	public static byte[] decode(byte[] data) {
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.decode(data, Base64.DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.decode(data);
	}
	public static byte[] decode(String data) {
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.decode(data, Base64. DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.decode(data);
	}
}
