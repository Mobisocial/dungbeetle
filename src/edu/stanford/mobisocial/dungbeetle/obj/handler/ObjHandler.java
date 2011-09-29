package edu.stanford.mobisocial.dungbeetle.obj.handler;

import android.content.Context;
import android.net.Uri;


public abstract class ObjHandler implements IObjHandler {

    /**
     * Return true to filter out all objects for the given uri,
     * prior to even examining the obj.
     */
    public boolean preFiltersObj(Context context, Uri feedUri) {
        return false;
    }
}