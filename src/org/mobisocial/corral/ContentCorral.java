
package org.mobisocial.corral;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.UUID;

import mobisocial.comm.BluetoothDuplexSocket;
import mobisocial.comm.DuplexSocket;
import mobisocial.comm.StreamDuplexSocket;
import mobisocial.socialkit.musubi.DbObj;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

public class ContentCorral {
    private static final String PREF_CORRAL_BT_UUID = "corral_bt";
    static final int SERVER_PORT = 8224;
    private static final String BT_CORRAL_NAME = "Content Corral";

    private static final String TAG = "ContentCorral";
    @SuppressWarnings("unused")
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
        mBluetoothAcceptThread.start();
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

    public static Uri storeContent(Context context, Uri contentUri) {
        return storeContent(context, contentUri, context.getContentResolver().getType(contentUri));
    }

    public static Uri storeContent(Context context, Uri contentUri, String type) {
        File contentDir = new File(context.getExternalCacheDir(), "local");
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        String ext = CorralClient.extensionForType(type);
        String fname = timestamp + "-" + contentUri.getLastPathSegment() + "." + ext;
        File copy = new File(contentDir, fname);
        try {
            contentDir.mkdirs();
            InputStream in = context.getContentResolver().openInputStream(contentUri);
            BufferedInputStream bin = new BufferedInputStream(in);
            byte[] buff = new byte[1024];
            OutputStream out = new FileOutputStream(copy);
            int r;
            while ((r = bin.read(buff)) > 0) {
                out.write(buff, 0, r);
            }
            out.close();
            bin.close();
            in.close();
            return Uri.fromFile(copy);
        } catch (IOException e) {
            Log.w(TAG, "Error copying file", e);
            if (copy.exists()) {
                copy.delete();
            }
            return null;
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
                    if (DBG) Log.d(TAG, "corral waiting for client...");
                    socket = mmServerSocket.accept();
                    if (DBG) Log.d(TAG, "corral client connected!");
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
                try {
                    if (DBG) Log.d(TAG, "Bluetooth corral listening on " +
                            adapter.getAddress() + ":" + coralUuid);
                    tmp = adapter.listenUsingRfcommWithServiceRecord(
                            BT_CORRAL_NAME, coralUuid);
                } catch (NoSuchMethodError e) {
                    // Let's not deal with pairing UI.
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not open bt server socket");
                e.printStackTrace(System.err);
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "Bluetooth Corral not available for this Android version.");
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
                    if (DBG) Log.d(TAG, "Corral bluetooth server waiting for client...");
                    socket = mmServerSocket.accept();
                    if (DBG) Log.d(TAG, "Corral bluetooth server connected!");
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
                CorralConnectedThread conThread = new CorralConnectedThread(duplex);
                conThread.start();
            }
            Log.d(TAG, "END mAcceptThread");
        }

