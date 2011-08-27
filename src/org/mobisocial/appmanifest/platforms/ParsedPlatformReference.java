package org.mobisocial.appmanifest.platforms;

/**
 * A {@link PlatformReference} parsed from a byte array.
 * @author bjdodson
 *
 */
public class ParsedPlatformReference extends PlatformReference {
	private int platformIdentifier;
	private int platformVersion;
	private int deviceModality;
	private byte[] appReference;
	
	public ParsedPlatformReference(int platformIdentifier,
			int platformVersion,
			int deviceModality,
			byte[] appReference) {
		
		this.platformIdentifier = platformIdentifier;
		this.platformVersion = platformVersion;
		this.deviceModality = deviceModality;
		this.appReference = appReference;
	}
	@Override
	public int getPlatformIdentifier() {
		return platformIdentifier;
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
