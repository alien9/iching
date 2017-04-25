package net.alien9.iching;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static android.view.View.GONE;
import static net.alien9.iching.R.id.perg_id;
import static net.alien9.iching.R.id.shortcut;

public class Question extends AppCompatActivity{
    private static final int TYPE_TEXT = 2;
    private static final int TYPE_RADIO = 3;
    private static final int TYPE_CHECKBOX = 4;
    private static final int TYPE_CAMERA = 5;
    private static final int TYPE_UNICA = 6;
    private static final int TYPE_DATE = 7;
    private static final int TYPE_MULTIPLA = 8;
    private static final int TYPE_NUMBER = 9;
    private static final int TYPE_YESORNO = 10;
    private static final int TYPE_MIDIA = 11;
    private static final int TYPE_TABLE = 12;
    private static final int TYPE_HABI = 13;
    private static final int TYPE_ENDE = 14;
    private static final Hashtable<String, Integer> FIELD_TYPES = new Hashtable<String, Integer>() {{
        put("radio", TYPE_RADIO);
        put("checkbox", TYPE_CHECKBOX);
        put("textual", TYPE_TEXT);
        put("camera", TYPE_CAMERA);
        put("unica", TYPE_UNICA);
        put("data", TYPE_DATE);
        put("multipla", TYPE_MULTIPLA);
        put("numerica", TYPE_NUMBER);
        put("simnao", TYPE_YESORNO);
        put("midia", TYPE_MIDIA);
        put("tabela", TYPE_TABLE);
        put("habitante", TYPE_HABI);
        put("endereco", TYPE_ENDE);
    }};
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int POSITION_UPDATE = 0;
    private static final int ICHING_REQUEST_GPS_PERMISSION = 0;
    private static final int PAGE_UP = 1;
    private static final int PAGE_DOWN = -1;
    private static final int NOTHING = 0;
    private File imageFile;
    private JSONObject polly;
    private String cookies;
    private boolean jadeu;
    private boolean encerrabody=false;
    private String prox="";
    private int previous_index=-1;
    private MediaPlayer mp;
    private SurfaceHolder sh;
    private String comment="";
    private boolean isPagingUp;
    private int toDo;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        isPagingUp=false;
        if(intent.hasExtra("poll")){
            String h = intent.getExtras().getString("poll");
            try {
                polly=new JSONObject(h);
                ((IChing)getApplicationContext()).setCod(polly.optString("cod"));
                if(polly.has("preset")){
                    ((IChing)getApplicationContext()).setRespostas(polly.optJSONObject("preset"));
                }
            } catch (JSONException ignore) {
                Snackbar.make(findViewById(R.id.main_view), "Dados Incorretos", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
        }
        if(intent.hasExtra("CNETSERVERLOGACAO")){
            cookies = intent.getExtras().getString("CNETSERVERLOGACAO");
        }
        setContentView(R.layout.activity_question);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.tolbar_icon);
        Bitmap bu=((IChing) getApplicationContext()).getItemBitmap(polly.optString("foco", "geral"));
        getSupportActionBar().setTitle(polly.optString("nom"));
        final IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
        final View te=findViewById(R.id.messenger_layout);
        if(polly.has("msgini")){
            findViewById(R.id.next).setVisibility(GONE);
            findViewById(R.id.previous).setVisibility(GONE);
            pu.setVisibility(GONE);
            ((TextView)te.findViewById(R.id.message_textView)).setText(polly.optString("msgini"));
            te.setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.next).setVisibility(View.VISIBLE);
            te.setVisibility(GONE);
            pu.setVisibility(View.VISIBLE);
        }
        PagerAdapter pa = new BunchViewAdapter(this);
        pu.setOffscreenPageLimit(pa.getCount());
        pu.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return true;
            }
        });

        pu.setAdapter(pa);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Activity budega = (Activity) this;
            ActivityCompat.requestPermissions(budega,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ICHING_REQUEST_GPS_PERMISSION);
            return;
        }
        jadeu=false;
        ((Button)findViewById(R.id.continue_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                te.setVisibility(GONE);
                if(jadeu)
                    encerra();
                else {
                    pu.setVisibility(View.VISIBLE);
                    findViewById(R.id.next).setVisibility(View.VISIBLE);
                    if(pu.getCurrentItem()>0)findViewById(R.id.previous).setVisibility(View.VISIBLE);
                }
            }
        });

        ((ImageButton)findViewById(R.id.next)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPagingUp) return;
                isPagingUp=true;
                NestedScrollView sv = (NestedScrollView)findViewById(R.id.content_scroller);
                if(sv.getScrollY()>0) {
                    setToDo(Question.PAGE_UP);
                    sv.scrollTo(0, 0);
                }else{
                    pageup();
                }
            }
        });
        ((ImageButton)findViewById(R.id.previous)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPagingUp) return;
                isPagingUp=true;
                NestedScrollView sv = (NestedScrollView)findViewById(R.id.content_scroller);
                setToDo(Question.PAGE_DOWN);
                if(sv.getScrollY()>0) {
                    setToDo(Question.PAGE_UP);
                    sv.scrollTo(0, 0);
                }else{
                    pagedown();
                }

            }
        });
        pu.addOnPageChangeListener(new IChingViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("iching Coisa",""+positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                findViewById(R.id.previous).setVisibility(View.VISIBLE);
                findViewById(R.id.next).setVisibility(View.VISIBLE);
                if(position==0) {
                    findViewById(R.id.previous).setVisibility(GONE);
                }
                //ViewGroup.LayoutParams params = pu.getLayoutParams();
                //params.height = 1000;
                //pu.setLayoutParams(params);
                //if(position==pu.getAdapter().getCount()-1)
                //    findViewById(R.id.next).setVisibility(View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d("iching page scroll",""+state);
                //if(state==ViewPager.SCROLL_STATE_IDLE)
                //isPagingUp=false;
            }
        });

        JSONObject respuestas = ((IChing)getApplicationContext()).getRespostas();
        Calendar c=Calendar.getInstance();
        try {
            if(!respuestas.has("dataehora")){
                respuestas.put("dataehora",Math.round(c.getTimeInMillis()/1000));
            }
            if(!respuestas.has("gps")){
                JSONObject g = ((IChing) getApplicationContext()).getLastKnownPosition();
                final SharedPreferences sharedpreferences=getSharedPreferences("position",MODE_PRIVATE);
                if(g==null){
                    g = new JSONObject(sharedpreferences.getString("gps","{}"));
                }else{
                    SharedPreferences.Editor e = sharedpreferences.edit();
                    e.putString("gps", g.toString());
                    e.commit();
                }

                if(g!=null) {
                    if (g.has("latitude")) {
                        respuestas.put("gps", String.format("%s %s", g.optString("latitude"), g.optString("longitude")));
                        respuestas.put("gpsprec", g.optString("accuracy"));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        NestedScrollView sv = (NestedScrollView)findViewById(R.id.content_scroller);
        sv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                switch (getToDo()){
                    case PAGE_DOWN:
                        pagedown();
                        break;
                    case PAGE_UP:
                        pageup();
                        break;

                }
                setToDo(Question.NOTHING);
            }
        });
    }

    private void pagedown() {
        final IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
        if(!((IChing)getApplication()).hasUndo()){
            int cu = pu.getCurrentItem();
            if(previous_index>=0){
                pu.setCurrentItem(previous_index);
                previous_index=-1;
            }else {
                if (cu > 0)
                    pu.setCurrentItem(cu - 1, true);
            }
            findViewById(R.id.previous).setVisibility((pu.getCurrentItem()==0)? GONE:View.VISIBLE);
        }else{
            isPagingUp=false;
        }
        ((IChing)getApplication()).setUndo();
    }

    private void pageup(String c){
        comment=c;
        pageup();
    }
    private void pageup() {
        if(!validate()) {
            isPagingUp=false;
            return;
        }
        IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
        ((IChing)getApplication()).resetUndo();
        int cu = pu.getCurrentItem();
        previous_index=cu;
        ArrayList keynames = ((BunchViewAdapter)pu.getAdapter()).keynames;
        if((cu<pu.getAdapter().getCount()-1)&&!encerrabody) {
            if(!prox.equals("")){
                boolean exist=false;
                JSONObject pergs = polly.optJSONObject("pergs");
                for(int i=0;i<keynames.size();i++){
                    String key= (String) keynames.get(i);
                    if(key.equals(prox)){
                        pu.setCurrentItem(i);
                        exist=true;
                        break;
                    }else if(pergs.has(key)){ // é uma pergunta convencional
                        if(pergs.optJSONObject(key).has("pergs")) { // tem sub questoes
                            if (pergs.optJSONObject(key).optJSONObject("pergs").has(prox)) {
                                pu.setCurrentItem(i);
                                exist = true;
                            }
                        }
                    }
                }
                if(!exist) pu.setCurrentItem(cu + 1, true); // situação anormal, a pregunta indicada COMO PRÓXIMA NÃO EXISTE
            }else {
                pu.setCurrentItem(cu + 1, true);
            }
        }else{
            termina();
        }
        isPagingUp=false;
    }

    private void termina() {
        isPagingUp=false;
        jadeu=true;
        if(polly.has("msgfim")){
            IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
            findViewById(R.id.next).setVisibility(GONE);
            findViewById(R.id.previous).setVisibility(GONE);
            pu.setVisibility(GONE);
            View te=findViewById(R.id.messenger_layout);
            ((TextView)te.findViewById(R.id.message_textView)).setText(polly.optString("msgfim"));
            te.setVisibility(View.VISIBLE);
            findViewById(R.id.main_view).setVisibility(GONE);
            return;
        }
        encerra();
    }

    private void encerra() {
        IChing iching = (IChing) getApplicationContext();
        JSONObject respuestas = iching.getRespostas();
        Intent intent = new Intent(this, Lista.class);
        intent.putExtra("result",respuestas.toString());
        intent.putExtra("cod",iching.getCod());
        intent.putExtra("CNETSERVERLOGACAO",cookies);
        iching.setRespostas(new JSONObject());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_question, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.action_quit:
                validate();
                interrompe();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void interrompe() {
        LayoutInflater li = LayoutInflater.from(this);
        final View promptsView = li.inflate(R.layout.encerrar_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setTitle("Cancelamento")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                JSONObject resps = ((IChing) getApplicationContext()).getRespostas();
                                try {
                                    resps.put("motivo_cancelamento",((TextView)promptsView.findViewById(R.id.motivo_cancelamento)).getText());
                                } catch (JSONException ignored) {}
                                ((IChing) getApplicationContext()).setRespostas(resps);
                                encerra();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void setToDo(int t) {
        toDo = t;
    }

    public int getToDo() {
        return toDo;
    }

    private class BunchViewAdapter extends PagerAdapter {
        private final Context context;
        public ArrayList<String> keynames;
        private int counta;

        public BunchViewAdapter(Context c) {
            context = c;
            counta=-1;
        }
        @Override
        public int getCount() {
            if(!polly.has("pergs")) return 0;
            if(counta>=0)return counta;
            JSONObject pergs = null;
            try {
                pergs = polly.getJSONObject("pergs");
            } catch (JSONException e) {
                pergs=new JSONObject();
            }

            keynames=new ArrayList<String>();
            List<JSONObject> things = new ArrayList<>();
            counta=0;

            if(polly.optBoolean("comhabi",true)){
                keynames.add("habi");
                counta++;
            }
            if(polly.optBoolean("comende",true)){
                keynames.add("ende");
                counta++;
            }

            Iterator<?> keys = pergs.keys();
            try {
                while( keys.hasNext() ) {
                    String k = (String) keys.next();
                    JSONObject j = pergs.optJSONObject(k);
                    j.put("cod",k);
                    things.add(j);
                    counta++;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Collections.sort(things,new ThingsSorter());
            for(int i=0;i<things.size();i++){
                keynames.add(things.get(i).optString("cod"));
            }
            return counta;
        }
        @Override
        public Object instantiateItem(ViewGroup collection, final int position) {
            LayoutInflater inflater = LayoutInflater.from(context);
            ViewGroup v = null;
            JSONObject respuestas = ((IChing) getApplication()).getRespostas();
            try {
                String perg_id=keynames.get(position);
                JSONObject item=new JSONObject();
                if(polly.getJSONObject("pergs").has(perg_id)){
                    item = polly.getJSONObject("pergs").getJSONObject(perg_id);
                }else{
                    if(perg_id.equals("habi")){
                        item.put("tipo","habitante");
                    }
                    if(perg_id.equals("ende")){
                        item.put("tipo","endereco");
                    }
                }
                if(item.has("pergs")){ // tipo multiplas pergs
                    item.put("tipo","tabela");
                }
                String tip=item.optString("tipo","text");
                int t = 0;
                try {
                    t = FIELD_TYPES.get(tip);
                }catch(NullPointerException xixi){
                    Log.d("ICHING",tip+" ********************** "+xixi.getMessage());
                }
                JSONObject a = item.optJSONObject("resps");
                ArrayList<String> respskeys = new ArrayList<String>();
                JSONObject resps = null;
                if(item.has("resps")) {
                    resps = item.optJSONObject("resps");
                    Iterator<?> keys = resps.keys();
                    int n = 0;
                    while (keys.hasNext()) {
                        respskeys.add((String) keys.next());
                    }
                }
                switch(t){
                    case TYPE_HABI:
                        v = (ViewGroup) inflater.inflate(R.layout.type_habitante, collection, false);
                        String data_atual;
                        Calendar c = Calendar.getInstance();
                        if(respuestas.has("hab1_dat_nasc"))
                            data_atual=respuestas.optString("hab1_dat_nasc");
                        else{
                            data_atual=String.format("%02d/%02d/%04d",c.get(c.DAY_OF_MONTH),c.get(c.MONTH)+1,c.get(Calendar.YEAR));
                        }
                        setupDateField(context,v,data_atual,c.get(Calendar.YEAR));
                        ((EditText)v.findViewById(R.id.editText_habi1_nom)).setText(respuestas.optString("habi1_nom",""));
                        if(respuestas.optString("habi1_sex","X").equals("M"))
                            ((RadioButton)v.findViewById(R.id.masculino_radiobutton)).setChecked(true);
                        if(respuestas.optString("habi1_sex","X").equals("F"))
                            ((RadioButton)v.findViewById(R.id.feminino_radiobutton)).setChecked(true);
                        break;
                    case TYPE_ENDE:
                        /*
ende1_cod
ende1_logr
ende1_num
ende1_bai
ende1_compl
ende1_lat
ende1_lng
                        *
                        * */

                        v = (ViewGroup) inflater.inflate(R.layout.type_endereco, collection, false);
                        ((EditText)v.findViewById(R.id.editText_nomedarua)).setText(respuestas.optString("ende1_logr"));
                        ((EditText)v.findViewById(R.id.editText_numero)).setText(respuestas.optString("ende1_num"));
                        ((EditText)v.findViewById(R.id.editText_complemento)).setText(respuestas.optString("ende1_compl"));
                        ((EditText)v.findViewById(R.id.editText_telefone)).setText(respuestas.optString("ende1_tele"));
                        ((EditText)v.findViewById(R.id.editText_bairro)).setText(respuestas.optString("ende1_bai"));
                        ((EditText)v.findViewById(R.id.editText_cidade)).setText(respuestas.optString("ende1_cida"));
                        ((TextView)v.findViewById(R.id.ende1_lat)).setText(respuestas.optString("ende1_lat"));
                        ((TextView)v.findViewById(R.id.ende1_lng)).setText(respuestas.optString("ende1_lng"));
                        ((TextView)v.findViewById(R.id.ende1_cod)).setText(respuestas.optString("ende1_cod"));
                        String[] estados=getResources().getStringArray(R.array.estados);
                        int uf=Arrays.asList(estados).indexOf(respuestas.optString("ende1_uf"));
                        if(uf>=0) {
                            ((Spinner) v.findViewById(R.id.estado_spinner)).setSelection(uf);
                        }

                        break;
                    case TYPE_RADIO:
                    case TYPE_UNICA:
                    case TYPE_YESORNO:
                        v = (ViewGroup) inflater.inflate(R.layout.type_radio_question, collection, false);
                        /*final JSONObject finalResps = resps;
                        final View vuc = v.findViewById(R.id.comments_request);
                        CompoundButton.OnCheckedChangeListener l=new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                String quem= (String) compoundButton.getTag();
                                if(compoundButton.isChecked()){
                                    TextView fu = (TextView) findViewById(R.id.ask_for_comment);
                                    if(finalResps.optJSONObject(quem).has("expl")){
                                        vuc.setVisibility(View.VISIBLE);
                                        if(fu!=null) fu.setText(finalResps.optJSONObject(quem).optString("ins"));
                                    }else{
                                        if(fu!=null) fu.setText("");
                                        vuc.setVisibility(View.GONE);
                                    }
                                    if(finalResps.optJSONObject(quem).optString("fim","0").equals("1")){
                                        encerrabody=true;
                                    }else{
                                        encerrabody=false;
                                    }
                                }
                            }
                        };*/
                        for(int i=0;i<respskeys.size();i++){
                            LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            RadioButton bu = (RadioButton) vi.inflate(R.layout.radio_button_item, null);
                            //RadioButton bu = new RadioButton(context);//,null,R.style.checkstyle);
                            bu.setText(resps.optJSONObject(respskeys.get(i)).optString("txt"));
                            String tag=respskeys.get(i);
                            bu.setTag(tag);
                            //bu.setOnCheckedChangeListener(l);
                            if(respuestas.has(perg_id)) {
                                if (respuestas.optJSONObject(perg_id).optString("v").equals(tag)) {
                                    bu.setChecked(true);
                                }
                            }
                            ((ViewGroup)v.findViewById(R.id.multipla_radio)).addView(bu);
                        }
                        break;
                    case TYPE_TABLE:
                        v = (ViewGroup) inflater.inflate(R.layout.type_table_question, collection, false);
                        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        JSONObject subpergs = item.optJSONObject("pergs");
                        Iterator<?> keys = subpergs.keys();
                        int n=0;
                        while (keys.hasNext()) {
                            String subitem_key = keys.next().toString();
                            JSONObject subitem = subpergs.optJSONObject(subitem_key);
                            int tipy;
                            try {
                                tipy = FIELD_TYPES.get(subitem.optString("tipo"));
                            }catch(NullPointerException xixi){
                                tipy=TYPE_TEXT;
                            }
                            LinearLayout lu;
                            switch(tipy){
                                case TYPE_RADIO:
                                case TYPE_UNICA:
                                case TYPE_YESORNO:
                                    lu = (LinearLayout) vi.inflate(R.layout.type_radio_mini, null);
                                    RadioGroup gu = (RadioGroup) lu.findViewById(R.id.radio_mini_group);
                                    ((TextView)lu.findViewById(R.id.perg_textview)).setText(subitem.optString("txt"));
                                    JSONObject subresps = subitem.optJSONObject("resps");
                                    Iterator<?> subkeys = subresps.keys();
                                    while (subkeys.hasNext()) {
                                        String k = (String) subkeys.next();
                                        JSONObject resp = subresps.optJSONObject(k);
                                        RadioButton bu = (RadioButton) vi.inflate(R.layout.radio_button_item, null);
                                        bu.setText(resp.optString("txt"));
                                        bu.setTag(k);
                                        gu.addView(bu);
                                    }
                                    break;
                                case TYPE_NUMBER:
                                    lu = (LinearLayout) vi.inflate(R.layout.type_number_mini, null);
                                    break;
                                case TYPE_DATE:
                                    lu = (LinearLayout) vi.inflate(R.layout.type_date_mini, null);
                                    c = Calendar.getInstance();
                                    if(respuestas.has(subitem_key)) {
                                        data_atual = respuestas.optJSONObject(subitem_key).optString("v");
                                    }else{
                                        data_atual=String.format("%02d/%02d/%04d",c.get(c.DAY_OF_MONTH),c.get(c.MONTH)+1,c.get(Calendar.YEAR));
                                    }
                                    setupDateField(context,lu,data_atual,item.optInt("anomax", c.get(Calendar.YEAR)));
                                    break;
                                case TYPE_TEXT:
                                default:
                                    lu = (LinearLayout) inflater.inflate(R.layout.type_text_mini, null);

                            }
                            lu.setBackgroundColor((n%2==0)?Color.parseColor("#0000ff00"):Color.parseColor("#ffdddddd"));
                            ((TextView)lu.findViewById(R.id.subperg_id)).setText(subitem_key);
                            TextView tit = (TextView) lu.findViewById(R.id.subtitle_text);
                            if(tit!=null) tit.setText(subitem.optString("txt"));
                            // isto com certeza está ruim - não mostramos os campos
                            lu.findViewById(R.id.decline_layout).setVisibility(GONE);
                            //lu.findViewById(R.id.comments_request).setVisibility(View.GONE);
                            /*
                            if(subitem.has("resps")){
                                if(subitem.optJSONObject("resps").optJSONObject("1").optString("expl","0").equals("1")){
                                    lu.findViewById(R.id.comments_request).setVisibility(View.VISIBLE);
                                }else{
                                    lu.findViewById(R.id.comments_request).setVisibility(View.GONE);
                                }
                            }
                            */
                            ((ViewGroup)v.findViewById(R.id.suport_table_layout)).addView(lu);
                            ((TextView)v.findViewById(R.id.perg_id)).setText(perg_id);
                            n++;
                        }
                        break;
                    case TYPE_CHECKBOX:
                    case TYPE_MULTIPLA:
                        JSONObject r = respuestas.optJSONObject(perg_id);
                        if(r==null)r=new JSONObject();
                        v = (ViewGroup) inflater.inflate(R.layout.type_checkbox_question, collection, false);
                        for(int i=0;i<respskeys.size();i++){
                            CheckBox bu = new CheckBox(context);
                            bu.setText(resps.optJSONObject(respskeys.get(i)).optString("txt"));
                            bu.setTag(respskeys.get(i));
                            if(r.optBoolean(respskeys.get(i)))
                                bu.setChecked(true);
                            ((ViewGroup)v.findViewById(R.id.checkbox_container)).addView(bu);
                        }
                        break;
                    case TYPE_CAMERA:
                        v = (ViewGroup) inflater.inflate(R.layout.type_camera_question, collection, false);
                        ((Button)v.findViewById(R.id.camera_button)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestCamera();
                            }
                        });
                        break;
                    case TYPE_DATE:
                        v = (ViewGroup) inflater.inflate(R.layout.type_date_split_question, collection, false);
                        c = Calendar.getInstance();
                        if(respuestas.has(perg_id))
                            data_atual=respuestas.optJSONObject(perg_id).optString("v");
                        else{
                            data_atual=String.format("%02d/%02d/%04d",c.get(c.DAY_OF_MONTH),c.get(c.MONTH)+1,c.get(Calendar.YEAR));
                        }
                        setupDateField(context,v,data_atual,item.optInt("anomax", c.get(Calendar.YEAR)));
                        break;
                    case TYPE_NUMBER:
                        n=0;
                        if(!item.has("resps")){
                            v = (ViewGroup) inflater.inflate(R.layout.type_number_question, collection, false);
                            ((EditText)v.findViewById(R.id.value_edittext)).setText(respuestas.optString(perg_id));
                        }else {
                            JSONObject jake = item.optJSONObject("resps").optJSONObject("1");
                            final int minimum = jake.optInt("menorval",0);
                            final int maximum= jake.optInt("maiorval",10);
                            v = (ViewGroup) inflater.inflate(R.layout.type_range_question, collection, false);
                            ((EditText)v.findViewById(R.id.value_edittext)).setText(respuestas.optString(perg_id));
                            Bitmap bm = Bitmap.createBitmap(1168, 172, Bitmap.Config.ARGB_8888);
                            Paint paint = new Paint();
                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(Color.BLACK);
                            paint.setTextSize(62);
                            Canvas canvas = new Canvas(bm);
                            canvas.drawText("" + minimum, 5, 160, paint);
                            paint.setTextAlign(Paint.Align.RIGHT);
                            canvas.drawText("" + maximum, 1163, 160, paint);
                            v.findViewById(R.id.seek_layout).setBackground(new BitmapDrawable(getResources(), bm));
                            SeekBar s = (SeekBar) v.findViewById(R.id.seek);
                            final View v2=v;
                            EditText seeker = (EditText) v.findViewById(R.id.value_edittext);
                            if(s!=null){
                                s.setMax(maximum-minimum);
                                s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                        ((EditText) v2.findViewById(R.id.value_edittext)).setText(""+(minimum+i));
                                    }

                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) {

                                    }

                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) {

                                    }
                                });
                                n = (maximum-minimum) / 2;
                                s.setProgress(n);
                            }

                            seeker.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                                @Override
                                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                    if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                            actionId == EditorInfo.IME_ACTION_DONE ||
                                            event.getAction() == KeyEvent.ACTION_DOWN &&
                                                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                        int where;
                                        try {
                                            where = Integer.parseInt(v.getText().toString());
                                        } catch (NumberFormatException ex) {
                                            return true;
                                        }
                                        if (where > maximum)
                                            where = maximum;
                                        if (where < minimum)
                                            where = minimum;
                                        ((SeekBar) v2.findViewById(R.id.seek)).setProgress(where - minimum);
                                        return false;
                                    }

                                    return false;
                                }
                            });
                        }
                        break;
                    case TYPE_TEXT:
                        v = (ViewGroup) inflater.inflate(R.layout.type_text_question, collection, false);
                        ((EditText)v.findViewById(R.id.value_edittext)).setText(respuestas.optString(perg_id));
                        break;
                    case TYPE_MIDIA:
                        v = (ViewGroup) inflater.inflate(R.layout.type_image_question, collection, false);
                        final String filename = item.optJSONObject("resps").optJSONObject("1").optString("midia");
                        if(filename.matches(".*\\.mp4")){
                            v.findViewById(R.id.play).setVisibility(View.VISIBLE);
                            v.findViewById(R.id.imageView).setVisibility(GONE);
                            ((ImageButton)v.findViewById(R.id.play)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    findViewById(R.id.content_scroller).setVisibility(GONE);
                                    findViewById(R.id.next).setVisibility(GONE);
                                    findViewById(R.id.previous).setVisibility(GONE);
                                    findViewById(R.id.video_container).setVisibility(View.VISIBLE);
                                    getSupportActionBar().hide();
                                    final VideoView vw = (android.widget.VideoView) findViewById(R.id.video_view);
                                    vw.setVideoPath(getExternalCacheDir() + File.separator + "midia" + File.separator+filename);
                                    vw.setOnTouchListener(new View.OnTouchListener() {
                                        @Override
                                        public boolean onTouch(View view, MotionEvent motionEvent) {
                                            if(motionEvent.getAction()==MotionEvent.ACTION_DOWN) {
                                                if((findViewById(R.id.video_controller).isShown())){
                                                    findViewById(R.id.video_controller).setVisibility(GONE);
                                                }else
                                                    findViewById(R.id.video_controller).setVisibility(View.VISIBLE);
                                            }
                                            return true;
                                        }
                                    });
                                    vw.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        public void onPrepared(MediaPlayer mediaPlayer) {
                                            vw.seekTo(0);
                                            vw.start();
                                        }
                                    });
                                    vw.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                            finishVideo();
                                        }
                                    });
                                    ((ImageButton)findViewById(R.id.rew)).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            vw.seekTo(0);
                                        }
                                    });
                                    ((ImageButton)findViewById(R.id.ff)).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            vw.pause();
                                            finishVideo();
                                        }
                                    });
                                    ((ImageButton)findViewById(R.id.pause)).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if(vw.isPlaying()) {
                                                vw.pause();
                                                ((ImageButton)findViewById(R.id.pause)).setImageResource(android.R.drawable.ic_media_play);
                                            }else{
                                                vw.start();
                                                ((ImageButton)findViewById(R.id.pause)).setImageResource(android.R.drawable.ic_media_pause);
                                            }
                                        }
                                    });


                                }
                            });
                        }else{
                            v.findViewById(R.id.play).setVisibility(GONE);
                            v.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
                            Bitmap b = BitmapFactory.decodeFile(getExternalCacheDir() + File.separator + "midia" + File.separator+filename);
                            if(b==null){
                                Snackbar.make(findViewById(R.id.main_view),"Arquivo não encontrado",Snackbar.LENGTH_LONG).show();
                            }
                            ((ImageView)v.findViewById(R.id.imageView)).setImageBitmap(b);
                        }
                        break;
                    default:
                        v = (ViewGroup) inflater.inflate(R.layout.type_text_question, collection, false);
                        break;
                }
                if(v.findViewById(R.id.type_of_question)!=null)
                    ((TextView)v.findViewById(R.id.type_of_question)).setText(""+t);
                v.findViewById(R.id.decline_layout).setVisibility(GONE);
                if(item.optBoolean("naosei",false)){
                    v.findViewById(R.id.decline_layout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.naosei).setVisibility(View.VISIBLE);
                }else{
                    v.findViewById(R.id.naosei).setVisibility(GONE);
                }
                if(item.optBoolean("naoresp",false)){
                    v.findViewById(R.id.decline_layout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.naoresp).setVisibility(View.VISIBLE);
                }else{
                    v.findViewById(R.id.naoresp).setVisibility(GONE);
                }
                if(v.findViewById(R.id.decline_layout)!=null) {
                    if (v.findViewById(R.id.decline_layout).getVisibility() == View.VISIBLE) {
                        final ViewGroup finalV = v;
                        ((CheckBox) v.findViewById(R.id.naosei)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                if (b) {
                                    ((CheckBox) finalV.findViewById(R.id.naoresp)).setChecked(false);
                                    FormControl.freeze(finalV);
                                    FormControl.thaw(finalV.findViewById(R.id.decline_layout));
                                }else if(!((CheckBox)finalV.findViewById(R.id.naoresp)).isChecked()){
                                    FormControl.thaw(finalV);
                                }
                            }
                        });
                        ((CheckBox) v.findViewById(R.id.naoresp)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                if (b) {
                                    ((CheckBox) finalV.findViewById(R.id.naosei)).setChecked(false);
                                    FormControl.freeze(finalV);
                                    FormControl.thaw(finalV.findViewById(R.id.decline_layout));
                                }else if(!((CheckBox)finalV.findViewById(R.id.naosei)).isChecked()){
                                    FormControl.thaw(finalV);
                                }
                            }
                        });
                    }
                }
                String title=item.optString("txt",null);
                if(title==null) title=item.optString("tit");
                if(title!=null){
                    TextView tw = (TextView) v.findViewById(R.id.title_text);
                    if(tw!=null)
                        tw.setText(title);
                }
                ((TextView)v.findViewById(R.id.perg_id)).setText(keynames.get(position));
                v.setTag(keynames.get(position));
                collection.addView(v);
            } catch (JSONException e) {
            }
            return v;
        }
        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private void setupDateField(Context context, final ViewGroup v, String data_atual, int am) {
        Calendar c = Calendar.getInstance();
        List<String> dias=new ArrayList<String>();
        for(int i=1;i<32;i++){
            dias.add(""+i);
        }
        ArrayAdapter<String> ass = new ArrayAdapter<String>(context,R.layout.spinner_item, dias);
        Spinner dup = (Spinner) v.findViewById(R.id.spinner_day);
        dup.setAdapter(ass);

        String[] months = getResources().getStringArray(R.array.meses);
        ArrayAdapter<String> mss = new ArrayAdapter<String>(context,R.layout.spinner_item, months);
        Spinner mup = (Spinner) v.findViewById(R.id.spinner_month);
        mup.setAdapter(mss);


        List<String> anos=new ArrayList<String>();
        for(int i=1800;i<=am;i++){
            anos.add(""+i);
        }
        ArrayAdapter<String> ssa = new ArrayAdapter<String>(context,R.layout.spinner_item, anos);
        Spinner yup = (Spinner) v.findViewById(R.id.spinner_year);
        yup.setAdapter(ssa);

        yup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                fixDays(v);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ((Spinner)v.findViewById(R.id.spinner_month)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int j, long l) {
                fixDays(v);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        if(!data_atual.equals(null)){
            String[] dat=data_atual.split("\\/");
            if(dat.length==3){
                dup.setSelection(ass.getPosition(dat[0]));
                mup = (Spinner) v.findViewById(R.id.spinner_month);
                mup.setSelection(Integer.parseInt(dat[1])-1);
                yup.setSelection(ssa.getPosition(dat[2]));
            }
        }
    }

    private void finishVideo() {
        findViewById(R.id.content_scroller).setVisibility(View.VISIBLE);
        findViewById(R.id.next).setVisibility(View.VISIBLE);
        if(((IChingViewPager) findViewById(R.id.main_view)).getCurrentItem()>0) findViewById(R.id.previous).setVisibility(View.VISIBLE);
        findViewById(R.id.video_container).setVisibility(View.GONE);
        getSupportActionBar().show();
    }

    private void fixDays(ViewGroup g) {
        Spinner dup = (Spinner) g.findViewById(R.id.spinner_day);
        int pit=dup.getSelectedItemPosition();
        int daisy=31;
        switch(((Spinner)g.findViewById(R.id.spinner_month)).getSelectedItemPosition()){
            case 0://jan
            case 2://mar
            case 4://mai
            case 6://jul
            case 8://set
            case 11://dez
                daisy=31;
                break;
            case 3://abr
            case 5://jun
            case 7://ago
            case 9://out
            case 10://nov
                daisy=30;
                break;
            case 1://fev
                int year=Integer.parseInt(((Spinner) g.findViewById(R.id.spinner_year)).getSelectedItem().toString());
                if((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0))
                    daisy=29;
                else
                    daisy=28;
                break;
        }
        List<String> ds=new ArrayList<String>();
        for(int i=1;i<daisy+1;i++){
            ds.add(""+i);
        }
        ArrayAdapter<String> ass = new ArrayAdapter<String>(this,R.layout.spinner_item, ds);
        dup.setAdapter(ass);
        if(dup.getCount()>pit)
            dup.setSelection(pit);

    }

    private void requestCamera() {
        try {
            imageFile = createImageFile();
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (imageFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "net.alien9.iching",
                        imageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }else{
                Snackbar.make(findViewById(R.id.main_view), "Erro ao abrir arquivo", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        } catch (IOException e) {
            Snackbar.make(findViewById(R.id.main_view), "Erro ao abrir arquivo", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView iw= (ImageView) findViewById(R.id.camera_image_view);
            if(iw!=null)
                iw.setImageBitmap(imageBitmap);
        }
    }
    protected boolean validate(String comentario){
        comment=comentario;
        return validate();
    }
    protected boolean validate(){
        IChingViewPager vu= (IChingViewPager) findViewById(R.id.main_view);
        int vi = vu.getCurrentItem();
        View v = vu.getChildAt(vi);
        if(v==null)return false;
        TextView pergfield = (TextView) v.findViewById(perg_id);
        prox="";
        if(pergfield!=null) {
            String perg_id = (String) pergfield.getText();
            JSONObject item=new JSONObject();
            if(polly.optJSONObject("pergs").has(perg_id)){
                item=polly.optJSONObject("pergs").optJSONObject(perg_id);
            };
            IChing ching = ((IChing) getApplication());
            JSONObject respuestas = ching.getRespostas();
            try {
                int tipo=Integer.parseInt((String) ((TextView)v.findViewById(R.id.type_of_question)).getText());
                if(v.findViewById(R.id.decline_layout).getVisibility()==View.VISIBLE){
                    if((v.findViewById(R.id.naosei).getVisibility()==View.VISIBLE)&&(((CheckBox)v.findViewById(R.id.naosei)).isChecked())){
                        respuestas.put(perg_id,"ns");
                        ching.setRespostas(respuestas);
                        return true;
                    }
                    if((v.findViewById(R.id.naoresp).getVisibility()==View.VISIBLE)&&(((CheckBox)v.findViewById(R.id.naoresp)).isChecked())){
                        respuestas.put(perg_id,"nr");
                        ching.setRespostas(respuestas);
                        return true;
                    }
                }
                JSONObject respostinha = new JSONObject();
                JSONObject resposta=new JSONObject();
                boolean fim=false;
                boolean require_comments=false;
                if(item.has("resps")){
                    require_comments=item.optJSONObject("resps").optJSONObject("1").optInt("expl",0)==1;
                    resposta = item.optJSONObject("resps").optJSONObject("1");
                }
                switch(tipo){
                    case TYPE_UNICA:
                    case TYPE_RADIO:
                    case TYPE_YESORNO:
                        respostinha=getRadioValue((RadioGroup)v.findViewById(R.id.multipla_radio));
                        if(respostinha==null){
                            Snackbar.make(findViewById(R.id.main_view),getString(R.string.value_required),Snackbar.LENGTH_LONG).show();
                            return false;
                        }
                        resposta = item.optJSONObject("resps").optJSONObject(respostinha.optString("v"));
                        prox=resposta.optString("prox");
                        fim = resposta.optInt("fim", 0) == 1;
                        require_comments=resposta.optInt("expl",0)==1;
                        break;
                    case TYPE_NUMBER:
                    case TYPE_TEXT:
                    case TYPE_CAMERA:
                        CharSequence t = ((TextView) v.findViewById(R.id.value_edittext)).getText();
                        if(t.length()==0) {
                            v.findViewById(R.id.value_edittext).requestFocus();
                            Snackbar.make(findViewById(R.id.main_view), getString(R.string.valor_requerido),Snackbar.LENGTH_LONG).show();
                            return false;
                        }
                        respostinha.put("v",t);
                        break;
                    case TYPE_DATE:
                        respostinha.put("v",String.format("%02d/%02d/%04d",new Integer[]{((Spinner)v.findViewById(R.id.spinner_day)).getSelectedItemPosition()+1, ((Spinner)v.findViewById(R.id.spinner_month)).getSelectedItemPosition()+1,Integer.parseInt(((Spinner)v.findViewById(R.id.spinner_year)).getSelectedItem().toString())}));
                        break;
                    case TYPE_CHECKBOX:
                    case TYPE_MULTIPLA:
                        JSONObject what = new JSONObject();
                        ViewGroup gr = (ViewGroup) v.findViewById(R.id.checkbox_container);
                        JSONObject ju=new JSONObject();
                        for (int i = 0; i < gr.getChildCount(); i++) {
                            CheckBox c= (CheckBox) gr.getChildAt(i);
                            what.put((String) c.getTag(),c.isChecked());
                        }
                        respostinha.put("v",what);

                        break;
                    case TYPE_TABLE:
                        // detectar o subtipo
                        LinearLayout tabela= (LinearLayout) v.findViewById(R.id.suport_table_layout);
                        JSONObject resposta_multi = new JSONObject();
                        for(int o=0;o<tabela.getChildCount();o++){
                            int subtipo;
                            View g = tabela.getChildAt(o);
                            try{
                                subtipo=FIELD_TYPES.get(item.optJSONObject("pergs").optJSONObject(((TextView)g.findViewById(R.id.subperg_id)).getText().toString()).optString("tipo"));
                            }catch(NullPointerException xixi){
                                subtipo=TYPE_TEXT;
                            }
                            JSONObject juk = null;
                            switch(subtipo){
                                case TYPE_RADIO:
                                case TYPE_UNICA:
                                case TYPE_YESORNO:
                                    juk = getRadioValue((ViewGroup)g.findViewById(R.id.radio_mini_group));
                                    if(juk==null){
                                        Snackbar.make(findViewById(R.id.main_view),R.string.value_required,Snackbar.LENGTH_LONG).show();
                                        g.findViewById(R.id.radio_mini_group).requestFocus();
                                        return false;
                                    }
                                    resposta_multi.put((String) ((TextView)g.findViewById(R.id.subperg_id)).getText(),juk);
                                    break;
                                case TYPE_DATE:
                                    String d=String.format("%02d/%02d/%04d",((Spinner)g.findViewById(R.id.spinner_day)).getSelectedItemPosition()+1, ((Spinner)g.findViewById(R.id.spinner_month)).getSelectedItemPosition()+1,Integer.parseInt(((Spinner)g.findViewById(R.id.spinner_year)).getSelectedItem().toString()));
                                    juk=new JSONObject();
                                    juk.put("v",d);
                                    resposta_multi.put((String) ((TextView)g.findViewById(R.id.subperg_id)).getText(),juk);
                                    break;
                                case TYPE_TEXT:
                                default:
                                    juk=new JSONObject();
                                    juk.put("v",((EditText)g.findViewById(R.id.value_edittext)).getText());
                                    resposta_multi.put((String) ((TextView)g.findViewById(R.id.subperg_id)).getText(),juk);
                                    break;
                            }
                        }
                        respostinha=resposta_multi;
                        break;
                }
                if(v.findViewById(R.id.habitante_layout)!=null){
                    respuestas.put("habi1_nom",((EditText)v.findViewById(R.id.editText_habi1_nom)).getText());
                    if(respuestas.optString("habi1_nom").length()<2){
                        Snackbar.make(v.findViewById(R.id.main_view), getString(R.string.valor_requerido),Snackbar.LENGTH_LONG).show();
                        v.findViewById(R.id.editText_habi1_nom).requestFocus();
                        return false;
                    }
                    if(((RadioButton)v.findViewById(R.id.masculino_radiobutton)).isChecked()){
                        respuestas.put("habi1_sex","M");
                    }
                    if(((RadioButton)v.findViewById(R.id.feminino_radiobutton)).isChecked()){
                        respuestas.put("habi1_sex","F");
                    }
                    respuestas.put("habi1_cod",((TextView)v.findViewById(R.id.habi1_cod)).getText());
                    respuestas.put("habi1_nom_mae",((EditText)v.findViewById(R.id.editText_habi1_nom_mae)).getText());
                    respuestas.put("habi1_nom_pai",((EditText)v.findViewById(R.id.editText_habi1_nom_pai)).getText());
                    respuestas.put("habi1_cns",((EditText)v.findViewById(R.id.editText_habi1_cns)).getText());
                    respuestas.put("habi1_cpf",((EditText)v.findViewById(R.id.editText_habi1_cpf)).getText());
                    respuestas.put("habi1_rg",((EditText)v.findViewById(R.id.editText_habi1_rg)).getText());
                    respuestas.put("habi1_dat_nasc",String.format("%02d/%02d/%04d",new Integer[]{((Spinner)v.findViewById(R.id.spinner_day)).getSelectedItemPosition()+1, ((Spinner)v.findViewById(R.id.spinner_month)).getSelectedItemPosition()+1,Integer.parseInt(((Spinner)v.findViewById(R.id.spinner_year)).getSelectedItem().toString())}));
                    /*

habi1_cod
habi1_nom
habi1_nom_mae
habi1_nom_pai;
habi1_cns
habi1_rg=$habi
habi1_cpf
habi1_sex
habi1_dat_nasc
*/
                }
                if(v.findViewById(R.id.endereco_layout)!=null) {
                    respuestas.put("ende1_logr", ((EditText) v.findViewById(R.id.editText_nomedarua)).getText());
                    if (respuestas.optString("ende1_logr").length() < 1) {
                        Snackbar.make(findViewById(R.id.main_view), getString(R.string.valor_requerido), Snackbar.LENGTH_LONG).show();
                        findViewById(R.id.editText_nomedarua).requestFocus();
                        return false;
                    }
                    respuestas.put("ende1_num", ((EditText) v.findViewById(R.id.editText_numero)).getText());
                    respuestas.put("ende1_tele", ((EditText) v.findViewById(R.id.editText_telefone)).getText());
                    respuestas.put("ende1_compl", ((EditText) v.findViewById(R.id.editText_complemento)).getText());
                }
                /*
((EditText)v.findViewById(R.id.editText_nomedarua)).setText(respuestas.optString("ende1_logr"));
                        ((EditText)v.findViewById(R.id.editText_numero)).setText(respuestas.optString("ende1_num"));
                        ((EditText)v.findViewById(R.id.editText_complemento)).setText(respuestas.optString("ende1_compl"));
                        ((EditText)v.findViewById(R.id.editText_telefone)).setText(respuestas.optString("ende1_tele"));
                        ((EditText)v.findViewById(R.id.editText_bairro)).setText(respuestas.optString("ende1_bai"));
                        ((EditText)v.findViewById(R.id.editText_cidade)).setText(respuestas.optString("ende1_cida"));
                        ((TextView)v.findViewById(R.id.ende1_lat)).setText(respuestas.optString("ende1_lat"));
                        ((TextView)v.findViewById(R.id.ende1_lng)).setText(respuestas.optString("ende1_lng"));
                        ((TextView)v.findViewById(R.id.ende1_cod)).setText(respuestas.optString("ende1_cod"));
                        String[] estados=getResources().getStringArray(R.array.estados);
                        int uf=Arrays.asList(estados).indexOf(respuestas.optString("ende1_uf"));
                        if(uf>=0) {
                            ((Spinner) v.findViewById(R.id.estado_spinner)).setSelection(uf);
                        }
                View vcl = v.findViewById(R.id.comments_request);
                if(vcl.getVisibility()==View.VISIBLE){
                    EditText vc=(EditText)vcl.findViewById(R.id.comments);
                    String c = ((EditText) vc).getText().toString();
                    if(c.length()<3){
                        Snackbar.make(findViewById(R.id.main_view), getString(R.string.minimo_tamanho_de_texto),Snackbar.LENGTH_LONG).show();
                        return false;
                    }else{
                        respostinha.put("c",c);
                    }
                }*/
                // descubra se a pergunta requer comments
                if(require_comments && !respostinha.optBoolean("c")){
                    if(comment.length()>0){
                        respostinha.put("c",comment);
                        comment="";
                    }else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(resposta.optString("ins", getString(R.string.comment_this)));
                        builder.setCancelable(false);
                        LayoutInflater li = LayoutInflater.from(this);
                        final View input = li.inflate(R.layout.comment_dialog, null);
                        builder.setView(input);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pageup(((EditText)((ViewGroup)input).findViewById(R.id.comment_text)).getText().toString());
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                        return false;
                    }
                }
                respuestas.put(perg_id,respostinha);
                if(fim) termina();
            } catch (JSONException e) {
            }
            ching.setRespostas(respuestas);
            return respuestas.has(perg_id);
        }
        return false;
    }

    private void comment(String s) {

    }

    private JSONObject getRadioValue(ViewGroup g) {
        int selectedId = ((RadioGroup)g).getCheckedRadioButtonId();
        RadioButton u = (RadioButton) g.findViewById(selectedId);
        if(u!=null) {
            JSONObject j = new JSONObject();
            try {
                j.put("v",u.getTag());
                return j;
            } catch (JSONException e) {}
        }
        return null;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        validate();
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onBackPressed() {
        IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
        if(pu.getCurrentItem()==0){
            finish();
        }
    }

    private class ThingsSorter implements java.util.Comparator<JSONObject> {
        @Override
        public int compare(JSONObject jsonObject, JSONObject t1) {

            return getOrd(jsonObject)-getOrd(t1);
        }

        private int getOrd(JSONObject t) {
            if(t.has("ord")) return t.optInt("ord");
            if(t.has("pergs")){
                JSONObject pergs = t.optJSONObject("pergs");
                if(pergs.keys().hasNext()){
                    return pergs.optJSONObject(pergs.keys().next()).optInt("ord");
                }
            }
            return 0;
        }
    }
}