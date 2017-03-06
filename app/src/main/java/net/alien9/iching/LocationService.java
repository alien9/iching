package net.alien9.iching;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tiago on 05/03/17.
 */

public class LocationService extends Service {
    private LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ICHING SERVICE", "cresated service");
//whatever else you have to to here...
        android.os.Debug.waitForDebugger();  // this line is key
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("ICHING SERVICE", "binded service");

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ICHING SERVICE", "start service");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        final Handler locator = new Chandler();
        LocationListenerDourado l = new LocationListenerDourado(locator);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("ICHING SERVICE", "permission estercated");
            return START_REDELIVER_INTENT;
        }else {
            Log.d("ICHING SERVICE", "go");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, l);
            return START_NOT_STICKY;
        }
    }
    private class Chandler extends Handler {
        public void handleMessage (Message msg){
            if(msg.what==IChing.POSITION_UPDATE){
                try {
                    Log.d("ICHING SERVICE", "position");
                    ((IChing)getApplicationContext()).setLastKnownPosition(new JSONObject(msg.getData().getString("location")));
                } catch (JSONException ignore) {
                    Log.d("ICHING SERVICE", "position error");
                }
            }
        }
    }

}
