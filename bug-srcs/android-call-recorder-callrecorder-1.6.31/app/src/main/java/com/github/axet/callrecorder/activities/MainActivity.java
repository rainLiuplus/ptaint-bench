package com.github.axet.callrecorder.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.app.SuperUser;
import com.github.axet.androidlibrary.services.StorageProvider;
import com.github.axet.androidlibrary.widgets.AboutPreferenceCompat;
import com.github.axet.androidlibrary.widgets.AppCompatThemeActivity;
import com.github.axet.androidlibrary.widgets.OptimizationPreferenceCompat;
import com.github.axet.audiolibrary.encoders.Format3GP;
import com.github.axet.audiolibrary.encoders.FormatFLAC;
import com.github.axet.audiolibrary.encoders.FormatM4A;
import com.github.axet.audiolibrary.encoders.FormatMP3;
import com.github.axet.audiolibrary.encoders.FormatOGG;
import com.github.axet.audiolibrary.encoders.FormatOPUS;
import com.github.axet.audiolibrary.encoders.FormatWAV;
import com.github.axet.callrecorder.R;
import com.github.axet.callrecorder.app.MainApplication;
import com.github.axet.callrecorder.app.MixerPaths;
import com.github.axet.callrecorder.app.Recordings;
import com.github.axet.callrecorder.app.Storage;
import com.github.axet.callrecorder.app.SurveysReader;
import com.github.axet.callrecorder.services.RecordingService;
import com.github.axet.callrecorder.widgets.MixerPathsPreferenceCompat;

