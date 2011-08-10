package edu.stanford.mobisocial.dungbeetle.feed.presence;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedPresence;
import edu.stanford.mobisocial.dungbeetle.feed.objects.LocationObj;

public class LocationPresence extends FeedPresence {
    private static final String TAG = "locationPresence";
    LocationManager mLocationManager;
    private boolean mShareLocation = false;
    private Context mContext;

    @Override
    public String getName() {
        return "Location";
    }

    @Override
    public void onPresenceUpdated(final Context context, final Uri feedUri, boolean present) {
        if (mLocationManager == null) {
            mContext = context.getApplicationContext();
            mLocationManager =
                    (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        }
        if (mShareLocation) {
            if (getFeedsWithPresence().size() == 0) {
                mLocationManager.removeUpdates(mLocationListener);
                Toast.makeText(context, "No longer sharing location", Toast.LENGTH_SHORT).show();
                mShareLocation = false;
            }
        } else {
            if (getFeedsWithPresence().size() > 0) {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, THREE_MINUTES, 0, mLocationListener);
                Toast.makeText(context, "Now sharing location", Toast.LENGTH_SHORT).show();
                mShareLocation = true;
            }
        }
    }

    private static final int THREE_MINUTES = 1000 * 60 * 3;

    /** Determines whether one Location reading is better than the current Location fix
      * @param location  The new Location that you want to evaluate
      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
      */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > THREE_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -THREE_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            for (Uri uri : getFeedsWithPresence()) {
                Helpers.sendToFeed(mContext, LocationObj.from(location), uri);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }  
    };
}