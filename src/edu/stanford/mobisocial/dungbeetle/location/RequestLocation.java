package edu.stanford.mobisocial.dungbeetle.location;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.stanford.mobisocial.dungbeetle.DBHelper;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class RequestLocation extends Service {
	
	// ---- KEYS -- //
	private BigInteger k_1 = BigInteger.valueOf(0); // this key is equal to the AES encryption of ( time concatenated with 1 )
	private BigInteger k_2 = BigInteger.valueOf(0); // this key is equal to the AES encryption of ( time concatenated with 2 )
	private Long myID = 0L; // my ID/public key
	
	
	// ----- CONSTANTS ------//
	private static final BigInteger PRIME = BigInteger.valueOf(28147497699961L);
	private static final String IP_ADDRESS = "184.106.71.177";
	
	//-----VARIABLES------//
	private Map<Long, ArrayList<BigInteger>> myLocationsMap = new HashMap<Long, ArrayList<BigInteger>>();
	private Set<Long> friendsList;
	private byte[] myPublicKey;
	private Map <Long, byte[]> idToSharedKeyMap;

	// the client class that will interact with the server (self implemented class)
	private ClientClass mClient; 
	
	@Override
	public void onCreate() {
			      
	    mClient = new ClientClass(IP_ADDRESS, 8080);
	    /*Connect and retry every 5 seconds*/
	    int numRetries = 10;
	    if (!mClient.connectMe()){
	    	for (int i = 0; i < numRetries; i++){
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
	    // Generate set of friend ids/public keys (Longs) to send to the server
	    final DBHelper dbh = new DBHelper(this);
	    friendsList = dbh.getPublicKeyPrints();
	    myID = dbh.getPublicKeyPrint();
	    idToSharedKeyMap = dbh.getPublicKeyPrintSharedSecretMap();
		/* display the latitude and longitude on the screen (mostly for debugging purposes)
	     mDisplay.setText(
	              new StringBuilder()
	                      .append("Latitude: ").append(pad(latitude)).append("\nLongitude: ").append(pad(longitude)));
		 */
   
	    Thread AThread = new Thread() {
	    	public void run() {
	    		for(;;) {
	    			iteration();
	    			try {
						Thread.sleep(1000 * 60);
					} catch (InterruptedException e) {}
	    		}
	    	}
	    	public void iteration() {
	    	
	    	   // System.err.println("Friends to send to server. Number of friends: " + friendsList.size());
	    	    for (Long f:friendsList){
	    	    	//System.err.println("Friend: " + f.longValue());
	    	    }
	    	      
	    	    // Send friend IDs to the server
	    	    mClient.sendReadyPacket(myID, friendsList);
	    			
	    	    // Receive info from the server -- get time byte and grid size corresponding to each friend ID:
	    	    Map<Long, ArrayList<Byte>> response = mClient.receiveInitialResponse();
	    	    
	    	    if (response != null)
	    	    {
	    		    Map<Long, ArrayList<BigInteger>> k_2Array = new HashMap<Long, ArrayList<BigInteger>>();     
	    		    
	    		    // Set up helper variables
	    		    GridHandler gridHandler = new GridHandler();
	    		    ArrayList<Byte> timeGridBytes;
	    		    int gridsize;
	    		    Byte timeByte;
	    		    AESKeyGenerator keygen = new AESKeyGenerator();  // Create a key generator 
	    		    Date curDate;
	    		    Long toEncrypt;     
	    		    String dhkey;

	    	    	for(Long friend:friendsList){
	    	    		
	    		    	//check if friend is in the response --> if not, friend is not available, so show as not nearby
	    	    		//only put into list if friend is in map
	    	    		if (response.containsKey(friend)){
	    	    			// Retrieve raw value of time and grid
	    	    			timeGridBytes = response.get(friend);
	    	    			//System.err.println("Friend that I'm about to send a location for: id=" + friend);
	    	    		
	    	    			// Separate bytes of time and grid
	    	    			timeByte = timeGridBytes.get(0);
	    	    			gridsize = timeGridBytes.get(1);
	    	    			int grid_toOR;
	    	    			for (int j = 2; j < 5; j++) {
	    	    				gridsize = gridsize << 8;
	    	    				grid_toOR = timeGridBytes.get(j);
	    	    				grid_toOR = grid_toOR & 0x000000FF;
	    	    				gridsize = gridsize | grid_toOR;
	    	    			}
	    	    			//System.err.println("Gridsize of this friend: " + gridsize + "\tTime byte: " + timeByte);
	    	    	  
	    	    			// Create the locations converted to the grid size and for every overlapping grid color/type
	    	    			ArrayList<BigInteger> myLocs = new ArrayList<BigInteger>();
	    	    			myLocs = gridHandler.getGridCoords(getApplicationContext(), gridsize);
	    		      
	    	    			// Create a place to store all three k_2's
	    	    			ArrayList<BigInteger> three_k_2 = new ArrayList<BigInteger>();
	    	    			
	    	    			// Get Diffie-Helman key corresponding to each friend id
	    	    			dhkey = new BigInteger(idToSharedKeyMap.get(friend)).toString();
	    		      
	    	    			// Calculate what to encrypt (time that B-side also encrypted). So here, we match the times of A and B
	    	    			curDate = new Date();
	    	    			toEncrypt = ((curDate.getTime()) >> 12);
	    	    			toEncrypt = toEncrypt & 0xFFFFFFFFFFFFFF00L; // Get rid of the least significant byte
	    	    			long toOR = timeByte.longValue();
	    	    			toOR = toOR & 0x00000000000000FFL; 
	    	    			toEncrypt = toEncrypt | toOR; // Add on the time byte that B sent to completely synchronize the times
	    	    			//Log.d("UpdateLocation.java","A-side, time to encrypt: " + toEncrypt);
	    	    			
	    	    			// k_1 is different for each friend and each overlapping grid
	    	    			for (int j = 0; j < 3; j++){
	    	    				k_1 = new BigInteger (keygen.generate_k(  dhkey, (Long.toString( (toEncrypt*10) + (2*j + 1)) )  )   ); // generate k_1 by encrypting time concatenated with 1
	    	    				BigInteger k_2_temp = new BigInteger (keygen.generate_k(dhkey, (Long.toString( (toEncrypt*10) + (2*j + 2) )) ) );
	    	    				k_2_temp = k_2_temp.mod(PRIME);
	    	    				three_k_2.add( k_2_temp ); // generate k_2 and store it in an array for later usage
	    	    				//Log.d("UpdateLocation.java","k_1: " + k_1 + "\tk_2: " + k_2_temp);
	    	    				myLocs.set( j, (k_1.add(myLocs.get(j))).mod(PRIME) );  
	    	    			}
	    	    			
	    	    			k_2Array.put(friend, three_k_2); // Store all three k_2s that correspond to the friend ID.
	    		      
	    	    			// Put the converted and encrypted overlapping grid in the map that will be sent to the server 
	    	    			myLocationsMap.put(friend, myLocs);
	    	    		}
	    	    		else{
	    	    			dbh.setNearby(friend, false);
	    	    		}
	    	    	}
	    	    
	    	    	//System.err.println ("About to send the server packet A3");
	    	    	// Send server my info corresponding to the grid size of each friend
	    	    	mClient.sendClosenessPacket(myLocationsMap, myID);
	    	    	
	    	    	// Receive response from server
	    	    	Map<Long, ArrayList<BigInteger>> finalResponse = mClient.receiveFinalResponse();
	    	    	boolean nearby = false;
	    	    	int keyIndex = 0;
	    	    	if (finalResponse != null)
	    	    	{
	    	    		for (Long friendID: myLocationsMap.keySet())
	    	    		{
	    	    			if (finalResponse.containsKey(friendID))
	    	    			{
	    	    				ArrayList<BigInteger> tempGrids = finalResponse.get(friendID);
	    	    				if (tempGrids != null)
	    	    				{
	    	    					keyIndex = 0;
	    	    					for (Iterator<BigInteger> g = tempGrids.iterator(); g.hasNext() && !nearby;)
	    	    					{
	    	    						BigInteger loc = g.next();
	    	    						ArrayList<BigInteger> keys = k_2Array.get(friendID);
	    	    						//Log.d("UpdateLocation.java", "Final response to match up with k_2: loc: " + loc + "\t key: " + keys.get(keyIndex));
	    	    						
	    	    	    				if (  (loc).equals( keys.get(keyIndex) )  ) // If the final response is k_2, then the friend is nearby
	    	    	    				{
	    	    	    					nearby = true;
	    	    	    					//Log.d("UpdateLocation.java", "Final response that matched up: " + loc);
	    	    	    				}
	    	    	    				else
	    	    	    					nearby = false;
	    	    	    			
	    	    	    				keyIndex++;
	    	    					}
	    	    				}
	    	    				else { nearby = false; }
	    	    			}
	    	    			else { nearby = false; }
	    	    			
	    	    			// Before moving on to the next friend, update the UI to display current friend's availability
	    	    			if (nearby)
	    	    				dbh.setNearby(friendID, true);
	    	    			else
	    	    				dbh.setNearby(friendID, false);
	    	    		}
	    	    	} // end of if statement with finalResponse
	    	    	else
	    	    	{
	    	    		for (Long key: myLocationsMap.keySet())
	    	    			dbh.setNearby(key, false);
	    	    	}
	    	    } // end of if statement with initial response
	    	    
	    	    else // If initial response message is null, it means all friends are not available
	    	    { 
		    	    for (Long friend:friendsList){
		    	    	//System.err.println("ID of Friend who isn't available: " + friend);
		    	    	dbh.setNearby(friend, false);
		    	    }
	    	    }
	    	}
	    	
	    };
		
	    AThread.start();
	    
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}


