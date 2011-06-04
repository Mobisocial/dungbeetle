package edu.stanford.mobisocial.dungbeetle.model;

import org.json.JSONObject;

import edu.stanford.mobisocial.dungbeetle.objects.InviteToSharedAppObj;

public class AppReference extends DbObject {

    public AppReference(JSONObject json) {
        super(InviteToSharedAppObj.TYPE, json);
    }

    public String pkg() {
        return this.mJson.optString("packageName");
    }
    

}
