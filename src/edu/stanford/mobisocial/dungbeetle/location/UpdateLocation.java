package edu.stanford.mobisocial.dungbeetle.location;


/* Changes needed:
 * 1. Unique ID to identify user. 
 * 2. Add Friends IDs
 */

//***************************** UpdateLocation.java *********************************** //
//This class is a service that is started up as soon as the app is started up. It will
//retrieve the phone's GPS coordinates and store them in a "preference" so that every
//class within this app can access them. The GPS coordinates are stored converted to
//coordinates on a hexgon grid that is user configurable and the latitude and  
//longitude are concatenated together.
//This specific class does the following calculation (this class does "Bob's" side of
//the protocol):
//			 b = k_2 + r (l + k_1), 
//where k_1 and k_2 are the keys shared between two users, r is a random number
//generated using AES, and l is the converted and concatenated location.
//*********************************************************************************** //

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import edu.stanford.mobisocial.dungbeetle.DBHelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class UpdateLocation extends Service {
	
	// ---- KEY VALUES ---- //
	private BigInteger k_1 = BigInteger.valueOf(11); // this key is equal to the AES encryption of ( time concatenated with 1 )
	private BigInteger k_2 = BigInteger.valueOf(7); // this key is equal to the AES encryption of ( time concatenated with 2 )
	private BigInteger randVal = BigInteger.valueOf(113); // this is the random number generated via AES
	private Long myID = 0L;
	
	// --- CONSTANTS --- //
	private static final long INTERVAL = 900000; // Interval in which to send encrypted location to server, in milliseconds (900000 = 15 minutes)
	private static final BigInteger PRIME = BigInteger.valueOf(28147497699961L);
	private static final int GRID_SIZE = 1000; // 1000 feet is default
	private static final String IP_ADDRESS = "184.106.71.177";

	
	//------SAVED PREFERENCES-------//
	private float minDistanceMeters = 10; // the minimum distance interval, in meters, for notifications
	private long minTimeMillisec = 5000; // the mimimum time interval, in milliseconds, for notifications (15 minutes)
	private int gridsize; // grid size that the user defines

	// ----- VARIABLES ----- //
	// Android class that allows us to retrieve the GPS coordinates of phone
	private LocationManager myLocator;
	private ClientClass mClient;
	private static final String TAG = "MyService";
	private static double latitude;
	private static double longitude;
	private BigInteger gridLocation[] = new BigInteger[3];
	private ArrayList<BigInteger> tempLoc = new ArrayList<BigInteger>();
	private Timer sendTimer = new Timer();
	private Set<Long> friendsList;
	private Map <Long, byte[]> idToSharedKeyMap;
	
	
	//private ExtendedCheckBoxListAdapter friendsList; // list of friends that is stored globally
	private Date curDate;
	private byte[] serverKey;
	
	/* Use if you want to update whenever location changes.*/
	
	private LocationListener locationUpdater = new LocationListener() {
		public void onLocationChanged(Location location) {
			Log.d("UpdateLocation.java","Location has changed. About to update location to server.");
			if (location != null)
			{
				
				latitude=location.getLatitude(); 
				longitude=location.getLongitude();
				System.err.println("Location is not null. Latitude: " + latitude + "\t Longitude: " + longitude);
				
			    // Saved Preferences method of storing variables
			    // place in random locations for gps locations
			    SharedPreferences savedLatitude = getSharedPreferences("savedLatitude", MODE_PRIVATE); 
			    SharedPreferences.Editor latitudeEditor = savedLatitude.edit();
			    latitudeEditor.putString("savedLatitude", Double.toString(latitude));
			    latitudeEditor.commit();
			    SharedPreferences savedLongitude = getSharedPreferences("savedLongitude", MODE_PRIVATE);
			    SharedPreferences.Editor longitudeEditor = savedLongitude.edit();
			    longitudeEditor.putString("savedLongitude", Double.toString(longitude));
			    longitudeEditor.commit();
				

				Thread BThread = new Thread() {
					public void run()
					{
						sendLocation();
					}
				};
				 
				BThread.start();
			    
			}
		}
		public void onProviderDisabled(String provider) {
			// required since methods defined by Android were abstract
		}
		public void onProviderEnabled(String provider) {
			// required since methods defined by Android were abstract
		}
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// required since methods defined by Android were abstract
		}
	};

	private void sendLocation()
	{
		// get grid size
	    gridsize = GRID_SIZE; // default is 300 FEET
	    
	    Log.d("UpdateLocation.java", "The grid size is: " + gridsize);
		
		// generate locations that are converted to the grid size
		GridHandler locHelper = new GridHandler();
		tempLoc = locHelper.getGridCoords(getApplicationContext(), gridsize); // create grid-converted location for all three grid types
		for (int i = 0; i < 3; i++)
		{
			if ( gridLocation[i].compareTo(tempLoc.get(i)) != 0 ) {
				gridLocation[i] = tempLoc.get(i);
			}
		}
		
		// Stuff to send the server: 
		ArrayList<Long> friendIDs = new ArrayList<Long>(); // list of friend IDs to send server
		ArrayList<BigInteger> friendLocations = new ArrayList<BigInteger>();
		
		// Get time and time byte
		curDate = new Date();
		// In this next step, I throw away 12 bits of resolution. In essence, I'm dividing the time by 2^12 (4096) and
		// so I'm losing precision, which is good since I'm sending only the last byte (after the shift) of time. Also, 
		// because of this lost of precision, I am restricting updating my location to not more than once every 5 seconds.
		long toEncrypt = ( curDate.getTime() ) >> 12;  
		
		Log.d("UpdateLocation.java","B-side, time to encrypt: " + toEncrypt);
		
		// Store the most least significant byte of the shifted-by-12-time.
		Byte timeToSend = new Byte ( (byte) ( toEncrypt & 0xFF) );
		
		Log.d("UpdateLocation.java", "Generating and sending keys...");
		
		// Create a key generator (that I created)
		AESKeyGenerator keygen = new AESKeyGenerator();
		
		String dhkey; // declare the string that will hold the Diffie Helman key

		int friendCount = 0; // to keep track of how many friends there are who actually have a diffie helman key
		// calculate the encrypted location for each friend
		for (Long friend:friendsList){
			dhkey = idToSharedKeyMap.get(friend).toString();
			Log.d("UpdateLocation.java","FriendID: " + idToSharedKeyMap.get(friend) + " with key: " + dhkey);
			if (dhkey != null){
				friendIDs.add(friend); // put the array in the map
				// Generate k_1 and k_2 using toEncrypt variable, each friend has a different k_1 and k_2 and each round in the loop is a different k_1 and k_2
				// so three pairs of k_1 and k_2 per friend and a different random value for each round in the loop and for each friend so 3 different randVals per friend 
				// for now we will use the same k_1 and k_2
				for (int j = 0; j < 3; j++){
					k_1 = new BigInteger (keygen.generate_k(dhkey, (Long.toString( (toEncrypt*10) + (2*j + 1) )) ) ); // generate k_1 by encrypting time concatenated with 1
					k_2 = new BigInteger (keygen.generate_k(dhkey, (Long.toString( (toEncrypt*10) + (2*j + 2) )) ) ); // generate k_1 by encrypting time concatenated with 2

					randVal = new BigInteger (keygen.generate_r(serverKey, Long.toString(toEncrypt) + Long.toString((3*friendCount) + j + 1) ) ); // generate the random value, r, by encrypting time concatenated with 1
					System.err.println ("to encrypt before concat: " + Long.toString(toEncrypt));
					System.err.println ("to encrypt for r: "  + Long.toString(toEncrypt) + Long.toString((3*friendCount) + j + 1) );
					// Note: We generate k_1, k_2, and randVal three times per location because of the three grid colors.
					Log.d("UpdateLocation.java","k_1: " + k_1 + "\t\t k_2: " + k_2.mod(PRIME));
					Log.d("UpdateLocation.java", "random value, r: "+randVal.toString());
					friendLocations.add((k_2.add( randVal.multiply(gridLocation[j].add(k_1)) )).mod(PRIME) );
				}
				friendCount++;
			}
		}
		
		System.err.println ("UpdateLocation GRID SIZE: " + gridsize);
		
		    
		if (mClient.sendCurrentLocation(myID, timeToSend, friendIDs, friendLocations, gridsize) && myID != 0)
		{
			Log.d("UpdateLocation.java", "Encrypted location has been sent to the server.");
			//Toast.makeText(getApplicationContext(), "Encrypted location has been sent to the server.", Toast.LENGTH_LONG).show();
		}
		
	}

	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		Toast.makeText(this, "Generating my encrypted location..", Toast.LENGTH_LONG).show();
		Log.d(TAG, "Within onCreate of UpdateLocation.java");

		DBHelper dbh = new DBHelper(this);
		// Initialize the list of friends
		friendsList = dbh.getPublicKeyPrints();
		idToSharedKeyMap = dbh.getPublicKeyPrintSharedSecretMap();

		
		 mClient = new ClientClass(IP_ADDRESS, 8080);
		
		 if (!mClient.connectMe()){
			 //TODO: should back off from a short retry delay to a longer one and 
			 //just keep doing that over time
		   	for (int i = 0; i < 10; i++){
	    		try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    		if (mClient.connectMe())
	    			break;
		    }
		   	stopSelf();
		 }
		// Get my id from saved pref
		SharedPreferences mySavedID = getSharedPreferences("myID", MODE_PRIVATE); 
	    myID = mySavedID.getLong("myID", 0L);
		
		for (int i = 0; i < 3; i++)
			gridLocation[i] = BigInteger.valueOf(0);
		
		myLocator = (LocationManager) 
		getSystemService(Context.LOCATION_SERVICE);    
		myLocator.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 
				minTimeMillisec, 
				minDistanceMeters, 
				locationUpdater);
		
		// Send shared key with server to server
		// generate 128 bit random value
		curDate = new Date();
		Long seed = new Long (curDate.getTime());
		byte[] seed_bytes = new byte[8];
		for (int i = 0; i < 8; i++)
		{
			seed_bytes[7-i] = (byte)(seed >>> (i*8));
		}
		SecureRandom randGenerator = new SecureRandom(seed_bytes);
		serverKey = new byte[16];
		randGenerator.nextBytes(serverKey);
		System.err.println("ServerKey sent to Server: " + new BigInteger(serverKey).toString());
		// send it to the server using client class's function sendSharedKey
		mClient.sendSharedKey(myID, serverKey);
		//Toast.makeText(this, "Shared key sent to server", Toast.LENGTH_LONG).show();
		Log.d("UpdateLocation.java", "Shared key sent to server.");
		
		// More or less tell the server that we are still alive, just haven't moved
		
		sendTimer.schedule(new TimerTask() {
			public void run() {
				sendLocation();
			}
		}, 0, INTERVAL);
		
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Sending encrypted locations stopped.", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onDestroy");
		//player.stop();
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		


	}
	

}