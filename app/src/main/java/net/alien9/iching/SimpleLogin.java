package net.alien9.iching;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class SimpleLogin extends AppCompatActivity{

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Context context;
    private String cookies;
    private JSONArray stuff;
    private JSONObject cidades;
    private String cidade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.username);
        //populateAutoComplete();
        final SharedPreferences sharedpreferences=getSharedPreferences("login",MODE_PRIVATE);
        String username = sharedpreferences.getString("username", "");
        String password = sharedpreferences.getString("password", "");

        cidade = sharedpreferences.getString("cidade", "");
        try {
            cidades=new JSONObject(sharedpreferences.getString("cidades","{}"));
        } catch (JSONException ignored) {
            cidades=new JSONObject();
        }
        if(!cidade.equals("")){
            findViewById(R.id.cidade_select).setVisibility(View.GONE);
            findViewById(R.id.login_form).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.login_form).setVisibility(View.GONE);
            findViewById(R.id.cidade_select).setVisibility(View.VISIBLE);
        }
        //((CheckBox)findViewById(R.id.remeber_me)).setChecked(username.length()>0);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        if(Debug.isDebuggerConnected()){
            mPasswordView.setText(password);
            mEmailView.setText(username);
        }
        Button mEmailSignInButton = (Button) findViewById(R.id.buttonok);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        context=this;
        if(!isNetworkAvailable()){
            serverNotAvailable();
        }else {
            if((cidade.equals(""))||!cidades.has(cidade)) {
                CityTask citytask = new CityTask();
                showProgress(true);
                findViewById(R.id.login_form).setVisibility(View.GONE);
                citytask.execute();
            }
        }
        ((Button)findViewById(R.id.cidade_ok)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cidade=((Spinner)findViewById(R.id.city_spinner)).getSelectedItem().toString();
                findViewById(R.id.cidade_select).setVisibility(View.GONE);
                findViewById(R.id.login_form).setVisibility(View.VISIBLE);
            }
        });

    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        SharedPreferences sharedpreferences = getSharedPreferences("login", Context.MODE_PRIVATE);

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        ((IChing)getApplicationContext()).setDomain(cidades.optJSONObject(cidade).optString("url"));
        ((IChing)getApplicationContext()).setEstado(cidades.optJSONObject(cidade).optString("uf"));
        SharedPreferences.Editor e = sharedpreferences.edit();
        e.putString("username", email);
        if(Debug.isDebuggerConnected()) e.putString("password", password);
        e.putString("cidade", cidade);
        e.commit();
        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }/* else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }
        */
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            findViewById(R.id.login_form).setVisibility(View.GONE);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private CookieJar cookieJar;
        private String mess;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            IChing c = (IChing) getApplicationContext();
            String url =String.format("%s%s",c.getDomain(),getString(R.string.login_url));
            cookieJar=c.getCookieJar();
            OkHttpClient client = new OkHttpClient.Builder().cookieJar(cookieJar).build();

            RequestBody formBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("u",mEmail)
                    .addFormDataPart("p",mPassword)
                    .addFormDataPart("m","login")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", RequestBody.create(null, new byte[0]))
                    .post(formBody)
                    .build();
            stuff=null;
            try {
                Response response = client.newCall(request).execute();
                String t=response.body().string();
                Pattern pat=Pattern.compile("SAVEREADY\\|(\\d+)");
                Matcher m = pat.matcher(t);
                if(m.find()) {
                    ((IChing) getApplicationContext()).setPesqId(m.group(1));
                }else{ //SERVER GONE WRONG
                    mess=t;
                    //Snackbar.make(findViewById(R.id.email_login_form),t,Snackbar.LENGTH_LONG).setAction("NOP",null).show();
                    return false;
                }
                ((IChing)getApplicationContext()).setCookieJar(cookieJar);
                return true;
            } catch (IOException e) {
                mess=e.getLocalizedMessage();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                Intent intent=new Intent(context,Lista.class);
                intent.putExtra("CNETSERVERLOGACAO",((IChing)getApplicationContext()).getCookieJarCookies());
                startActivity(intent);
                finish();
            } else {
                mLoginFormView.setVisibility(View.VISIBLE);
                if(mess!=null) {
                    if (mess.equals("FAIL")) {
                        mess = getString(R.string.error_incorrect_password);
                        mPasswordView.setError(mess);
                        mPasswordView.requestFocus();
                        return;
                    }
                }
                serverNotAvailable();
                Snackbar.make(findViewById(R.id.email_login_form), mess, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private class CityTask extends AsyncTask<String, String, Boolean> {
        private String mess;

        @Override
        protected Boolean doInBackground(String... params) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder()
                    .url(getString(R.string.cidades_url))
                    .build();
            Response response_l = null;
            try {
                response_l = client.newCall(request).execute();
                String j = response_l.body().string();
                cidades = new JSONObject(j);
                return true;
            }
            catch (IOException e) {
                mess=e.getLocalizedMessage();
            } catch (JSONException e) {
                mess=e.getLocalizedMessage();
            }
            return false;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                findViewById(R.id.cidade_select).setVisibility(View.VISIBLE);
                List<String> lic= new ArrayList<>();
                Iterator<String> ky = cidades.keys();
                while (ky.hasNext()){
                    lic.add((String)ky.next());
                }
                ArrayAdapter<String> ass = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item, lic);
                ((Spinner)findViewById(R.id.city_spinner)).setAdapter(ass);
                SharedPreferences sharedpreferences = getSharedPreferences("login", Context.MODE_PRIVATE);
                SharedPreferences.Editor e = sharedpreferences.edit();
                e.putString("cidades",cidades.toString());
                e.commit();
                String cidade=sharedpreferences.getString("cidade","");
                if(!cidade.equals("")){
                    int spinnerPosition = ass.getPosition(cidade);
                    ((Spinner)findViewById(R.id.city_spinner)).setSelection(spinnerPosition);
                }
            } else {
                Snackbar.make(findViewById(R.id.email_login_form), mess, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        }
    }

    private void serverNotAvailable() {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(getString(R.string.server_not_available))
                .setMessage(getString(R.string.try_again))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        startActivity(getIntent());
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}

