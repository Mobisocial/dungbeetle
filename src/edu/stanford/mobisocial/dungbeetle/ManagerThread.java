package edu.stanford.mobisocial.dungbeetle;
import android.os.Message;
import edu.stanford.mobisocial.dungbeetle.transport.StateListener;
import edu.stanford.mobisocial.dungbeetle.transport.IncomingMessage;
import edu.stanford.mobisocial.dungbeetle.transport.MessageListener;
import edu.stanford.mobisocial.dungbeetle.transport.XMPPMessengerService;
import edu.stanford.mobisocial.dungbeetle.transport.MessengerService;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONObject;
import android.database.ContentObserver;
import java.util.Date;
import android.net.Uri;
import android.content.Context;
import android.util.Log;
import android.os.Handler;

public class ManagerThread extends Thread {
    private Handler mToastHandler;
    private Context mContext;
    private MessengerService mMessenger;

    private LinkedBlockingQueue<JSONObject> receiveQueue = 
        new LinkedBlockingQueue<JSONObject>();

    private LinkedBlockingQueue<JSONObject> sendQueue = 
        new LinkedBlockingQueue<JSONObject>();

    public ManagerThread(IdentityProvider ident, 
                         Context context, 
                         Handler toastHandler){
        mToastHandler = toastHandler;
        mContext = context;
		mMessenger = new XMPPMessengerService(ident);
		mMessenger.addStateListener(new StateListener() {
                public void onReady() {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Messenger ready!";
                    mToastHandler.sendMessage(m);
                }
                public void onNotReady() {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Messenger NOT ready.";
                    mToastHandler.sendMessage(m);
                }
            });
		mMessenger.addMessageListener(new MessageListener() {
                public void onMessage(IncomingMessage incoming) {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = incoming.contents();
                    mToastHandler.sendMessage(m);
                }
            });
    }


    @Override
    public void run(){
        Log.i("ManagerThread", "Starting DungBeetle manager thread");
        Log.i("ManagerThread", "Starting messenger...");
		mMessenger.init();
        final ObjectContentObserver oco = new ObjectContentObserver(
            new Handler(mContext.getMainLooper()));
		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds"), 
            true, oco);
		for(;;) {
            update();
			try {
				Thread.sleep(10000);
			} catch(InterruptedException e) {}
		}
    }


    private void update(){
        if(receiveQueue.size() > 0){
        }
    }


    class ObjectContentObserver extends ContentObserver {
		public boolean changed;
		public ObjectContentObserver(Handler h)  {
			super(h);
			changed = false;
		}
		@Override
		public synchronized void onChange(boolean self) {
			changed = true;
			notify();
		}
		public synchronized void waitForChange() {
			if(changed)
				return;
			try {
				wait();
				changed = false;
			} catch(InterruptedException e) {}
		}
		public synchronized void clearChanged() {
			changed = false;
		}
	};


}