package net.alien9.iching;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Lista extends AppCompatActivity {

    private JSONObject groselha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Context context=this;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, Question.class);
                intent.putExtra("content",groselha.toString());
                startActivity(intent);
            }
        });
        Intent intent=getIntent();
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        JSONArray journal;
        try {
            journal=new JSONArray(sharedpreferences.getString("journal","[]"));
        } catch (JSONException e) {
            journal=new JSONArray();
        }
        JSONArray results=null;
        if(intent.getExtras()!=null) {
            try {
                results = new JSONArray(intent.getExtras().getString("results"));
            } catch (JSONException e) {
                results = null;
            }
        }
        if(results!=null) { // está trazendo json pra gravar
            journal.put(results);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("journal", journal.toString());
            editor.commit();
        }

        //JSONOBJECT vem carregado no intent
        try {
            String u = "";
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.sample);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            u += new String(b);
            groselha = new JSONObject(u);
        } catch (JSONException ignored) {
        } catch (IOException ignored) {
        }
        ((TextView)findViewById(R.id.list_summary)).setText(String.format("%s de %s",journal.length(), groselha.optJSONArray("items").length()));
    }

}
