package net.alien9.iching;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Created by tiago on 10/01/17.
 */
public class Util {
    public static boolean hasValidSession(Context context) {
        Intent intent = ((Activity) context).getIntent();
        if (!intent.hasExtra("CNETSERVERLOGACAO"))
            return false;
        String cookie = (String) intent.getExtras().get("CNETSERVERLOGACAO");
        if (cookie != null)
            return true;
        return false;
    }

    public static CookieJar getCookieJar(Context context) {
        return new CookiePot();
    }

    private static class CookiePot implements CookieJar {
        private List<Cookie> cookies;

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            this.cookies = cookies;
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            if (cookies != null)
                return cookies;
            return new ArrayList<Cookie>();
        }

        public List<Cookie> getCookies() {
            return cookies;
        }

        public String getCookie(String name) {
            return cookies.toString();
        }
    }

    public static boolean unpackZip(String path, String zipname) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(path + File.separator + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                // zapis do souboru
                filename = ze.getName();

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }
                FileOutputStream fout = new FileOutputStream(path + filename);

                // cteni zipu a zapis
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                fout.close();
                zis.closeEntry();
            }

            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean unzip(String path, String zipname) {
        try {
            FileInputStream fin = new FileInputStream(path + File.separator + zipname);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                Log.v("Decompress", "Unzipping " + ze.getName());

                if (ze.isDirectory()) {
                    Util._dirChecker(ze.getName(), path);
                } else {
                    FileOutputStream fout = new FileOutputStream(path + File.separator + ze.getName());
                    byte b[] = new byte[1024];
                    int n;
                    while ((n = zin.read(b, 0, 1024)) >= 0) {
                        fout.write(b, 0, n);
                    }

                    //for (int c = zin.read(); c != -1; c = zin.read()) {
                    //    fout.write(c);
                    //}

                    zin.closeEntry();
                    fout.close();
                }

            }
            zin.close();
        } catch (Exception e) {
            Log.e("Decompress", "unzip", e);
            return false;
        }
        return true;
    }

    private static void _dirChecker(String dir, String location) {
        File f = new File(location + File.separator + dir);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    static boolean isValid(final File file) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }

    public static boolean cnsValido(String cns) {
        if(cns.length()==0)return true;
        if (cns.length() > 15) return false;
        while (cns.length() < 15) cns = '0' + cns;
        Pattern ptn1 = Pattern.compile("\\A[1-2]\\d{10}00[0-1]\\d\\Z", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern ptn2 = Pattern.compile("\\A[7-9]\\d{14}\\Z", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        if (ptn1.matcher(cns).matches() || ptn2.matcher(cns).matches()) {
            return (mod11CNS(cns) % 11 == 0);
        }
        return false;
    }

    static int mod11CNS(String cns) {
        String[] ac = cns.split("");
        int soma = 0;
        for (int i = 1; i < ac.length; i++) {
            soma += Integer.parseInt(ac[i]) * (16 - i);
        }
        return soma;
    }
}

