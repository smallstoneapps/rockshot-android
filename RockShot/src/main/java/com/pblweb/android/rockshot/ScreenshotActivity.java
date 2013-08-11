package com.pblweb.android.rockshot;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
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
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import java.util.UUID;

public class ScreenshotActivity extends Activity {

    private String TAG = ScreenshotActivity.class.getSimpleName();

    // private UUID app_uuid = UUID.fromString("9141b628-bc89-498e-b147-c884f0160215"); LONDON TUBE
    private UUID app_uuid = UUID.fromString("9141b628-bc89-498e-b147-826565cd3d24");
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

    protected MenuItem mi_settings;
    protected ScreenshotGenerator.WrapperColor currentColour = ScreenshotGenerator.WrapperColor.Black;
    public boolean show_wrapper = true;
    protected ScreenshotGenerator lastScreenshot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot);

        iv_screenshot = (ImageView)this.findViewById(R.id.imageView);
        tv_status = (TextView)this.findViewById(R.id.tvErrorMessage);
        pb_capture = (ProgressBar)this.findViewById(R.id.progressBar);
        btn_main = (Button)this.findViewById(R.id.button);

        btn_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestScreenshot();
            }
        });

        spn_wrapper = (Spinner)this.findViewById(R.id.spinner);
        spn_wrapper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ScreenshotGenerator.WrapperColor[] colors = ScreenshotGenerator.WrapperColor.values();
                currentColour = colors[i];
                updateScreenshotView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentColour = ScreenshotGenerator.WrapperColor.Black;
                updateScreenshotView();
            }
        });

        cb_watch = (CheckBox)this.findViewById(R.id.checkBox);
        cb_watch.setChecked(this.show_wrapper);
        cb_watch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                show_wrapper = b;
                updateScreenshotView();
            }
        });

        PebbleKit.registerPebbleConnectedReceiver(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi();
            }
        });

        PebbleKit.registerPebbleDisconnectedReceiver(this, new BroadcastReceiver() {
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUi();
        updateScreenshotView();

        if (app_uuid != null) {
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareScreenshot(ScreenshotGenerator shot) {
        if (shot == null) {
            return;
        }
        shot.getWrappedScreenshot();
    }

    protected void askForUuid() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.dialog_uuid_title));
        alert.setMessage(getString(R.string.dialog_uuid_message));

        final EditText input = new EditText(this);
        alert.setView(input);
        input.setHint("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");

        alert.setPositiveButton("Set UUID", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                setReceiver(UUID.fromString(input.getText().toString()));
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }


    private boolean setReceiver(UUID uuid) {
        final Handler handler = new Handler();
        final Resources resources = this.getResources();

        if (dataReceiver != null) {
            unregisterReceiver(dataReceiver);
            dataReceiver = null;
        }

        this.app_uuid = uuid;

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

                        Log.d(TAG, String.valueOf(transactionId));

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
        PebbleKit.sendDataToPebble(this.getApplicationContext(), app_uuid, dict);
    }

    protected void updateUi() {

        if (mi_settings == null) {
            return;
        }

        if (! PebbleKit.isWatchConnected(getApplicationContext())) {
            tv_status.setText(R.string.message_not_connected);
            tv_status.setBackgroundResource(R.color.error);
            mi_share.setVisible(false);
            return;
        }
        if (app_uuid == null) {
            tv_status.setText(R.string.message_no_uuid);
            tv_status.setBackgroundResource(R.color.error);
            mi_share.setVisible(false);
            return;
        }
        if (screenshot_in_progress) {
            tv_status.setText(R.string.message_screenshot_in_progress);
            tv_status.setBackgroundResource(R.color.info);
            mi_share.setVisible(false);
            return;
        }

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
        if (this.lastScreenshot == null) {
            if (this.show_wrapper) {
                iv_screenshot.setImageDrawable(this.getResources().getDrawable(ScreenshotGenerator.wrapper_map.get(currentColour)));
            }
            else {
                iv_screenshot.setImageDrawable(null);
            }
        }
        else {
            if (this.show_wrapper) {
                iv_screenshot.setImageBitmap(lastScreenshot.getWrappedScreenshot(currentColour, true));
            }
            else {
                iv_screenshot.setImageBitmap(lastScreenshot.getScreenshot());
            }
        }
    }

}