        @SuppressWarnings("unused")
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
                if (DBG) Log.d(TAG, "read " + bytes + " header bytes");
                String header = new String(buffer, 0, bytes);
                if (DBG) Log.d(TAG, header);
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
                    mmOutStream.write(header("HTTP/1.1 404 NOT FOUND\r\n\r\n"));
                    mmOutStream.close();
                } catch (IOException e) {
                }
                return;
            }
            if (targetUri.getQueryParameter("hash") == null) {
                try {
                    mmOutStream.write(header("HTTP/1.1 401 UNAUTHORIZED\r\n\r\n"));
                    mmOutStream.close();
                } catch (IOException e) {
                }
                return;
            }

            // Verify the hash is for an obj with the given filepath.
            // TODO: This is not secure. Require challenge/response authentication.
            Long hash = Long.parseLong(targetUri.getQueryParameter("hash"));
            String contentPath = targetUri.getQueryParameter("content");

            DbObj obj = App.instance().getMusubi().objForHash(hash);
            if (obj == null) {
                try {
                    mmOutStream.write(header("HTTP/1.1 410 GONE\r\n\r\n"));
                    mmOutStream.close();
                } catch (IOException e) {
                }
                return;
            }

            String localPath = obj.getJson().optString(CorralClient.OBJ_LOCAL_URI);
            if (!contentPath.equals(localPath)) {
                try {
                    mmOutStream.write(header("HTTP/1.1 400 BAD REQUEST\r\n\r\n"));
                    mmOutStream.close();
                } catch (IOException e) {
                }
                return;
            }

            // OK to download:
            Uri requestPath = Uri.parse(contentPath);
            String scheme = requestPath.getScheme();
            if ("content".equals(scheme) || "file".equals(scheme)) {
                if (DBG) Log.d(TAG, "Retrieving for " + requestPath.getAuthority());
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
                String type = mContext.getContentResolver().getType(requestPath);
                if (type == null) {
                    int p = requestPath.toString().lastIndexOf(".");
                    if (p > 0) {
                        String ext = requestPath.toString().substring(p + 1);
                        type = CorralClient.typeForExtension(ext);
                    }
                }
                in = mContext.getContentResolver().openInputStream(requestPath);
                mmOutStream.write(header("HTTP/1.1 200 OK"));
                if (type != null) {
                    mmOutStream.write(header("Content-Type: " + type));
                }
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
    private class CorralConnectedThread extends Thread {
        private final DuplexSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int BUFFER_LENGTH = 1024;

        public CorralConnectedThread(DuplexSocket socket) {
            if (DBG) Log.d(TAG, "create CorralConnectedThread");

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
            Log.d(TAG, "BEGIN CorralConnectedThread");
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytes;

            if (mmInStream == null || mmOutStream == null)
                return;

            // Read header information, determine connection type
            try {
                PosiServerProtocol protocol = new PosiServerProtocol(mmSocket);
                CorralRequestHandler handler = protocol.getRequestHandler();

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

                /**
                 * Your task is to find out which friends are nearby.
                 * We're just going to try to connect to all of their
                 * CORRAL_BLUETOOTH ports and send a quick HELLO.
                 * 
                 * First visual is to show this in a "nearby" list.
                 * We'll easily up-convert to groups.
                 * 
                 * Dumb algorithm for now just iterates over MACs and
                 * tries to connect, following protocol.
                 */

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

    public static UUID getLocalBluetoothServiceUuid(Context c) {
        SharedPreferences prefs = c.getSharedPreferences("main", 0);
        if (!prefs.contains(PREF_CORRAL_BT_UUID)) {
            UUID btUuid = UUID.randomUUID();
            prefs.edit().putString(PREF_CORRAL_BT_UUID, btUuid.toString()).commit();
        }
        String uuidStr = prefs.getString(PREF_CORRAL_BT_UUID, null);
        return (uuidStr == null) ? null : UUID.fromString(uuidStr);
    }

    static class PosiServerProtocol {
        public static final int POSI_MARKER = 0x504f5349;
        public static final int POSI_VERSION = 0x01;

        static SecureRandom sSecureRandom;
        private final DuplexSocket mmDuplexSocket;

        public PosiServerProtocol(DuplexSocket socket) {
            if (sSecureRandom == null) {
                sSecureRandom = new SecureRandom();
            }
            mmDuplexSocket = socket;
        }

        private byte[] getHeader() {
            byte[] header = new byte[16];
            ByteBuffer buffer = ByteBuffer.wrap(header);
            buffer.putInt(POSI_MARKER);
            buffer.putInt(POSI_VERSION);
            buffer.putLong(sSecureRandom.nextLong());
            return header;
        }

        // TODO:
        public CorralRequestHandler getRequestHandler() throws IOException {
            if (DBG) Log.d(TAG, "Getting request handler for posi session");
            OutputStream out = mmDuplexSocket.getOutputStream();
            byte[] header = getHeader();
            if (DBG) Log.d(TAG, "Writing header " + new String(header));
            out.write(header);
            if (DBG) Log.d(TAG, "Flushing header bytes");
            out.flush();
            if (DBG) Log.d(TAG, "Done writing header.");
            /**
             * TODO:
             * SignedObj obj = ObjDecoder.decode(readObj())
             * Authenticate signer and select protocol.
             *   Authentication verifies nonce and ensures timestamp is more
             *   recent than the users' last transmitted obj's timestamp.
             */
            return new NonceRequestHandler();
        }
    }

    /**
     * The trivial request handler that sends a nonce and hangs up.
     *
     */
    static class NonceRequestHandler implements CorralRequestHandler {
        // Does nothing.
    }

    interface CorralRequestHandler {
    }
}
