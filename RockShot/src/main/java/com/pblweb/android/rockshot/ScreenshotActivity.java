package com.pblweb.android.rockshot;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class ScreenshotActivity extends Activity {

    private static final int REQUEST_UUID = 101;
    private String TAG = ScreenshotActivity.class.getSimpleName();
    public static final String PREFS_NAME = "RockShot";

    // UUID.fromString("9141b628-bc89-498e-b147-c884f0160215"); LONDON TUBE
    // UUID.fromString("9141b628-bc89-498e-b147-826565cd3d24"); BATTLESHIP
    private UUID app_uuid = null;
    private final int ROCKSHOT_KEY_OFFSET = 76250;
    private final int ROCKSHOT_KEY_DATA =  76251;
    private final int ROCKSHOT_KEY_CHECK =  76252;

    private boolean screenshot_in_progress = false;
    private PebbleKit.PebbleDataReceiver dataReceiver = null;

    protected ImageView iv_screenshot;
    protected TextView tv_status;
    protected ProgressBar pb_capture;
    protected MenuItem mi_share;
    private Spinner spn_wrapper;
    protected Button btn_main;
    private CheckBox cb_watch;

    private ArrayList<String> recent_screenshots = new ArrayList<String>();

    protected MenuItem mi_settings;
    protected ScreenshotGenerator.WrapperColor currentColour = ScreenshotGenerator.WrapperColor.Black;
    public boolean show_wrapper = true;
    protected ScreenshotGenerator lastScreenshot = null;
    private BroadcastReceiver connectedReceiver = null;
    private BroadcastReceiver disconnectedReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot);

