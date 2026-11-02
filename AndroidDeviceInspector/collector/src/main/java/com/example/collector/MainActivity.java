package com.example.collector;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int GALLERY_PERMISSION_REQUEST = 100;

    private TextView permissionStatus;
    private TextView photoCount;
    private TextView syncStatus;
    private CheckBox consentCheckBox;

    private boolean paused = false;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();
        updatePermissionStatus();
    }

    private void createInterface() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(0xFFF5F5F5);

        TextView heading = new TextView(this);
        heading.setText("Device Inspector Collector");
        heading.setTextSize(25);
        heading.setTextColor(0xFF1565C0);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, 20, 0, 30);
        layout.addView(heading);

        TextView explanation = new TextView(this);
        explanation.setText(
                "This app allows you to share selected device information " +
                "and gallery photos with your paired Admin device."
        );
        explanation.setTextSize(16);
        explanation.setTextColor(0xFF222222);
        layout.addView(explanation);

        consentCheckBox = new CheckBox(this);
        consentCheckBox.setText(
                "I understand and agree to allow gallery access."
        );
        consentCheckBox.setTextColor(0xFF222222);
        consentCheckBox.setPadding(0, 25, 0, 20);
        layout.addView(consentCheckBox);

        Button permissionButton = new Button(this);
        permissionButton.setText("Allow Gallery Access");
        layout.addView(permissionButton);

        permissionStatus = createStatusText();
        layout.addView(permissionStatus);

        photoCount = createStatusText();
        layout.addView(photoCount);

        TextView pairingHeading = new TextView(this);
        pairingHeading.setText("Device Pairing");
        pairingHeading.setTextSize(20);
        pairingHeading.setTextColor(0xFF1565C0);
        pairingHeading.setPadding(0, 30, 0, 10);
        layout.addView(pairingHeading);

        TextView pairingStatus = createStatusText();
        pairingStatus.setText("Not paired with an Admin device");
        layout.addView(pairingStatus);

        TextView syncHeading = new TextView(this);
        syncHeading.setText("Photo Sharing");
        syncHeading.setTextSize(20);
        syncHeading.setTextColor(0xFF1565C0);
        syncHeading.setPadding(0, 25, 0, 10);
        layout.addView(syncHeading);

        syncStatus = createStatusText();
        syncStatus.setText("Cloud sync is not configured");
        layout.addView(syncStatus);

        Button pauseButton = new Button(this);
        pauseButton.setText("Pause Sharing");
        layout.addView(pauseButton);

        Button disconnectButton = new Button(this);
        disconnectButton.setText("Disconnect Device");
        layout.addView(disconnectButton);

        TextView notice = new TextView(this);
        notice.setText(
                "\nPrivacy notice:\n" +
                "Gallery access requires your permission. " +
                "This version does not upload or transfer photos. " +
                "You can close this app or pause sharing at any time."
        );
        notice.setTextSize(14);
        notice.setTextColor(0xFF555555);
        notice.setPadding(0, 20, 0, 10);
        layout.addView(notice);

        permissionButton.setOnClickListener(v -> {
            if (!consentCheckBox.isChecked()) {
                permissionStatus.setText(
                        "Please agree before requesting gallery access."
                );
                return;
            }

            requestGalleryPermission();
        });

        pauseButton.setOnClickListener(v -> {
            paused = !paused;

            if (paused) {
                pauseButton.setText("Resume Sharing");
                syncStatus.setText("Sharing paused by device owner");
            } else {
                pauseButton.setText("Pause Sharing");
                syncStatus.setText(
                        "Sharing resumed. Cloud sync is not configured."
                );
            }
        });

        disconnectButton.setOnClickListener(v -> {
            connected = false;
            paused = true;
            syncStatus.setText("Device disconnected");
            pairingStatus.setText("Not paired with an Admin device");
        });

        setContentView(layout);
    }

    private TextView createStatusText() {
        TextView text = new TextView(this);
        text.setTextSize(15);
        text.setTextColor(0xFF333333);
        text.setPadding(0, 10, 0, 10);
        return text;
    }

    private void requestGalleryPermission() {
        String permission;

        if (Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED) {
            updatePermissionStatus();
        } else {
            requestPermissions(
                    new String[]{permission},
                    GALLERY_PERMISSION_REQUEST
            );
        }
    }

    private void updatePermissionStatus() {
        String permission;

        if (Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED) {
            permissionStatus.setText("Gallery permission: Granted");
            updatePhotoCount();
        } else {
            permissionStatus.setText("Gallery permission: Not granted");
            photoCount.setText("Photos available: Not accessible");
        }
    }

    private void updatePhotoCount() {
        int count = 0;

        try {
            String[] projection = {
                    MediaStore.Images.Media._ID
            };

            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
            )) {
                if (cursor != null) {
                    count = cursor.getCount();
                }
            }

            photoCount.setText("Photos available: " + count);

        } catch (Exception e) {
            photoCount.setText("Unable to read gallery");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == GALLERY_PERMISSION_REQUEST) {
            updatePermissionStatus();

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionStatus.setText("Gallery permission: Granted");
            } else {
                permissionStatus.setText(
                        "Gallery permission: Denied. You can enable it in Settings."
                );
            }
        }
    }
}
