package com.example.admin;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private LinearLayout root;

    private TextView createText(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setPadding(0, 10, 0, 10);
        return text;
    }

    private void addSection(String heading, String details) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 18, 24, 18);
        card.setBackgroundColor(Color.WHITE);

        TextView title = createText(heading, 19, Color.BLACK);
        TextView body = createText(details, 15, Color.DKGRAY);

        card.addView(title);
        card.addView(body);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10);

        root.addView(card, params);
    }

    private void addButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setEnabled(false);
        root.addView(button);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(242, 244, 248));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 24, 20, 24);

        scroll.addView(root);
        setContentView(scroll);

        TextView heading = createText(
                "Device Inspector Admin", 26, Color.BLACK);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        TextView subtitle = createText(
                "Manage your paired device", 15, Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        addSection(
                "Connection status",
                "No device paired yet.");

        addSection(
                "Cloud service",
                "Not configured. Device communication and photo transfer are not active.");

        addSection(
                "Device information",
                "Device name: Not connected\n" +
                "Battery: Not available\n" +
                "Android version: Not available\n" +
                "Last sync: Never");

        addSection(
                "Photo transfer",
                "No photos received.\n" +
                "Photo syncing will be available after secure pairing and cloud setup.");

        addSection(
                "Pair a device",
                "QR pairing will be added in a later development step.");

        addButton("Generate pairing QR — coming soon");
        addButton("Pause syncing — coming soon");
        addButton("Resume syncing — coming soon");
        addButton("Disconnect device — coming soon");
    }
}
