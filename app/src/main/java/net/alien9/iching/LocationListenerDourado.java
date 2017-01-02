package net.alien9.iching;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * Created by tiago on 02/01/17.
 */

public class LocationListenerDourado implements android.location.LocationListener {
    private final Handler handler;

    public LocationListenerDourado(Handler h){
        handler=h;
    }
    final String LOG_LABEL = "Location Listener>>";

    @Override
    public void onLocationChanged(Location location) {
        Log.d("ICHING", LOG_LABEL + "Location Changed");
        if (location != null) {
            double longitude = location.getLongitude();
            Log.d("ICHING", LOG_LABEL + "Longitude:" + longitude);
            //Toast.makeText(getApplicationContext(),"Long::"+longitude,Toast.LENGTH_SHORT).show();
            double latitude = location.getLatitude();
            ///Toast.makeText(getApplicationContext(),"Lat::"+latitude,Toast.LENGTH_SHORT).show();
            Log.d("ICHING", LOG_LABEL + "Latitude:" + latitude);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}