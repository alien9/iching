package net.alien9.iching;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Lista extends AppCompatActivity {

    private JSONObject groselha;
    private String cookies;
    private OkHttpClient client;
    private JSONArray stuff;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context=this;
        Intent intent=getIntent();
        if(intent.hasExtra("CNETSERVERLOGACAO")){
            cookies = intent.getExtras().getString("CNETSERVERLOGACAO");
        }else{
            requestLogin();
            return;
        }
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        JSONObject journal;
        try {
            journal=new JSONObject(sharedpreferences.getString("journal","{}"));
        } catch (JSONException e) {
            journal=new JSONObject();
        }
        if(intent.hasExtra("result")){ // está trazendo json pra gravar
            SharedPreferences.Editor editor = sharedpreferences.edit();
            try {
                JSONObject resultado=new JSONObject(intent.getExtras().getString("result"));
                String cod=intent.getExtras().getString("cod");
                if(!journal.has(cod)) journal.put(cod,new JSONArray());
                journal.optJSONArray(cod).put(resultado);

            } catch (JSONException ignored) {}
            editor.putString("journal", journal.toString());
            editor.commit();
        }

        stuff=((IChing)getApplicationContext()).getStuff();
        if(stuff==null){
            requestLogin();
            return;
        }

        show();

        client = IChing.getInstance().getClient();
        ((ListView)findViewById(R.id.lista_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(context, Question.class);
                intent.putExtra("poll",stuff.optJSONObject(i).toString());
                startActivity(intent);
            }
        });
    }

    private void requestLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_lista, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.reload_groselha:
                reload();
                break;
        }
        return true;

    }

    private void reload() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
        progressBar.setVisibility(View.VISIBLE);
        ReloadTask reloader = new ReloadTask();
        reloader.execute((Void) null);
    }


    private class ReloadTask extends AsyncTask<Void,Void,Boolean>{
        private boolean loading=false;
        @Override
        protected Boolean doInBackground(Void... voids) {
            if(loading)return false;
            loading=true;
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            //RequestBody bode = RequestBody.create(JSON, ((IChing)getApplicationContext()).getRespostas().toString());
            RequestBody bode = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("c",((IChing)getApplicationContext()).getPesqId())
                    .addFormDataPart("d",((IChing)getApplicationContext()).getRespostas().toString())
                    .addFormDataPart("m","save")
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.save_url))
                    .post(bode)
                    .method("POST", RequestBody.create(null, new byte[0]))
                    .build();
            Response response = null;
            try {
                CookieJar cookieJar = ((IChing) getApplicationContext()).getCookieJar(context);
                OkHttpClient client = new OkHttpClient.Builder().cookieJar(cookieJar).build();
                response = client.newCall(request).execute();
                String j=response.body().string();
                Pattern p = Pattern.compile("\\[\\{.*");
                Matcher m = p.matcher(j);
                if(m.find()) {
                    String s = m.group();
                    stuff = new JSONArray(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            loading=false;
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
            progressBar.setVisibility(View.GONE);
            show();
        }
    }

    private void show() {
        List<String> names=new ArrayList<>();
        ((ListView)findViewById(R.id.lista_list)).setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_selectable_list_item,
                android.R.id.text1, names));
        cleanUp();
        for(int i=0;i<stuff.length();i++){
            JSONObject it = stuff.optJSONObject(i);
            names.add(it.optString("nom"));
            if(it.has("midia")){
                new MediaLoader(it.optString("midia")).execute();
            }
        }
    }

    private class MediaLoader extends AsyncTask<Void, Void, Boolean> {
        private final String filename;

        public MediaLoader(String f) {
            filename=f;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File directory = new File(getExternalCacheDir(),"midia");
            if(!directory.exists()) {
                directory.mkdirs();
            }
            File destination= new File(getExternalCacheDir()+File.separator+"midia"+File.separator+filename);
            if(!destination.exists()) {
                String url =getString(R.string.login_url);
                CookieJar cookieJar = ((IChing) getApplicationContext()).getCookieJar(context);
                OkHttpClient client = new OkHttpClient.Builder().cookieJar(cookieJar).build();
                RequestBody formBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("c",((IChing) getApplicationContext()).getPesqId())
                        .addFormDataPart("midia",filename)
                        .addFormDataPart("m","midia")
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .method("POST", RequestBody.create(null, new byte[0]))
                        .post(formBody)
                        .build();
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    FileOutputStream fos = new FileOutputStream(getExternalCacheDir() + File.separator + "midia" + File.separator + filename);
                    fos.write(response.body().bytes());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Util.unzip(getExternalCacheDir()+File.separator+"midia",filename);
            }
            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
        }
    }
    private void cleanUp(){
        JSONObject filenames = new JSONObject();
        try {
            for(int i=0;i<stuff.length();i++) {
                JSONObject json = stuff.optJSONObject(i);
                String file = json.optString("midia", "");
                if(file.length()>0){
                    filenames.put(file,true);
                }
                JSONObject pergs = json.optJSONObject("pergs");
                Iterator<String> iter = pergs.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    if(pergs.optJSONObject(key).has("resps")){
                        JSONObject jk = pergs.optJSONObject(key).optJSONObject("resps");
                        Iterator<String> biter = jk.keys();
                        while(biter.hasNext()){
                            String bey = biter.next();
                            if(jk.optJSONObject(bey).has("midia"))
                                filenames.put(jk.optJSONObject(bey).optString("midia"),false);
                        }
                    }
                }
            }
            File directory = new File(getExternalCacheDir()+File.separator+"midia");
            File[] files = directory.listFiles();
            for(int i=0;i<files.length;i++){
                if(!filenames.has(files[i].getName())){
                    files[i].delete();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
