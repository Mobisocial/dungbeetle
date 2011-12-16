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

package org.mobisocial.appmanifest.platforms;

import org.mobisocial.appmanifest.ApplicationManifest;

public class WebPlatformReference extends PlatformReference {
	private String url;
	private int deviceModality = ApplicationManifest.MODALITY_UNSPECIFIED;
	
	public WebPlatformReference(String url) {
		this.url = url;
	}

	@Override
	public int getPlatformIdentifier() {
		return ApplicationManifest.PLATFORM_WEB_GET;
	}

	@Override
	public int getPlatformVersion() {
		return 0;
	}

	@Override
	public int getDeviceModality() {
		return deviceModality;
	}

	@Override
	public byte[] getAppReference() {
		return url.getBytes();
	}
}