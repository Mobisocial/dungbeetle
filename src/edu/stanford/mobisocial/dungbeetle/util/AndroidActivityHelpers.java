package edu.stanford.mobisocial.dungbeetle.util;

import android.content.Context;
import android.widget.Toast;

public class AndroidActivityHelpers {
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }
    public static void toast(final String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }
}