/*        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor e = settings.edit();
        e.clear();
        e.commit();*/

        iv_screenshot = (ImageView)findViewById(R.id.imageView);
        tv_status = (TextView)findViewById(R.id.tvErrorMessage);
        pb_capture = (ProgressBar)findViewById(R.id.progressBar);
        btn_main = (Button)findViewById(R.id.button);

        loadSettings();

        btn_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestScreenshot();
            }
        });

        spn_wrapper = (Spinner)findViewById(R.id.spinner);
        spn_wrapper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ScreenshotGenerator.WrapperColor[] colors = ScreenshotGenerator.WrapperColor.values();
                currentColour = colors[i];
                saveSettings();
                updateScreenshotView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentColour = ScreenshotGenerator.WrapperColor.Black;
                updateScreenshotView();
            }
        });

        cb_watch = (CheckBox)findViewById(R.id.checkBox);
        cb_watch.setChecked(show_wrapper);
        cb_watch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                show_wrapper = b;
                saveSettings();
                spn_wrapper.setEnabled(b);
                updateScreenshotView();
            }
        });

        registerConnectedReceiver();
        registerDisconnectedReceiver();
    }

    private void registerDisconnectedReceiver() {
        if (disconnectedReceiver != null) {
            return;
        }
        disconnectedReceiver = PebbleKit.registerPebbleDisconnectedReceiver(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi();
            }
        });
    }

    private void registerConnectedReceiver() {
        if (connectedReceiver != null) {
            return;
        }
        connectedReceiver = PebbleKit.registerPebbleConnectedReceiver(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (dataReceiver != null) {
            unregisterReceiver(dataReceiver);
            dataReceiver = null;
        }

        if (connectedReceiver != null) {
            unregisterReceiver(connectedReceiver);
            connectedReceiver = null;
        }

        if (disconnectedReceiver != null) {
            unregisterReceiver(disconnectedReceiver);
            disconnectedReceiver = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadSettings();
        updateUi();
        updateScreenshotView();

        registerConnectedReceiver();
        registerDisconnectedReceiver();

        if (app_uuid != null && dataReceiver == null) {
            setReceiver(app_uuid);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.screenshot, menu);
        mi_share = menu.findItem(R.id.action_share);
        mi_settings = menu.findItem(R.id.action_settings);

        updateUi();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_share:
                shareScreenshot(lastScreenshot);
                return true;
            case R.id.action_settings:
                askForUuid();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareScreenshot(ScreenshotGenerator shot) {
        if (shot == null) {
            return;
        }
        File screenshot_file = saveScreenshot(shot);
        if (screenshot_file == null) {
            Toast.makeText(getApplicationContext(), "Screenshot failed to save.", Toast.LENGTH_LONG);
        }
        else {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenshot_file));
            startActivity(Intent.createChooser(share, "Share Screenshot"));
        }
    }

    protected void askForUuid() {
        Intent intent_uuid = new Intent(this, SetUuidActivity.class);
        if (app_uuid != null) {
            intent_uuid.putExtra("uuid", app_uuid.toString());
        }
        startActivityForResult(intent_uuid, REQUEST_UUID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_UUID:
                if (resultCode == RESULT_OK) {
                    String uuid_str = data.getExtras().getString("uuid");
                    UUID new_uuid = null;
                    try {
                        new_uuid = UUID.fromString(uuid_str);
                    }
                    catch (IllegalArgumentException ex) {
                        Toast.makeText(getApplicationContext(), "Invalid UUID.", Toast.LENGTH_LONG).show();
                    }
                    if (new_uuid != null) {
                        setReceiver(new_uuid);
                    }
                }
                break;
        }
    }

    private File saveScreenshot(ScreenshotGenerator shot) {
        try {
            File f = new File(Environment.getExternalStorageDirectory() + File.separator + "rockshot-latest.png");
            FileOutputStream out = new FileOutputStream(f);
            Bitmap bmp;
            if (show_wrapper) {
                bmp = shot.getWrappedScreenshot(currentColour);
            }
            else {
                bmp = shot.getScreenshot();
            }
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            return f;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean setReceiver(UUID uuid) {
        final Handler handler = new Handler();
        final Resources resources = getResources();

        if (dataReceiver != null) {
            unregisterReceiver(dataReceiver);
            dataReceiver = null;
        }

        app_uuid = uuid;

        dataReceiver = new PebbleKit.PebbleDataReceiver(app_uuid) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        int fixedTransactionId = transactionId;
                        if (fixedTransactionId < 0) {
                            fixedTransactionId += 256;
                        }

                        PebbleKit.sendAckToPebble(context, fixedTransactionId);

                        if (data == null) {
                            return;
                        }

                        if (! data.contains(ROCKSHOT_KEY_OFFSET)) {
                            return;
                        }

                        long offset = data.getInteger(ROCKSHOT_KEY_OFFSET);
                        if (offset == 0) {
                            screenshot_in_progress = true;
                            updateUi();
                            lastScreenshot = new ScreenshotGenerator(resources);
                            pb_capture.setProgress(0);
                        }

                        byte[] bmp_data = data.getBytes(ROCKSHOT_KEY_DATA);

                        if (bmp_data == null) {
                            return;
                        }

                        lastScreenshot.addPixels(bmp_data);

                        updateScreenshotView();
                        pb_capture.setProgress(lastScreenshot.getProgress());

                        if (bmp_data.length != 128) {
                            screenshot_in_progress = false;
                            updateUi();
                        }
                    }
                });
            }
        };
        PebbleKit.registerReceivedDataHandler(this, dataReceiver);

        updateUi();

        return true;
    }

    private void requestScreenshot() {
        if (screenshot_in_progress) {
            return;
        }
        if (app_uuid == null) {
            return;
        }
        screenshot_in_progress = true;
        updateUi();
        PebbleDictionary dict = new PebbleDictionary();
        dict.addString(ROCKSHOT_KEY_CHECK, "rockshot");
        PebbleKit.sendDataToPebble(getApplicationContext(), app_uuid, dict);
    }

    protected void updateUi() {

        if (mi_settings == null) {
            return;
        }

        cb_watch.setChecked(show_wrapper);
        int wc_pos = 0;
        ScreenshotGenerator.WrapperColor[] colours = ScreenshotGenerator.WrapperColor.values();
        for (int w = 0; w < colours.length; w  += 1) {
            if (colours[w] == currentColour) {
                spn_wrapper.setSelection(w);
                break;
            }
        }

        if (! PebbleKit.isWatchConnected(getApplicationContext())) {
            tv_status.setText(R.string.message_not_connected);
            tv_status.setBackgroundResource(R.color.error);
            mi_share.setVisible(false);
            btn_main.setEnabled(false);
            return;
        }
        if (app_uuid == null) {
            tv_status.setText(R.string.message_no_uuid);
            tv_status.setBackgroundResource(R.color.error);
            mi_share.setVisible(false);
            btn_main.setEnabled(false);
            return;
        }
        if (screenshot_in_progress) {
            tv_status.setText(R.string.message_screenshot_in_progress);
            tv_status.setBackgroundResource(R.color.info);
            mi_share.setVisible(false);
            btn_main.setEnabled(false);
            return;
        }

        btn_main.setEnabled(true);

        tv_status.setText(R.string.message_screenshot_ready);
        tv_status.setBackgroundResource(R.color.ok);
        if (lastScreenshot == null) {
            mi_share.setVisible(false);
        }
        else {
            mi_share.setVisible(true);

        }
        return;
    }

    protected void updateScreenshotView() {
        if (lastScreenshot == null) {
            if (show_wrapper) {
                iv_screenshot.setImageDrawable(getResources().getDrawable(ScreenshotGenerator.wrapper_map.get(currentColour)));
            }
            else {
                iv_screenshot.setImageDrawable(null);
            }
        }
        else {
            if (show_wrapper) {
                iv_screenshot.setImageBitmap(lastScreenshot.getWrappedScreenshot(currentColour, true));
            }
            else {
                iv_screenshot.setImageBitmap(lastScreenshot.getScreenshot());
            }
        }
    }

    protected void loadSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.contains("appUuid")) {
            try {
                app_uuid = UUID.fromString(settings.getString("appUuid", null));
            }
            catch (IllegalArgumentException ex) {
                app_uuid = null;
            }
        }
        currentColour = ScreenshotGenerator.WrapperColor.valueOf(settings.getString("currentColour", "Black"));
        show_wrapper = settings.getBoolean("showWrapper", true);
    }

    protected void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("appUuid", app_uuid == null ? "" : app_uuid.toString());
        editor.putString("currentColour", currentColour.toString());
        editor.putBoolean("showWrapper", show_wrapper);
        editor.commit();
    }

}

