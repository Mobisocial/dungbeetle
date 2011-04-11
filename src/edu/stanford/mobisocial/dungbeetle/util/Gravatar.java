package edu.stanford.mobisocial.dungbeetle.util;
import android.net.Uri;

public class Gravatar {

    public static Uri gravatarUri(String email){
        return gravatarUri(email, 80);
    }

    public static Uri gravatarUri(String email, int size){
        String lowed = email.toLowerCase();
        String hash = Util.MD5(lowed);
        return Uri.parse("http://www.gravatar.com/avatar/" + hash + "?s=" + size);
    }

}
