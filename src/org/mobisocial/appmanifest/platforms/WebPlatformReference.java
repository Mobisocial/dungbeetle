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