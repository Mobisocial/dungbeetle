/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.util;

public class JSON {
	public static String fastAddBase64(String original, String key, byte[] data) {
		String encoded = FastBase64.encodeToString(data);
		StringBuilder sb = new StringBuilder(encoded.length() + key.length() + original.length() + 32);
		sb.append(original.subSequence(0, original.lastIndexOf('}')));
		sb.append(",\"");
		sb.append(key);
		sb.append("\":\"");
		sb.append(encoded);
		sb.append("\"}");
		return sb.toString();
	}
}