import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatThemeActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final static String TAG = MainActivity.class.getSimpleName();

    public static String SHOW_PROGRESS = MainActivity.class.getCanonicalName() + ".SHOW_PROGRESS";
    public static String SET_PROGRESS = MainActivity.class.getCanonicalName() + ".SET_PROGRESS";
    public static String SHOW_LAST = MainActivity.class.getCanonicalName() + ".SHOW_LAST";

    public static String SURVEY_URL = "https://docs.google.com/forms/d/e/1FAIpQLSdNWW4nmCXTrGFKbd_9_bPrxwlrfyPyzKtRESsGeaKist06VA/viewform?usp=pp_url&entry.1823308770=%MANUFACTURER%&entry.856269988=%MODEL%&entry.2054570575=%OSVERSION%&entry.1549394127=%ROOT%&entry.2121261645=%BASEBAND%&entry.648583455=%ENCODER%&entry.1739416324=%SOURCE%&entry.1221822567=%QUALITY%&entry.533092626=%INSTALLED%&entry.992467367=%VERSION%";
    public static String SURVEY_URL_VIEW = "https://axet.gitlab.io/android-call-recorder/?m=%MANUFACTURER%&d=%MODEL%";

    public static final int RESULT_CALL = 1;

    public static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
    };

    public static final String[] MUST = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
    };

    FloatingActionButton fab;
    FloatingActionButton fab_stop;
    View fab_panel;
    TextView status;
    boolean show;
    Boolean recording;
    int encoding;
    String phone;
    long sec;

    View progressText;
    View progressEmpty;

    MenuItem resumeCall;

    Recordings recordings;
    Storage storage;
    ListView list;
    Handler handler = new Handler();

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (a.equals(SHOW_PROGRESS)) {
                encoding = -1;
                show = intent.getBooleanExtra("show", false);
                recording = (Boolean) intent.getExtras().get("recording");
                sec = intent.getLongExtra("sec", 0);
                phone = intent.getStringExtra("phone");
                updatePanel();
            }
            if (a.equals(SET_PROGRESS)) {
                encoding = intent.getIntExtra("set", 0);
                updatePanel();
            }
            if (a.equals(SHOW_LAST)) {
                last();
            }
        }
    };

    public static void showProgress(Context context, boolean show, String phone, long sec, Boolean recording) {
        Intent intent = new Intent(SHOW_PROGRESS);
        intent.putExtra("show", show);
        intent.putExtra("recording", recording);
        intent.putExtra("sec", sec);
        intent.putExtra("phone", phone);
        context.sendBroadcast(intent);
    }

    public static void setProgress(Context context, int p) {
        Intent intent = new Intent(SET_PROGRESS);
        intent.putExtra("set", p);
        context.sendBroadcast(intent);
    }

    public static void last(Context context) {
        Intent intent = new Intent(SHOW_LAST);
        context.sendBroadcast(intent);
    }

    public static void startActivity(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);
    }

    public static void setSolid(Drawable background, int color) {
        if (background instanceof ShapeDrawable) {
            ShapeDrawable shapeDrawable = (ShapeDrawable) background;
            shapeDrawable.getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(color);
        } else if (background instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) background;
            if (Build.VERSION.SDK_INT >= 11)
                colorDrawable.setColor(color);
        }
    }

    public static String join(String... args) {
        StringBuilder bb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (bb.length() != 0)
                bb.append(args[0]);
            bb.append(args[i]);
        }
        return bb.toString();
    }

    @Override
    public int getAppTheme() {
        return MainApplication.getTheme(this, R.style.RecThemeLight_NoActionBar, R.style.RecThemeDark_NoActionBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (OptimizationPreferenceCompat.needKillWarning(this, MainApplication.PREFERENCE_NEXT))
            OptimizationPreferenceCompat.buildKilledWarning(this, true, MainApplication.PREFERENCE_OPTIMIZATION).show();

        progressText = findViewById(R.id.progress_text);
        progressEmpty = findViewById(R.id.progress_empty);

        storage = new Storage(this);

        IntentFilter ff = new IntentFilter();
        ff.addAction(SHOW_PROGRESS);
        ff.addAction(SET_PROGRESS);
        ff.addAction(SHOW_LAST);
        registerReceiver(receiver, ff);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab_panel = findViewById(R.id.fab_panel);
        status = (TextView) fab_panel.findViewById(R.id.status);

        fab_stop = (FloatingActionButton) findViewById(R.id.fab_stop);
        fab_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordingService.stopButton(MainActivity.this);
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecordingService.stopButton(MainActivity.this);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });

        updatePanel();

        list = (ListView) findViewById(R.id.list);
        recordings = new Recordings(this, list);
        list.setAdapter(recordings);
        list.setEmptyView(findViewById(R.id.empty_list));
        recordings.setToolbar((ViewGroup) findViewById(R.id.recording_toolbar));

        RecordingService.startIfEnabled(this);

        final Context context = this;

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        if (shared.getBoolean("warning", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(R.layout.warning);
            builder.setCancelable(false);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor edit = shared.edit();
                    edit.putBoolean("warning", false);
                    edit.commit();
                }
            });
            final AlertDialog d = builder.create();
            d.setOnShowListener(new DialogInterface.OnShowListener() {
                Button b;
                SwitchCompat sw1, sw2, sw3, sw4;

                @Override
                public void onShow(DialogInterface dialog) {
                    b = d.getButton(DialogInterface.BUTTON_POSITIVE);
                    b.setEnabled(false);
                    Window w = d.getWindow();
                    sw1 = (SwitchCompat) w.findViewById(R.id.recording);
                    sw1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked)
                                sw1.setClickable(false);
                            update();
                        }
                    });
                    sw2 = (SwitchCompat) w.findViewById(R.id.quality);
                    sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked)
                                sw2.setClickable(false);
                            update();
                        }
                    });
                    sw3 = (SwitchCompat) w.findViewById(R.id.taskmanagers);
                    sw3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                sw3.setClickable(false);
                            }
                            update();
                        }
                    });
                    sw4 = (SwitchCompat) w.findViewById(R.id.mixedpaths_switch);
                    final MixerPaths m = new MixerPaths();
                    if (!m.isCompatible() || m.isEnabled()) {
                        View v = w.findViewById(R.id.mixedpaths);
                        v.setVisibility(View.GONE);
                        sw4.setChecked(true);
                    } else {
                        sw4.setChecked(m.isEnabled());
                        sw4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked)
                                    sw4.setClickable(false);
                                m.load();
                                if (isChecked && !m.isEnabled())
                                    MixerPathsPreferenceCompat.show(MainActivity.this);
                                update();
                            }
                        });
                    }
                }

                void update() {
                    b.setEnabled(sw1.isChecked() && sw2.isChecked() && sw3.isChecked() && sw4.isChecked());
                }
            });
            d.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem i = menu.findItem(R.id.action_call);
        boolean b = RecordingService.isEnabled(this);
        i.setChecked(b);

        MenuItem m = menu.findItem(R.id.action_show_folder);
        Intent ii = StorageProvider.getProvider().openFolderIntent(storage.getStoragePath());
        m.setIntent(ii);
        if (!StorageProvider.isFolderCallable(this, ii, StorageProvider.getProvider().getAuthority()))
            m.setVisible(false);

        MenuItem search = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                recordings.search(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                recordings.searchClose();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar base clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        if (id == R.id.action_about) {
            final Runnable survey = new Runnable() {
                @Override
                public void run() {
                    String url = SURVEY_URL;
                    url = url.replaceAll("%MANUFACTURER%", Build.MANUFACTURER);
                    url = url.replaceAll("%MODEL%", android.os.Build.MODEL);
                    String ver = "Android: " + Build.VERSION.RELEASE;
                    String cm = MainApplication.getprop("ro.cm.version");
                    if (cm != null && !cm.isEmpty())
                        ver += "; " + cm;
                    ver += "; " + System.getProperty("os.version");
                    url = url.replaceAll("%OSVERSION%", ver);
                    try {
                        PackageManager pm = getPackageManager();
                        PackageInfo pInfo = pm.getPackageInfo(getPackageName(), 0);
                        String version = pInfo.versionName;
                        url = url.replaceAll("%VERSION%", version);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.d(TAG, "unable to get version", e);
                    }
                    url = url.replaceAll("%ROOT%", SuperUser.isRooted() ? "Yes" : "No");
                    url = url.replaceAll("%BASEBAND%", Build.VERSION.SDK_INT < 14 ? Build.RADIO : Build.getRadioVersion());
                    String encoder = shared.getString(MainApplication.PREFERENCE_ENCODING, "-1");
                    if (Storage.isMediaRecorder(encoder))
                        encoder = join(", ", Format3GP.EXT, Storage.EXT_AAC);
                    else
                        encoder = join(", ", FormatOGG.EXT, FormatWAV.EXT, FormatFLAC.EXT, FormatM4A.EXT, FormatMP3.EXT, FormatOPUS.EXT);
                    url = url.replaceAll("%ENCODER%", encoder);
                    String source = shared.getString(MainApplication.PREFERENCE_SOURCE, "-1");
                    String[] vv = MainApplication.getStrings(MainActivity.this, new Locale("en"), R.array.source_values);
                    String[] ss = MainApplication.getStrings(MainActivity.this, new Locale("en"), R.array.source_text);
                    int i = Arrays.asList(vv).indexOf(source);
                    url = url.replaceAll("%SOURCE%", ss[i]);
                    url = url.replaceAll("%QUALITY%", "");
                    boolean system = (getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM;
                    url = url.replaceAll("%INSTALLED%", system ? "System Preinstalled" : "User Installed");
                    AboutPreferenceCompat.openUrl(MainActivity.this, url);
                }
            };
            AlertDialog.Builder b = AboutPreferenceCompat.buildDialog(this, R.raw.about);
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.survey_title, null);
            ImageView icon = (ImageView) ll.findViewById(R.id.survey_image);
            TextView status = (TextView) ll.findViewById(R.id.survey_status);
            TextView text = (TextView) ll.findViewById(R.id.survey_text);
            final Drawable drawable = icon.getDrawable();

            int raw = getResources().getIdentifier("surveys", "raw", getPackageName()); // R.raw.surveys
            if (raw == 0) {
                setSolid(drawable, Color.GRAY);
                status.setText(R.string.survey_none);
            } else {
                SurveysReader reader = new SurveysReader(getResources().openRawResource(raw), new String[]{null, null, Build.MANUFACTURER, android.os.Build.MODEL});
                CSVRecord review = reader.getApproved();
                if (review != null) {
                    text.setText(getString(R.string.survey_know_issues) + "\n" + review.get(SurveysReader.INDEX_MSG));
                    switch (reader.getStatus(review)) {
                        case UNKNOWN:
                            setSolid(drawable, Color.GRAY);
                            break;
                        case RED:
                            setSolid(drawable, Color.RED);
                            status.setText(R.string.survey_bad);
                            break;
                        case GREEN:
                            setSolid(drawable, Color.GREEN);
                            status.setText(R.string.survey_good);
                            break;
                        case YELLOW:
                            setSolid(drawable, Color.YELLOW);
                            status.setText(R.string.survey_few_issues);
                            break;
                    }
                } else {
                    text.setVisibility(View.GONE);
                    switch (reader.getStatus()) {
                        case UNKNOWN:
                            setSolid(drawable, Color.GRAY);
                            status.setText(R.string.survey_none);
                            break;
                        case RED:
                            setSolid(drawable, Color.RED);
                            status.setText(R.string.survey_bad);
                            break;
                        case GREEN:
                            setSolid(drawable, Color.GREEN);
                            status.setText(R.string.survey_good);
                            break;
                        case YELLOW:
                            setSolid(drawable, Color.YELLOW);
                            status.setText(R.string.survey_few_issues);
                            break;
                    }
                }
            }

            View surveyButton = ll.findViewById(R.id.survey_button);
            surveyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = SURVEY_URL_VIEW;
                    url = url.replaceAll("%MANUFACTURER%", Build.MANUFACTURER);
                    url = url.replaceAll("%MODEL%", android.os.Build.MODEL);
                    AboutPreferenceCompat.openUrl(MainActivity.this, url);
                }
            });
            ll.addView(AboutPreferenceCompat.buildTitle(this), 0);
            b.setCustomTitle(ll);
            b.setNeutralButton(R.string.send_survey, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            final AlertDialog d = b.create();
            d.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button b = d.getButton(DialogInterface.BUTTON_NEUTRAL);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            survey.run();
                        }
                    });
                }
            });
            d.show();
            return true;
        }

        if (id == R.id.action_call) {
            item.setChecked(!item.isChecked());
            if (item.isChecked() && !Storage.permitted(MainActivity.this, PERMISSIONS, RESULT_CALL)) {
                resumeCall = item;
                return true;
            }
            call(item.isChecked());
            return true;
        }

        if (id == R.id.action_show_folder) {
            Intent intent = item.getIntent();
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void call(boolean b) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = shared.edit();
        edit.putBoolean(MainApplication.PREFERENCE_CALL, b);
        edit.commit();
        if (b) {
            RecordingService.startService(this);
            Toast.makeText(this, R.string.recording_enabled, Toast.LENGTH_SHORT).show();
        } else {
            RecordingService.stopService(this);
            Toast.makeText(this, R.string.recording_disabled, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        invalidateOptionsMenu();

        try {
            storage.migrateLocalStorage();
        } catch (RuntimeException e) {
            Error(e);
        }

        Runnable done = new Runnable() {
            @Override
            public void run() {
                progressText.setVisibility(View.VISIBLE);
                progressEmpty.setVisibility(View.GONE);
            }
        };
        progressText.setVisibility(View.GONE);
        progressEmpty.setVisibility(View.VISIBLE);

        recordings.load(false, done);

        updateHeader();

        fab.setClickable(true);
    }

    void last() {
        Runnable done = new Runnable() {
            @Override
            public void run() {
                final int selected = getLastRecording();
                progressText.setVisibility(View.VISIBLE);
                progressEmpty.setVisibility(View.GONE);
                if (selected != -1) {
                    recordings.select(selected);
                    list.smoothScrollToPosition(selected);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            list.setSelection(selected);
                        }
                    });
                }
            }
        };
        progressText.setVisibility(View.GONE);
        progressEmpty.setVisibility(View.VISIBLE);
        recordings.load(false, done);
    }

    int getLastRecording() {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        String last = shared.getString(MainApplication.PREFERENCE_LAST, "");
        last = last.toLowerCase();
        for (int i = 0; i < recordings.getCount(); i++) {
            Storage.RecordingUri f = recordings.getItem(i);
            String n = Storage.getDocumentName(f.uri).toLowerCase();
            if (n.equals(last)) {
                SharedPreferences.Editor edit = shared.edit();
                edit.putString(MainApplication.PREFERENCE_LAST, "");
                edit.commit();
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_CALL:
                if (Storage.permitted(this, MUST)) {
                    try {
                        storage.migrateLocalStorage();
                    } catch (RuntimeException e) {
                        Error(e);
                    }
                    recordings.load(false, null);
                    if (resumeCall != null) {
                        call(resumeCall.isChecked());
                        resumeCall = null;
                    }
                } else {
                    Toast.makeText(this, R.string.not_permitted, Toast.LENGTH_SHORT).show();
                    if (!Storage.permitted(this, MUST)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Permissions");
                        builder.setMessage("Call permissions must be enabled manually");
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Storage.showPermissions(MainActivity.this);
                            }
                        });
                        builder.show();
                        resumeCall = null;
                    }
                }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handler.post(new Runnable() {
            @Override
            public void run() {
                list.smoothScrollToPosition(recordings.getSelected());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordings.close();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    void updatePanel() {
        fab_panel.setVisibility(show ? View.VISIBLE : View.GONE);
        if (encoding >= 0) {
            status.setText(getString(R.string.encoding_title) + encoding + "%");
            fab.setVisibility(View.GONE);
            fab_stop.setVisibility(View.INVISIBLE);
        } else {
            String text = phone;
            if (!text.isEmpty())
                text += " - ";
            text += MainApplication.formatDuration(this, sec * 1000);
            text = text.trim();
            status.setText(text);
            fab.setVisibility(show ? View.VISIBLE : View.GONE);
            fab_stop.setVisibility(View.INVISIBLE);
        }
        if (recording == null) {
            fab.setVisibility(View.GONE);
        } else if (recording) {
            fab.setImageResource(R.drawable.ic_stop_black_24dp);
        } else {
            fab.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }
    }

    void updateHeader() {
        Uri f = storage.getStoragePath();
        long free = storage.getFree(f);
        long sec = Storage.average(this, free);
        TextView text = (TextView) findViewById(R.id.space_left);
        text.setText(MainApplication.formatFree(this, free, sec));
    }

    void Error(Throwable e) {
        Log.d(TAG, "Error", e);
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            Throwable t = e;
            while (t.getCause() != null)
                t = t.getCause();
            msg = t.getClass().getSimpleName();
        }
        Error(msg);
    }

    void Error(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(MainApplication.PREFERENCE_STORAGE)) {
            recordings.load(true, null);
        }
    }
}
