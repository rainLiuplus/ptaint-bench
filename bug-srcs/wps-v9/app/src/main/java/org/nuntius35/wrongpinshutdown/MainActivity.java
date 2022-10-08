package org.nuntius35.wrongpinshutdown;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    static final int ACTIVATION_REQUEST = 1;
    final Context context = this;

    DevicePolicyManager devicePolicyManager;
    ComponentName demoDeviceAdmin;

    ShutdownManager sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        sm = new ShutdownManager(context);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        demoDeviceAdmin = new ComponentName(this, LogInReceiver.class);

        boolean isActive = devicePolicyManager.isAdminActive(demoDeviceAdmin);
        if (!isActive) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, demoDeviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.adminExplanation));
            startActivityForResult(intent, ACTIVATION_REQUEST);
        }

        sm.testRoot();

        updateStats(sm);
    }

    @Override
    public void onResume() {
        super.onResume();
        sm = new ShutdownManager(context);
        updateStats(sm);
    }

    public void updateStats(ShutdownManager sm) {
        TextView textWrong = findViewById(R.id.lastWrong);
        textWrong.setText(getString(R.string.lastWrong));
        textWrong.append(sm.getLastWrong());
        TextView textShutdown = findViewById(R.id.lastShutdown);
        textShutdown.setText(getString(R.string.lastShutdown));
        textShutdown.append(sm.getLastShutdown());
    }

    public static class MyPreferenceFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            onSharedPreferenceChanged(sharedPrefs, "shutdown_cmd");
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                EditTextPreference listPref = (EditTextPreference) pref;
                pref.setSummary(listPref.getText());
            }
        }
    }
}
