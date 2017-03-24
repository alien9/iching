package net.alien9.iching;


import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Created by tiago on 11/18/15.
 */
public class FormControl {
    private static OkHttpClient client;

    public static void freeze(View v){
        if(v==null)return;
        if(v instanceof Spinner){
            View u = ((Spinner) v).getSelectedView();
            if(u!=null) {
                u.setEnabled(false);
                u.setFocusable(false);
            }
            v.setEnabled(false);
            v.setFocusable(false);
        }
        if((v instanceof CheckBox)||(v instanceof EditText)||(v instanceof AutoCompleteTextView)){
            v.setEnabled(false);
            v.setFocusable(false);
            v.setClickable(false);
        }

        if((v instanceof Button)||(v instanceof ImageButton)||(v instanceof ListView)){
            v.setEnabled(false);
            v.setClickable(false);
        }
        if(v instanceof ViewGroup){
            for(int i=0;i<((ViewGroup)v).getChildCount();i++){
                View child = ((ViewGroup)v).getChildAt(i);
                freeze((View) child);
            }
        }
    }
    public static void thaw(View v){
        if(v==null)return;
        if(v instanceof Spinner){
            View u = ((Spinner) v).getSelectedView();
            if(u!=null) {
                u.setEnabled(true);
                u.setFocusable(false);
                u.setFocusableInTouchMode(false);
            }
            v.setEnabled(true);
            v.setFocusable(false);
            v.setFocusableInTouchMode(false);
        }
        if((v instanceof EditText)||(v instanceof AutoCompleteTextView)){
            v.setEnabled(true);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.setClickable(true);
        }
        if(v instanceof CheckBox){
            v.setEnabled(true);
            v.setFocusable(true);
            v.setClickable(true);
        }
        if((v instanceof Button)||(v instanceof ImageButton)||(v instanceof ListView)){
            v.setClickable(true);
            v.setEnabled(true);
        }
        if(v instanceof ViewGroup){
            for(int i=0;i<((ViewGroup)v).getChildCount();i++){
                View child = ((ViewGroup)v).getChildAt(i);
                thaw((View) child);
            }
        }
    }
    public static OkHttpClient getHttpClient(Context context) {

        TrustManagerFactory trustManagerFactory = null;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            SSLContext sslContext = null;
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient client = new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, trustManager).build();
            return client;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
