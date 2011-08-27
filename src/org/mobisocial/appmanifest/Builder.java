package org.mobisocial.appmanifest;

import java.util.LinkedList;

import org.mobisocial.appmanifest.platforms.PlatformReference;

public class Builder {
		ApplicationManifest mApplicationManifest;
		
		public Builder() {
			mApplicationManifest = new ApplicationManifest();
			mApplicationManifest.mPlatformReferences = new LinkedList<PlatformReference>();
		}
		
		public ApplicationManifest create() {
			return mApplicationManifest;
		}
		
		public Builder setName(String name) {
			mApplicationManifest.mName = name;
			return this;
		}
		
		public Builder addPlatformReference(PlatformReference reference) {
			mApplicationManifest.mPlatformReferences.add(reference);
			return this;
		}
	}