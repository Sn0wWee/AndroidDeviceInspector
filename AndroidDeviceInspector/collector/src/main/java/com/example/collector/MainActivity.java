
package com.example.collector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private TextView status;
    private Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 40, 32, 32);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.WHITE);

        TextView heading = new TextView(this);
        heading.setText("Device Inspector Collector");
        heading.setTextSize(24);
        heading.setTextColor(Color.BLACK);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading);

        TextView instructions = new TextView(this);
        instructions.setText(
                "\n1. Connect this phone to the Admin phone's hotspot."
                + "\n\n2. Open the Admin app on the Realme."
                + "\n\n3. Press Start Wi-Fi Connection."
                + "\n\n4. Scan the Admin QR code below."
        );
        instructions.setTextSize(16);
        instructions.setTextColor(Color.DKGRAY);
        layout.addView(instructions);

        scanButton = new Button(this);
        scanButton.setText("Scan Admin QR Code");
        layout.addView(scanButton);

        status = new TextView(this);
        status.setText("Status: Not connected");
        status.setTextSize(17);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, 28, 0, 28);
        layout.addView(status);

        Button resetButton = new Button(this);
        resetButton.setText("Reset Status");
        layout.addView(resetButton);

        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator =
                    new IntentIntegrator(this);

            integrator.setDesiredBarcodeFormats(
                    IntentIntegrator.QR_CODE
            );
            integrator.setPrompt("Scan the Admin QR code");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });

        resetButton.setOnClickListener(v -> {
            status.setText("Status: Not connected");
            scanButton.setEnabled(true);
        });

        setContentView(layout);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        IntentResult result =
                IntentIntegrator.parseActivityResult(
                        requestCode, resultCode, data
                );

        if (result != null) {
            String scanned = result.getContents();

            if (scanned == null) {
                status.setText("Scan cancelled");
            } else {
                connectToAdmin(scanned);
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToAdmin(String scanned) {

        final URL pairingUrl;

        try {
            pairingUrl = new URL(scanned);

            if (!"http".equalsIgnoreCase(pairingUrl.getProtocol())
                    || pairingUrl.getHost() == null
                    || pairingUrl.getPort() != 8988
                    || !"/pair".equals(pairingUrl.getPath())
                    || pairingUrl.getQuery() == null
                    || !pairingUrl.getQuery().startsWith("token=")) {

                status.setText("Invalid Admin QR code");
                return;
            }

        } catch (Exception e) {
            status.setText("Invalid QR code");
            return;
        }

        status.setText("Connecting to Admin...");
        scanButton.setEnabled(false);

        new Thread(() -> {

            HttpURLConnection connection = null;
            String message;

            try {
                connection = (HttpURLConnection)
                        pairingUrl.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(false);

                int responseCode = connection.getResponseCode();

                InputStream stream =
                        responseCode >= 400
                                ? connection.getErrorStream()
                                : connection.getInputStream();

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream();

                if (stream != null) {
                    byte[] buffer = new byte[1024];
                    int count;

                    while ((count = stream.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }

                    stream.close();
                }

                String response = output.toString("UTF-8").trim();

                if (responseCode == 200
                        && "PAIR_OK".equals(response)) {

                    message = "SUCCESS: Connected to Admin!";

                } else {
                    message = "Admin rejected pairing. HTTP "
                            + responseCode
                            + " - "
                            + response;
                }

            } catch (Exception e) {

                message = "CONNECTION FAILED: "
                        + e.getClass().getSimpleName();

                if (e.getMessage() != null) {
                    message += " - " + e.getMessage();
                }

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            final String finalMessage = message;

            runOnUiThread(() -> {
                status.setText(finalMessage);
                scanButton.setEnabled(true);
            });

        }).start();
    }
}
