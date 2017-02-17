package net.alien9.iching;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import static android.R.attr.numbersInnerTextColor;
import static android.R.attr.tag;

public class Question extends AppCompatActivity {

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
    }};
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int FIELD_INDEX = 99;
    public static final int POSITION_UPDATE = 0;
    private static final int ICHING_REQUEST_GPS_PERMISSION = 0;
    private File imageFile;
    private JSONObject polly;
    private JSONObject last_known_position;
    private boolean turning=false;
    private DatePickerDialog datepicker;
    private String cookies;

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
        final ViewPager pu = (ViewPager) findViewById(R.id.main_view);
        final View te=findViewById(R.id.messenger_layout);
        if(polly.has("msgini")){
            pu.setVisibility(View.GONE);
            ((TextView)te.findViewById(R.id.message_textView)).setText(polly.optString("msgini"));
            te.setVisibility(View.VISIBLE);
        }else{
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
        final Handler locator = new Chandler();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        LocationListenerDourado l = new LocationListenerDourado(locator);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Activity budega = (Activity) this;
            ActivityCompat.requestPermissions(budega,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ICHING_REQUEST_GPS_PERMISSION);
            return;
        }
        ((Button)findViewById(R.id.continue_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                te.setVisibility(View.GONE);
                pu.setVisibility(View.VISIBLE);
            }
        });
        ((FloatingActionButton)findViewById(R.id.next)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
                ((IChing)getApplication()).resetUndo();
                int cu = pu.getCurrentItem();
                if(cu<pu.getAdapter().getCount()-1) {
                    pu.setCurrentItem(pu.getCurrentItem() + 1, true);
                }else{
                    termina();
                }
            }
        });
        ((FloatingActionButton)findViewById(R.id.previous)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
                if(!((IChing)getApplication()).hasUndo()){
                    int cu = pu.getCurrentItem();
                    if (cu > 0)
                        pu.setCurrentItem(cu - 1, true);

                }
                ((IChing)getApplication()).setUndo();
            }
        });
        pu.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("iching Coisa",""+positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                findViewById(R.id.previous).setVisibility(View.VISIBLE);
                findViewById(R.id.next).setVisibility(View.VISIBLE);
                if(position==0){
                    findViewById(R.id.previous).setVisibility(View.GONE);
                }
                //if(position==pu.getAdapter().getCount()-1)
                //    findViewById(R.id.next).setVisibility(View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d("iching page scroll",""+state);
            }
        });

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, l);
    }

    private void termina() {
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
                save();
                break;
        }

        return super.onOptionsItemSelected(item);
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
                        for(int i=0;i<respskeys.size();i++){
                            RadioButton bu = new RadioButton(context);
                            bu.setText(resps.optJSONObject(respskeys.get(i)).optString("txt"));
                            String tag=respskeys.get(i);
                            bu.setTag(tag);
                            if(respuestas.optString(perg_id).equals(tag)){
                                bu.setChecked(true);
                            }
                            ((ViewGroup)v.findViewById(R.id.multipla_radio)).addView(bu);
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
                        v = (ViewGroup) inflater.inflate(R.layout.type_date_question, collection, false);
                        final ViewGroup finalV = v;
                        ((ImageButton)v.findViewById(R.id.datepicker_butt)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DatePickerDialog.OnDateSetListener datePickerListener=new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                                        ((EditText) finalV.findViewById(R.id.datepicker_text)).setText(String.format("%02d/%02d/%04d",i2,1+i1,i));
                                        datepicker.dismiss();
                                    }
                                };
                                Calendar cal = Calendar.getInstance();
                                int day = cal.get(Calendar.DAY_OF_MONTH);
                                int month = cal.get(Calendar.MONTH);
                                int year = cal.get(Calendar.YEAR);
                                datepicker = new DatePickerDialog(context, datePickerListener, year, month, day);
                                datepicker.show();
                            }
                        });
                        ((EditText)v.findViewById(R.id.datepicker_text)).setText(respuestas.optString(perg_id));
                        break;
                    case TYPE_NUMBER:
                        //"resps":{"1":{"txt":"","menorval":"0","maiorval":"240"}}}
                        if(!item.has("resps")){
                            v = (ViewGroup) inflater.inflate(R.layout.type_number_question, collection, false);
                        }else {
                            v = (ViewGroup) inflater.inflate(R.layout.type_range_question, collection, false);
                            JSONObject jake = item.optJSONObject("resps").optJSONObject("1");
                            ((SeekBar)v.findViewById(R.id.seek)).setMax(jake.optInt("maiorval"));
                            //((SeekBar)v.findViewById(R.id.seek)).mini(jake.optInt("menorval"));

                        }
                        ((EditText)v.findViewById(R.id.number_edittext)).setText(respuestas.optString(perg_id));
                        break;
                    case TYPE_TEXT:
                        v = (ViewGroup) inflater.inflate(R.layout.type_text_question, collection, false);
                        ((EditText)v.findViewById(R.id.textao_editText)).setText(respuestas.optString(perg_id));
                        break;
                    default:
                        v = (ViewGroup) inflater.inflate(R.layout.type_text_question, collection, false);
                        break;
                }
                String title=item.optString("txt",null);
                if(title!=null){
                    TextView tw = (TextView) v.findViewById(R.id.title_text);
                    if(tw!=null)
                        tw.setText(title);
                }
                ((TextView)v.findViewById(R.id.perg_id)).setText(keynames.get(position));
                v.setTag(keynames.get(position));
            } catch (JSONException e) {
            }
            collection.addView(v);
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
    protected void save(){
        ViewPager vu= (ViewPager) findViewById(R.id.main_view);
        int vi = vu.getCurrentItem();
        View v = vu.getChildAt(vi);
        String perg_id = (String) ((TextView)v.findViewById(R.id.perg_id)).getText();
        iterate(v,perg_id);
    }
    protected void saveAll(){
        ViewGroup vu = (ViewGroup) findViewById(R.id.main_view);
        for(int i=0;i<vu.getChildCount();i++){
            View v = vu.getChildAt(i);
            //int ix= (int) v.getTag();
            //iterate(v,ix);
        }
    }
    protected boolean verify(){
        return true;
    }

    private void iterate(View v, String ix) {
        TextView pergfield = (TextView) v.findViewById(R.id.perg_id);
        if(pergfield!=null) {
            String perg_id = (String) pergfield.getText();
            IChing ching = ((IChing) getApplication());
            JSONObject respuestas = ching.getRespostas();
            try {
                if (v.findViewById(R.id.textao_editText) != null) {
                    respuestas.put(perg_id, ((TextView) v.findViewById(R.id.textao_editText)).getText());
                }
                if (v.findViewById(R.id.number_edittext) != null) {
                    respuestas.put(perg_id, ((TextView) v.findViewById(R.id.number_edittext)).getText());
                }
                if (v.findViewById(R.id.datepicker_text) != null) {
                    respuestas.put(perg_id, ((TextView) v.findViewById(R.id.datepicker_text)).getText());
                }
                if (v.findViewById(R.id.multipla_radio) != null) {
                    int selectedId = ((RadioGroup)v.findViewById(R.id.multipla_radio)).getCheckedRadioButtonId();
                    RadioButton u = (RadioButton) v.findViewById(selectedId);
                    if(u!=null)
                        respuestas.put(perg_id, u.getTag());
                }
                if(v.findViewById(R.id.checkbox_container)!=null){
                    respuestas.put(perg_id,new JSONObject());
                    ViewGroup g = (ViewGroup) v.findViewById(R.id.checkbox_container);
                    for (int i = 0; i < g.getChildCount(); i++) {
                        CheckBox c= (CheckBox) g.getChildAt(i);
                        String resp_id = (String) c.getTag();
                        respuestas.optJSONObject(perg_id).put(resp_id,c.isChecked());
                    }
                }
            } catch (JSONException e) {
            }
            ching.setRespostas(respuestas);
            return;
        }
        try {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                iterate(g.getChildAt(i), ix);
            }
        } catch (ClassCastException exx) {
            try{
                IChing ching = ((IChing) getApplication());
                JSONObject respuestas = ching.getRespostas();
                if (v.getClass().getCanonicalName().equals(CheckBox.class.getCanonicalName())) {

                    JSONObject j = respuestas.optJSONObject(ix);
                    if (j == null) j = new JSONObject();
                    String resp_id = (String) v.getTag();
                    j.put(resp_id, ((CheckBox) v).isChecked());
                    respuestas.put(ix, j);

                }
                if(v.getClass().getCanonicalName().equals(AppCompatEditText.class.getCanonicalName())){
                    respuestas.put(ix,((EditText)v).getText());
                }
                if(v.getClass().getCanonicalName().equals(RadioButton.class.getCanonicalName())){
                    if(((RadioButton)v).isChecked())
                        respuestas.put(ix,((RadioButton)v).getTag());
                }

                ching.setRespostas(respuestas);




            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private class Chandler extends Handler {
        public void handleMessage (Message msg){
            if(msg.what==POSITION_UPDATE){
                try {
                    last_known_position = new JSONObject(msg.getData().getString("location"));
                } catch (JSONException e) {

                }

            }
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        save();
        super.onConfigurationChanged(newConfig);
    }
}