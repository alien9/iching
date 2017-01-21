package net.alien9.iching;

import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Created by tiago on 20/01/17.
 */

public class IChing extends Application {
    private static IChing singleton;
    private static OkHttpClient client;

    public static IChing getInstance(){
        return singleton;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }
    public static void setClient(OkHttpClient c){
        getInstance().client=c;
    }

    public static OkHttpClient getClient(){
        return getInstance().client;
    }
    public static CookieJar getCookieJar(Context context) {
        return new IChing.CookiePot();
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

        public List<Cookie> getCookies(){
            return cookies;
        }
        public String getCookie(String name){
            return cookies.toString();
        }
    }
}
