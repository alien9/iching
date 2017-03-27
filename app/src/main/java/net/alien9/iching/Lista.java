package net.alien9.iching;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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
import java.net.URLEncoder;
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

    private static final int EXIT = 0;
    private JSONObject groselha;
    private String cookies;
    private OkHttpClient client;
    private JSONArray stuff;
    private Context context;
    private boolean reloading=false;
    private boolean exiting=false;
    private ProgressDialog prog;
    private List<String> media;
    private int totalsize;
    private int currentsize;

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
        stuff=((IChing)getApplicationContext()).getStuff();
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
        ((IChing)getApplicationContext()).startGPS(this);
        client = IChing.getInstance().getClient();
        ((ListView)findViewById(R.id.lista_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(context, Question.class);
                intent.putExtra("poll",stuff.optJSONObject(i).toString());
                startActivity(intent);
            }
        });
        reload();
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
                if(!isNetworkAvailable()){
                    Snackbar.make(findViewById(R.id.content_lista),R.string.no_network_available, Snackbar.LENGTH_LONG).show();
                    return true;
                }
                reload();
                break;
            case R.id.logout:
                if(!isNetworkAvailable()){
                    Snackbar.make(findViewById(R.id.content_lista),R.string.no_network_available, Snackbar.LENGTH_LONG).show();
                    return true;
                }
                logout();
                break;
            case R.id.send_data:
                if(!isNetworkAvailable()){
                    Snackbar.make(findViewById(R.id.content_lista),R.string.no_network_available, Snackbar.LENGTH_LONG).show();
                    return true;
                }
                logout();
                break;
        }
        return true;

    }

    private void logout() {
        if(!exiting){
            exiting=true;
        }
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
        progressBar.setVisibility(View.VISIBLE);
        ReloadTask reloader = new ReloadTask();
        reloader.execute(EXIT);
    }

    private void reload() {
        if(!isNetworkAvailable()) {
            show();
            return;
        }
        if(!isReloading()) {
            setReloading(true);
            prog = new ProgressDialog(this);
            prog.setCancelable(false);
            prog.setMessage(getString(R.string.carregando));
            prog.setTitle(getString(R.string.aguarde));
            prog.show();
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
            progressBar.setVisibility(View.VISIBLE);
            ReloadTask reloader = new ReloadTask();
            reloader.execute();
        }
    }

    private boolean isReloading() {
        return ((IChing)getApplicationContext()).getIsReloading();
    }
    private void setReloading(boolean r){
        ((IChing)getApplicationContext()).setReloading(r);
    }

    private JSONObject getJournal() {
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        JSONObject journal;
        try {
            journal = new JSONObject(sharedpreferences.getString("journal", "{}"));
        } catch (JSONException e) {
            journal = new JSONObject();
        }
        return journal;
    }

    private class ReloadTask extends AsyncTask<Integer,Integer,Boolean>{
        private Integer todo;

        @Override
        protected Boolean doInBackground(Integer... integers) {
            if (integers.length > 0) {
                todo = integers[0];
            }
            String j = null;
            RequestBody bode = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("c", ((IChing) getApplicationContext()).getPesqId())
                    .addFormDataPart("d",getJournal().toString())
                    .addFormDataPart("m", "save")
                    .build();
            Request request = new Request.Builder()
                    .url(String.format("%s%s", ((IChing) getApplicationContext()).getDomain(), getString(R.string.save_url)))
                    .method("POST", RequestBody.create(null, new byte[0]))
                    .post(bode)
                    .build();
            Response response = null;
            try {
                CookieJar cookieJar = ((IChing) getApplicationContext()).getCookieJar();
                OkHttpClient client = new OkHttpClient.Builder().cookieJar(cookieJar).build();
                //String r = ((IChing) getApplicationContext()).getRespostas().toString();
                //request=new Request.Builder().url(String.format("%s?c=%sm=save&d=%s", new String[]{getString(R.string.save_url),((IChing)getApplicationContext()).getPesqId(), URLEncoder.encode(r,"UTF-8")})).build();

                response = client.newCall(request).execute();
                j = response.body().string();

                JSONObject resp = new JSONObject(j);
                stuff = resp.optJSONArray("pesqs");
                if (resp.has("saved")) {
                    if (resp.optBoolean("saved")) {
                        resetJournal("{}");
                    }
                }
                ((IChing)getApplicationContext()).setStuff(stuff);
            } catch (IOException e) {
                Snackbar.make(findViewById(R.id.content_lista), e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                return false;
            } catch (JSONException e) {
                e.printStackTrace();
                Snackbar.make(findViewById(R.id.content_lista), String.format("Problema de Comunicação. Mensagem do servidor: %s", j), Snackbar.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
            progressBar.setVisibility(View.GONE);

            if (prog != null && prog.isShowing()) {
                prog.dismiss();
            }
            if(todo!=null){
                switch(todo){
                    case EXIT:
                        ((IChing)getApplicationContext()).setCookieJar(null);
                        requestLogin();
                        break;
                }

            }else {
                show();
            }
        }

        public void execute(Void aVoid, int exit) {
        }
    }

    private void resetJournal(String j) {
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("journal", j);
        editor.commit();
    }

    private void show() {
        if(stuff==null)return;
        List<String> names=new ArrayList<>();
        media=new ArrayList<>();
        cleanUp();
        totalsize = 0;
        currentsize=0;

        for(int i=0;i<stuff.length();i++){
            JSONObject it = stuff.optJSONObject(i);
            names.add(it.optString("nom"));
            if(it.has("midia")){
                String filename=it.optString("midia");
                SharedPreferences sharedpreferences = getSharedPreferences("files", Context.MODE_PRIVATE);
                JSONObject saved_files;
                try {
                    saved_files=new JSONObject(sharedpreferences.getString("saved","{}"));
                } catch (JSONException e) {
                    saved_files=new JSONObject();
                }
                if(!saved_files.has(filename)){
                    media.add(filename);
                    totalsize+=it.optInt("msize");
                }
            }
        }
        //for(int i=media.size()-1;i>=0;i--)
        //    new MediaLoader(media.get(i)).execute();
        setReloading(false);
        if(prog!=null){
            prog.dismiss();
            if(media.size()>0) {
                showProgressDialog();
                new MediaLoader(media.get(0)).execute();
            }
        }
        ((ListView)findViewById(R.id.lista_list)).setAdapter(new StuffAdapter<String>(this,R.layout.content_lista_item,names));
    }

    private void showProgressDialog() {
        prog = new ProgressDialog(this);
        findViewById(R.id.content_lista).setKeepScreenOn(true);
        prog.setCancelable(false);
        prog.setProgressNumberFormat("%1d/%2d kB");
        prog.setMessage(getString(R.string.carregando));
        prog.setTitle(getString(R.string.aguarde));
        prog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        prog.setMax(totalsize/1024);
        prog.setMessage(getString(R.string.loading_files));
        prog.show();
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
            //if(!destination.exists()) {
            String url =String.format("%s%s",((IChing) getApplicationContext()).getDomain(),getString(R.string.login_url));
            CookieJar cookieJar = ((IChing) getApplicationContext()).getCookieJar();
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
            InputStream input=null;
            try {
                response = client.newCall(request).execute();
                input = response.body().byteStream();
                byte[] buff = new byte[1024 * 4];


                FileOutputStream fos = new FileOutputStream(getExternalCacheDir() + File.separator + "midia" + File.separator + filename);
                byte[] data = new byte[1024];

                long total = 0;

                int count;
                while ((count = input.read(data)) != -1) {
                    currentsize+=1024;
                    prog.setProgress(currentsize/1024);
                    fos.write(data, 0, count);
                }
                fos.flush();
                fos.close();
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(Util.unzip(getExternalCacheDir()+File.separator+"midia",filename)){
                SharedPreferences sharedpreferences = getSharedPreferences("files", Context.MODE_PRIVATE);
                JSONObject saved_files;
                try {
                    saved_files=new JSONObject(sharedpreferences.getString("saved","{}"));
                    saved_files.put(filename,true);
                } catch (JSONException e) {
                    saved_files=new JSONObject();
                }
                SharedPreferences.Editor e = sharedpreferences.edit();
                e.putString("saved", saved_files.toString());
                e.commit();

            };
            //}
            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            media.remove(media.indexOf(filename));
            if(media.size()==0) {
                setReloading(false);
                if(prog!=null && prog.isShowing())
                    prog.dismiss();
            }else{
                new MediaLoader(media.get(0)).execute();
            }
        }
    }
    private void cleanUp(){
        if(stuff==null)return;
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

            SharedPreferences sharedpreferences = getSharedPreferences("files", Context.MODE_PRIVATE);
            JSONObject saved_files;
            try {
                saved_files=new JSONObject(sharedpreferences.getString("saved","{}"));
            } catch (JSONException e) {
                saved_files=new JSONObject();
            }
            File directory = new File(getExternalCacheDir()+File.separator+"midia");
            File[] files = directory.listFiles();
            if(files!=null) {
                for (int i = 0; i < files.length; i++) {
                    if (!filenames.has(files[i].getName())) {
                        if(saved_files.has(files[i].getName()))
                            saved_files.remove(files[i].getName());
                        files[i].delete();
                    }
                }
            }
            SharedPreferences.Editor e = sharedpreferences.edit();
            e.putString("saved", saved_files.toString());
            e.commit();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class StuffAdapter<String> extends ArrayAdapter {
        private final List<String> names;
        private final int resourceId;


        public StuffAdapter(Context context, int resource, List<String> n){
            super(context, resource, n);
            resourceId=resource;
            names=n;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(context);
                v = vi.inflate(resourceId, null);
            }
            ((TextView)v.findViewById(R.id.text1)).setText(names.get(position).toString());
            return v;
        }
    }
    private void dismissProgressDialog() {
        if (prog != null && prog.isShowing()) {
            prog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        setReloading(false);
        super.onDestroy();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
