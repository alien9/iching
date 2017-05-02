package net.alien9.iching;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by tiago on 01/05/17.
 */

class PinchaWebView extends WebView {
    public static final int SAIR = 0;
    private boolean lastZoomOutResult=false;
    private Handler zoomrespond;

    public PinchaWebView(Context context) {
        super(context);
    }
    public PinchaWebView(Context context, AttributeSet attrs){
        super(context,attrs);
    }
    @Override
    public boolean zoomOut() {
        if(lastZoomOutResult){
            if(zoomrespond!=null){
                Message m = new Message();
                m.what=SAIR;
                zoomrespond.sendMessage(m);
            }
        }
        lastZoomOutResult = super.zoomOut();
        return lastZoomOutResult;
    }
    public void setZoomDetector(Handler h){
        zoomrespond=h;
    }
}
