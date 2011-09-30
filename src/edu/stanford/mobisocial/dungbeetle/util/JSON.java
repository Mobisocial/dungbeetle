package edu.stanford.mobisocial.dungbeetle.util;

public class JSON {
	public static String fastAddBase64(String original, String key, byte[] data) {
		String encoded = FastBase64.encodeToString(data);
		StringBuilder sb = new StringBuilder(encoded.length() + key.length() + original.length() + 32);
		sb.append(original.subSequence(0, original.lastIndexOf('}')));
		sb.append(",\"");
		sb.append(key);
		sb.append("\":\"");
		sb.append(data);
		sb.append("\"}");
		return sb.toString();
	}
}
