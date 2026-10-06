package com.shortsblocker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Shorts Blocker");
        title.setTextSize(26);
        box.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, pad / 2, 0, pad / 2);
        box.addView(status);

        TextView steps = new TextView(this);
        steps.setText(
                "Setup:\n"
              + "1. Tap \"Open App Info\" > top-right menu (3 dots) > \"Allow restricted settings\" "
              + "(Android 13+ only; if you don't see it, skip).\n"
              + "2. Tap \"Open Accessibility Settings\" > Installed apps > Shorts Blocker > turn ON.\n"
              + "3. Open YouTube. The Shorts button can no longer be tapped.");
        steps.setPadding(0, 0, 0, pad);
        box.addView(steps);

        Button appInfo = new Button(this);
        appInfo.setText("Open App Info");
        appInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        });
        box.addView(appInfo);

        Button acc = new Button(this);
        acc.setText("Open Accessibility Settings");
        acc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        box.addView(acc);

        TextView lbl = new TextView(this);
        lbl.setText("\nButton label (change if YouTube is not in English):");
        box.addView(lbl);

        EditText label = new EditText(this);
        label.setSingleLine(true);
        label.setText(p.getString("label", "Shorts"));
        label.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                p.edit().putString("label", s.toString()).apply();
            }
        });
        box.addView(label);

        Switch debug = new Switch(this);
        debug.setText("Show wall in red (for testing)");
        debug.setChecked(p.getBoolean("debug", false));
        debug.setPadding(0, pad / 2, 0, pad / 2);
        debug.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                p.edit().putBoolean("debug", on).apply();
            }
        });
        box.addView(debug);

        Switch exit = new Switch(this);
        exit.setText("Experimental: auto-exit if a Short opens another way");
        exit.setChecked(p.getBoolean("exitShorts", false));
        exit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                p.edit().putBoolean("exitShorts", on).apply();
            }
        });
        box.addView(exit);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isServiceEnabled();
        status.setText(on ? "Status: ON - blocker is running"
                          : "Status: OFF - enable it in Accessibility Settings");
    }

    private boolean isServiceEnabled() {
        String s = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getPackageName() + "/");
    }
}
