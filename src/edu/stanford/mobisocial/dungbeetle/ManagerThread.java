package edu.stanford.mobisocial.dungbeetle;
import android.os.Message;
import edu.stanford.mobisocial.dungbeetle.transport.StateListener;
import edu.stanford.mobisocial.dungbeetle.transport.IncomingMessage;
import edu.stanford.mobisocial.dungbeetle.transport.MessageListener;
import edu.stanford.mobisocial.dungbeetle.transport.StandardIdentity;
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

    public ManagerThread(Context context, Handler toastHandler){
        mToastHandler = toastHandler;
        mContext = context;
		mMessenger = new XMPPMessengerService(
            new StandardIdentity(myPubKey, myPrivKey));
		mMessenger.addStateListener(new StateListener() {
                public void onReady() {
                    Message m = mToastHandler.obtainMessage();
                    m.obj = "Messenger ready!";
                    mToastHandler.sendMessage(m);
                }
                public void onNotReady() {
                }
            });
		mMessenger.addMessageListener(new MessageListener() {
                public void onMessage(IncomingMessage m) {
                    System.out.println("Got message! " + m.toString());
                }
            });

		mMessenger.init();
    }


    @Override
    public void run(){
        Log.i("ManagerThread", "Starting DungBeetle manager thread");
        final ObjectContentObserver oco = new ObjectContentObserver(
            new Handler(mContext.getMainLooper()));
		mContext.getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds"), 
            true, oco);

		for(;;) {
			Date start = new Date();
            processReceiveQueue();
            processSendQueue();
			Date stop = new Date();
			try {
				Thread.sleep(10000);
			} catch(InterruptedException e) {}
		}
    }

    private void processReceiveQueue(){
        if(receiveQueue.size() > 0){
        }
    }


    private void processSendQueue(){
        if(sendQueue.size() > 0){
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