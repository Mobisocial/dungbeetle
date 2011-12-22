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

import android.os.Build;
import android.util.Base64;

public class FastBase64 {
	public static byte[] encode(byte[] data) {
		if(data == null)
			return new byte[0]; 
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.encode(data, Base64.DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.encodeToByte(data, false);
	}
	public static String encodeToString(byte[] data) {
		if (data == null) {
			return "";
		}
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.encodeToString(data, Base64.DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.encodeToString(data, false);
	}
	public static byte[] decode(byte[] data) {
		if(data == null)
			return new byte[0]; 
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.decode(data, Base64.DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.decode(data);
	}
	public static byte[] decode(String data) {
		if(data == null)
			return new byte[0]; 
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.decode(data, Base64. DEFAULT);
		return edu.stanford.mobisocial.dungbeetle.util.Base64.decode(data);
	}
}
