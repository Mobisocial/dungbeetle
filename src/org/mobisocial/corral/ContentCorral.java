package org.mobisocial.corral;

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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PictureObj;

public class ContentCorral {
	private static final int SERVER_PORT = 8224;
	private static File CAMERA_ROOT = null;
	private static final String TAG = "imageserver";
	private AcceptThread mAcceptThread;
	private Context mContext;

	public static final boolean ENABLE_CONTENT_CORRAL = false;

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
            //Log.d(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytes;
            
            if (mmInStream == null || mmOutStream == null) return;
            
            // Read header information, determine connection type
            try {
            	bytes = mmInStream.read(buffer);
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
        	    } catch (IOException e) {}
        	    return;
        	}
        	Uri img = Uri.parse(targetUri.getQueryParameter("content"));

        	InputStream in;
        	try {
        		//img = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, imgId);
        		in = mContext.getContentResolver().openInputStream(img);
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
        		
        		in = mContext.getContentResolver().openInputStream(img);
        		String filename = "img.jpg";
        		mmOutStream.write(header("HTTP/1.1 200 OK"));
        		mmOutStream.write(header("Content-Type: " + mContext.getContentResolver().getType(img)));
        		mmOutStream.write(header("Content-Length: " + size));
        		//mmOutStream.write(header("Content-Disposition: attachment; filename=\""+filename+"\""));
        		mmOutStream.write(header(""));
        		
	        	
	        	while ((r = in.read(buffer)) > 0) {
	        		mmOutStream.write(buffer, 0, r);
	        	}
	        	
        	} catch (Exception e) {
        		Log.e(TAG, "Error sending file", e);
        	} finally {
        		try {
        			mmOutStream.close();
        		} catch (IOException e) {}
        	}
        }
        
        public void cancel() {
        	try {
        		mmSocket.close();
        	} catch (IOException e) {}
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

	/**
	 * Returns a Uri that can be used remotely to
	 * retrieve content from this device.
	 */
	public static Uri uriForContent(String localContent) {
	    String ip = getLocalIpAddress();
	    if (ip == null) return null;
	    return uriForContent(ip, localContent);
	}

	public static Uri uriForContent(String host, String localContent) {
        Uri baseUri = Uri.parse("http://" + host + ":" + SERVER_PORT);
        return baseUri.buildUpon().appendQueryParameter("content", localContent).build();
	}

	public static Uri fetchTempFile(Context context, JSONObject obj, String suffix) throws IOException {
	    if (!(obj.has(PictureObj.LOCAL_IP) && obj.has(PictureObj.LOCAL_URI))) {
	        return null;
	    }

	    try {
	        String localIp = getLocalIpAddress();
	        String ip = obj.getString(PictureObj.LOCAL_IP);
	        if (localIp != null && localIp.equals(ip)) {
	            return Uri.parse(obj.getString(PictureObj.LOCAL_URI));
	        }

	        // Remote
	        Uri remoteUri = uriForContent(
	                obj.getString(PictureObj.LOCAL_IP), obj.getString(PictureObj.LOCAL_URI));
    	    URL url = new URL(remoteUri.toString());
    	    InputStream is = url.openConnection().getInputStream();

    	    // Local
    	    String fname = "sha-" + HashUtils.SHA1(remoteUri.toString()) + "." + suffix;
    	    File tmp = new File(context.getExternalCacheDir(), fname);
    	    if (!tmp.exists()) {
    	        try {
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