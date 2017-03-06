package net.alien9.iching;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static android.R.id.undo;

/**
 * Created by tiago on 20/01/17.
 */

public class IChing extends Application {
    private static final int ICHING_REQUEST_GPS_PERMISSION = 0;
    private static IChing singleton;
    private static OkHttpClient client;
    private static CookiePot cookieJar;
    private JSONObject respostas;
    private int undid;
    private JSONArray stuff;
    private String cod;
    private String pesqId;
    public static final int POSITION_UPDATE = 0;
    private JSONObject last_known_position;
    private LocationManager locationManager;
    private String domain;

    public static IChing getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        respostas = new JSONObject();
        undid = 0;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        singleton = this;
    }
    public void startGPS(Activity a){
        Log.d("ICHING SERVICE","should start");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("ICHING SERVICE","permission nort granted");
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
            tasks.get(0);

            ActivityCompat.requestPermissions(a,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ICHING_REQUEST_GPS_PERMISSION);
            return;
        }
        Log.d("ICHING SERVICE","start now");
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), LocationService.class);
        startService(intent);
        //final Handler locator = new Chandler();
        //LocationListenerDourado l = new LocationListenerDourado(locator);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, l);
    }


    public static void setClient(OkHttpClient c){
        getInstance().client=c;
    }

    public static OkHttpClient getClient(){
        return getInstance().client;
    }
    public static CookieJar getCookieJar() {
        if(cookieJar==null)
            cookieJar=new CookiePot();
        return cookieJar;
    }

    public void setRespostas(JSONObject resps) {
        respostas = resps;
    }

    public JSONObject getRespostas() {
        return respostas;
    }

    public void resetUndo() {
        undid=0;
    }

    public boolean hasUndo() {
        return undid>0;
    }

    public void setUndo() {
        undid++;
    }

    public void setStuff(JSONArray s) {
        stuff = s;
    }

    public JSONArray getStuff() {
        return stuff;
    }

    public void setCod(String c) {
        cod = c;
    }

    public String getCod() {
        return cod;
    }

    public void setPesqId(String p) {
        pesqId = p;
    }

    public String getPesqId() {
        return pesqId;
    }

    public void setCookieJar(CookieJar c) {
        cookieJar = (CookiePot) c;
    }

    public JSONObject getLastKnownPosition() {
        return last_known_position;
    }

    public void setDomain(String d) {
        domain = d;
    }
    public String getDomain(){
        return domain;
    }

    public void setLastKnownPosition(JSONObject lastKnownPosition) {
        this.last_known_position = lastKnownPosition;
    }

    private static class CookiePot implements CookieJar {
        private List<Cookie> cookies;

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            this.cookies =  cookies;
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            if (cookies != null)
                return cookies;
            return new ArrayList<Cookie>();
        }
    }
}
