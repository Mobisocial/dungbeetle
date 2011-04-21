package edu.stanford.mobisocial.dungbeetle.util;

import android.content.Intent;

public interface ActivityCallout {
	public Intent getStartIntent();
	public void handleResult(int resultCode, Intent data);
}