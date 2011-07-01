/*
 * Copyright (c) 2010 Evenflow, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.stanford.mobisocial.dungbeetle;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.client.DropboxAPI;


public class LoginAsyncTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "LoginAsyncTask";

    String mUser;
    String mPassword;
    String mErrorMessage="";
    DropboxBackupActivity mDropboxSample;
    DropboxAPI.Config mConfig;
    DropboxAPI.Account mAccount;
    
    // Will just log in
    public LoginAsyncTask(DropboxBackupActivity act, String user, String password, DropboxAPI.Config config) {
        super();

        mDropboxSample = act;
        mUser = user;
        mPassword = password;
        mConfig = config;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
        	DropboxAPI api = mDropboxSample.getAPI();
        	
        	int success = DropboxAPI.STATUS_NONE;
        	if (!api.isAuthenticated()) {
	            mConfig = api.authenticate(mConfig, mUser, mPassword);
	            mDropboxSample.setConfig(mConfig);
            
	            success = mConfig.authStatus;

	            if (success != DropboxAPI.STATUS_SUCCESS) {
	            	return success;
	            }
        	}
        	mAccount = api.accountInfo();

        	if (!mAccount.isError()) {
        		return DropboxAPI.STATUS_SUCCESS;
        	} else {
        		Log.e(TAG, "Account info error: " + mAccount.httpCode + " " + mAccount.httpReason);
        		return DropboxAPI.STATUS_FAILURE;
        	}
        } catch (Exception e) {
            Log.e(TAG, "Error in logging in.", e);
            return DropboxAPI.STATUS_NETWORK_ERROR;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == DropboxAPI.STATUS_SUCCESS) {
            	if (mConfig != null && mConfig.authStatus == DropboxAPI.STATUS_SUCCESS) {
                	mDropboxSample.storeKeys(mConfig.accessTokenKey, mConfig.accessTokenSecret);
                	mDropboxSample.setLoggedIn(true);
                }
            	if (mAccount != null) {
            		mDropboxSample.action(mAccount);
            	}
            } else {
            	if (result == DropboxAPI.STATUS_NETWORK_ERROR) {
            		mDropboxSample.showToast("Network error: " + mConfig.authDetail);
            	} else {
            		mDropboxSample.showToast("Unsuccessful login.");
            	}
        }
    }

}

