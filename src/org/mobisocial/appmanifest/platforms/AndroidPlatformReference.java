package org.mobisocial.appmanifest.platforms;

import org.mobisocial.appmanifest.ApplicationManifest;

public class AndroidPlatformReference extends PlatformReference {
	private int platformVersion;
	private int deviceModality = ApplicationManifest.MODALITY_UNSPECIFIED;
	private byte[] appReference;
	
	public AndroidPlatformReference(int version, String pkg, String argument) {
		platformVersion = version;
		appReference = (pkg + ":" + argument).getBytes();
	}

	@Override
	public int getPlatformIdentifier() {
		return ApplicationManifest.PLATFORM_ANDROID_PACKAGE;
	}

	@Override
	public int getPlatformVersion() {
		return platformVersion;
	}

	@Override
	public int getDeviceModality() {
		return deviceModality;
	}

	@Override
	public byte[] getAppReference() {
		return appReference;
	}
}