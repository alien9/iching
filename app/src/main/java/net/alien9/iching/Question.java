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
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static net.alien9.iching.R.id.perg_id;

public class Question extends AppCompatActivity implements MediaPlayer.OnPreparedListener, SurfaceHolder.Callback {
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
    }};
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int FIELD_INDEX = 99;
    public static final int POSITION_UPDATE = 0;
    private static final int ICHING_REQUEST_GPS_PERMISSION = 0;
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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent.hasExtra("poll")){
            String h = intent.getExtras().getString("poll");
            try {
                polly=new JSONObject(h);
                ((IChing)getApplicationContext()).setCod(polly.optString("cod"));
            } catch (JSONException ignore) {
                Snackbar.make(findViewById(R.id.main_view), "Dados Incorretos", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        }
        if(intent.hasExtra("CNETSERVERLOGACAO")){
            cookies = intent.getExtras().getString("CNETSERVERLOGACAO");
        }

        setContentView(R.layout.activity_question);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(polly.optString("nom"));
        final Context context = this;
        setTitle(polly.optString("nom"));
        final IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
        final View te=findViewById(R.id.messenger_layout);
        if(polly.has("msgini")){
            findViewById(R.id.next).setVisibility(View.GONE);
            findViewById(R.id.previous).setVisibility(View.GONE);
            pu.setVisibility(View.GONE);
            ((TextView)te.findViewById(R.id.message_textView)).setText(polly.optString("msgini"));
            te.setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.next).setVisibility(View.VISIBLE);
            te.setVisibility(View.GONE);
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
                te.setVisibility(View.GONE);

                if(jadeu)
                    encerra();
                else {
                    pu.setVisibility(View.VISIBLE);
                    findViewById(R.id.next).setVisibility(View.VISIBLE);
                    findViewById(R.id.previous).setVisibility(View.VISIBLE);
                }
            }
        });



        ((FloatingActionButton)findViewById(R.id.next)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageup();
            }
        });
        ((FloatingActionButton)findViewById(R.id.previous)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validate();
                NestedScrollView sv = (NestedScrollView)findViewById(R.id.content_scroller);
                sv.scrollTo(0, 0);
                if(!((IChing)getApplication()).hasUndo()){
                    int cu = pu.getCurrentItem();
                    if(previous_index>=0){
                        pu.setCurrentItem(previous_index);
                        previous_index=-1;
                    }else {
                        if (cu > 0)
                            pu.setCurrentItem(cu - 1, true);
                    }
                }
                ((IChing)getApplication()).setUndo();
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
                    findViewById(R.id.previous).setVisibility(View.GONE);
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
        mp=new MediaPlayer();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                findViewById(R.id.surface_view).setVisibility(View.GONE);
                findViewById(R.id.main_view).setVisibility(View.VISIBLE);
            }
        });
        sh=((SurfaceView)findViewById(R.id.surface_view)).getHolder();
        sh.addCallback(Question.this);
    }
    private void pageup(String c){
        comment=c;
        pageup();
    }
    private void pageup() {
        if(!validate())
            return;
        IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
        NestedScrollView sv = (NestedScrollView)findViewById(R.id.content_scroller);
        sv.scrollTo(0, 0);
        ((IChing)getApplication()).resetUndo();
        int cu = pu.getCurrentItem();
        previous_index=cu;
        if((cu<pu.getAdapter().getCount()-1)&&!encerrabody) {
            if(!prox.equals("")){
                int i=0;
                JSONObject pergs = polly.optJSONObject("pergs");
                Iterator<?> keys = pergs.keys();
                int n=0;
                while( keys.hasNext() ) {
                    String k=pergs.optJSONObject((String)keys.next()).optString("ord");
                    if(k.equals(prox)){
                        pu.setCurrentItem(n);
                    }
                    n++;
                }
            }else {
                pu.setCurrentItem(cu + 1, true);
            }
        }else{
            termina();
        }
    }

    private void termina() {
        jadeu=true;
        if(polly.has("msgfim")){
            IChingViewPager pu = (IChingViewPager) findViewById(R.id.main_view);
            findViewById(R.id.next).setVisibility(View.GONE);
            findViewById(R.id.previous).setVisibility(View.GONE);
            pu.setVisibility(View.GONE);
            View te=findViewById(R.id.messenger_layout);
            ((TextView)te.findViewById(R.id.message_textView)).setText(polly.optString("msgfim"));
            te.setVisibility(View.VISIBLE);
            findViewById(R.id.main_view).setVisibility(View.GONE);
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

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mp.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mp.setDisplay(sh);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mp.stop();
        mp.reset();
    }

    private class BunchViewAdapter extends PagerAdapter {
        private final Context context;
        private ArrayList<String> keynames;

        public BunchViewAdapter(Context c) {
            context = c;
        }
        @Override
        public int getCount() {
            if(!polly.has("pergs")) return 0;
            JSONObject pergs = null;
            try {
                pergs = polly.getJSONObject("pergs");
            } catch (JSONException e) {
                pergs=new JSONObject();
            }
            Iterator<?> keys = pergs.keys();
            keynames=new ArrayList<String>();
            int n=0;
            while( keys.hasNext() ) {
                keynames.add((String)keys.next());
                n++;
            }
            return n;
        }
        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(context);
            ViewGroup v = null;
            JSONObject respuestas = ((IChing) getApplication()).getRespostas();
            try {
                String perg_id=keynames.get(position);
                JSONObject item=polly.getJSONObject("pergs").getJSONObject(perg_id);
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
                                    lu = (LinearLayout) vi.inflate(R.layout.type_number_question, null);
                                    break;
                                case TYPE_TEXT:
                                default:
                                    lu = (LinearLayout) inflater.inflate(R.layout.type_text_mini, null);

                            }
                            ((TextView)lu.findViewById(R.id.subperg_id)).setText(subitem_key);
                            TextView tit = (TextView) lu.findViewById(R.id.title_text);
                            if(tit!=null) tit.setText(subitem.optString("txt"));
                            // isto com certeza está ruim - não mostramos os campos
                            lu.findViewById(R.id.decline_layout).setVisibility(View.GONE);
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
                        String du;
                        Calendar c = Calendar.getInstance();
                        if(respuestas.has(perg_id))
                            du=respuestas.optJSONObject(perg_id).optString("v");
                        else{
                            du=String.format("%02d/%02d/%04d",c.get(c.DAY_OF_MONTH),c.get(c.MONTH)+1,c.get(Calendar.YEAR));
                        }
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
                        int am = item.optInt("anomax", c.get(Calendar.YEAR));
                        for(int i=1800;i<=am;i++){
                            anos.add(""+i);
                        }
                        ArrayAdapter<String> ssa = new ArrayAdapter<String>(context,R.layout.spinner_item, anos);
                        Spinner yup = (Spinner) v.findViewById(R.id.spinner_year);
                        yup.setAdapter(ssa);

                        yup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                fixDays();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });

                        ((Spinner)v.findViewById(R.id.spinner_month)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int j, long l) {
                                fixDays();
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });
                        if(!du.equals(null)){
                            String[] dat=du.split("\\/");
                            if(dat.length==3){
                                dup.setSelection(ass.getPosition(dat[0]));
                                mup = (Spinner) v.findViewById(R.id.spinner_month);
                                mup.setSelection(Integer.parseInt(dat[1])-1);
                                yup.setSelection(ssa.getPosition(dat[2]));
                            }
                        }
                        break;
                    case TYPE_NUMBER:
                        int n=0;
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
                        findViewById(R.id.surface_view).setVisibility(View.GONE);
                        final String filename = item.optJSONObject("resps").optJSONObject("1").optString("midia");
                        if(filename.matches(".*\\.mp4")){
                            v.findViewById(R.id.play).setVisibility(View.VISIBLE);
                            v.findViewById(R.id.imageView).setVisibility(View.GONE);
                            final ViewGroup finalV1 = v;
                            ((ImageButton)v.findViewById(R.id.play)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    View sv = findViewById(R.id.surface_view);
                                    sv.setVisibility(View.VISIBLE);
                                    findViewById(R.id.main_view).setVisibility(View.GONE);
                                    try {
                                        mp.setDataSource(getExternalCacheDir() + File.separator + "midia" + File.separator+filename);
                                        mp.prepare();
                                        mp.setOnPreparedListener(Question.this);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }else{
                            v.findViewById(R.id.play).setVisibility(View.GONE);
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

                v.findViewById(R.id.decline_layout).setVisibility(View.GONE);
                if(item.optBoolean("naosei",false)){
                    v.findViewById(R.id.decline_layout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.naosei).setVisibility(View.VISIBLE);
                }else{
                    v.findViewById(R.id.naosei).setVisibility(View.GONE);
                }
                if(item.optBoolean("naoresp",false)){
                    v.findViewById(R.id.decline_layout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.naoresp).setVisibility(View.VISIBLE);
                }else{
                    v.findViewById(R.id.naoresp).setVisibility(View.GONE);
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

    private void fixDays() {
        Spinner dup = (Spinner) findViewById(R.id.spinner_day);
        int pit=dup.getSelectedItemPosition();
        int daisy=31;
        switch(((Spinner)findViewById(R.id.spinner_month)).getSelectedItemPosition()){
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
                int year=Integer.parseInt(((Spinner) findViewById(R.id.spinner_year)).getSelectedItem().toString());
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
        if(pergfield!=null) {
            String perg_id = (String) pergfield.getText();
            JSONObject item=polly.optJSONObject("pergs").optJSONObject(perg_id);
            IChing ching = ((IChing) getApplication());
            JSONObject respuestas = ching.getRespostas();
            try {
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
                if (v.findViewById(R.id.value_edittext) != null) {
                    CharSequence t = ((TextView) v.findViewById(R.id.value_edittext)).getText();
                    if(t.length()==0) {
                        v.findViewById(R.id.value_edittext).requestFocus();
                        Snackbar.make(findViewById(R.id.main_view), getString(R.string.valor_requerido),Snackbar.LENGTH_LONG).show();

                        return false;
                    }
                    respostinha.put("v",t);
                    //respuestas.put(perg_id, t);
                }

                /*
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
                JSONObject resposta=new JSONObject();
                boolean fim=false;
                boolean require_comments=false;
                if(item.has("resps")){
                    require_comments=item.optJSONObject("resps").optJSONObject("1").optInt("expl",0)==1;
                    resposta = item.optJSONObject("resps").optJSONObject("1");
                }



                if (v.findViewById(R.id.spinner_month) != null) {
                    respostinha.put("v",String.format("%02d/%02d/%04d",new Integer[]{((Spinner)v.findViewById(R.id.spinner_day)).getSelectedItemPosition()+1, ((Spinner)v.findViewById(R.id.spinner_month)).getSelectedItemPosition()+1,Integer.parseInt(((Spinner)v.findViewById(R.id.spinner_year)).getSelectedItem().toString())}));
                }
                if (v.findViewById(R.id.multipla_radio) != null) {
                    respostinha=getRadioValue((RadioGroup)v.findViewById(R.id.multipla_radio));
                    if(respostinha==null){
                        Snackbar.make(findViewById(R.id.main_view),getString(R.string.value_required),Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                    resposta = item.optJSONObject("resps").optJSONObject(respostinha.optString("v"));
                    prox=resposta.optString("prox");
                    fim = resposta.optInt("fim", 0) == 1;
                    require_comments=resposta.optInt("expl",0)==1;
                }
                if(v.findViewById(R.id.checkbox_container)!=null){
                    JSONObject what = new JSONObject();
                    ViewGroup g = (ViewGroup) v.findViewById(R.id.checkbox_container);
                    JSONObject ju=new JSONObject();
                    for (int i = 0; i < g.getChildCount(); i++) {
                        CheckBox c= (CheckBox) g.getChildAt(i);
                        what.put((String) c.getTag(),c.isChecked());
                    }
                    respostinha.put("v",what);
                    //respuestas.put(perg_id,what);
                }
                if(v.findViewById(R.id.suport_table_layout)!=null){ // tipo table
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
                                    g.findViewById(R.id.radio_mini_group).requestFocus();
                                    return false;
                                }
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
                }
                if(require_comments && !respostinha.optBoolean("c")){
                    if(comment.length()>0){
                        respostinha.put("c",comment);
                        comment="";
                    }else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(resposta.optString("ins"));
                        final EditText input = new EditText(this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pageup(input.getText().toString());
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

    protected boolean verify(){
        return true;
    }

    private void iterate(View v, String ix) {

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
    }
}