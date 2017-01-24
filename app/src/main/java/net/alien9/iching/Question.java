package net.alien9.iching;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

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
    private JSONObject last_known_position;
    private JSONObject polly;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent.hasExtra("poll")){
            String h = intent.getExtras().getString("poll");
            try {
                polly=new JSONObject(h);
            } catch (JSONException ignore) {
                Snackbar.make(findViewById(R.id.main_view), "Dados Incorretos", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, l);
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
            try {
                JSONObject item=polly.getJSONObject("pergs").getJSONObject(keynames.get(position));
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
                            bu.setTag(i);
                            v.addView(bu);
                        }
                        break;
                    case TYPE_CHECKBOX:
                    case TYPE_MULTIPLA:
                        v = (ViewGroup) inflater.inflate(R.layout.type_checkbox_question, collection, false);
                        for(int i=0;i<respskeys.size();i++){
                            CheckBox bu = new CheckBox(context);
                            bu.setText(resps.optJSONObject(respskeys.get(i)).optString("txt"));
                            bu.setTag(i);
                            v.addView(bu);
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
                        break;
                    case TYPE_NUMBER:
                        v = (ViewGroup) inflater.inflate(R.layout.type_number_question, collection, false);
                        break;
                    case TYPE_TEXT:
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
                v.setTag(position);
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
        int ix= (int) v.getTag();
        iterate(v,ix);
    }
    protected void saveAll(){
        ViewGroup vu = (ViewGroup) findViewById(R.id.main_view);
        for(int i=0;i<vu.getChildCount();i++){
            View v = vu.getChildAt(i);
            int ix= (int) v.getTag();
            iterate(v,ix);
        }
    }

    private void iterate(View v, int ix) {
        try {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                iterate(g.getChildAt(i), ix);
            }
        }catch(ClassCastException exx){
                /*
                if(v.getClass().getCanonicalName().equals(CheckBox.class.getCanonicalName())){
                    JSONObject option = groselha.getJSONArray("items").getJSONObject(ix).getJSONArray("options").getJSONObject((Integer) v.getTag());
                    option.put("checked",((CheckBox)v).isChecked());
                    JSONArray vs = results.optJSONObject(ix).optJSONArray("value");
                    if(vs==null) results.optJSONObject(ix).put("value",new JSONArray());
                    results.optJSONObject(ix).optJSONArray("value").put(option);
//                    groselha.getJSONArray("items").getJSONObject(ix).getJSONArray("options").getJSONObject((Integer) v.getTag()).put("checked",((CheckBox)v).isChecked());
                }else if(v.getClass().getCanonicalName().equals(RadioButton.class.getCanonicalName())){
                    if(((RadioButton)v).isChecked())
                        results.optJSONObject(ix).put("value",v.getTag());
                    //groselha.optJSONArray("items").getJSONObject(ix).put("value",v.getTag());
                }else if(v.getClass().getCanonicalName().equals(AppCompatEditText.class.getCanonicalName())){
                    results.getJSONObject(ix).put("value",((TextView)v).getText());

                }else if(v.getClass().getCanonicalName().equals(AppCompatImageView.class.getCanonicalName())){
                    BitmapDrawable drawable = (BitmapDrawable) ((ImageView)v).getDrawable();
                    if(drawable!=null) {
                        Bitmap bitmap = drawable.getBitmap();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        results.getJSONObject(ix).put("value", Base64.encodeToString(byteArray, Base64.DEFAULT));
                    }
                }
                if(groselha.getJSONArray("items").getJSONObject(ix).optBoolean("gps",false)){
                    results.getJSONObject(ix).put("location",last_known_position);
                }
                */
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
}