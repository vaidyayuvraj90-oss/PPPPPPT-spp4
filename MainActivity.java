package com.example.pptopener;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int PICK_PPT = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);
        root.setBackgroundColor(Color.parseColor("#F7F7FB"));

        TextView title = new TextView(this);
        title.setText("PPT Opener");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1F2340"));
        title.setGravity(Gravity.CENTER);

        TextView hint = new TextView(this);
        hint.setText("Pick a presentation (.ppt, .pptx, .ppsx) and open it.");
        hint.setTextSize(16);
        hint.setTextColor(Color.parseColor("#5A5F7A"));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 24, 0, 48);

        Button pick = new Button(this);
        pick.setText("Choose presentation");
        pick.setAllCaps(false);
        pick.setOnClickListener(v -> pickFile());

        root.addView(title);
        root.addView(hint);
        root.addView(pick);
        setContentView(root);

        // Opened from a file manager / "Open with"
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
            openPresentation(intent.getData(), intent.getType());
        }
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
        });
        startActivityForResult(i, PICK_PPT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PPT && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            Uri uri = data.getData();
            openPresentation(uri, getContentResolver().getType(uri));
        }
    }

    private void openPresentation(Uri uri, String mime) {
        if (mime == null || mime.equals("application/octet-stream")) {
            mime = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, mime);
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(view, "Open presentation with"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "No app found to show presentations. Install PowerPoint, WPS Office or Google Slides.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
