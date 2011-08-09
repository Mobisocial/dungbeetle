package edu.stanford.mobisocial.dungbeetle.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

public class BluetoothBeacon {
    public static final UUID NEAR_GROUPS = UUID.fromString("1aba3c40-c2a5-11e0-962b-0800200c9a66");
    public static final int NEAR_PORT = 27; // bluetooth uuid service discovery not working.
    private static final String TAG = "btbeacon";
    private static final boolean DBG = true;

    public static void share(final Activity context, final byte[] data, int duration) {
        new AcceptThread(context, data, duration).start();
    }

    public static void discover(Activity activity, BluetoothDevice device, OnDiscovered discovered) {
        try {
            //BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(NEAR_GROUPS);
            BluetoothSocket socket = null;
            try {
                Method listener = device.getClass().getMethod("createInsecureRfcommSocket", int.class);
                socket = (BluetoothSocket) listener.invoke(device, NEAR_PORT);
            } catch (Exception e) {
                if (DBG) Log.w(TAG, "Could not connect to channel.", e);
            }
        
            
            socket.connect();
            byte[] receivedBytes = new byte[2048];
            int r = socket.getInputStream().read(receivedBytes);
            byte[] returnBytes = new byte[r];
            System.arraycopy(receivedBytes, 0, returnBytes, 0, r);
            discovered.onDiscovered(returnBytes);
        } catch (IOException e) {
            toast(activity, "Couldn't connect to " + device.getName());
            Log.d(TAG, "failed bluetooth connection", e);
        }
    }

    private static void toast(final Activity context, final String text) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
        
    }

    public interface OnDiscovered {
        public void onDiscovered(byte[] data);
    }

    private static class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private final Activity mmContext;
        private final byte[] mmData;
        private final int mmDuration;

        private AcceptThread(Activity context, byte[] data, int duration) {
            mmContext = context;
            mmData = data;
            mmDuration = duration;

            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            int listeningPort = -1;
            try {
                tmp = getServerSocket(NEAR_PORT, NEAR_GROUPS);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Wait for one connection.
            int port = getBluetoothListeningPort(mmServerSocket);
            toast(mmContext, "Listening on " + port + "...");
            try {
                socket = mmServerSocket.accept(1000*mmDuration);
                toast(mmContext, "Connected!");
            } catch (IOException e) {
                toast(mmContext, "Accept() failed");
                if (DBG) Log.e(TAG, "accept() failed", e);
                return;
            }

            if (socket == null) {
                toast(mmContext, "Failed.");
                return;
            }

            doConnection(socket);

            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
            
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }

        private void doConnection(BluetoothSocket socket) {
            toast(mmContext, "Sending over bluetooth.");
            try {
                socket.getOutputStream().write(mmData);
                socket.getOutputStream().flush();
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error writing content", e);
            }
        }

        private int getBluetoothListeningPort(BluetoothServerSocket serverSocket) {
            try {
                Field socketField = BluetoothServerSocket.class.getDeclaredField("mSocket");
                socketField.setAccessible(true);
                BluetoothSocket socket = (BluetoothSocket)socketField.get(serverSocket);

                Field portField = BluetoothSocket.class.getDeclaredField("mPort");
                portField.setAccessible(true);
                int port = (Integer)portField.get(socket);
                return port;
            } catch (Exception e) {
                Log.d(TAG, "Error getting port from socket", e);
                return -1;
            }
        }

        private BluetoothServerSocket getServerSocket(int port, UUID service) 
                throws IOException {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (port > 0) {
                try {
                    Method listener = adapter.getClass().getMethod("listenUsingInsecureRfcommOn", int.class);
                    return (BluetoothServerSocket) listener.invoke(adapter, port);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            return adapter.listenUsingInsecureRfcommWithServiceRecord("mobinear", NEAR_GROUPS);
        }
    }
}
