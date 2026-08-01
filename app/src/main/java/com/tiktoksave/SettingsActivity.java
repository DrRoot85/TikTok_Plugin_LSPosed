package com.tiktoksave;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * Settings UI of the module app itself.
 * Talks to LSPosed through libxposed:service and stores values in the same
 * remote preference group the hooked TikTok process reads.
 */
public class SettingsActivity extends Activity implements XposedServiceHelper.OnServiceListener {

    private TextView status;
    private SharedPreferences prefs;

    private EditText folder;
    private EditText prefix;
    private CheckBox noWm;
    private CheckBox images;
    private CheckBox notify;
    private CheckBox fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings_title);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        status = new TextView(this);
        status.setText(R.string.service_binding);
        status.setTextColor(Color.GRAY);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        // -- Download folder
        root.addView(label(R.string.pref_folder));
        folder = new EditText(this);
        folder.setSingleLine(true);
        folder.setHint(R.string.pref_folder_hint);
        root.addView(folder);

        // -- Filename prefix
        root.addView(label(R.string.pref_filename));
        prefix = new EditText(this);
        prefix.setSingleLine(true);
        prefix.setHint(R.string.pref_filename_hint);
        root.addView(prefix);

        // -- No watermark
        noWm = new CheckBox(this);
        noWm.setText(R.string.pref_no_watermark);
        noWm.setPadding(0, dp(12), 0, 0);
        root.addView(noWm);
        root.addView(desc(R.string.pref_no_watermark_desc));

        // -- Save images
        images = new CheckBox(this);
        images.setText(R.string.pref_save_images);
        images.setPadding(0, dp(12), 0, 0);
        root.addView(images);
        root.addView(desc(R.string.pref_save_images_desc));

        // -- Notifications
        notify = new CheckBox(this);
        notify.setText(R.string.pref_notify);
        notify.setPadding(0, dp(12), 0, 0);
        root.addView(notify);

        // -- FAB
        fab = new CheckBox(this);
        fab.setText(R.string.pref_fab);
        fab.setPadding(0, dp(8), 0, 0);
        root.addView(fab);
        root.addView(desc(R.string.pref_fab_desc));

        // -- Save button
        Button save = new Button(this);
        save.setText("Save");
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(20);
        save.setLayoutParams(blp);
        root.addView(save);

        save.setOnClickListener(v -> {
            if (prefs == null) {
                Toast.makeText(this, R.string.service_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences.Editor e = prefs.edit();
            e.putString("save_folder", folder.getText().toString().trim());
            e.putString("filename_prefix", prefix.getText().toString().trim());
            e.putBoolean("no_watermark", noWm.isChecked());
            e.putBoolean("save_images", images.isChecked());
            e.putBoolean("notify", notify.isChecked());
            e.putBoolean("fab", fab.isChecked());
            e.apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });

        setContentView(root);

        // Live-apply toggle changes too (e.g. hiding the FAB immediately).
        CompoundButton.OnCheckedChangeListener live = (b, checked) -> {
            if (prefs == null) return;
            SharedPreferences.Editor e = prefs.edit();
            if (b == noWm) e.putBoolean("no_watermark", checked);
            else if (b == images) e.putBoolean("save_images", checked);
            else if (b == notify) e.putBoolean("notify", checked);
            else if (b == fab) e.putBoolean("fab", checked);
            e.apply();
        };
        noWm.setOnCheckedChangeListener(live);
        images.setOnCheckedChangeListener(live);
        notify.setOnCheckedChangeListener(live);
        fab.setOnCheckedChangeListener(live);

        XposedServiceHelper.registerListener(this);
    }

    private TextView label(int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextSize(14);
        t.setTextColor(Color.BLACK);
        t.setPadding(0, dp(10), 0, dp(2));
        return t;
    }

    private TextView desc(int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextColor(Color.GRAY);
        t.setTextSize(12);
        t.setPadding(dp(28), 0, 0, 0);
        return t;
    }

    private void loadValues() {
        if (prefs == null) return;
        folder.setText(prefs.getString("save_folder", "TikTokSave"));
        prefix.setText(prefs.getString("filename_prefix", "TikTok"));
        noWm.setChecked(prefs.getBoolean("no_watermark", true));
        images.setChecked(prefs.getBoolean("save_images", true));
        notify.setChecked(prefs.getBoolean("notify", true));
        fab.setChecked(prefs.getBoolean("fab", true));
    }

    @Override
    public void onServiceBind(@NonNull XposedService service) {
        try {
            SharedPreferences p = service.getRemotePreferences(Settings.PREFS_GROUP);
            if (p != null) {
                prefs = p;
                runOnUiThread(() -> {
                    status.setText(R.string.service_bound);
                    status.setTextColor(Color.GREEN);
                    loadValues();
                });
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        prefs = null;
        runOnUiThread(() -> {
            status.setText(R.string.service_unavailable);
            status.setTextColor(Color.RED);
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
