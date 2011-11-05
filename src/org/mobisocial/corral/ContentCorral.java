
package org.mobisocial.corral;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.UUID;

import mobisocial.comm.BluetoothDuplexSocket;
import mobisocial.comm.DuplexSocket;
import mobisocial.comm.StreamDuplexSocket;
import mobisocial.comm.TcpDuplexSocket;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.musubi.DbUser;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

public class ContentCorral {
    public static final String OBJ_MIME_TYPE = "mimeType";
    public static final String OBJ_LOCAL_URI = "localUri";
    private static final String PREF_CORRAL_BT_UUID = "corral_bt";
    private static final int SERVER_PORT = 8224;
    private static final String BT_CORRAL_NAME = "Content Corral";

    private static final String TAG = "ContentCorral";
    private static final boolean DBG = true;

    private BluetoothAcceptThread mBluetoothAcceptThread;
    private HttpAcceptThread mHttpAcceptThread;
    private Context mContext;
    public static final boolean CONTENT_CORRAL_ENABLED = true;

    public ContentCorral(Context context) {
        mContext = context;
    }

    public void start() {
        startHttpServer();
        startBluetoothService();
    }

    private void startBluetoothService() {
        if (mBluetoothAcceptThread != null)
            return;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return;
        }

        mBluetoothAcceptThread = new BluetoothAcceptThread(adapter,
                getLocalBluetoothServiceUuid(mContext));
    }

    /**
     * Starts the simple image server
     */
    private synchronized void startHttpServer() {
        if (mHttpAcceptThread != null)
            return;

        String ip = getLocalIpAddress();
        if (ip == null) {
            Toast.makeText(mContext, "Image server failed to start. Are you on a wifi network?",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mHttpAcceptThread = new HttpAcceptThread(SERVER_PORT);
        mHttpAcceptThread.start();
    }

    public synchronized void stop() {
        if (mHttpAcceptThread != null) {
            mHttpAcceptThread.cancel();
            mHttpAcceptThread = null;
        }
    }

    private class HttpAcceptThread extends Thread {
        // The local server socket
        private final ServerSocket mmServerSocket;

        public HttpAcceptThread(int port) {
            ServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = new ServerSocket(port);
            } catch (IOException e) {
                System.err.println("Could not open server socket");
                e.printStackTrace(System.err);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            // Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            Socket socket = null;

            // Listen to the server socket always
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    // Log.d(TAG, "waiting for client...");
                    socket = mmServerSocket.accept();
                    // Log.d(TAG, "Client connected!");
                } catch (SocketException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket == null) {
                    break;
                }

                DuplexSocket duplex;
                try {
                    duplex = new StreamDuplexSocket(socket.getInputStream(),
                            socket.getOutputStream());
                } catch (IOException e) {
                    Log.e(TAG, "Failed to connect to socket", e);
                    return;
                }
                HttpConnectedThread conThread = new HttpConnectedThread(duplex);
                conThread.start();
            }
            Log.d(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    private class BluetoothAcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public BluetoothAcceptThread(BluetoothAdapter adapter, UUID coralUuid) {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = adapter.listenUsingInsecureRfcommWithServiceRecord(BT_CORRAL_NAME, coralUuid);
            } catch (IOException e) {
                System.err.println("Could not open bt server socket");
                e.printStackTrace(System.err);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            // Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket always
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    // Log.d(TAG, "waiting for client...");
                    socket = mmServerSocket.accept();

                    // Log.d(TAG, "Client connected!");
                } catch (SocketException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket == null) {
                    break;
                }

                DuplexSocket duplex = new BluetoothDuplexSocket(socket);
                ObjExConnectedThread conThread = new ObjExConnectedThread(duplex);
                conThread.start();
            }
            Log.d(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It supports
     * incoming and outgoing transmissions over HTTP.
     */
    private class HttpConnectedThread extends Thread {
        private final DuplexSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int BUFFER_LENGTH = 1024;

        public HttpConnectedThread(DuplexSocket socket) {
            // Log.d(TAG, "create ConnectedThread");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytes;

            if (mmInStream == null || mmOutStream == null)
                return;

            // Read header information, determine connection type
            try {
                bytes = mmInStream.read(buffer);
                Log.d(TAG, "read " + bytes + " header bytes");
                String header = new String(buffer, 0, bytes);

                // determine request type
                if (header.startsWith("GET ")) {
                    doGetRequest(header);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading connection header", e);
            }

            // No longer listening.
            cancel();
        }

        /**
         * TODO: This is completely and totally insecure. At the very least,
         * check to make sure the connector is a friend, and can verify
         * over oauth or similar.
         */
        private void doGetRequest(String header) {
            String[] headers = header.split("\r\n");
            if (!headers[0].startsWith("GET "))
                return;
            String[] request = headers[0].split(" ");
            Uri targetUri = Uri.parse("http://mock" + URLDecoder.decode(request[1]));
            if (targetUri.getQueryParameter("content") == null) {
                try {
                    mmOutStream.write(header("HTTP/1.1 404 NOT FOUND"));
                } catch (IOException e) {
                }
                return;
            }
            Uri requestPath = Uri.parse(targetUri.getQueryParameter("content"));

            if ("content".equals(requestPath.getScheme())) {
                Log.d(TAG, "Retrieving for " + requestPath.getAuthority());
                if (DungBeetleContentProvider.AUTHORITY.equals(requestPath.getAuthority())) {
                    if (requestPath.getQueryParameter("obj") != null) {
                        int objIndex = Integer.parseInt(requestPath.getQueryParameter("obj"));
                        sendObj(requestPath, objIndex);
                    } else {
                        sendObjs(requestPath);
                    }
                } else {
                    sendContent(requestPath);
                }
            }
        }

        private void sendContent(Uri requestPath) {
            InputStream in;
            try {
                // img = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI,
                // imgId);
                in = mContext.getContentResolver().openInputStream(requestPath);
            } catch (Exception e) {
                Log.d(TAG, "Error opening file", e);
                return;
            }

            try {
                byte[] buffer = new byte[4096];
                int r = 0;

                // Gross way to get length. What's the right way??
                int size = 0;
                while ((r = in.read(buffer)) > 0) {
                    size += r;
                }

                in = mContext.getContentResolver().openInputStream(requestPath);
                mmOutStream.write(header("HTTP/1.1 200 OK"));
                mmOutStream.write(header("Content-Type: "
                        + mContext.getContentResolver().getType(requestPath)));
                mmOutStream.write(header("Content-Length: " + size));
                // mmOutStream.write(header("Content-Disposition: attachment; filename=\""+filename+"\""));
                mmOutStream.write(header(""));

                while ((r = in.read(buffer)) > 0) {
                    mmOutStream.write(buffer, 0, r);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending file", e);
            } finally {
                try {
                    mmOutStream.close();
                } catch (IOException e) {
                }
            }
        }

        private void sendObj(Uri requestPath, int objIndex) {
            InputStream in;
            byte[] bytes;
            try {
                Uri uri = Feed.uriForName(requestPath.getPath().substring(1));
                String[] projection = new String[] {
                        DbObject._ID, DbObject.JSON
                };
                String selection = DbObjects.getFeedObjectClause(null);
                String[] selectionArgs = null;
                String sortOrder = DbObject._ID + " ASC";
                Cursor cursor = mContext.getContentResolver().query(uri, projection, selection,
                        selectionArgs, sortOrder);
                try {

                    if (!cursor.moveToPosition(objIndex)) {
                        Log.d(TAG, "No obj found for " + uri);
                        return;
                    }
                    String jsonStr = cursor.getString(1);
                    bytes = jsonStr.getBytes();
                    in = new ByteArrayInputStream(bytes);
                } finally {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "Error opening obj", e);
                return;
            }

            try {
                byte[] buffer = new byte[4096];
                int r = 0;

                mmOutStream.write(header("HTTP/1.1 200 OK"));
                mmOutStream.write(header("Content-Type: text/plain"));
                mmOutStream.write(header("Content-Length: " + bytes.length));
                // mmOutStream.write(header("Content-Disposition: attachment; filename=\""+filename+"\""));
                mmOutStream.write(header(""));

                while ((r = in.read(buffer)) > 0) {
                    Log.d(TAG, "sending: " + new String(buffer));
                    mmOutStream.write(buffer, 0, r);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending file", e);
            } finally {
                try {
                    mmOutStream.close();
                } catch (IOException e) {
                }
            }
        }

        private void sendObjs(Uri requestPath) {
            // TODO: hard-coded limit of 30 in place.
            InputStream in;
            byte[] bytes;
            StringBuilder jsonArrayBuilder = new StringBuilder("[");
            try {
                Uri uri = Feed.uriForName(requestPath.getPath().substring(1));
                String[] projection = new String[] {
                        DbObject._ID, DbObject.JSON
                };
                String selection = DbObjects.getFeedObjectClause(null);
                String[] selectionArgs = null;
                String sortOrder = DbObject._ID + " ASC LIMIT 30";
                Cursor cursor = mContext.getContentResolver().query(uri, projection, selection,
                        selectionArgs, sortOrder);

                try {
                    if (!cursor.moveToFirst()) {
                        Log.d(TAG, "No objs found for " + uri);
                        return;
                    }
                    jsonArrayBuilder.append(cursor.getString(1));
                    while (!cursor.isLast()) {
                        cursor.moveToNext();
                        String jsonStr = cursor.getString(1);
                        jsonArrayBuilder.append(",").append(jsonStr);
                    }
                } finally {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "Error opening obj", e);
                return;
            }

            bytes = jsonArrayBuilder.append("]").toString().getBytes();
            in = new ByteArrayInputStream(bytes);
            try {
                byte[] buffer = new byte[4096];
                int r = 0;

                mmOutStream.write(header("HTTP/1.1 200 OK"));
                mmOutStream.write(header("Content-Type: text/plain"));
                mmOutStream.write(header("Content-Length: " + bytes.length));
                // mmOutStream.write(header("Content-Disposition: attachment; filename=\""+filename+"\""));
                mmOutStream.write(header(""));

                while ((r = in.read(buffer)) > 0) {
                    Log.d(TAG, "sending: " + new String(buffer));
                    mmOutStream.write(buffer, 0, r);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending file", e);
            } finally {
                try {
                    mmOutStream.close();
                } catch (IOException e) {
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private byte[] header(String str) {
        return (str + "\r\n").getBytes();
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        // not ready for IPv6, apparently.
                        if (!inetAddress.getHostAddress().contains(":")) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return null;
    }

    /**
     * This thread runs during a connection with a remote device. It supports
     * incoming and outgoing transmissions over HTTP.
     */
    private class ObjExConnectedThread extends Thread {
        private final DuplexSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int BUFFER_LENGTH = 1024;

        public ObjExConnectedThread(DuplexSocket socket) {
            // Log.d(TAG, "create ConnectedThread");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytes;

            if (mmInStream == null || mmOutStream == null)
                return;

            // Read header information, determine connection type
            try {
                /**
                 * TODO: SNEP-like protocol here, for ObjEx.
                 * Remember, we have authenticated objs, ndef does not.
                 * 
                 * server: NONCE CHALLENGE
                 * client: AUTHED REQUEST
                 * server: AUTHED RESPONSE
                 */
                bytes = mmInStream.read(buffer);
                Log.d(TAG, "read " + bytes + " header bytes");
                String header = new String(buffer, 0, bytes);

                // TODO
                Log.d(TAG, "BJD BLUETOOTH CORRAL NOT READY: ObjEx needs defining.");
            } catch (Exception e) {
                Log.e(TAG, "Error reading connection header", e);
            }

            // No longer listening.
            cancel();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private static Uri uriForContent(String host, String localContent) {
        Uri baseUri = Uri.parse("http://" + host + ":" + SERVER_PORT);
        return baseUri.buildUpon().appendQueryParameter("content", localContent).build();
    }

    /**
     * Synchronized method that retrieves content by any possible transport, and
     * returns a uri representing it locally. This method blocks until the file
     * is available locally, or it has been determined that the file cannot
     * currently be fetched.
     */
    public static Uri fetchContent(Context context, SignedObj obj) throws IOException {
        if (!obj.getJson().has(OBJ_LOCAL_URI)) {
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

        DbUser user = App.instance().getMusubi()
                .userForGlobalId(obj.getContainingFeed().getUri(), obj.getSender().getId());
        File localFile = localFileForContent(context, obj);
        if (localFile.exists()) {
            return Uri.fromFile(localFile);
        }

        try {
            if (userAvailableOnLan(context, user)) {
                return getFileOverLan(context, user, obj);
            }
        } catch (IOException e) {
        }

        try {
            return getFileOverBluetooth(context, user, obj);
        } catch (IOException e) {
        }

        if (!localFile.exists()) {
            throw new IOException("Failed to fetch file");
        }
        return Uri.fromFile(localFile);
    }

    private static Uri getFileOverBluetooth(Context context, DbUser user, SignedObj obj)
            throws IOException {
        String macStr = DbContactAttributes.getAttribute(context, user.getLocalId(),
                Contact.ATTR_BT_MAC);
        if (macStr == null) {
            throw new IOException("No bluetooth mac address for user");
        }
        String uuidStr = DbContactAttributes.getAttribute(context, user.getLocalId(),
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

    private static Uri getFileOverLan(Context context, DbUser user, SignedObj obj)
            throws IOException {
        try {
            // Remote
            String ip = getUserLanIp(context, user);
            Uri remoteUri = uriForContent(ip, obj.getJson().getString(OBJ_LOCAL_URI));
            URL url = new URL(remoteUri.toString());
            if (DBG)
                Log.d(TAG, "Attempting to pull file " + remoteUri);

            File localFile = localFileForContent(context, obj);
            if (!localFile.exists()) {
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
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static boolean userAvailableOnLan(Context context, DbUser user) {
        // TODO: ipv6 compliance.
        // TODO: Try multiple ip endpoints; multi-sourced download;
        // torrent-style sharing
        // (mobile, distributed CDN)
        return null == DbContactAttributes.getAttribute(context, user.getLocalId(),
                Contact.ATTR_LAN_IP);
    }

    private static String getUserLanIp(Context context, DbUser user) {
        return DbContactAttributes.getAttribute(context, user.getLocalId(), Contact.ATTR_LAN_IP);
    }

    private static File localFileForContent(Context context, SignedObj obj) {
        try {
            JSONObject json = obj.getJson();
            Uri remoteUri = uriForContent(json.getString(Contact.ATTR_LAN_IP),
                    json.getString(OBJ_LOCAL_URI));

            String suffix = suffixForType(json.optString(OBJ_MIME_TYPE));
            String fname = "sha-" + HashUtils.SHA1(remoteUri.toString()) + "." + suffix;
            return new File(context.getExternalCacheDir(), fname);
        } catch (Exception e) {
            Log.e(TAG, "Error looking up file name", e);
            return null;
        }
    }

    private static String suffixForType(String type) {
        if (type == null) {
            return null;
        }
        if (type.equals("image/jpeg")) {
            return "jpg";
        }
        if (type.equals("video/3gpp")) {
            return "3gp";
        }
        return null;
    }

    public static boolean fileAvailableLocally(Context context, SignedObj obj) {
        try {
            DbUser dbUser = App.instance().getMusubi()
                    .userForGlobalId(obj.getContainingFeed().getUri(), obj.getSender().getId());
            long contactId = dbUser.getLocalId();
            if (contactId == Contact.MY_ID) {
                return true;
            }
            // Local
            return localFileForContent(context, obj).exists();
        } catch (Exception e) {
            Log.w(TAG, "Error checking file availability", e);
            return false;
        }
    }

    private static class HashUtils {
        private static String convertToHex(byte[] data) {
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

    public static UUID getLocalBluetoothServiceUuid(Context c) {
        SharedPreferences prefs = c.getSharedPreferences("main", 0);
        if (!prefs.contains(PREF_CORRAL_BT_UUID)) {
            UUID btUuid = UUID.randomUUID();
            prefs.edit().putString(PREF_CORRAL_BT_UUID, btUuid.toString()).commit();
        }
        String uuidStr = prefs.getString(PREF_CORRAL_BT_UUID, null);
        return (uuidStr == null) ? null : UUID.fromString(uuidStr);
    }
}
