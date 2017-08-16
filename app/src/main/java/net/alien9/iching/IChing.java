package net.alien9.iching;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
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
    public static final int POSITION_UPDATE = 0;
    private JSONObject last_known_position;
    private boolean isReloading=false;
    Hashtable<String,Integer> BULLETS=new Hashtable<String,Integer>() {{
        put((String) "habi", R.drawable.bullet_pessoa);
        put((String) "fami", R.drawable.bullet_familia);
        put((String) "ende", R.drawable.bullet_casa);
        put((String) "geral", R.drawable.bullet_doc);
    }};
    private JSONObject result;

    public static IChing getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        respostas = new JSONObject();
        undid = 0;
        singleton = this;
    }

    public void startGPS(Activity a){
        Log.d("ICHING SERVICE","should start");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("ICHING SERVICE","permission nort granted");
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tasks = activityManager.getAppTasks();
                tasks.get(0);
            }
            ActivityCompat.requestPermissions(a,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ICHING_REQUEST_GPS_PERMISSION);
            return;
        }
        Log.d("ICHING SERVICE","start now");
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), LocationService.class);
        startService(intent);
    }

    public CookieJar getCookieJar() {
        if(cookieJar==null) {
            cookieJar = new CookiePot();
            SharedPreferences sharedpreferences = getSharedPreferences("COOKIES", Context.MODE_PRIVATE);
            if(sharedpreferences.contains("CNETSERVERLOGACAO")){
                try {
                    JSONObject cu=new JSONObject(sharedpreferences.getString("CNETSERVERLOGACAO","{}"));
                    Cookie ebom=new Cookie.Builder()
                            .domain(cu.optString("domain"))
                            .path(cu.optString("path"))
                            .name(cu.optString("name"))
                            .value(cu.optString("value"))
                            .expiresAt(cu.optLong("expiresAt"))
                            .httpOnly()
                            .secure()
                            .build();
                    List<Cookie> lc=new ArrayList<>();
                    lc.add(ebom);
                    cookieJar.saveFromResponse(HttpUrl.parse(getString(R.string.load_url)),lc);
                    return cookieJar;
                } catch (JSONException|IllegalArgumentException estercado) {
                    return cookieJar;
                }

            }
        }
        return cookieJar;
    }
    public String getCookieJarCookies(){
        CookieJar c= getCookieJar();
        if(((CookiePot)c).cookies==null) return null;
        if(((CookiePot)c).cookies.size()>0)
            return cookieJar.cookies.get(0).toString();
        return null;
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
        if(stuff!=null) {
            SharedPreferences sharedpreferences = getSharedPreferences(String.format("stuff_%s", getPesqId()), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("stuff", stuff.toString());
            editor.commit();
        }
    }

    public JSONArray getStuff() {
        if(stuff==null){
            SharedPreferences sharedpreferences = getSharedPreferences(String.format("stuff_%s",getPesqId()), Context.MODE_PRIVATE);
            try {
                stuff= new JSONArray(sharedpreferences.getString("stuff","{}"));
            } catch (JSONException e) {
                return null;
            }
        }
        return stuff;
    }

    public void setCod(String c) {
        cod = c;
    }

    public String getCod() {
        return cod;
    }

    public void setPesqId(String p) {
        SharedPreferences sharedpreferences = getSharedPreferences("PESQUISADOR", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("id", p);
        editor.commit();
    }

    public String getPesqId() {
        SharedPreferences sharedpreferences = getSharedPreferences("PESQUISADOR", Context.MODE_PRIVATE);
        return sharedpreferences.getString("id",null);
    }

    public void setCookieJar(CookieJar c) {
        cookieJar = (CookiePot) c;
        SharedPreferences sharedpreferences = getSharedPreferences("COOKIES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        if(c!=null){
            if(((CookiePot) c).cookies!=null){
                if(((CookiePot) c).cookies.size()>0){
                    JSONObject cu=new JSONObject();
                    Cookie cookie = ((CookiePot) c).cookies.get(0);
                    try {
                        cu.put("name",cookie.value());
                        cu.put("domain",cookie.domain());
                        cu.put("value",cookie.value());
                        cu.put("expiresAt",cookie.expiresAt());
                        cu.put("path",cookie.path());
                    } catch (JSONException ignore) {

                    }
                    editor.putString("CNETSERVERLOGACAO",cu.toString());
                    editor.commit();
                    return;
                }
            }
        }
        editor.remove("CNETSERVERLOGACAO");
        editor.commit();
    }


    public void setEstado(String estado) {
        SharedPreferences sharedpreferences = getSharedPreferences("PESQUISADOR", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("uf", estado);
        editor.commit();
    }
    public String getEstado() {
        SharedPreferences sharedpreferences = getSharedPreferences("PESQUISADOR", Context.MODE_PRIVATE);
        return sharedpreferences.getString("uf", null);
    }

    public void setDomain(String d) {
        SharedPreferences sharedpreferences = getSharedPreferences("PESQUISADOR", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("domain", d);
        editor.commit();
    }
    public String getDomain(){
        SharedPreferences sharedpreferences = getSharedPreferences("PESQUISADOR", Context.MODE_PRIVATE);
        return sharedpreferences.getString("domain",null);
    }

    public void setLastKnownPosition(JSONObject lastKnownPosition) {
        last_known_position = lastKnownPosition;
    }
    public JSONObject getLastKnownPosition() {
        if(last_known_position==null)last_known_position=new JSONObject();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location loca = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(loca!=null){
                last_known_position.put("gps", String.format("%s %s", loca.getLatitude(), loca.getLongitude()));
                last_known_position.put("gpsprec", loca.getAccuracy());
            }
        }catch(SecurityException sx){} catch (JSONException ignore) {}
        return last_known_position;
    }

    public boolean getIsReloading() {
        return isReloading;
    }

    public void setReloading(boolean reloading) {
        isReloading = reloading;
    }

    public Bitmap getItemBitmap(String string, Boolean esus) {
        Integer bitmap_res = BULLETS.get(string);
        if(bitmap_res==null){
            bitmap_res=R.drawable.bullet_doc;
        }
        Bitmap bm = BitmapFactory.decodeResource(getResources(), bitmap_res);
        Bitmap bi= Bitmap.createBitmap(bm.getWidth(),bm.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bi);
        Paint paint = new Paint();
        int color = ContextCompat.getColor(this, (esus)?R.color.darkGreen:R.color.red);
        paint.setColor(color);
        canvas.drawBitmap(bi, new Matrix(), null);
        canvas.drawCircle(bi.getWidth()/2,bi.getHeight()/2, bi.getHeight()/2, paint);
        canvas.drawBitmap(bm,0,0,null);
        return bi;
    }

    public void setResult(JSONObject s) {
        result=s;
    }

    public JSONObject getResult() {
        return result;
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
