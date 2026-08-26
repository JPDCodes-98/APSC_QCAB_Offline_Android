package com.apsc.qcab;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(false);
        }

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallbackNew,
                    FileChooserParams fileChooserParams) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePathCallbackNew;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "application/json", "text/json", "text/plain"
                });

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this,
                            "No file picker available.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        // Everything required by the QCAB app is bundled in this local asset.
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void savePdfBase64(String base64Data, String requestedFileName) {
            new Thread(() -> {
                String fileName = sanitizeFileName(requestedFileName);
                if (!fileName.toLowerCase().endsWith(".pdf")) {
                    fileName += ".pdf";
                }

                try {
                    byte[] data;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        data = Base64.getDecoder().decode(base64Data);
                    } else {
                        data = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    }

                    String savedLocation;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentResolver resolver = getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                        values.put(MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS + "/APSC_QCAB");
                        values.put(MediaStore.Downloads.IS_PENDING, 1);

                        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                        Uri item = resolver.insert(collection, values);
                        if (item == null) {
                            throw new Exception("Could not create a Downloads file.");
                        }

                        try (OutputStream out = resolver.openOutputStream(item)) {
                            if (out == null) throw new Exception("Could not open output stream.");
                            out.write(data);
                            out.flush();
                        }

                        values.clear();
                        values.put(MediaStore.Downloads.IS_PENDING, 0);
                        resolver.update(item, values, null, null);
                        savedLocation = "Downloads/APSC_QCAB/" + fileName;
                    } else {
                        File downloads = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        File dir = new File(downloads, "APSC_QCAB");
                        if (!dir.exists() && !dir.mkdirs()) {
                            throw new Exception("Could not create output folder.");
                        }
                        File outFile = new File(dir, fileName);
                        try (FileOutputStream out = new FileOutputStream(outFile)) {
                            out.write(data);
                            out.flush();
                        }
                        savedLocation = outFile.getAbsolutePath();
                    }

                    String finalSavedLocation = savedLocation;
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            "QCAB saved to " + finalSavedLocation,
                            Toast.LENGTH_LONG
                    ).show());

                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            "PDF save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show());
                }
            }).start();
        }

        private String sanitizeFileName(String name) {
            if (name == null || name.trim().isEmpty()) {
                return "APSC_Mains_QCAB.pdf";
            }
            return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        }
    }
}
