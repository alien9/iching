package net.alien9.iching;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Hashtable;

public class Question extends AppCompatActivity {

    private static final int TYPE_TEXT = 2;
    private static final int TYPE_RADIO = 3;
    private static final int TYPE_CHECKBOX = 4;
    private static final Hashtable<String,Integer> FIELD_TYPES = new Hashtable<String, Integer>(){{
        put("radio",TYPE_RADIO);
        put("checkbox",TYPE_CHECKBOX);
        put("text",TYPE_TEXT);
    }};

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
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
                            v.addView(bu);
                        }
                        break;
                    case TYPE_CHECKBOX:
                        v = (ViewGroup) inflater.inflate(R.layout.type_checkbox_question, collection, false);
                        for(int i=0;i<a.length();i++){
                            CheckBox bu = new CheckBox(context);
                            bu.setText(a.optString(i));
                            v.addView(bu);
                        }
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
}