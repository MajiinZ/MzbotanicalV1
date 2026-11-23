package org.mz.botanicals.storefrontpreview;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/** Renders only the bundled, offline storefront review build. */
public final class PreviewActivity extends Activity {
    private static final String HOST = "storefront-preview.invalid";
    private static final Map<String, String> HEADERS = Collections.singletonMap(
        "Content-Security-Policy", "default-src 'self'; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'");
    private WebView web;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isPreviewUri(request.getUrl());
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getEncodedPath();
                if (isPreviewUri(uri) && "GET".equals(request.getMethod()) && path != null) {
                    String mime = assetType(path);
                    if (mime != null) {
                        try {
                            return new WebResourceResponse(mime, mime.startsWith("text/") ? "UTF-8" : null,
                                200, "OK", HEADERS, getAssets().open(path.substring(1)));
                        } catch (IOException ignored) { /* Return the local 404 below. */ }
                    }
                }
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", HEADERS,
                    new ByteArrayInputStream(new byte[0]));
            }
        });
        setContentView(web);
        web.loadUrl("https://" + HOST + "/index.html");
    }

    private static boolean isPreviewUri(Uri uri) {
        return "https".equals(uri.getScheme()) && HOST.equals(uri.getHost()) &&
            uri.getPort() == -1 && uri.getUserInfo() == null;
    }

    private static String assetType(String path) {
        switch (path) {
            case "/index.html": return "text/html";
            case "/assets/app.js": return "text/javascript";
            case "/assets/app.css": return "text/css";
            case "/brand/exo2-regular.otf": return "font/otf";
            case "/brand/mz-brand.png":
            case "/brand/mz-welcome.png": return "image/png";
            default: return null;
        }
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
