package edu.stanford.mobisocial.dungbeetle;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class JunctionQueryService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//mJunctionMaker.newJunction(uri, code);
		return START_STICKY;
	}
}
