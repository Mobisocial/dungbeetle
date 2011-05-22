package edu.stanford.mobisocial.dungbeetle.util;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

public class RemoteActivity implements ActivityCallout {
    private static final String ACTION_LAUNCH_TAPBOARD = "mobisocial.db.action.UPDATE_STATUS";
	private final ResultHandler mResultHandler;
	private final Context mContext;

	public RemoteActivity(Context c, ResultHandler handler) {
		mContext = c;
		mResultHandler = handler;
	}

	@Override
	public Intent getStartIntent() {
		final Intent intent = new Intent(ACTION_LAUNCH_TAPBOARD); // TODO
		return intent;
	}

	@Override
	public void handleResult(int resultCode, Intent resultData) {
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		String data = resultData.getStringExtra(Intent.EXTRA_TEXT);
		mResultHandler.onResult(data);
	}

	public interface ResultHandler {
		public void onResult(String data);
	}
}