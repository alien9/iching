package net.alien9.iching;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

public class Question extends AppCompatActivity {

    private static final int TYPE_TEXT = 2;
    private static final int TYPE_RADIO = 3;
    private static final int TYPE_CHECKBOX = 4;
    private static final int TYPE_CAMERA = 5;
    private static final Hashtable<String,Integer> FIELD_TYPES = new Hashtable<String, Integer>(){{
        put("radio",TYPE_RADIO);
        put("checkbox",TYPE_CHECKBOX);
        put("text",TYPE_TEXT);
        put("camera",TYPE_CAMERA);
    }};
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int FIELD_INDEX = 99;


    private JSONObject groselha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                save();
                ViewPager pu = (ViewPager) findViewById(R.id.main_view);
                pu.setCurrentItem(pu.getCurrentItem()+1);
            }
        });
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
        ViewPager pu = (ViewPager) findViewById(R.id.main_view);
        PagerAdapter pa = new BunchViewAdapter(this);
        pu.setOffscreenPageLimit(pa.getCount());
        pu.setAdapter(pa);
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class BunchViewAdapter extends PagerAdapter {
        private final Context context;

        public BunchViewAdapter(Context c) {
            context = c;
        }
        @Override
        public int getCount() {
            try {
                return groselha.getJSONArray("items").length();
            } catch (JSONException e) {
                return 0;
            }
        }
        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(context);
            ViewGroup v = null;
            try {
                JSONObject item=groselha.getJSONArray("items").getJSONObject(position);
                int t=FIELD_TYPES.get(item.optString("type","text"));
                JSONArray a = item.optJSONArray("options");
                switch(t){
                    case TYPE_RADIO:
                        v = (ViewGroup) inflater.inflate(R.layout.type_radio_question, collection, false);
                        for(int i=0;i<a.length();i++){
                            RadioButton bu = new RadioButton(context);
                            bu.setText(a.optString(i));
                            bu.setTag(i);
                            v.addView(bu);
                        }
                        break;
                    case TYPE_CHECKBOX:
                        v = (ViewGroup) inflater.inflate(R.layout.type_checkbox_question, collection, false);
                        for(int i=0;i<a.length();i++){
                            CheckBox bu = new CheckBox(context);
                            bu.setText(a.optJSONObject(i).optString("caption"));
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
                    case TYPE_TEXT:
                    default:
                        v = (ViewGroup) inflater.inflate(R.layout.type_text_question, collection, false);
                        break;
                }
                String title=item.optString("title",null);
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
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
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

    private void iterate(View v, int ix) {
        try {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                iterate(g.getChildAt(i), ix);
            }
        }catch(ClassCastException exx){
            try {
                if(v.getClass().getCanonicalName().equals(CheckBox.class.getCanonicalName())){
                    groselha.getJSONArray("items").getJSONObject(ix).getJSONArray("options").getJSONObject((Integer) v.getTag()).put("checked",((CheckBox)v).isChecked());
                }else if(v.getClass().getCanonicalName().equals(RadioButton.class.getCanonicalName())){
                    if(((RadioButton)v).isChecked())
                        groselha.optJSONArray("items").getJSONObject(ix).put("value",v.getTag());
                }else if(v.getClass().getCanonicalName().equals(AppCompatEditText.class.getCanonicalName())){
                    groselha.getJSONArray("items").getJSONObject(ix).put("value",((TextView)v).getText());
                }else if(v.getClass().getCanonicalName().equals(AppCompatImageView.class.getCanonicalName())){
                    BitmapDrawable drawable = (BitmapDrawable) ((ImageView)v).getDrawable();
                    Bitmap bitmap = drawable.getBitmap();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream .toByteArray();
                    groselha.getJSONArray("items").getJSONObject(ix).put("value",Base64.encodeToString(byteArray, Base64.DEFAULT));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}