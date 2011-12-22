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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class RemoteActivity implements ActivityCallout {
    private static final String ACTION_LAUNCH_TAPBOARD = "mobisocial.db.action.UPDATE_STATUS";
	private final ResultHandler mResultHandler;
	@SuppressWarnings("unused")
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