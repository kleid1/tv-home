package org.tvhome;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.io.File;

/**
 * The TV's Home: CleanFrame's web interface (web/cleanframe, packaged as assets) in a full-screen WebView.
 * Pages are served from Android's private appassets origin, so nothing needs file:// access; installed apps'
 * icons are served from the cache at /icons/. CleanFrame scales its 1920 x 1080 stage to the window itself.
 */
public class HomeActivity extends Activity {
    private static final String HOME = "https://" + WebViewAssetLoader.DEFAULT_DOMAIN + "/assets/cleanframe/index.html";

    /** CleanFrame's Back: keyCode 27 down and up on the document, where its key listener sits. */
    private static final String BACK_JS = "(function(){['keydown','keyup'].forEach(function(t){"
            + "var e=new KeyboardEvent(t,{bubbles:true,cancelable:true,key:'Escape'});"
            + "Object.defineProperty(e,'keyCode',{value:27});document.dispatchEvent(e);});})()";

    private WebView web;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        File icons = new File(getCacheDir(), Bridge.ICON_DIR);
        icons.mkdirs();
        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/" + Bridge.ICON_DIR + "/", new WebViewAssetLoader.InternalStoragePathHandler(this, icons))
                .build();

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // CleanFrame keeps its choices in localStorage
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);                    // the TV's font scale must not resize a fixed 1920 x 1080 design
        // The page is CleanFrame's 1920 x 1080 design (index.html's viewport), zoomed out to fill the screen
        // whatever its pixel density; the TV's 4K density otherwise doubles it.
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assets.shouldInterceptRequest(request.getUrl());
            }
        });
        Bridge bridge = new Bridge(this);
        bridge.connectBluetooth();
        web.addJavascriptInterface(bridge, "AndroidTV");
        setContentView(web);
        web.loadUrl(HOME);
        web.requestFocus();
    }

    /** Back goes to CleanFrame (it closes its own panels), never to Android, which would leave Home. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) web.evaluateJavascript(BACK_JS, null);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /** The remote's Home button reaches this already-open Home screen: close any panel. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        web.evaluateJavascript("window.FrameHome&&FrameHome()", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }
}
