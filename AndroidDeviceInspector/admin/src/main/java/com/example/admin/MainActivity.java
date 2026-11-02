
package com.example.admin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Enumeration;

public class MainActivity extends Activity {

    private TextView status;
    private TextView address;
    private ImageView qrImage;
    private ServerSocket serverSocket;
    private String token;

    private void addText(LinearLayout layout, String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(Color.BLACK);
        text.setPadding(8, 16, 8, 16);
        layout.addView(text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.WHITE);
        scroll.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("Device Inspector Admin");
        heading.setTextSize(25);
        heading.setTextColor(Color.BLACK);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading);

        status = new TextView(this);
        status.setText("Server not started");
        status.setTextSize(17);
        status.setTextColor(Color.DKGRAY);
        layout.addView(status);

        address = new TextView(this);
        address.setText("Start the server to generate pairing details.");
        address.setTextSize(15);
        address.setTextColor(Color.BLACK);
        layout.addView(address);

        qrImage = new ImageView(this);
        LinearLayout.LayoutParams qrParams =
                new LinearLayout.LayoutParams(600, 600);
        qrParams.gravity = Gravity.CENTER;
        layout.addView(qrImage, qrParams);

        Button start = new Button(this);
        start.setText("Start Wi-Fi Connection");
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("Stop Connection");
        layout.addView(stop);

        addText(layout,
                "Keep the Realme hotspot ON and connect the TECNO to it.",
                15);

        start.setOnClickListener(v -> startServer());
        stop.setOnClickListener(v -> stopServer());

        setContentView(scroll);
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses =
                        network.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();

                    if (!ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(':') < 0) {
                        String value = ip.getHostAddress();

                        if (value.startsWith("192.168.")
                                || value.startsWith("10.")) {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String createToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);

        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private void showQr(String value) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    value, BarcodeFormat.QR_CODE, 600, 600);

            Bitmap bitmap = Bitmap.createBitmap(
                    600, 600, Bitmap.Config.RGB_565);

            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {
                    bitmap.setPixel(x, y,
                            matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            qrImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            status.setText("Could not create QR code: " + e.getMessage());
        }
    }

    private void startServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            status.setText("Server is already running.");
            return;
        }

        String ip = getLocalIp();

        if (ip == null) {
            status.setText("Could not find hotspot IP. Check hotspot.");
            return;
        }

        token = createToken();
        String pairingData = "http://" + ip + ":8988/pair?token=" + token;

        showQr(pairingData);
        address.setText("Admin address: " + ip + ":8988\n"
                + "Pairing QR is ready. Keep this screen open.");

        status.setText("Starting local Wi-Fi server...");

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8988, 10,
                        InetAddress.getByName("0.0.0.0"));

                runOnUiThread(() ->
                        status.setText("Waiting for Collector connection"));

                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();

                    new Thread(() -> handleClient(client)).start();
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        status.setText("Server error: " + e.getMessage()));
            }
        }).start();
    }


private void handleClient(Socket client) {
    try {
        java.io.BufferedReader input =
                new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                                client.getInputStream()));

        String request = input.readLine();
        String line;

        // Read each HTTP header line exactly once.
        while ((line = input.readLine()) != null && !line.isEmpty()) {
            // Headers are not needed for pairing.
        }

        boolean valid = request != null
                && token != null
                && request.contains("/pair?token=" + token);

        String body = valid ? "PAIR_OK" : "PAIR_DENIED";

        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: " + body.length() + "\r\n"
                + "Connection: close\r\n\r\n"
                + body;

        client.getOutputStream().write(response.getBytes("UTF-8"));
        client.getOutputStream().flush();

        if (valid) {
            runOnUiThread(() ->
                    status.setText("Collector reached Admin server"));
        }

    } catch (Exception e) {
        runOnUiThread(() ->
                status.setText("Connection error: " + e.getMessage()));
    } finally {
        try {
            client.close();
        } catch (Exception ignored) {
        }
    }
}

    private void stopServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            qrImage.setImageDrawable(null);
            status.setText("Connection stopped");
            address.setText("Server is offline.");
        } catch (Exception e) {
            status.setText("Could not stop server");
        }
    }

    @Override
    protected void onDestroy() {
        stopServer();
        super.onDestroy();
    }
}
