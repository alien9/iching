package net.alien9.iching;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by tiago on 02/01/17.
 */

public class LocationListenerDourado implements android.location.LocationListener {
    private static final long MIN_TIME_ALLOWED = 5000;
    private final Handler handler;
    private long last;

    public LocationListenerDourado(Handler h){
        handler=h;
        last=0;
    }
    final String LOG_LABEL = "Location Listener>>";

    @Override
    public void onLocationChanged(Location location) {
        Log.d("ICHING", LOG_LABEL + "Location Changed");
        if (location != null) {
            long past = location.getTime() - last;
            if(past>MIN_TIME_ALLOWED) {
                Log.d("ICHING", "entrou");
                Message mess = new Message();
                mess.what = Question.POSITION_UPDATE;
                JSONObject h = new JSONObject();
                try {
                    h.put("accuracy", location.getAccuracy());
                    h.put("altitude", location.getAltitude());
                    h.put("latitude", location.getLatitude());
                    h.put("longitude", location.getLongitude());
                    h.put("speed", location.getSpeed());
                    h.put("bearing", location.getBearing());
                    h.put("time", location.getTime());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                last = location.getTime();
                mess.getData().putString("location", h.toString());
                handler.sendMessage(mess);
                double longitude = location.getLongitude();
                Log.d("ICHING", LOG_LABEL + "Longitude:" + longitude);
                //Toast.makeText(getApplicationContext(),"Long::"+longitude,Toast.LENGTH_SHORT).show();
                double latitude = location.getLatitude();
                ///Toast.makeText(getApplicationContext(),"Lat::"+latitude,Toast.LENGTH_SHORT).show();
                Log.d("ICHING", "Latitude:" + latitude);
            }
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