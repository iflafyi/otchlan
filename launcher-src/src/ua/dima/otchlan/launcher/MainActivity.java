package ua.dima.otchlan.launcher;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class MainActivity extends Activity {
    static final String BASE = "https://raw.githubusercontent.com/iflafyi/otchlan/main/";
    static final String GAME_PKG = "ua.dima.otchlan";

    static MainActivity instance;
    private WebView web;
    private volatile boolean busy = false;
    private File pendingApk;      // downloaded APK waiting for the "unknown sources" permission
    private String pendingKind;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        instance = this;
        getWindow().setStatusBarColor(0xFF0B0706);
        getWindow().setNavigationBarColor(0xFF0B0706);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setBackgroundColor(0xFF0B0706);
        web.addJavascriptInterface(new Bridge(), "Launcher");
        setContentView(web);
        web.loadUrl("file:///android_asset/launcher.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        instance = this;
        js("window.onAppResume&&onAppResume()");
        if (pendingApk != null && canInstall()) {
            File f = pendingApk; String k = pendingKind;
            pendingApk = null; pendingKind = null;
            startInstall(f, k);
        }
    }

    @Override
    protected void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    // ---------- helpers ----------
    void js(final String code) {
        runOnUiThread(new Runnable() { public void run() { if (web != null) web.evaluateJavascript(code, null); } });
    }

    long installedCode(String pkg) {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(pkg, 0);
            return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) { return -1; }
    }

    String installedName(String pkg) {
        try { return getPackageManager().getPackageInfo(pkg, 0).versionName; }
        catch (PackageManager.NameNotFoundException e) { return ""; }
    }

    boolean canInstall() {
        return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls();
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[16384]; int n;
        while ((n = in.read(b)) > 0) bo.write(b, 0, n);
        return bo.toByteArray();
    }

    static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setUseCaches(false);
        c.setRequestProperty("Cache-Control", "no-cache");
        c.setInstanceFollowRedirects(true);
        return c;
    }

    // ---------- install ----------
    void startInstall(File apk, String kind) {
        if (!canInstall()) {
            pendingApk = apk; pendingKind = kind;
            js("onNeedPermission()");
            return;
        }
        try {
            PackageInstaller pi = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            p.setAppPackageName("launcher".equals(kind) ? getPackageName() : GAME_PKG);
            p.setSize(apk.length());
            if (Build.VERSION.SDK_INT >= 31) p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
            int id = pi.createSession(p);
            PackageInstaller.Session ses = pi.openSession(id);
            OutputStream out = ses.openWrite("base.apk", 0, apk.length());
            InputStream in = new FileInputStream(apk);
            byte[] b = new byte[65536]; int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            in.close();
            ses.fsync(out);
            out.close();
            Intent i = new Intent(this, InstallReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            PendingIntent pend = PendingIntent.getBroadcast(this, id, i, flags);
            ses.commit(pend.getIntentSender());
            ses.close();
            js("onInstallStarted()");
        } catch (Exception e) {
            busy = false;
            js("onError(" + JSONObject.quote("Instalacja nie powiodła się: " + e.getMessage()) + ")");
        }
    }

    void askUserToConfirm(final Intent confirm) {
        runOnUiThread(new Runnable() { public void run() {
            try { startActivity(confirm); }
            catch (Exception e) {
                busy = false;
                js("onError(" + JSONObject.quote("Nie można otworzyć instalatora: " + e.getMessage()) + ")");
            }
        }});
    }

    void onInstallResult(int status, String msg) {
        busy = false;
        js("onInstallResult(" + status + "," + JSONObject.quote(msg == null ? "" : msg) + ")");
    }

    // ---------- JS bridge ----------
    class Bridge {
        @JavascriptInterface
        public String state() {
            try {
                JSONObject o = new JSONObject();
                o.put("gameCode", installedCode(GAME_PKG));
                o.put("gameName", installedName(GAME_PKG));
                o.put("launcherCode", installedCode(getPackageName()));
                o.put("launcherName", installedName(getPackageName()));
                o.put("canInstall", canInstall());
                o.put("busy", busy);
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }

        @JavascriptInterface
        public void check() {
            new Thread(new Runnable() { public void run() {
                try {
                    HttpURLConnection c = open(BASE + "version.json?t=" + System.currentTimeMillis());
                    int code = c.getResponseCode();
                    if (code != 200) throw new Exception("HTTP " + code);
                    String txt = new String(readAll(c.getInputStream()), "UTF-8");
                    c.disconnect();
                    js("onManifest(" + JSONObject.quote(txt) + ")");
                } catch (Exception e) {
                    js("onManifestError(" + JSONObject.quote(String.valueOf(e.getMessage())) + ")");
                }
            }}).start();
        }

        @JavascriptInterface
        public void download(final String path, final String sha256, final String kind) {
            if (busy) return;
            if (path == null || path.contains("..") || path.startsWith("/") || path.contains(":")) return;
            busy = true;
            new Thread(new Runnable() { public void run() {
                File f = new File(getCacheDir(), "launcher".equals(kind) ? "launcher.apk" : "game.apk");
                try {
                    HttpURLConnection c = open(BASE + path + "?t=" + System.currentTimeMillis());
                    int code = c.getResponseCode();
                    if (code != 200) throw new Exception("HTTP " + code);
                    long total = c.getContentLength();
                    InputStream in = c.getInputStream();
                    OutputStream out = new FileOutputStream(f);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] b = new byte[32768]; int n; long done = 0, last = 0;
                    while ((n = in.read(b)) > 0) {
                        out.write(b, 0, n); md.update(b, 0, n); done += n;
                        long now = System.currentTimeMillis();
                        if (now - last > 120) { last = now; js("onProgress(" + done + "," + total + ")"); }
                    }
                    out.close(); in.close(); c.disconnect();
                    js("onProgress(" + done + "," + (total > 0 ? total : done) + ")");
                    if (sha256 != null && sha256.length() == 64) {
                        StringBuilder sb = new StringBuilder();
                        for (byte x : md.digest()) sb.append(String.format("%02x", x));
                        if (!sb.toString().equalsIgnoreCase(sha256)) throw new Exception("plik uszkodzony (zła suma kontrolna), spróbuj ponownie");
                    }
                    js("onDownloaded()");
                    startInstall(f, kind);
                } catch (Exception e) {
                    busy = false;
                    js("onError(" + JSONObject.quote("Pobieranie nie powiodło się: " + e.getMessage()) + ")");
                }
            }}).start();
        }

        @JavascriptInterface
        public void play() {
            runOnUiThread(new Runnable() { public void run() {
                Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
                if (i != null) startActivity(i);
                else js("onError('Gra nie jest zainstalowana')");
            }});
        }

        @JavascriptInterface
        public void openPermission() {
            runOnUiThread(new Runnable() { public void run() {
                if (Build.VERSION.SDK_INT >= 26) {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                    }
                }
            }});
        }

        @JavascriptInterface
        public void cancelPending() { pendingApk = null; pendingKind = null; busy = false; }
    }
}
