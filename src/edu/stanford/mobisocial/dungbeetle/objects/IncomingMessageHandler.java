package edu.stanford.mobisocial.dungbeetle.objects;
import android.content.Context;
import org.json.JSONObject;
import edu.stanford.mobisocial.dungbeetle.model.Contact;

public interface IncomingMessageHandler {
    boolean willHandle(Contact from, JSONObject msg);
    void handleReceived(Context context, Contact from, JSONObject msg);
}