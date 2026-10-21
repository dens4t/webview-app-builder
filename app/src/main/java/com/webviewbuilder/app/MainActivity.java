package com.webviewbuilder.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.adsbase.StartAppSDK;

public class MainActivity extends Activity {

    private static final String TAG = "StartIoBanner";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float density = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(Color.WHITE);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        final ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 100) {
                    progress.setVisibility(View.GONE);
                } else {
                    progress.setVisibility(View.VISIBLE);
                    progress.setProgress(newProgress);
                }
            }
        });

        content.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        content.addView(progress, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        root.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f));

        setContentView(root);

        if (getResources().getBoolean(R.bool.show_ads)) {
            // Prefer a Start.io banner when an App ID is configured for this
            // build; otherwise fall back to the built-in App Builder promo.
            String appId = startIoAppId();
            if (appId.isEmpty() || appId.equals("0")) {
                addAdBanner(root, density);
            } else {
                initStartIo(root, density, appId);
            }
        }

        webView.loadUrl(getString(R.string.app_url));
    }

    /**
     * Initializes Start.io explicitly (the manifest auto-init provider is
     * removed in AndroidManifest.xml) and only attaches the banner once the
     * SDK reports it is ready. This avoids the blank banner that appears when
     * an ad request races SDK initialization.
     */
    private void initStartIo(final LinearLayout root, final float density, String appId) {
        try {
            if (getResources().getBoolean(R.bool.startio_test_ads)) {
                StartAppSDK.setTestAdsEnabled(true);
                Log.i(TAG, "Start.io test ads enabled");
            }
            Log.i(TAG, "Start.io init appId=" + appId);
            StartAppSDK.initParams(this, appId)
                    .setReturnAdsEnabled(false)
                    .setCallback(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!addStartIoBanner(root, density)) {
                                        addAdBanner(root, density);
                                    }
                                }
                            });
                        }
                    })
                    .init();
        } catch (Throwable error) {
            Log.e(TAG, "Start.io init failed", error);
            addAdBanner(root, density);
        }
    }

    /** Start.io App ID baked into the manifest meta-data ("" when unset). */
    private String startIoAppId() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            Object value = info.metaData != null
                    ? info.metaData.get("com.startapp.sdk.APPLICATION_ID") : null;
            return value != null ? String.valueOf(value).trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Adds a Start.io banner docked at the bottom, below the WebView.
     * Returns false when the banner cannot be created, so the caller can fall
     * back to the built-in App Builder promo. Any SDK problem is swallowed:
     * ads must never break the WebView.
     */
    private boolean addStartIoBanner(LinearLayout root, float density) {
        try {
            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#111111"));
            root.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.max(1, (int) (3 * density))));

            Banner banner = new Banner(this, new BannerListener() {
                @Override
                public void onReceiveAd(View view) {
                    Log.i(TAG, "banner received");
                }

                @Override
                public void onFailedToReceiveAd(View view) {
                    Log.w(TAG, "banner failed to receive ad (no fill or network)");
                }

                @Override
                public void onImpression(View view) {
                    Log.i(TAG, "banner impression");
                }

                @Override
                public void onClick(View view) {
                    Log.i(TAG, "banner click");
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            root.addView(banner, params);
            return true;
        } catch (Throwable error) {
            Log.e(TAG, "banner creation failed", error);
            return false;
        }
    }

    private void addAdBanner(LinearLayout root, float density) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#111111"));
        root.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, (int) (3 * density))));

        TextView banner = new TextView(this);
        banner.setText("IKLAN  \u2022  Dibuat dengan App Builder  \u2192  builder.deni.ai.id");
        banner.setTextSize(12);
        banner.setTypeface(Typeface.DEFAULT_BOLD);
        banner.setTextColor(Color.parseColor("#111111"));
        banner.setBackgroundColor(Color.parseColor("#ffd23f"));
        banner.setGravity(Gravity.CENTER);
        int pad = (int) (10 * density);
        banner.setPadding(pad, pad, pad, pad);
        banner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.ad_url))));
                } catch (Exception ignored) {
                }
            }
        });
        root.addView(banner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
