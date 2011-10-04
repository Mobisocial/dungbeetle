package edu.stanford.mobisocial.dungbeetle.group_providers;

import android.content.Context;
import android.net.Uri;

public interface GroupRefreshHandler{
    public boolean willHandle(Uri uri);
    public void handle(long id, Uri uri, Context
    		context, int version, boolean updateProfile);
}
