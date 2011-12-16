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

package org.mobisocial.appmanifest.test;

import java.util.Arrays;

import org.mobisocial.appmanifest.ApplicationManifest;
import org.mobisocial.appmanifest.Builder;
import org.mobisocial.appmanifest.platforms.AndroidPlatformReference;
import org.mobisocial.appmanifest.platforms.PlatformReference;


public class AndroidPlatformTest {

	public static void main(String... args) {
		PlatformReference androidReference = new AndroidPlatformReference(10, "org.openjunction.wescribble", "http://openjunction.org/demo/whiteboard");
		ApplicationManifest manifest =
			new Builder()
				.setName("weScribble")
				.addPlatformReference(androidReference)
				.create();
		byte[] bytes = manifest.toByteArray();
		
		ApplicationManifest parsedManifest = new ApplicationManifest(bytes);
		
		System.out.println("Created application manifest of size " + bytes.length + " bytes.");
		
		if (!parsedManifest.getName().equals(manifest.getName())) {
			System.err.println("Mismatching names");
		}
		
		int platformCount = manifest.getPlatformReferences().size();
		if (platformCount != parsedManifest.getPlatformReferences().size()) {
			System.err.println("Mismatch platform reference length.");
			return;
		}
		
		System.out.println("Comparing " + platformCount + " references.");
		for (int i = 0; i < platformCount; i++) {
			PlatformReference original = manifest.getPlatformReferences().get(i);
			PlatformReference parsed = manifest.getPlatformReferences().get(i);
			
			if (original.getPlatformIdentifier() != parsed.getPlatformIdentifier()) {
				System.err.println("Mismatched platform identifier");
			}
			
			if (original.getPlatformVersion() != parsed.getPlatformVersion()) {
				System.err.println("Mismatched platform version");
			}
			
			if (original.getDeviceModality() != parsed.getDeviceModality()) {
				System.err.println("Mismatched device modality");
			}
			
			if (!Arrays.equals(original.getAppReference(), parsed.getAppReference())) {
				System.err.println("Mismatched platform arguments");
			}
			
			String arg = new String(parsed.getAppReference());
			System.out.println("Result: " + arg);
		}
		
		
	}
}
