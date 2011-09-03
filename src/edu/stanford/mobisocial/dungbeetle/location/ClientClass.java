package edu.stanford.mobisocial.dungbeetle.location;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import android.util.Log;

public class ClientClass {

	private String uri;
	private int myKey = 221; // TEMPORARY BOGUS VALUE. TODO: generate my own key and upload to Facebook...?
	private HttpClient client;
	private PostMethod postMethod;
	private static final int TRANSMIT_SIZE = 6;
	private static final BigInteger serverPK = BigInteger.valueOf(65537);
	private static final BigInteger prime = BigInteger.valueOf(28147497699961L);
	private static final BigInteger RSAmod = new BigInteger("12694219218781329705748652330064241" +
			"32876567060377252671965562728778379597445920850187463" +
			"64753954450262954027491132190217804197573118748199754481792479239949" +
			"265338550227737039760729449292181953594614276814295738904266460590930337991640368023335931" +
			"284127132839615665571953261183977860758358803259102062236595411");
	
	// --- Constructor --- //
	public ClientClass(String IP_address, int port)
	{
		uri = "http://"+ IP_address +":" + Integer.toString(port) + "/detectionserver/DetectionServer";
		connectMe();
		Log.d("ClientClass.java", "Connected to server");
	}
	
	// --- Connect to the server in order to send data --- //
	private boolean connectMe()
	{
		client = new HttpClient();
		postMethod = new PostMethod(uri);
		if (client == null)
			return false;
		else return true;
	}
	
	// --- Disconnect from the server. Useful for quitting the app --- //
	public void disconnectMe()
	{
		postMethod.releaseConnection();
		Log.d("ClientClass.java", "Disconneccted from the server");
	}
	
	//sends packet to server as a http post method
	private boolean sendPacketToServer(byte[] packet)
	{
		try{
			postMethod.removeRequestHeader("content-length");
			RequestEntity request = new ByteArrayRequestEntity(packet);
			postMethod.setRequestEntity(request);
			client.executeMethod(postMethod);
		}
		catch(IOException e){
			return false;
		}
		return true;
	}
	
