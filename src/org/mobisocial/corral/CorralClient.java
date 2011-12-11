package org.mobisocial.corral;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.DbUser;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.model.Feed;
import edu.stanford.mobisocial.dungbeetle.util.FastBase64;

public class CorralClient {
    private static final String TAG = "corral";
    private static final boolean DBG = true;

    public static final String OBJ_MIME_TYPE = "mimeType";
    public static final String OBJ_LOCAL_URI = "localUri";

    private final Context mContext;

    public static CorralClient getInstance(Context context) {
        return new CorralClient(context);
    }

    private CorralClient(Context context) {
        mContext = context;
    }

    public boolean fileAvailableLocally(SignedObj obj) {
        try {
            Uri feedName = Feed.uriForName(obj.getFeedName());
            DbUser dbUser = App.instance().getMusubi()
                    .userForGlobalId(feedName, obj.getSender().getId());
            long contactId = dbUser.getLocalId();
            if (contactId == Contact.MY_ID) {
                return true;
            }
            // Local
            return localFileForContent(obj).exists();
        } catch (Exception e) {
            Log.w(TAG, "Error checking file availability", e);
            return false;
        }
    }

    /**
     * Synchronized method that retrieves content by any possible transport, and
     * returns a uri representing it locally. This method blocks until the file
     * is available locally, or it has been determined that the file cannot
     * currently be fetched.
     */
    public Uri fetchContent(SignedObj obj) throws IOException {
        if (obj.getJson() == null || !obj.getJson().has(OBJ_LOCAL_URI)) {
            if (DBG) {
                Log.d(TAG, "no local uri for obj.");
            }
            return null;
        }
        String localId = App.instance().getLocalPersonId();
        if (localId.equals(obj.getSender().getId())) {
            try {
                // TODO: Objects shared out from the content corral should
                // be accessible through the content corral. We don't have
                // to copy all files but we should have the option to create
                // a locate cache.
                return Uri.parse(obj.getJson().getString(OBJ_LOCAL_URI));
            } catch (JSONException e) {
                Log.e(TAG, "json exception getting local uri", e);
                return null;
            }
        }

        Uri feedName = Feed.uriForName(obj.getFeedName());
        DbUser user = App.instance().getMusubi()
                .userForGlobalId(feedName, obj.getSender().getId());
        File localFile = localFileForContent(obj);
        if (localFile.exists()) {
            return Uri.fromFile(localFile);
        }

        try {
            if (userAvailableOnLan(user)) {
                return getFileOverLan(user, obj);
            } else {
                if (DBG) Log.d(TAG, "User not avaialable on LAN.");
            }
        } catch (IOException e) {
            if (DBG) Log.d(TAG, "Failed to pull LAN file", e);
        }

        try {
            return getFileOverBluetooth(user, obj);
        } catch (IOException e) {
        }

        if (!localFile.exists()) {
            throw new IOException("Failed to fetch file");
        }
        return Uri.fromFile(localFile);
    }

    public String getMimeType(DbObj obj) {
        if (obj.getJson() != null && obj.getJson().has(OBJ_MIME_TYPE)) {
            try {
                return obj.getJson().getString(OBJ_MIME_TYPE);
            } catch (JSONException e) {
            }
        }
        return null;
    }

    private Uri getFileOverBluetooth(DbUser user, SignedObj obj)
            throws IOException {
        String macStr = DbContactAttributes.getAttribute(mContext, user.getLocalId(),
                Contact.ATTR_BT_MAC);
        if (macStr == null) {
            throw new IOException("No bluetooth mac address for user");
        }
        String uuidStr = DbContactAttributes.getAttribute(mContext, user.getLocalId(),
                Contact.ATTR_BT_CORRAL_UUID);
        if (uuidStr == null) {
            throw new IOException("No corral uuid for user");
        }
        UUID uuid = UUID.fromString(uuidStr);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(macStr);
        BluetoothSocket socket;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } else {
            socket = device.createRfcommSocketToServiceRecord(uuid);
        }

