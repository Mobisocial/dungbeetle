package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;

public class Gravatar {

    public static Uri gravatarUri(String email){
        String lowed = email.toLowerCase();
        String hash = Util.MD5(lowed);
        return Uri.parse("http://www.gravatar.com/avatar/" + hash);
    }

}
