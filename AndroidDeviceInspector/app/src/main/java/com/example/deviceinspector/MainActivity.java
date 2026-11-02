package com.example.deviceinspector;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.*;
import android.net.wifi.WifiManager;
import android.os.*;
import android.text.format.Formatter;
import android.view.Gravity;
import android.widget.*;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView reportView;
    private String report = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 36, 32, 24);
        layout.setBackgroundColor(0xFFF7F8FA);

        TextView title = new TextView(this);
        title.setText("Android Device Inspector");
        title.setTextSize(24);
        title.setTextColor(0xFF172033);
        title.setGravity(Gravity.CENTER_VERTICAL);
        layout.addView(title);

        TextView note = new TextView(this);
        note.setText("A transparent, on-device diagnostic. Nothing is uploaded or sent automatically. Review the report before sharing it.");
        note.setTextSize(14);
        note.setTextColor(0xFF465268);
        note.setPadding(0, 12, 0, 18);
        layout.addView(note);

        Button refresh = new Button(this);
        refresh.setText("Collect device information");
        layout.addView(refresh);

        reportView = new TextView(this);
        reportView.setTextSize(14);
        reportView.setTextColor(0xFF172033);
        reportView.setPadding(0, 18, 0, 18);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(reportView);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button share = new Button(this);
        share.setText("Review and share report");
        layout.addView(share);

        refresh.setOnClickListener(v -> collect());
        share.setOnClickListener(v -> {
            if (report.isEmpty()) collect();
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "Android device diagnostic report");
            send.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(send, "Choose how to share your report"));
        });
        setContentView(layout);
        collect();
    }

    private void collect() {
        StringBuilder s = new StringBuilder();
        s.append("ANDROID DEVICE DIAGNOSTIC REPORT\n");
        s.append("Generated: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n\n");
        s.append("DEVICE\n");
        s.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        s.append("Brand: ").append(Build.BRAND).append("\n");
        s.append("Model: ").append(Build.MODEL).append("\n");
        s.append("Device code: ").append(Build.DEVICE).append("\n");
        s.append("Android version: ").append(Build.VERSION.RELEASE).append("\n");
        s.append("SDK level: ").append(Build.VERSION.SDK_INT).append("\n");
        s.append("Security patch: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        s.append("Build fingerprint: ").append(Build.FINGERPRINT).append("\n\n");

        s.append("SYSTEM\n");
        s.append("Uptime: ").append(formatDuration(SystemClock.elapsedRealtime())).append("\n");
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        s.append("Total RAM: ").append(formatBytes(mi.totalMem)).append("\n");
        s.append("Available RAM: ").append(formatBytes(mi.availMem)).append("\n");
        File data = getFilesDir();
        s.append("Internal storage total: ").append(formatBytes(data.getTotalSpace())).append("\n");
        s.append("Internal storage free: ").append(formatBytes(data.getFreeSpace())).append("\n\n");

        s.append("BATTERY\n");
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) s.append("Charge: ").append(Math.round(level * 100f / scale)).append("%\n");
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            s.append("Charging: ").append(status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ? "Yes" : "No / not reported").append("\n");
        } else s.append("Battery details unavailable\n");
        s.append("\nNETWORK (Wi-Fi details may be restricted by Android)\n");
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null && wm.getDhcpInfo() != null) {
                android.net.DhcpInfo d = wm.getDhcpInfo();
                s.append("Wi-Fi IP: ").append(ip(d.ipAddress)).append("\n");
                s.append("Gateway: ").append(ip(d.gateway)).append("\n");
                s.append("DNS 1: ").append(ip(d.dns1)).append("\n");
                s.append("DNS 2: ").append(ip(d.dns2)).append("\n");
                s.append("DHCP server: ").append(ip(d.serverAddress)).append("\n");
            } else s.append("Wi-Fi network details unavailable\n");
        } catch (Exception e) {
            s.append("Wi-Fi details unavailable: ").append(e.getClass().getSimpleName()).append("\n");
        }
        s.append("\nPRIVACY\n");
        s.append("This app has no Internet permission and does not transmit data.\n");
        s.append("No files, messages, call logs, contacts, microphone, camera, or location are accessed.\n");
        s.append("The user chooses whether and how to share this report.\n");

        report = s.toString();
        reportView.setText(report);
    }

    private String ip(int value) {
        return String.format(Locale.US, "%d.%d.%d.%d",
                value & 0xff, (value >> 8) & 0xff, (value >> 16) & 0xff, (value >> 24) & 0xff);
    }
    private String formatBytes(long b) {
        if (b < 1024) return b + " B";
        double k = b / 1024.0;
        if (k < 1024) return String.format(Locale.US, "%.1f KB", k);
        double m = k / 1024.0;
        if (m < 1024) return String.format(Locale.US, "%.1f MB", m);
        return String.format(Locale.US, "%.2f GB", m / 1024.0);
    }
    private String formatDuration(long ms) {
        long sec = ms / 1000, days = sec / 86400, hours = (sec % 86400) / 3600;
        long mins = (sec % 3600) / 60;
        return days + "d " + hours + "h " + mins + "m";
    }
}