	public boolean sendSharedKey (long myID, byte[] sharedKey)
	{
		try{
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSAmod,serverPK);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey pk = fact.generatePublic(keySpec);
			Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, pk);
			byte[] encryptedData = cipher.doFinal(sharedKey);
			//put encrypted data into packet with packet type, id, and data to send to server
			byte[] packet = new byte[9+encryptedData.length];
			System.err.println("Packet length of the server shared key and user id: " + packet.length);
			packet[0] = new Byte("7").byteValue();
			int startIndex = 1;
			//next 8 bytes are myID
			BigInteger convertToByte = BigInteger.valueOf(myID);
			byte[] byteRep = convertToByte.toByteArray();
			if (byteRep.length < 8){
				for (int i = 0; i < (8-byteRep.length); i++){
					packet[startIndex] = new Byte("0").byteValue();
					startIndex++;
				}
			}
			for (int i = 0; i < byteRep.length; i++){
				packet[startIndex] = byteRep[i];
				startIndex++;
			}
			for (int i = 0; i < encryptedData.length; i++){
				packet[startIndex] = encryptedData[i];
				startIndex++;
			}
			boolean result = sendPacketToServer(packet);
			Log.d("ClientClass.java", "Shared key sent to the server");
			return result;
		}
		catch(Exception e){
			System.err.println("Exception in shared key: " + e.toString());
			return false;
		}
	}
	/* Server know that A is ready and wants to find closeness for a set of friends 
	 * (first packet that A side sends) Packet Type: 1
	 */
	public boolean sendReadyPacket(long myID, Set<Long> friendIDs)
	{  
		//1 byte to indicate packet type, 8 bytes for myID and 8 bytes for each friendID
		int size = 9 + friendIDs.size()*8;
		byte[] packet = new byte[size];
		//first byte indicates packet type
		packet[0] = new Byte("1").byteValue();
		int startIndex = 1;
		//next 8 bytes are myID
		BigInteger convertToByte = BigInteger.valueOf(myID);
		byte[] byteRep = convertToByte.toByteArray();
		if (byteRep.length < 8){
			for (int i = 0; i < (8-byteRep.length); i++){
				packet[startIndex] = new Byte("0").byteValue();
				startIndex++;
			}
		}
		for (int i = 0; i < byteRep.length; i++){
			packet[startIndex] = byteRep[i];
			startIndex++;
		}
		//iterate through friendID set and convert each long into a byte array and put into packet
		for (Long friend:friendIDs){
			//System.err.println ("in client class, the friend id is: " + friendID[i]);
			BigInteger friendToByte = BigInteger.valueOf(friend);
			byte[] byteArray = friendToByte.toByteArray();
			if (byteArray.length < 8){
				for (int j = 0; j < (8-byteArray.length); j++){
					packet[startIndex] = new Byte("0").byteValue();
					startIndex++;
				}
			}
			for (int j = 0; j < byteArray.length; j++){
				packet[startIndex] = byteArray[j];
				startIndex++;
			}
		}
		boolean result = sendPacketToServer(packet);
		Log.d("ClientClass.java", "Friend IDs (packet A1) sent to the server");
		return result;
	}
	/* A sends location for every friend she wants to request closeness 
	 * (2nd packet that A sends to the server) key is friend ID and value is arraylist of his/her masked location
	 * corresponding shared k1 with the friend (Packet Type 3)
	 */
	public boolean sendClosenessPacket(Map<Long, ArrayList<BigInteger>> locations, long myID)
	{
		/* 1 byte for packet type, 8 bytes for myID, 8 bytes for each friend ID, and 6 bytes for each BigInteger in the map arrayList
		 * (we know there are 3 grids, so arraylist is size 3)
		 */
		int size = 9 + locations.keySet().size()*(8+3*TRANSMIT_SIZE);
		//go through map once to get total number of bytes needed
		byte[] packet = new byte[size];
		//first byte indicates packet type
		packet[0] = new Byte("3").byteValue();
		int startIndex = 1;
		//next 8 bytes are myID
		BigInteger convertToByte = BigInteger.valueOf(myID);
		byte[] byteRep = convertToByte.toByteArray();
		if (byteRep.length < 8){
			for (int i = 0; i < (8-byteRep.length); i++){
				packet[startIndex] = new Byte("0").byteValue();
				startIndex++;
			}
		}
		for (int i = 0; i < byteRep.length; i++){
			packet[startIndex] = byteRep[i];
			startIndex++;
		}
		Iterator<Long> it = locations.keySet().iterator();
		while(it.hasNext()){
			Long key = it.next();
			long keyLong = key.longValue();
			ArrayList<BigInteger> locationValues = locations.get(key);
			//friendID first
			BigInteger friendToByte = BigInteger.valueOf(keyLong);
			byte[] byteArray = friendToByte.toByteArray();
			if (byteArray.length < 8){
				for (int j = 0; j < (8-byteArray.length); j++){
					packet[startIndex] = new Byte("0").byteValue();
					startIndex++;
				}
			}
			for (int j = 0; j < byteArray.length; j++){
				packet[startIndex] = byteArray[j];
				startIndex++;
			}
			for (int i = 0; i < locationValues.size(); i++){
				BigInteger loc = locationValues.get(i);
				byte[] locBytes = loc.toByteArray();
				//byte arrays are done using big endian, so add 0 bytes to front if less than 6 (TRANSMIT_SIZE) bytes
				if (locBytes.length < TRANSMIT_SIZE){					
					for (int j = 0; j < (TRANSMIT_SIZE - locBytes.length); j++){
						packet[startIndex] = new Byte("0").byteValue();
						startIndex++;
					}
				}
				for (int j = 0; j < locBytes.length; j++){
					packet[startIndex] = locBytes[j];
					startIndex++;
				}
			}
		}
		boolean result = sendPacketToServer(packet);
		Log.d("ClientClass.java","Sent encrypted locations (packet A3) to server");
		return result;
	}
	/* B sends location whenever his location changes (but every 5 seconds at the most) and every 15 minutes at the least.
	 * The key of the map in the parameter corresponds to the friend ID of the shared key included in the location (values)
	 * Packet Type: 5
	 */
	public boolean sendCurrentLocation(long myID, Byte lsb, ArrayList<Long>friends, ArrayList<BigInteger> locations, int gridsize)
	{
		/* 1 byte for packet type, 8 bytes for myID, 4 bytes for gridsize, 1 bytes for lsb, 8 bytes for each friend ID, and 6 bytes for each BigInteger 
		 * in the map arrayList (we know there are 3 grids, so arraylist is size 3) (packet type 5)
		 */
		
		int size = 14 + friends.size()*(8+3*TRANSMIT_SIZE);
		//go through map once to get total number of bytes needed
		byte[] packet = new byte[size];
		//first byte indicates packet type
		packet[0] = new Byte("5").byteValue();
		int startIndex = 1;
		//next 8 bytes are myID
		BigInteger convertToByte = BigInteger.valueOf(myID);
		byte[] byteRep = convertToByte.toByteArray();
		if (byteRep.length < 8){
			for (int i = 0; i < (8-byteRep.length); i++){
				packet[startIndex] = new Byte("0").byteValue();
				startIndex++;
			}
		}
		for (int i = 0; i < byteRep.length; i++){
			packet[startIndex] = byteRep[i];
			startIndex++;
		}
		//next byte is lsb time
		packet[startIndex] = (byte) lsb.byteValue();
		//next 4 bytes is gridsize
		packet[startIndex + 1] = (byte)(gridsize >>> 24);
		packet[startIndex + 2] = (byte)(gridsize >>> 16);
		packet[startIndex + 3] = (byte)(gridsize >>> 8);
		packet[startIndex + 4] = (byte)(gridsize);
		startIndex += 5;
		for (int i = 0; i < friends.size(); i++){
			BigInteger friendToByte = BigInteger.valueOf(friends.get(i));
			byte[] byteArray = friendToByte.toByteArray();
			if (byteArray.length < 8){
				for (int j = 0; j < (8-byteArray.length); j++){
					packet[startIndex] = new Byte("0").byteValue();
					startIndex++;
				}
			}
			for (int j = 0; j < byteArray.length; j++){
				packet[startIndex] = byteArray[j];
				startIndex++;
			}
			for (int j = i*3; j <= i*3+2; j++){
				BigInteger loc = locations.get(j);
				byte[] locBytes = loc.toByteArray();
				//byte arrays are done using big endian, so add 0 bytes to front if less than 6 (TRANSMIT_SIZE) bytes
				if (locBytes.length < TRANSMIT_SIZE){
					for (int k = 0; k < (TRANSMIT_SIZE - locBytes.length); k++){
						packet[startIndex] = new Byte("0").byteValue();
						startIndex++;
					}
				}
				for (int k = 0; k < locBytes.length; k++){
					packet[startIndex] = locBytes[k];
					startIndex++;
				}
			}
		}
		boolean result = sendPacketToServer(packet);
		Log.d("ClientClass.java","Sent B-side location to server.");
		return result;
	}
	
	
	// --- Receives packets from the server --- returns byte array
	private byte[] receivePacket()
	{
		try{
			byte[] serverResponse = postMethod.getResponseBody();
			return serverResponse;
		}
		catch (IOException e){
			return null;
		}
	}
	
	public int getKeyFromServer()
	{
		// TODO: get key from server -- server gets it from the other party/Facebook?.
		int key = 223; //TEMPORARY BOGUS VALUE
		return key;
	}
	
	public int returnKeytoServer()
	{
		return myKey;
	}
	
	public String getURI()
	{
		return uri;
	}
	
	// --- Kina put these functions here ---- //
	
	// This is the initial response that the A-side gets. The return value is a Map
	// that maps each friend ID to a byte array. The byte array is formatted as follows:
	// first byte = least significant byte of the time that the server got from the associated friend
	// last four bytes = an integer that represents the grid size that the associated friend used
	public Map<Long, ArrayList<Byte>> receiveInitialResponse()
	{
		byte[] response = receivePacket();
		if (response == null)
			return null;
		System.err.println("Initial response length: " + response.length);
		Map <Long, ArrayList<Byte>> retVal = new HashMap<Long, ArrayList<Byte>>();
		byte firstByte = response[0];
		//index to keep track of response packet
		int index = 1;
		if (new Byte(firstByte).intValue() == 2){
			//each friend has 5 bytes for lsb time and grid and a friendID(8 bytes)
			int numFriends = (response.length -1)/(8+5);
			for (int i = 0; i < numFriends; i++){
				ArrayList<Byte> gridtimeInfo = new ArrayList<Byte>();
				byte[] convertLong = new byte[8];
				for (int j = 0; j < convertLong.length; j++)
				{
					convertLong[j] = response[index+j];
					
				}
				index += 8;
				long friend = new BigInteger(convertLong).longValue();
				for (int j = 0; j < 5; j++){
					gridtimeInfo.add(response[index+j]);
				}
				index +=5;
				retVal.put(friend, gridtimeInfo);
				
			}
			Log.d("ClientClass.java","Received the initial response (packet A2) from server.");
			return retVal;
		}
		else
			return null;
	}
	
	// This is the final response that the A-side gets and contains the "answer" as to whether or not B
	// is close to A. The return value is a Map that maps each friend ID to the response (biginteger).
	public Map<Long, ArrayList<BigInteger>> receiveFinalResponse()
	{
		byte[] response = receivePacket();
		if (receivePacket() == null)
			return null;
		Map <Long, ArrayList<BigInteger>> retVal = new HashMap<Long, ArrayList<BigInteger>>();
		byte firstByte = response[0];
		//index to keep track of response packet
		int index = 1;
		if (new Byte(firstByte).intValue() == 4){
			//each friend has 5 bytes for lsb time and grid and a friendID(4 bytes)
			int numFriends = (response.length -1)/(8+TRANSMIT_SIZE*3);
			for (int i = 0; i < numFriends; i++){
				ArrayList<BigInteger> closenessInfo = new ArrayList<BigInteger>();
				byte[] convertLong = new byte[8];
				for (int j = 0; j < convertLong.length; j++)
					convertLong[j] = response[index+j];
				index += 8;
				long friend = new BigInteger(convertLong).longValue();
				byte[] tempLoc = new byte[TRANSMIT_SIZE];
				for (int j = 0; j < TRANSMIT_SIZE*3; j++){
					tempLoc[(int)(j%TRANSMIT_SIZE)] = response[index];
					if ((j%TRANSMIT_SIZE) == (TRANSMIT_SIZE-1)){
						 closenessInfo.add(new BigInteger(tempLoc));
					}
					index++;
				}
				retVal.put(friend, closenessInfo);
			}
			Log.d("ClientClass.java", "Received final response (packet A4) from the server.");
			return retVal;
		}
		else
			return null;
	}
	
}
