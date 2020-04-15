package ai.hearo.tester;

import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private View decorView;
    private WebView webView;
    private AccessibilityService a;
    private boolean screenIsDarkened;

    private static final String TAG = "screen-sharing " + MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        screenIsDarkened = false;


        webView = (WebView)findViewById(R.id.hearoWebView); 
        setUpWebViewDefaults(webView);
        requestPermissions();

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "onPermissionRequest");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, request.getOrigin().toString());
                        request.grant(request.getResources());
                    }
                });
            }
        });

        webView.clearSslPreferences();

        decorView = getWindow().getDecorView();
        hideSystemUI();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                hideSystemUI();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Button brightnessButton = (Button)findViewById(R.id.brightness);
        brightnessButton.setText("Darken Screen");

        brightnessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(screenIsDarkened) {
                    // Brighten Screen Screen
                    brightnessButton.setText("Darken Screen");
                    screenIsDarkened = false;

                    WindowManager.LayoutParams layout = getWindow().getAttributes();
                    layout.screenBrightness = 1F;
                    getWindow().setAttributes(layout);
                } else {
                    // Darken Screen
                    brightnessButton.setText("Brighten Screen");
                    screenIsDarkened = true;

                    WindowManager.LayoutParams layout = getWindow().getAttributes();
                    layout.screenBrightness = 0.0F;
                    getWindow().setAttributes(layout);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setRationale(getString(R.string.rationale_ask_again))
                    .setPositiveButton("Settings")
                    .setNegativeButton("Cancel")
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            webView.loadUrl("https://opentokrtc.com/room/test?userName=test123Hearo");
        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic so you can perform video calls", RC_VIDEO_APP_PERM, perms);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        PowerManager.WakeLock  wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "HEARO::WakeLock");

        //acquire will turn on the display
        wakeLock.acquire();

        //release will release the lock from CPU, in case of that, screen will go back to sleep mode in defined time bt device settings
        //wakeLock.release();
    }

    public List<Map<String, String>> getWifiListNearby() {
        Log.d("wifi", "testing wifi");
        List<Map<String, String>> arrayList = new ArrayList();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) {
            return arrayList;
        }

        List<ScanResult> scanResults = scanResults = wifiManager.getScanResults();

        if (scanResults == null) {
            return arrayList;
        }
        for (ScanResult scanResult : scanResults) {
            Map hashMap = new HashMap();
            hashMap.put("wifiMac", scanResult.BSSID == null ? "" : scanResult.BSSID);
            hashMap.put("ssid", scanResult.SSID);
            hashMap.put("rssi", scanResult.level);
            arrayList.add(hashMap);
            Log.d("wifi", scanResult.SSID);
        }
        return arrayList;
    }


    @Override
    public void onBackPressed() {
        return;
    }

    public void hideSystemUI() {
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpWebViewDefaults(WebView _webView) {
        WebSettings settings = _webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDomStorageEnabled(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        WebView.setWebContentsDebuggingEnabled(true);
        _webView.setWebViewClient(new WebViewClient());

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(_webView, true);
    }
}
