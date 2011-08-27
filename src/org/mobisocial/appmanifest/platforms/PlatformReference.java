package org.mobisocial.appmanifest.platforms;

public abstract class PlatformReference {
	public abstract int getPlatformIdentifier();
	public abstract int getPlatformVersion();
	public abstract int getDeviceModality();
	public abstract byte[] getAppReference();
}