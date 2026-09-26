package com.piopionihongo.app;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pio Pio Nihongo
 *
 * - Uygulama arayüzü assets/index.html içinde (tools/build.py üretir).
 * - Japonca sesli okuma için cihazın TextToSpeech motoru kullanılır (JS: window.AndroidTTS).
 * - Veri güncellemeleri assets/config.json içindeki remoteBaseUrl adresinden
 *   (GitHub Pages) indirilir ve cihazda saklanır (JS: window.AndroidData).
 * - Premium ve destek satın alımları Google Play Billing ile yapılır (JS: window.AndroidBilling).
 *   Play Console'da şu ürünler oluşturulmalı:
 *     premium_unlock  – tek seferlik ürün (tüketilmez, bir kez alınır)
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final String DATA_FILE = "remote-data.json";
    private static final int MAX_DOWNLOAD_BYTES = 30 * 1024 * 1024;

    private WebView web;
    private FrameLayout root;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private volatile String noVoiceMessage = "Japonca ses bulunamadı.";
    private String remoteBaseUrl = "";
    private boolean pageLoaded = false;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OnBackInvokedCallback backCallback;

    // Satın alma
    private static final String PREMIUM_ID = "premium_unlock";
    private BillingClient billing;
    private volatile boolean billingReady = false;
    private volatile boolean premiumOwned = false;
    private volatile boolean premiumPending = false;
    private final Map<String, ProductDetails> products = new ConcurrentHashMap<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        remoteBaseUrl = readRemoteBaseUrl();
        prefs = getSharedPreferences("kanji", MODE_PRIVATE);
        premiumOwned = prefs.getBoolean("premium", false);

        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int bg = Color.parseColor(night ? "#0E0F26" : "#ECEEFB");

        web = new WebView(this);
        web.setBackgroundColor(bg);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        // Telefonun yazı boyutu ayarı düzeni bozmasın; uygulama kendi boyutlarını ekrana göre ayarlıyor
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Kaynaklar ekranındaki bağlantıları tarayıcıda aç
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                    }
                    return true;
                }
                // Hata bildirimi: telefondaki e-posta uygulamasını aç (sunucu kullanılmaz)
                if ("mailto".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_SENDTO, uri));
                    } catch (Exception e) {
                        try {
                            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, uri), null));
                        } catch (Exception ignored) {
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
            }
        });

        web.addJavascriptInterface(new TtsBridge(), "AndroidTTS");
        web.addJavascriptInterface(new DataBridge(), "AndroidData");
        web.addJavascriptInterface(new BillingBridge(), "AndroidBilling");
        web.addJavascriptInterface(new NotifyBridge(), "AndroidNotify");
        web.addJavascriptInterface(new SoundBridge(), "AndroidSound");
        web.addJavascriptInterface(new BackupBridge(), "AndroidBackup");

        // WebView kenar boşluğunu (padding) içerik için dikkate almadığından
        // sistem çubuklarının boşluğu bu kapsayıcıya verilir.
        root = new FrameLayout(this);
        root.setBackgroundColor(bg);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        setupSystemBars(night, bg);
        setupBackHandling();

        tts = new TextToSpeech(this, this);
        setupBilling();
        web.loadUrl("file:///android_asset/index.html");
    }

    // ---------------------------------------------------------------- Sistem çubukları

    private void setupSystemBars(boolean night, int bg) {
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15+ uygulamayı kenardan kenara çizer; içeriği sistem çubuklarının dışına it.
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    Insets bars = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    // Uygulama durum çubuğunun altında, gezinme çubuğunun (geri/ana ekran) üstünde kalır
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return WindowInsets.CONSUMED;
                }
            });
        } else {
            getWindow().setStatusBarColor(bg);
            getWindow().setNavigationBarColor(bg);
        }

        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(night ? 0 : light, light);
            }
        } else if (!night) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
            if (Build.VERSION.SDK_INT < 26) getWindow().setNavigationBarColor(Color.BLACK);
        }
    }

    // ---------------------------------------------------------------- Geri tuşu

    private void setupBackHandling() {
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    handleBack();
                }
            };
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    /** Android 12 ve altı için. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    /** Önce uygulama içinde geri git (menü, alt sayfa); gidilecek yer yoksa kapat. */
    private void handleBack() {
        if (!pageLoaded) {
            finish();
            return;
        }
        web.evaluateJavascript("window.appBack ? window.appBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) finish();
            }
        });
    }

    // ---------------------------------------------------------------- Sesli okuma

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            int r = tts.setLanguage(Locale.JAPANESE);
            ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(0.85f);
        }
    }

    private class TtsBridge {
        @JavascriptInterface
        public void setMessage(String msg) {
            if (msg != null) noVoiceMessage = msg;
        }

        @JavascriptInterface
        public void speak(final String text) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!ttsReady || tts == null) {
                        Toast.makeText(MainActivity.this, noVoiceMessage, Toast.LENGTH_LONG).show();
                        return;
                    }
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kanji");
                }
            });
        }
    }

    // ---------------------------------------------------------------- Veri güncelleme

    // ---------------------------------------------------------------- Günlük hatırlatma bildirimi
    private static final int UPDATE_NOTIF_ID = 4203;
    private static final int TEST_NOTIF_ID = 4204;

    /** Bildirim kanalını oluşturur ve (Android 13+) izni ister; uygulama böylece
     *  telefonun Ayarlar > Bildirimler listesinde görünür olur. */
    private void ensureNotificationSetup() {
        ReminderReceiver.ensureChannel(this);
        ReminderReceiver.schedule(this); // kayıtlı hatırlatma varsa yeniden kur
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4201);
        }
    }

    /** Açılış ekranındaki "cık cık" sesi (res/raw/chirp.wav). Kullanıcı dokunmadan da çalar. */
    private class SoundBridge {
        @JavascriptInterface
        public void chirp(final int delayMs) {
            mainHandler.postDelayed(new Runnable() {
                public void run() { playChirp(); }
            }, Math.max(0, Math.min(delayMs, 5000)));
        }
    }

    private void playChirp() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.chirp);
            if (mp == null) return;
            mp.setVolume(0.8f, 0.8f);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer p) { p.release(); }
            });
            mp.start();
        } catch (Exception e) { /* ses çalınamazsa sessizce geç */ }
    }

    // ---------------------------------------------------------------- Yedekleme (dosya kullanıcının seçtiği yere: telefon veya kendi Google Drive'ı)
    private static final int REQ_BACKUP_SAVE = 7101;
    private static final int REQ_BACKUP_OPEN = 7102;
    private volatile String pendingBackup = null;

    private class BackupBridge {
        @JavascriptInterface
        public void save(final String fileName, final String json) {
            pendingBackup = json;
            mainHandler.post(new Runnable() {
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("application/json");
                        i.putExtra(Intent.EXTRA_TITLE, fileName);
                        startActivityForResult(i, REQ_BACKUP_SAVE);
                    } catch (Exception e) {
                        callJs("window.onBackupSaved && window.onBackupSaved(false)");
                    }
                }
            });
        }

        @JavascriptInterface
        public void open() {
            mainHandler.post(new Runnable() {
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
                        startActivityForResult(i, REQ_BACKUP_OPEN);
                    } catch (Exception e) { /* dosya seçici yoksa sessizce geç */ }
                }
            });
        }
    }

    private void callJs(final String js) {
        mainHandler.post(new Runnable() {
            public void run() { if (web != null) web.evaluateJavascript(js, null); }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
        if (requestCode == REQ_BACKUP_SAVE) {
            final String json = pendingBackup;
            pendingBackup = null;
            if (uri == null || json == null) return;
            io.execute(new Runnable() {
                public void run() {
                    boolean ok = false;
                    try {
                        java.io.OutputStream os = getContentResolver().openOutputStream(uri, "wt");
                        if (os != null) {
                            os.write(json.getBytes("UTF-8"));
                            os.close();
                            ok = true;
                        }
                    } catch (Exception e) { ok = false; }
                    callJs("window.onBackupSaved && window.onBackupSaved(" + ok + ")");
                }
            });
        } else if (requestCode == REQ_BACKUP_OPEN) {
            if (uri == null) return;
            io.execute(new Runnable() {
                public void run() {
                    try {
                        java.io.InputStream is = getContentResolver().openInputStream(uri);
                        if (is == null) return;
                        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                        byte[] buf = new byte[16384];
                        int n, total = 0;
                        while ((n = is.read(buf)) > 0) {
                            total += n;
                            if (total > 20 * 1024 * 1024) break;
                            bo.write(buf, 0, n);
                        }
                        is.close();
                        String text = new String(bo.toByteArray(), "UTF-8");
                        callJs("window.onBackupFile && window.onBackupFile(" + JSONObject.quote(text) + ")");
                    } catch (Exception e) {
                        callJs("window.onBackupFile && window.onBackupFile('')");
                    }
                }
            });
        }
    }

    private class NotifyBridge {
        /** Telefonun bu uygulamaya ait bildirim ayarları sayfasını açar. */
        @JavascriptInterface
        public void openNotificationSettings() {
            mainHandler.post(new Runnable() {
                public void run() {
                    Intent i;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        i = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                    } else {
                        i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName()));
                    }
                    try { startActivity(i); } catch (Exception e) {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName())));
                    }
                }
            });
        }

        /** Bildirim izni verilmiş mi? (Android 13 öncesinde her zaman true) */
        @JavascriptInterface
        public boolean hasPermission() {
            if (Build.VERSION.SDK_INT < 33) return true;
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        /** Tam saatinde alarm kurulabiliyor mu? (Android 12+ "Alarmlar ve hatırlatıcılar" izni) */
        @JavascriptInterface
        public boolean canExactAlarm() {
            return ReminderReceiver.canExact(MainActivity.this);
        }

        /** "Alarmlar ve hatırlatıcılar" izin sayfasını açar. */
        @JavascriptInterface
        public void openExactAlarmSettings() {
            mainHandler.post(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT < 31) return;
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName())));
                    }
                }
            });
        }

        /** Hatırlatma bildirimini hemen gösterir (ayarların doğru olduğunu denemek için). */
        @JavascriptInterface
        public void testNotification() {
            mainHandler.post(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4201);
                        return;
                    }
                    ReminderReceiver.showReminder(MainActivity.this);
                }
            });
        }

        @JavascriptInterface
        public void showUpdateNotification(final String version) {
            mainHandler.post(new Runnable() {
                public void run() {
                    boolean tr = ReminderReceiver.isTurkish(MainActivity.this);
                    ReminderReceiver.post(MainActivity.this, UPDATE_NOTIF_ID, "Pio Pio Nihongo",
                            tr ? "Uygulama güncellendi! Yeni içerik hazır (v" + version + ")."
                               : "App updated! New content is ready (v" + version + ").");
                }
            });
        }

        /** Günlük hatırlatmayı saat/dakikaya kurar; ayarlar kalıcıdır (yeniden başlatmadan sonra da). */
        @JavascriptInterface
        public void scheduleReminder(final int hour, final int minute, final String lang) {
            mainHandler.post(new Runnable() {
                public void run() {
                    getSharedPreferences(ReminderReceiver.PREFS, MODE_PRIVATE).edit()
                            .putString("lang", lang).putBoolean("enabled", true)
                            .putInt("hour", hour).putInt("minute", minute).apply();
                    if (Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4201);
                    }
                    ReminderReceiver.schedule(MainActivity.this);
                }
            });
        }

        @JavascriptInterface
        public void cancelReminder() {
            mainHandler.post(new Runnable() {
                public void run() {
                    getSharedPreferences(ReminderReceiver.PREFS, MODE_PRIVATE).edit()
                            .putBoolean("enabled", false).apply();
                    ReminderReceiver.schedule(MainActivity.this);
                }
            });
        }
    }

    private class DataBridge {
        /** Cihazda saklanan son indirilen veri ("" = yok). */
        @JavascriptInterface
        public String getData() {
            File f = new File(getFilesDir(), DATA_FILE);
            if (!f.exists()) return "";
            try {
                return new String(readAll(new FileInputStream(f), MAX_DOWNLOAD_BYTES), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        }

        @JavascriptInterface
        public int appVersion() {
            return BuildConfig.VERSION_CODE;
        }

        @JavascriptInterface
        public boolean canUpdate() {
            return isRemoteConfigured();
        }

        /** Arka planda denetler; sonucu window.onDataStatus(durum, sürüm) ile bildirir. */
        @JavascriptInterface
        public void checkForUpdate(final int currentVersion) {
            io.execute(new Runnable() {
                @Override
                public void run() {
                    runUpdate(currentVersion);
                }
            });
        }

        /** İndirilen veriyi siler, uygulama içindeki veriye döner. */
        @JavascriptInterface
        public void clearData() {
            //noinspection ResultOfMethodCallIgnored
            new File(getFilesDir(), DATA_FILE).delete();
        }
    }

    private boolean isRemoteConfigured() {
        return remoteBaseUrl.startsWith("https://") && !remoteBaseUrl.contains("KULLANICI");
    }

    private void runUpdate(int currentVersion) {
        if (!isRemoteConfigured()) {
            notifyJs("disabled", currentVersion);
            return;
        }
        try {
            String verText = new String(
                    httpGet(remoteBaseUrl + "version.json?t=" + System.currentTimeMillis()),
                    StandardCharsets.UTF_8);
            JSONObject ver = new JSONObject(verText);
            int remoteVersion = ver.getInt("dataVersion");
            int minApp = ver.optInt("minAppVersion", 1);
            String expectedSha = ver.getString("sha256");
            String file = ver.optString("file", "data.json");

            if (minApp > BuildConfig.VERSION_CODE) {
                notifyJs("appTooOld", remoteVersion);
                return;
            }
            if (remoteVersion <= currentVersion) {
                notifyJs("uptodate", currentVersion);
                return;
            }

            byte[] body = httpGet(remoteBaseUrl + file + "?v=" + remoteVersion);
            if (!sha256(body).equalsIgnoreCase(expectedSha)) throw new IOException("sha mismatch");

            JSONObject data = new JSONObject(new String(body, StandardCharsets.UTF_8));
            if (data.getJSONObject("meta").getInt("version") != remoteVersion
                    || data.getJSONArray("kanji").length() < 100) {
                throw new IOException("invalid data");
            }

            File tmp = new File(getFilesDir(), DATA_FILE + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(body);
            }
            File target = new File(getFilesDir(), DATA_FILE);
            if (!tmp.renameTo(target)) throw new IOException("rename failed");

            notifyJs("updated", remoteVersion);
        } catch (Exception e) {
            notifyJs("failed", currentVersion);
        }
    }

    private void notifyJs(final String status, final int version) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (web == null) return;
                web.evaluateJavascript(
                        "window.onDataStatus && window.onDataStatus('" + status + "'," + version + ")", null);
            }
        });
    }

    private byte[] httpGet(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setUseCaches(false);
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            return readAll(c.getInputStream(), MAX_DOWNLOAD_BYTES);
        } finally {
            c.disconnect();
        }
    }

    private static byte[] readAll(InputStream in, int limit) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] b = new byte[16384];
            int n;
            int total = 0;
            while ((n = is.read(b)) != -1) {
                total += n;
                if (total > limit) throw new IOException("too large");
                buf.write(b, 0, n);
            }
            return buf.toByteArray();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte x : h) sb.append(String.format(Locale.ROOT, "%02x", x));
        return sb.toString();
    }

    private String readRemoteBaseUrl() {
        try {
            byte[] b = readAll(getAssets().open("config.json"), 64 * 1024);
            String url = new JSONObject(new String(b, StandardCharsets.UTF_8)).optString("remoteBaseUrl", "");
            if (!url.isEmpty() && !url.endsWith("/")) url += "/";
            return url;
        } catch (Exception e) {
            return "";
        }
    }


    // ---------------------------------------------------------------- Satın alma (Google Play Billing)

    private final PurchasesUpdatedListener purchasesListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
            int code = result.getResponseCode();
            if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase p : purchases) handlePurchase(p, true);
            } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                queryPurchases("restored");
            } else if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
                pushBilling(null);
            } else {
                pushBilling("error");
            }
        }
    };

    private void setupBilling() {
        billing = BillingClient.newBuilder(this)
                .setListener(purchasesListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        billing.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    billingReady = true;
                    queryProducts();
                    queryPurchases(null);
                } else {
                    pushBilling(null);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() bir sonraki çağrıda bağlantıyı kendisi yeniler.
            }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        list.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(list).build();
        billing.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(BillingResult result, QueryProductDetailsResult details) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && details != null) {
                    for (ProductDetails pd : details.getProductDetailsList()) {
                        products.put(pd.getProductId(), pd);
                    }
                }
                pushBilling(null);
            }
        });
    }

    /** Hesaptaki satın alımları kontrol eder (açılışta, geri dönüşte ve "geri yükle"de). */
    private void queryPurchases(final String messageAfter) {
        if (billing == null || !billingReady) {
            pushBilling(messageAfter != null ? "unavailable" : null);
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billing.queryPurchasesAsync(params, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(BillingResult result, List<Purchase> purchases) {
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    pushBilling(messageAfter != null ? "unavailable" : null);
                    return;
                }
                boolean owned = false;
                boolean pending = false;
                for (Purchase p : purchases) {
                    if (p.getProducts().contains(PREMIUM_ID)) {
                        if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) owned = true;
                        else if (p.getPurchaseState() == Purchase.PurchaseState.PENDING) pending = true;
                    }
                    handlePurchase(p, false);
                }
                setPremium(owned);
                premiumPending = pending && !owned;
                String msg = messageAfter;
                if ("restored".equals(messageAfter) && !owned) msg = "notOwned";
                pushBilling(msg);
            }
        });
    }

    private void handlePurchase(final Purchase p, final boolean fromFlow) {
        List<String> ids = p.getProducts();
        if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (ids.contains(PREMIUM_ID)) {
                setPremium(true);
                premiumPending = false;
                if (!p.isAcknowledged()) {
                    // 3 gün içinde onaylanmayan satın alımlar Google tarafından iade edilir.
                    AcknowledgePurchaseParams ap = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(p.getPurchaseToken())
                            .build();
                    billing.acknowledgePurchase(ap, new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(BillingResult result) {
                            pushBilling(null);
                        }
                    });
                }
                if (fromFlow) pushBilling("purchased");
            }
        } else if (p.getPurchaseState() == Purchase.PurchaseState.PENDING && ids.contains(PREMIUM_ID)) {
            premiumPending = true;
            if (fromFlow) pushBilling("pending");
        }
    }

    private void setPremium(boolean owned) {
        premiumOwned = owned;
        prefs.edit().putBoolean("premium", owned).apply();
    }

    private void launchPurchase(String productId) {
        ProductDetails pd = products.get(productId);
        if (billing == null || !billingReady || pd == null) {
            pushBilling("unavailable");
            if (billingReady) queryProducts();
            return;
        }
        BillingFlowParams.ProductDetailsParams.Builder b =
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd);
        List<ProductDetails.OneTimePurchaseOfferDetails> offers = pd.getOneTimePurchaseOfferDetailsList();
        if (offers != null && !offers.isEmpty()) {
            String token = offers.get(0).getOfferToken();
            if (token != null && !token.isEmpty()) b.setOfferToken(token);
        }
        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(b.build()))
                .build();
        BillingResult r = billing.launchBillingFlow(this, flow);
        if (r.getResponseCode() != BillingClient.BillingResponseCode.OK) pushBilling("error");
    }

    private String priceOf(String productId) {
        ProductDetails pd = products.get(productId);
        if (pd == null) return "";
        List<ProductDetails.OneTimePurchaseOfferDetails> offers = pd.getOneTimePurchaseOfferDetailsList();
        if (offers == null || offers.isEmpty()) return "";
        return offers.get(0).getFormattedPrice();
    }

    private String billingState() {
        try {
            JSONObject o = new JSONObject();
            o.put("available", billingReady && products.containsKey(PREMIUM_ID));
            o.put("owned", premiumOwned);
            o.put("pending", premiumPending);
            o.put("price", priceOf(PREMIUM_ID));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private void pushBilling(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (web == null) return;
                String js = "window.onBillingUpdate && window.onBillingUpdate("
                        + JSONObject.quote(billingState()) + ","
                        + (message == null ? "null" : JSONObject.quote(message)) + ")";
                web.evaluateJavascript(js, null);
            }
        });
    }

    private class BillingBridge {
        @JavascriptInterface
        public String getState() {
            return billingState();
        }

        @JavascriptInterface
        public void buy(final String productId) {
            if (!PREMIUM_ID.equals(productId)) return;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    launchPurchase(productId);
                }
            });
        }

        @JavascriptInterface
        public void restore() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    queryPurchases("restored");
                }
            });
        }
    }

    private boolean notificationSetupDone = false;

    @Override
    protected void onResume() {
        super.onResume();
        // Uygulama dışında tamamlanan (ör. bekleyen) satın alımları yakala
        if (billingReady) queryPurchases(null);
        // İzin isteği pencere tamamen kurulduktan sonra tetiklenmeli, yoksa bazı
        // cihazlarda sessizce başarısız olup diyalog hiç görünmüyor.
        if (!notificationSetupDone) {
            notificationSetupDone = true;
            ensureNotificationSetup();
        }
    }

    // ---------------------------------------------------------------- Yaşam döngüsü

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        io.shutdownNow();
        if (billing != null) {
            billing.endConnection();
            billing = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
