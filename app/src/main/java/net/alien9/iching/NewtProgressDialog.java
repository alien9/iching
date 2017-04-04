package net.alien9.iching;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import static net.alien9.iching.R.attr.icon;

/**
 * Created by tiago on 04/04/17.
 */
public class NewtProgressDialog extends ProgressDialog {
    private final int layout;
    private final Context context;
    private int max_value;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout);
    }


    public NewtProgressDialog(Context c, int l) {
        super(c,android.R.style.Theme_NoTitleBar_Fullscreen);
        layout=l;
        setIndeterminate(true);
        setCancelable(false);
        context = c;
    }

    @Override
    public void setProgress(int progress){
        ((ProgressBar)findViewById(R.id.progressBar)).setProgress(progress);
    }
    @Override
    public void setMax(int m){
        max_value=m;
        ((ProgressBar)findViewById(R.id.progressBar)).setMax(max_value);
    }
    @Override
    public int getMax(){
        return ((ProgressBar)findViewById(R.id.progressBar)).getMax();
    }
    @Override
    public void setTitle(CharSequence title){
        ((TextView) findViewById(R.id.dialog_title)).setText(title);
    }
}
