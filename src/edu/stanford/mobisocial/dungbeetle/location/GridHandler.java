// ********************************** GridHandler.java ************************************//
// This class retrieves the GPS location from shared preferences and outputs coordinates
// that are converted in respect to a hexagonal grid. The output is the coordinates
// concatenated.
// ****************************************************************************************//

package edu.stanford.mobisocial.dungbeetle.location;

import java.math.BigInteger;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class GridHandler {
	
	// ----- CONSTANTS -----
	private static double HALF_MERIDIANAL_CIRCUMFERENCE=2003.93*1000; //half meridianal circumference of the earth in meters
	private static double RADIUS=6378100; //radius of the earth in meters
	private static final double CONVERSION = 3.2808399; // used to convert between meters and feet.
	
	private SharedPreferences savedLatitude;
	private SharedPreferences savedLongitude;
	private double mlatitude = 10;
	private double mlongitude = 10;
	
	public GridHandler()
	{
		// constructor
	}
	
	// Main function (most important) that retreives all three grid types corresponding to inputed grid size
	public ArrayList<BigInteger> getGridCoords(Context c, int gridsize_feet)
	{
		ArrayList<BigInteger> retVal = new ArrayList<BigInteger>();
		
		savedLatitude = c.getSharedPreferences("savedLatitude", c.MODE_PRIVATE);
		String temp = savedLatitude.getString("savedLatitude", "FAIL_LAT");
		if (temp != "FAIL_LAT")
    	    { mlatitude = new Double(temp); }
    	  
		savedLongitude = c.getSharedPreferences("savedLongitude", c.MODE_PRIVATE);
		temp = savedLongitude.getString("savedLongitude", "FAIL_LONG");
		if (temp != "FAIL_LONG")
    	    { mlongitude = new Double(temp); }
    	
		int gridsize_meters = (int) (gridsize_feet / CONVERSION); // All grids conversions are done in meters.
		
		System.err.println("Location retrieved is : Latitude: " + mlatitude + "\t Longitude: " + mlongitude + "\t Grid size (in feet): " + gridsize_feet);
		
		
		float xyz[];
      	String xy[] = getXY(mlatitude, mlongitude, gridsize_meters);
      	
  	    int exp=24;		//use : 2 ^ 48 + 21
  		BigInteger leftshift = BigInteger.valueOf(2);
  		leftshift = leftshift.pow(exp);
  		
  		BigInteger latlon;
  		
  		// Get all three grid types --- remember there are three grids that are overlapping that we need to check against. 
  		xyz = getXYZ(mlatitude, mlongitude, gridsize_meters, 0);
  		latlon = BigInteger.valueOf((long) (xyz[0]*1E6));
   	    latlon.multiply(leftshift);
   	    latlon = latlon.or(BigInteger.valueOf((long) (xyz[1]*1E6)));
   	    retVal.add(latlon);
   	    
   	    xyz = getXYZ(mlatitude, mlongitude, gridsize_meters, 1);
		latlon = BigInteger.valueOf((long) (xyz[0]*1E6));
	    latlon.multiply(leftshift);
	    latlon = latlon.or(BigInteger.valueOf((long) (xyz[1]*1E6)));
	    retVal.add(latlon);
	    
	    xyz = getXYZ(mlatitude, mlongitude, gridsize_meters, 2);
  		latlon = BigInteger.valueOf((long) (xyz[0]*1E6));
   	    latlon.multiply(leftshift);
   	    latlon = latlon.or(BigInteger.valueOf((long) (xyz[1]*1E6)));
   	    retVal.add(latlon);
   	   
		return retVal;
	
	}
	
	// written by Naren
	// There weren't any comments. It looks like x = latitude	&   y = longitude
	// I also renamed the following variables: newLat was previously called X ; numVertLines was previously called N ; newLong was prev called Y
	private String[] getXY(double x, double y,int gridsize) {
		int M;
		String xy[]={"",""};
		M=(int) HALF_MERIDIANAL_CIRCUMFERENCE/gridsize;
		int newLat; 
		if (x<0)
			newLat= normalize(0,-90,M/2,M,x);
		else newLat= normalize(0,90,1,M/2,x);
		
		int newLong;
		int numVertLines; // the number of vertical grid lines
		
		//round off latitude for computation of numVertLines
		double roundoff_x=(int)Math.abs(x)+ 0.5;
		numVertLines=(int) ((2*Math.PI*RADIUS*Math.cos(roundoff_x))/gridsize); //calculate numVertLines based on circumference of earth at that latitude
		
		double l;
		if (y>=0) l=y;
		else l=360+y;
		newLong=normalize(0,360,1,numVertLines,l);
		
		
		int thetax=normalize(0,360,1,M*gridsize,gridsize/2);
		int offsetX=normalize(0,360,1,M,newLong-thetax);
		
		int theta=normalize(0,360,1,numVertLines*gridsize,gridsize/2);
		int offsetY=normalize(0,360,1,numVertLines,newLat-theta);
		Log.d("location","x and y values input were: "+x+" and "+y+" and roundoff_x is"+ roundoff_x+" and value of l is: "+l);
		Log.d("location","newLat is: "+Integer.toString(newLat)+" newLong is: "+Integer.toString(newLong)+ " numVertLines is: "+ Integer.toString(numVertLines));
		Log.d("location","offsetX is: "+Integer.toString(offsetX)+" offsetY is: "+Integer.toString(offsetY)+ " M is: "+ Integer.toString(M));
		xy[0]=Integer.toString(newLat+numVertLines*newLong);
		xy[1]=Integer.toString(offsetX+numVertLines*offsetY);
		Log.d("location","xy is: "+xy[0]);
		Log.d("location","offsetxy is:"+xy[1]);
		return xy;
	}
	
	//normalize x which is in range a to b, to a scale of range A to B
	private int normalize(double a, double b, int A, int B, double x) {
		int X;
		X= (int)(B+ (x-b)*(B-A)/(b-a));
		return X;
	}
	
	
	private float[] getXYZ(double x, double y, int gridsize, int gridType){
		
		//Support for 3 grid types
		float sqrt3 = (float) Math.sqrt(3);
		float step = gridsize;
		float iStep = step;
		float jStep = (float)step*sqrt3;
		
		String xyz[] = {"", ""};
		
		double stripHeightPerDeg = HALF_MERIDIANAL_CIRCUMFERENCE/180.0;
		double roundoff_x=(int)Math.abs(x)+ 0.5;
		double stripWidthPerDeg = (2*Math.PI*RADIUS*Math.cos(roundoff_x))/360.0;
		
		System.err.println("x : " + x);
		double xDist = x % 1.0;
		double xMod = x - xDist;
		System.err.println("xDist : " + xDist);
		
		System.err.println("stripHeightPerDeg : " + stripHeightPerDeg);
		xDist *= stripHeightPerDeg;
		System.err.println("xDist : " + xDist);
		
		System.err.println("y : " + y);
		y += 180;
		System.err.println("y : " + y);
		System.err.println("stripWidthPerDeg : " + stripWidthPerDeg);
		double yDist = y * stripWidthPerDeg;
		System.err.println("yDist : " + yDist);
		
		switch(gridType){
		case 0:
			break;
		case 1:
			xDist -= 0.5*jStep;
			yDist -= 0.5*iStep;
			break;
		case 2:
			//x -= 0.5*jStep;
			yDist -= iStep;
			break;
		}
		
		float[] res = hexagonMap((float)xDist, (float)yDist, gridsize);
		
		switch(gridType){
		case 0:
			break;
		case 1:
			res[0] += 0.5*jStep;
			res[1] += 0.5*iStep;
			break;
		case 2:
			//x -= 0.5*jStep;
			res[1] += iStep;
			break;
		}
		
		System.err.println("res : " + res[0] + "," + res[1]);
		res[0]/=stripHeightPerDeg;
		res[0] += xMod;
		res[1] = (float) (res[1]/stripWidthPerDeg);
		System.err.println("lat lon : " + res[0] + "," + res[1]);
		//return xyz;
		return res;
	}
	
	float[] hexagonMap(float touchX, float touchY, int step){
		
		float sqrt3 = (float) Math.sqrt(3);
		//int step = 100;
		float iStep = step*3;
		float jStep = (float)step*sqrt3;
		boolean isTouched = true;
		
		float res[] = new float[2];
		
		if(isTouched){
			float yIndex = (touchY % iStep)/iStep;
			//System.err.println("touchY : " + touchY +" yIndex : " + yIndex);
			//Simple case
			if(yIndex <= 0.33 || (yIndex > 0.5 && yIndex < 0.83)){
				float lineBlock = touchY / iStep;
				//first row
				if(yIndex < 0.5){
					float xIndex = touchX / jStep;
					float hexX = (float)((int)xIndex) * jStep;
					float hexY = (float)((int)lineBlock) * iStep;
					//drawHexagon(hexX, hexY, step, canvas, true);
					res[0] = hexX; res[1] = hexY;
				}else //Second row
				{
					float xIndex = (touchX - (jStep/2)) / jStep;
					float hexX = (float)(((int)xIndex)+0.5) * jStep;
					float hexY = (float)(((int)lineBlock)+0.5 ) * iStep;
					//drawHexagon(hexX, hexY, step, canvas, true);
					res[0] = hexX; res[1] = hexY;
				}
			}else{
				if(yIndex < 0.5){
					float yNum = touchY / iStep;
					float xNum = touchX / jStep;
					
					float xAdder = (float)((int)xNum);
					float yAdder = (float)((int)yNum);
					
					float x1 = (float) (xAdder + 0.5);
					float y1 = (float) (yAdder + 0.165);
					float dist1 = getDist(x1, y1, xNum, yNum);
					
					float x2 = (float) (xAdder);
					float y2 = (float) (yAdder + 0.665);
					float dist2 = getDist(x2, y2, xNum, yNum);
					
					float x3 = (float) (xAdder + 1);
					float y3 = (float) (yAdder + 0.665);
					float dist3 = getDist(x3, y3, xNum, yNum);
					
					System.err.println("xNum, yNum " + xNum + "," + yNum);
					System.err.println("xAddr, yAddr " + xAdder + "," + yAdder);
					System.err.println("x1, y1, dist1 " + x1 + "," + y1 + "," + dist1);
					System.err.println("x2, y2, dist2 " + x2 + "," + y2 + "," + dist2);
					System.err.println("x3, y3, dist3 " + x3 + "," + y3 + "," + dist3);
					
					if(dist1 < dist2){
						if(dist1 < dist3){
							//drawHexagon((x1-0.5f)*jStep, (y1-0.165f)*iStep, step, canvas, true);
							res[0] = (x1-0.5f)*jStep; res[1] = (y1-0.165f)*iStep;
						}
						else{
							//drawHexagon((x3-0.5f)*jStep, (y3-0.165f)*iStep, step, canvas, true);
							res[0] = (x3-0.5f)*jStep; res[1] = (y3-0.165f)*iStep;
						}
					}else{
						if(dist2 < dist3){
							//drawHexagon((x2-0.5f)*jStep, (y2-0.165f)*iStep, step, canvas, true);
							res[0] = (x2-0.5f)*jStep; res[1] = (y2-0.165f)*iStep;
						}else{
							//drawHexagon((x3-0.5f)*jStep, (y3-0.165f)*iStep, step, canvas, true);
							res[0] = (x3-0.5f)*jStep; res[1] = (y3-0.165f)*iStep;
						}
					}
				}else{
					float yNum = touchY / iStep;
					float xNum = touchX / jStep;
					
					float xAdder = (float)((int)xNum);
					float yAdder = (float)((int)yNum);
					
					float x1 = (float) (xAdder + 0.5);
					float y1 = (float) (yAdder + 1.165);
					float dist1 = getDist(x1, y1, xNum, yNum);
					
					float x2 = (float) (xAdder);
					float y2 = (float) (yAdder + 0.665);
					float dist2 = getDist(x2, y2, xNum, yNum);
					
					float x3 = (float) (xAdder + 1);
					float y3 = (float) (yAdder + 0.665);
					float dist3 = getDist(x3, y3, xNum, yNum);
					
					System.err.println("xNum, yNum " + xNum + "," + yNum);
					System.err.println("xAddr, yAddr " + xAdder + "," + yAdder);
					System.err.println("x1, y1, dist1 " + x1 + "," + y1 + "," + dist1);
					System.err.println("x2, y2, dist2 " + x2 + "," + y2 + "," + dist2);
					System.err.println("x3, y3, dist3 " + x3 + "," + y3 + "," + dist3);
					
					if(dist1 < dist2){
						if(dist1 < dist3){
							//drawHexagon((x1-0.5f)*jStep, (y1-0.165f)*iStep, step, canvas, true);
							res[0] = (x1-0.5f)*jStep; res[1] = (y1-0.165f)*iStep;
						}
						else{
							//drawHexagon((x3-0.5f)*jStep, (y3-0.165f)*iStep, step, canvas, true);
							res[0] = (x3-0.5f)*jStep; res[1] = (y3-0.165f)*iStep;
						}
					}else{
						if(dist2 < dist3){
							//drawHexagon((x2-0.5f)*jStep, (y2-0.165f)*iStep, step, canvas, true);
							res[0] = (x2-0.5f)*jStep; res[1] = (y2-0.165f)*iStep;
						}else{
							//drawHexagon((x3-0.5f)*jStep, (y3-0.165f)*iStep, step, canvas, true);
							res[0] = (x3-0.5f)*jStep; res[1] = (y3-0.165f)*iStep;
						}
					}
				}
			}
		}
		return res;
	}
	
	float getDist(float x1, float y1, float x2, float y2){
		return (float) Math.sqrt((x1-x2)*(x1-x2)+ (y1-y2)*(y1-y2));
	}

}