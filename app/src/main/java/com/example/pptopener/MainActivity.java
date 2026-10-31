package com.example.pptopener;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {

    private static final int PICK_PPT = 1001;

    private WebView webView;
    private boolean pageLoaded = false;
    private Uri pendingUri = null;
    private volatile String currentData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                if (pendingUri != null) {
                    Uri u = pendingUri;
                    pendingUri = null;
                    loadPresentation(u);
                }
            }
        });
        webView.loadUrl("file:///android_asset/index.html");

        handleIncoming(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncoming(intent);
    }

    private void handleIncoming(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())
                && intent.getData() != null) {
            openUri(intent.getData());
        }
    }

    private void openUri(Uri uri) {
        if (pageLoaded) {
            loadPresentation(uri);
        } else {
            pendingUri = uri;
        }
    }

    public class Bridge {
        @JavascriptInterface
        public void pick() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pickFile();
                }
            });
        }

        @JavascriptInterface
        public String getData() {
            return currentData;
        }
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                "application/vnd.ms-powerpoint",
                "application/octet-stream"
        });
        startActivityForResult(i, PICK_PPT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PPT && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            openUri(data.getData());
        }
    }

    private void loadPresentation(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String name = queryName(uri);
                    InputStream in = getContentResolver().openInputStream(uri);
                    if (in == null) {
                        throw new Exception("Could not read the file");
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                    in.close();

                    currentData = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
                    final String js = "openPptx(Android.getData(), '" + escape(name) + "')";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript(js, null);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Could not open file: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String queryName(Uri uri) {
        String name = "Presentation";
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String n = c.getString(idx);
                    if (n != null) {
                        name = n;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return name;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\n", " ").replace("\r", " ");
    }
}
