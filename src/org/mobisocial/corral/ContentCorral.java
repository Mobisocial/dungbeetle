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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.VideoObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbContactAttributes;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.Feed;

public class ContentCorral {
    public static final String OBJ_MIME_TYPE = "mimeType";
	private static final int SERVER_PORT = 8224;
	private static final String TAG = "ContentCorral";
	private static final boolean DBG = true;
	private AcceptThread mAcceptThread;
	private Context mContext;

	public static final boolean CONTENT_CORRAL_ENABLED = false;

	public ContentCorral(Context context) {
	    mContext = context;
	}
	/**
	 * Starts the simple image server
	 */
	public synchronized void start() {
		if (mAcceptThread != null) return;
		
		String ip = getLocalIpAddress();
		if (ip == null) {
			Toast.makeText(mContext, "Image server failed to start. Are you on a wifi network?",
			        Toast.LENGTH_SHORT).show();
			return;
		}
		mAcceptThread = new AcceptThread(SERVER_PORT);
		mAcceptThread.start();
	}
	
	public synchronized void stop() {
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
	}
	
	private class AcceptThread extends Thread {
        // The local server socket
        private final ServerSocket mmServerSocket;

        public AcceptThread(int port) {
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
            //Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            Socket socket = null;

            // Listen to the server socket always
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	//Log.d(TAG, "waiting for client...");
                    socket = mmServerSocket.accept();
                    //Log.d(TAG, "Client connected!");
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
                
                ConnectedThread conThread = new ConnectedThread(socket);
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
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int BUFFER_LENGTH = 1024;

        public ConnectedThread(Socket socket) {
            //Log.d(TAG, "create ConnectedThread");

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
            
            if (mmInStream == null || mmOutStream == null) return;
            
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
         * cryptographically.
         */
        private void doGetRequest(String header) {
        	String[] headers = header.split("\r\n");
        	if (!headers[0].startsWith("GET ")) return;
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
                String filename = "img.jpg";
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
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
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

	private static Uri uriForContent(String host, String localContent) {
        Uri baseUri = Uri.parse("http://" + host + ":" + SERVER_PORT);
        return baseUri.buildUpon().appendQueryParameter("content", localContent).build();
	}

	public static Uri fetchContent(Context context, long contactId, JSONObject obj)
	        throws IOException {
	    if (!obj.has(PictureObj.LOCAL_URI)) {
	        return null;
	    }
	    if (contactId == Contact.MY_ID) {
	        try {
	            return Uri.parse(obj.getString(PictureObj.LOCAL_URI));
	        } catch (JSONException e) {
	            Log.e(TAG, "json exception getting local uri", e);
	            return null;
	        }
	    }

	    String ip = DbContactAttributes.getAttribute(context, contactId, Contact.ATTR_LAN_IP);
	    if (ip == null) {
	        return null;
	    }

	    try {
	        // Remote
	        Uri remoteUri = uriForContent(ip, obj.getString(PictureObj.LOCAL_URI));
    	    URL url = new URL(remoteUri.toString());
            if (DBG) Log.d(TAG, "Attempting to pull file " + remoteUri);

    	    // Local
            String suffix = suffixFor(obj);
    	    String fname = "sha-" + HashUtils.SHA1(remoteUri.toString()) + "." + suffix;
    	    File tmp = new File(context.getExternalCacheDir(), fname);
    	    if (!tmp.exists()) {
    	        try {
    	            InputStream is = url.openConnection().getInputStream();
        	        OutputStream out = new FileOutputStream(tmp);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
    	        } catch (IOException e) {
	                if (tmp.exists()) {
	                    tmp.delete();
	                }
    	            throw e;
    	        }
    	    }
    	    return Uri.parse("file://" + tmp.getAbsolutePath());
	    } catch (JSONException e) {
	        return null;
	    } catch (NoSuchAlgorithmException e) {
	        return null;
	    }
	}

	private static String suffixFor(JSONObject obj) {
	    if (obj.has(OBJ_MIME_TYPE)) {
	        String suffix = suffixForType(obj.optString(OBJ_MIME_TYPE));
	        if (suffix != null) {
	            return suffix;
	        }
	    }

	    // TODO: safe to remove.
        if (PictureObj.TYPE.equals(obj.optString(DbObjects.TYPE))) {
            return "jpg";
        } else if (VideoObj.TYPE.equals(obj.optString(DbObjects.TYPE))) {
            return "3gp";
        }
        return "tmp";
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

    public static boolean fileAvailableLocally(Context context, long contactId, JSONObject obj) {
	    try {
    	    String localIp = getLocalIpAddress();
            String ip = obj.getString(Contact.ATTR_LAN_IP);
            if (localIp != null && localIp.equals(ip)) {
                return true;
            }
    
            // Remote
            Uri remoteUri = uriForContent(
                    obj.getString(Contact.ATTR_LAN_IP), obj.getString(PictureObj.LOCAL_URI));

            // Local
            String suffix = suffixFor(obj);
            String fname = "sha-" + HashUtils.SHA1(remoteUri.toString()) + "." + suffix;
            File tmp = new File(context.getExternalCacheDir(), fname);
            return tmp.exists();
	    } catch (Exception e) {
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
}