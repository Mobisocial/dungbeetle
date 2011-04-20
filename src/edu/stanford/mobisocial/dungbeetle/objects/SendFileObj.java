package edu.stanford.mobisocial.dungbeetle.objects;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.R;
import org.json.JSONException;
import org.json.JSONObject;

public class SendFileObj implements IncomingMessageHandler, FeedRenderer {

    public static final String TYPE = "send_file";
    public static final String URI = "uri";
    public static final String MIME_TYPE = "mimeType";

    public static JSONObject json(String uri, String mimeType){
        JSONObject obj = new JSONObject();
        try{
            obj.put("mimeType", mimeType);
            obj.put("uri", uri);
        }catch(JSONException e){}
        return obj;
    }

	public boolean willHandle(Contact from, JSONObject msg) {
		return msg.optString("type").equals(TYPE);
	}

	public void handleReceived(Context context, Contact from, JSONObject obj) {
		String mimeType = obj.optString(MIME_TYPE);
		String uri = obj.optString(URI);
		Intent i = new Intent();
		i.setAction(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		i.setType(mimeType);
		i.setData(Uri.parse(uri));
		i.putExtra(Intent.EXTRA_TEXT, uri);

		PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, i,
            PendingIntent.FLAG_CANCEL_CURRENT);
		(new PresenceAwareNotify(context)).notify(
            "New Shared File...",
            "New Shared File", mimeType + "  " + uri, contentIntent);
	}

	public boolean willRender(JSONObject object) {
		return object.optString("type").equals(TYPE);
	}
    
    public void render(View frame, JSONObject content) {
    	String status = "Shared file: " + content.optString(URI);
        TextView bodyText = (TextView)frame.findViewById(R.id.body_text);
        bodyText.setText(status);
    }

}