        // TODO:
        // Custom wire protocol, look for header bits to map to protocol handler.
        Log.d(TAG, "BJD BLUETOOTH CORRAL NOT READY: can't pull file over bluetooth.");
        return null;
    }

    private Uri getFileOverLan(DbUser user, SignedObj obj)
            throws IOException {
        try {
            // Remote
            String ip = getUserLanIp(mContext, user);
            Uri remoteUri = uriForContent(ip, obj);
            URL url = new URL(remoteUri.toString());
            if (DBG)
                Log.d(TAG, "Attempting to pull file " + url);

            File localFile = localFileForContent(obj);
            if (!localFile.exists()) {
                localFile.getParentFile().mkdirs();
                try {
                    InputStream is = url.openConnection().getInputStream();
                    OutputStream out = new FileOutputStream(localFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                } catch (IOException e) {
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                    throw e;
                }
            }
            return Uri.fromFile(localFile);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private boolean userAvailableOnLan(DbUser user) {
        // TODO: ipv6 compliance.
        // TODO: Try multiple ip endpoints; multi-sourced download;
        // torrent-style sharing
        // (mobile, distributed CDN)
        return null != DbContactAttributes.getAttribute(mContext, user.getLocalId(),
                Contact.ATTR_LAN_IP);
    }


    private static Uri uriForContent(String host, SignedObj obj) {
        try {
            String localContent = obj.getJson().getString(OBJ_LOCAL_URI);
            Uri baseUri = Uri.parse("http://" + host + ":" + ContentCorral.SERVER_PORT);
            return baseUri.buildUpon()
                    .appendQueryParameter("content", localContent)
                    .appendQueryParameter("hash", "" + obj.getHash()).build();
        } catch (Exception e) {
            Log.d(TAG, "No uri for content " + obj.getHash() + "; " + obj.getJson());
            return null;
        }
    }

    private static String getUserLanIp(Context context, DbUser user) {
        return DbContactAttributes.getAttribute(context, user.getLocalId(), Contact.ATTR_LAN_IP);
    }

    private File localFileForContent(SignedObj obj) {
        try {
            JSONObject json = obj.getJson();
            String suffix = extensionForType(json.optString(OBJ_MIME_TYPE));
            File feedDir = new File(mContext.getExternalCacheDir(), obj.getFeedName());
            String fname = hashToString(obj.getHash()) + "." + suffix;
            return new File(feedDir, fname);
        } catch (Exception e) {
            Log.e(TAG, "Error looking up file name", e);
            return null;
        }
    }

    static String extensionForType(String type) {
        final String DEFAULT = "dat";
        if (type == null) {
            return DEFAULT;
        }
        if (type.equals("image/jpeg")) {
            return "jpg";
        }
        if (type.equals("video/3gpp")) {
            return "3gp";
        }
        if (type.equals("image/png")) {
            return "png";
        }
        return DEFAULT;
    }

    static String typeForExtension(String ext) {
        if (ext == null) {
            return null;
        }
        if (ext.equals("jpg")) {
            return "image/jpeg";
        }
        if (ext.equals("3gp")) {
            return "video/3gp";
        }
        if (ext.equals("png")) {
            return "image/png";
        }
        return null;
    }

    private static class HashUtils {
        static String convertToHex(byte[] data) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9))
                        buf.append((char) ('0' + halfbyte));
                    else
                        buf.append((char) ('a' + (halfbyte - 10)));
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
            return buf.toString();
        }

        public static String SHA1(String text) throws NoSuchAlgorithmException,
                UnsupportedEncodingException {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            sha1hash = md.digest();
            return convertToHex(sha1hash);
        }
    }

    private static String hashToString(long hash) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();  
            DataOutputStream dos = new DataOutputStream(bos);  
            dos.writeLong(hash);  
            dos.writeInt(-4);  
            byte[] data = bos.toByteArray();
            return FastBase64.encodeToString(data).substring(0, 11);
        } catch (IOException e) {
            return null;
        }
    }
}
