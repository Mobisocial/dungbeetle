package edu.stanford.mobisocial.dungbeetle.objects;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InviteToGroupObj{
    public static final String TYPE = "invite_group";

    public static final String SHARED_FEED_NAME = "sharedFeedName";
    public static final String GROUP_NAME = "groupName";
    public static final String PARTICIPANTS = "participants";

    public static JSONObject json(
        long[] participants, String groupName, String feedName){
        JSONObject obj = new JSONObject();
        try{
            obj.put(GROUP_NAME, groupName);
            obj.put(SHARED_FEED_NAME, feedName);
            JSONArray parts = new JSONArray();
            for(int i = 0; i < participants.length; i++){
                String localId = "@l" + participants[i];
                parts.put(i, localId);
            }
            // Need to add ourself to participants
            parts.put(parts.length(), "@l" + Contact.MY_ID);
            obj.put(PARTICIPANTS, parts);
        }
        catch(JSONException e){}
        return obj;
    }

}
