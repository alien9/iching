package net.alien9.iching;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Created by tiago on 10/01/17.
 */
public class Util {
    public static boolean hasValidSession(Context context){
        Intent intent=((Activity)context).getIntent();
        if(!intent.hasExtra("CNETSERVERLOGACAO"))
            return false;
        String cookie= (String) intent.getExtras().get("CNETSERVERLOGACAO");
        if(cookie!=null)
            return true;
        return false;
    }

    public static CookieJar getCookieJar(Context context) {
        return new CookiePot();
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

