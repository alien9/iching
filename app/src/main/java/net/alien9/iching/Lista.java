package net.alien9.iching;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.concurrent.TimeUnit;

import okhttp3.CookieJar;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Lista extends AppCompatActivity {

    private static final int EXIT = 0;
    private String cookies;
    private JSONArray stuff;
    private Context context;
    private boolean exiting=false;
    private ProgressDialog prog;
    private List<String> media;
    private int totalsize;
    private int currentsize;
    private boolean cancel_download;
    private JSONObject current_pesquisa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        IChing iching=(IChing)getApplicationContext();
        cancel_download=false;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_logo_newt_completo);
        context=this;
        Intent intent=getIntent();
        cookies=null;
        if(intent.hasExtra("CNETSERVERLOGACAO")) {
            cookies = intent.getExtras().getString("CNETSERVERLOGACAO");
        }else{
            cookies=iching.getCookieJarCookies();
        }
        if((cookies==null)||(iching.getPesqId()==null)||(iching.getDomain()==null)){
            requestLogin();
            return;
        }
        stuff=iching.getStuff();
        final SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        JSONObject journal;
        try {
            journal=new JSONObject(sharedpreferences.getString("journal","{}"));
        } catch (JSONException e) {
            journal=new JSONObject();
        }
        iching.startGPS(this);
        ((ListView)findViewById(R.id.lista_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                JSONObject preset;
                try {
                    preset=new JSONObject(((TextView)view.findViewById(R.id.preset)).getText().toString());
                } catch (JSONException e) {
                    preset=new JSONObject();
                }
                if(current_pesquisa==null){ // CLICKOU NUMA PESQUISA com agendamentos
                    current_pesquisa = stuff.optJSONObject(i);
                    if(current_pesquisa.optString("foco").equals("ende")){
                        JSONArray arr = current_pesquisa.optJSONArray("ende");
                        if(arr==null)arr=new JSONArray();
                        if(arr.length()>0){
                            showEnderecosOrHabitantes(current_pesquisa);
                            return;
                        }else{
                            if(current_pesquisa.optBoolean("obrig",false)){
                                current_pesquisa=null;
                                Snackbar.make(findViewById(R.id.content_lista), getString(R.string.no_questions_available), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                return;
                            }
                        }
                    }
                    if(current_pesquisa.optString("foco").equals("habi")){
                        JSONArray arr = current_pesquisa.optJSONArray("habi");
                        if(arr==null)arr=new JSONArray();
                        if(arr.length()>0){
                            if(current_pesquisa.optJSONArray("habi").length()>0) {
                                showEnderecosOrHabitantes(current_pesquisa);
                                return;
                            }
                        }else{
                            if(current_pesquisa.optBoolean("obrig",false)){
                                current_pesquisa=null;
                                Snackbar.make(findViewById(R.id.content_lista), getString(R.string.no_questions_available), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                return;
                            }
                        }
                    }
                }

                /*else{
                    //selecionando pessoa para aplicar uma pesquisa
                    if(current_pesquisa.optString("foco").equals("habi")){
                        try {
                            current_pesquisa.put("preset",preset);
                        } catch (JSONException ignore) {
                            Log.d("ICHING","habitante mal informado!");
                        }
                    }
                    if(current_pesquisa.optString("foco").equals("ende")){
                        try {
                            current_pesquisa.put("preset",preset);
                        } catch (JSONException ignore) {
                            Log.d("ICHING","endereço mal informado!");
                        }
                    }
                }*/
                Intent intent = new Intent(context, Question.class);
                intent.putExtra("poll",current_pesquisa.toString());
                intent.putExtra("preset",preset.toString());
                intent.putExtra("CNETSERVERLOGACAO",cookies);
                startActivity(intent);
            }
        });
        if(intent.hasExtra("cod")){ // está trazendo json pra gravar
            SharedPreferences.Editor editor = sharedpreferences.edit();
            JSONObject resultado;
            try {
                resultado=iching.getResult();//new JSONObject(intent.getExtras().getString("result"));
                String cod=intent.getExtras().getString("cod");
                if(!journal.has(cod)) journal.put(cod,new JSONArray());
                journal.optJSONArray(cod).put(resultado);
            } catch (JSONException ignored) {
                resultado=new JSONObject();
            }
            editor.putString("journal", journal.toString());
            editor.commit();
            for(int k=0;k<stuff.length();k++) {
                JSONObject p = stuff.optJSONObject(k);
                SharedPreferences sp = getSharedPreferences("iching", Context.MODE_PRIVATE);
                if (!Debug.isDebuggerConnected() || (sp.getBoolean("remove_data",false))) {
                    if (p.has("habi")) {
                        for (int l = 0; l < p.optJSONArray("habi").length(); l++) {
                            if (p.optJSONArray("habi").optJSONObject(l).optString("habi1_cod").equals(resultado.optString("habi1_cod"))) {
                                //if(debug)
                                p.optJSONArray("habi").remove(l);
                            }
                        }
                    }
                    if (p.has("ende")) {
                        for (int l = 0; l < p.optJSONArray("ende").length(); l++) {
                            if (p.optJSONArray("ende").optJSONObject(l).optString("ende1_cod").equals(resultado.optString("ende1_cod"))) {
                                p.optJSONArray("ende").remove(l);
                            }
                        }
                    }
                    try {
                        stuff.put(k, p);
                    } catch (JSONException ignoree) {
                    }
                }
            }
            iching.setStuff(stuff);
            showPesquisas();

        }else {
            if(!intent.hasExtra("quiet"))
                reload();
            else
                showPesquisas();
        }
    }

    private int contagem() {
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        JSONObject journal;
        try {
            journal=new JSONObject(sharedpreferences.getString("journal","{}"));
        } catch (JSONException e) {
            journal=new JSONObject();
        }
        Iterator<String> iter = journal.keys();
        int total=0;
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                total+=journal.getJSONArray(key).length();
            } catch (JSONException ignore) {}
        }
        return total;
    }

    private void requestLogin() {
        Intent intent = new Intent(this, SimpleLogin.class);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.send_data);
        JSONObject j = getJournal();
        //item.setEnabled((j.length()>0)?true:false);
        int n=contagem();
        String itemzinho=(n==1)?"item":"itens";
        if(j.length()>0)
            menu.findItem(R.id.send_data).setTitle(String.format("Enviar: %s %s",""+contagem(),itemzinho));
        else
            menu.findItem(R.id.send_data).setTitle(getString(R.string.synchronize));

        menu.findItem(R.id.remove_data).setVisible(Debug.isDebuggerConnected());
        if(Debug.isDebuggerConnected()) {
            SharedPreferences sharedpreferences = getSharedPreferences("iching", Context.MODE_PRIVATE);
            menu.findItem(R.id.remove_data).setChecked(sharedpreferences.getBoolean("remove_data",false));
        }
        return super.onPrepareOptionsMenu(menu);
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
                reload();
                break;
            case R.id.remove_data:
                SharedPreferences sharedpreferences = getSharedPreferences("iching", Context.MODE_PRIVATE);
                SharedPreferences.Editor e = sharedpreferences.edit();
                e.putBoolean("remove_data",!item.isChecked());
                item.setChecked(item.isChecked());
                e.commit();

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
            showPesquisas();
            return;
        }
        if(!isReloading()) {
            setReloading(true);
            prog = new NewtProgressDialog(this,R.layout.preloader_dialog);
            prog.show();
            prog.setTitle(getString(R.string.app_name));
            prog.setIcon(ContextCompat.getDrawable(context, R.drawable.tolbar_icon));
            ((Button)prog.findViewById(R.id.cancel_butt)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelDownload();
                    finish();
                    requestLogin();
                }
            });
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
                OkHttpClient client = new OkHttpClient.Builder().cookieJar(cookieJar).connectTimeout(60,TimeUnit.SECONDS).build();
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
                Snackbar.make(findViewById(R.id.content_lista), e.getMessage(), Snackbar.LENGTH_LONG).show();
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
                final boolean[] remember = new boolean[]{true};
                switch(todo){
                    case EXIT:
                        if(Debug.isDebuggerConnected()) {
                            final CharSequence[] items = {getString(R.string.remember_city)};
                            AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setCancelable(false)
                                    .setTitle(getString(R.string.logout))
                                    .setMultiChoiceItems(items, remember, new DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                                            remember[0] = isChecked;
                                        }
                                    }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (!remember[0]) {
                                                SharedPreferences sharedpreferences = getSharedPreferences("login", Context.MODE_PRIVATE);
                                                SharedPreferences.Editor e = sharedpreferences.edit();
                                                if (sharedpreferences.contains("cidade")) {
                                                    e.remove("cidade");
                                                    e.commit();
                                                }
                                            }
                                            ((IChing) getApplicationContext()).setCookieJar(null);
                                            requestLogin();
                                        }
                                    }).create();
                            dialog.show();
                        }else{
                            ((IChing) getApplicationContext()).setCookieJar(null);
                            requestLogin();
                        }
                        break;
                }
            }else {
                showPesquisas();
            }
        }
    }

    private void resetJournal(String j) {
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("journal", j);
        editor.commit();
    }

    private void showEnderecosOrHabitantes(JSONObject pesquisa){
        setTitle(pesquisa.optString("nom"));
        List<String> names=new ArrayList<>();
        List<String> presets=new ArrayList<>();
        List<String> focos=new ArrayList<>();
        List<String> quants=new ArrayList<>();
        SharedPreferences sharedpreferences = getSharedPreferences("results", Context.MODE_PRIVATE);
        media=new ArrayList<>();
        cleanUp();
        totalsize = 0;
        currentsize=0;
        List<Boolean> issus = new ArrayList<>();
        if(pesquisa.has("habi")) {
            JSONArray habi = pesquisa.optJSONArray("habi");
            for (int i = 0; i < habi.length(); i++) {
                JSONObject it = habi.optJSONObject(i);
                names.add(it.optString("habi1_nom"));
                presets.add(it.toString());
                focos.add("habi");
                issus.add(pesquisa.optBoolean("sus"));
                quants.add("");
            }
        }else if(pesquisa.has("ende")){
            JSONArray ende = pesquisa.optJSONArray("ende");
            for (int i = 0; i < ende.length(); i++) {
                JSONObject it = ende.optJSONObject(i);
                String l=it.optString("ende1_logr");
                if(it.optString("ende1_num", "").length()>0) l+=", "+it.optString("ende1_num");
                names.add(l);
                presets.add(it.toString());
                focos.add("ende");
                issus.add(pesquisa.optBoolean("sus"));
                quants.add("");
            }
        }
        if(!pesquisa.optBoolean("obrig",false)){
            names.add(getString(R.string.nova));
            presets.add("");
            if(pesquisa.has("ende"))
                focos.add("ende");
            else
                focos.add("habi");
            issus.add(pesquisa.optBoolean("sus"));
            quants.add("");
        }
        ((ListView) findViewById(R.id.lista_list)).setAdapter(new StuffAdapter<String>(this, R.layout.content_lista_item, names, presets, focos, issus, quants));
    }

    private void showPesquisas() {
        setTitle(getString(R.string.app_name));
        if(stuff==null)return;
        List<String> names=new ArrayList<>();
        List<String> focos=new ArrayList<>();
        List<Boolean> issus=new ArrayList<>();
        List<String> quants=new ArrayList<>();
        List<String> presets=new ArrayList<>();
        media=new ArrayList<>();
        cleanUp();
        totalsize = 0;
        currentsize=0;
        for(int i=0;i<stuff.length();i++){
            JSONObject it = stuff.optJSONObject(i);
            presets.add("");
            names.add(it.optString("nom"));
            focos.add(it.optString("foco","geral"));
            issus.add(it.optBoolean("sus"));
            String q="";
            if(it.has("habi")){
                if(it.optJSONArray("habi").length()>0)
                    q=""+it.optJSONArray("habi").length();
            }
            if(it.has("ende")){
                if(it.optJSONArray("ende").length()>0)
                    q=""+it.optJSONArray("ende").length();
            }
            quants.add(q);
            if(it.has("midia")){
                String filename=it.optString("midia");
                File file = new File(getExternalCacheDir()+File.separator+"midia"+File.separator+filename);
                if(!Util.isValid(file)){
                    media.add(filename);
                    totalsize+=it.optInt("msize");
                }
            }
        }
        setReloading(false);
        if(prog!=null){
            prog.dismiss();
            if(media.size()>0) {
                showProgressDialog();
                new MediaLoader(media.get(0)).execute();
            }
        }
        ((ListView)findViewById(R.id.lista_list)).setAdapter(new StuffAdapter<String>(this,R.layout.content_lista_item,names,presets,focos,issus, quants));
    }

    private void showProgressDialog() {
        prog = new NewtProgressDialog(this,R.layout.medialoader_dialog);
        findViewById(R.id.content_lista).setKeepScreenOn(true);
        prog.show();
        ((Button)prog.findViewById(R.id.cancel_butt)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelDownload();
                finish();
                requestLogin();
            }
        });
        prog.setMax(totalsize);
        prog.setTitle(getString(R.string.app_name));
        prog.setIcon(ContextCompat.getDrawable(context, R.drawable.tolbar_icon));
    }

    private void cancelDownload() {
        cancel_download=true;
    }

    private class MediaLoader extends AsyncTask<Void, Void, Boolean> {
        private final String filename;
        private final Handler handler;
        private String mess;

        public MediaLoader(String f) {
            filename=f;
            handler=new MediaLoaderHandler();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File directory = new File(getExternalCacheDir(),"midia");
            if(!directory.exists()) {
                directory.mkdirs();
            }
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
                FileOutputStream fos = new FileOutputStream(getExternalCacheDir() + File.separator + "midia" + File.separator + filename);
                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    currentsize+=1024;
                    prog.setProgress(currentsize);
                    fos.write(data, 0, count);
                    Message msg = new Message();
                    msg.what=currentsize;
                    handler.sendMessage(msg);
                }
                fos.flush();
                fos.close();
                input.close();
            } catch (IOException e) {
                mess=e.getMessage();
                return false;
            }
            Util.unzip(getExternalCacheDir()+File.separator+"midia",filename);
            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            if(!success){
                Snackbar.make(findViewById(R.id.content_lista),mess,Snackbar.LENGTH_LONG).show();
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle(getString(R.string.problems_downloading))
                        .setMessage(getString(R.string.try_again))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showPesquisas();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                prog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

                return;
            }
            media.remove(media.indexOf(filename));
            if(media.size()==0) {
                setReloading(false);
                if(prog!=null && prog.isShowing())
                    prog.dismiss();
            }else{
                if(!cancel_download)
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
                        if(jk!=null){
                            Iterator<String> biter = jk.keys();
                            while(biter.hasNext()){
                                String bey = biter.next();
                                if(jk.optJSONObject(bey).has("midia"))
                                    filenames.put(jk.optJSONObject(bey).optString("midia"),false);
                            }
                        }
                    }
                }
            }

            File directory = new File(getExternalCacheDir()+File.separator+"midia");
            File[] files = directory.listFiles();
            if(files!=null) {
                for (int i = 0; i < files.length; i++) {
                    if (!filenames.has(files[i].getName())) {
                        files[i].delete();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class StuffAdapter<String> extends ArrayAdapter {
        private final List<String> names;
        private final int resourceId;
        private final List<String> focos;
        private final List<Boolean> issus;
        private final List<String> quants;
        private final List<String> presets;


        public StuffAdapter(Context context, int resource, List<String> n, List<String> p, List<String> f, List<Boolean> sus, List<String> q){
            super(context, resource, n);
            resourceId=resource;
            names=n;
            focos=f;
            issus = sus;
            quants=q;
            presets=p;
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
            Bitmap bi=((IChing)getApplicationContext()).getItemBitmap((java.lang.String) focos.get(position),issus.get(position));
            ((ImageView)v.findViewById(R.id.bullet)).setImageBitmap(bi);
            ((TextView)v.findViewById(R.id.counter)).setText(quants.get(position).toString());
            ((TextView)v.findViewById(R.id.preset)).setText(presets.get(position).toString());
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
    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
    }

    private class MediaLoaderHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int completed= (int) Math.round(100.0*msg.what/prog.getMax());
            if(completed>100)completed=100;
            ((TextView)prog.findViewById(R.id.completed_text)).setText(String.format("%s%%",completed));
        }
    }
    /*
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
    }
    */
    @Override
    public void onBackPressed() {
        if(current_pesquisa!=null){
            current_pesquisa=null;
            showPesquisas();
        }
    }
}
