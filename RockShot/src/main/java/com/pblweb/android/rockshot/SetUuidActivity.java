package com.pblweb.android.rockshot;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

public class SetUuidActivity extends Activity {

    EditText et_uuid_1;
    EditText et_uuid_2;
    EditText et_uuid_3;
    EditText et_uuid_4;
    EditText et_uuid_5;
    private String TAG = SetUuidActivity.class.getSimpleName();
    private Button btn_clear;
    private Button btn_httpebble;
    private Button btn_httpebble_prefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setuuid);

        et_uuid_1 = (EditText)findViewById(R.id.et_uuid_1);
        et_uuid_2 = (EditText)findViewById(R.id.et_uuid_2);
        et_uuid_3 = (EditText)findViewById(R.id.et_uuid_3);
        et_uuid_4 = (EditText)findViewById(R.id.et_uuid_4);
        et_uuid_5 = (EditText)findViewById(R.id.et_uuid_5);

        et_uuid_1.requestFocus();

        btn_clear = (Button)findViewById(R.id.btn_clear);
        btn_httpebble = (Button)findViewById(R.id.btn_httpebble);
        btn_httpebble_prefix = (Button)findViewById(R.id.btn_httpebble_prefix);

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_uuid_1.setText("");
                et_uuid_2.setText("");
                et_uuid_3.setText("");
                et_uuid_4.setText("");
                et_uuid_5.setText("");
                et_uuid_1.requestFocus();
            }
        });

        btn_httpebble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_uuid_1.setText("9141b628");
                et_uuid_2.setText("bc89");
                et_uuid_3.setText("498e");
                et_uuid_4.setText("b147");
                et_uuid_5.setText("049f49c099ad");
            }
        });

        btn_httpebble_prefix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                et_uuid_1.setText("9141b628");
                et_uuid_2.setText("bc89");
                et_uuid_3.setText("498e");
                et_uuid_4.setText("b147");
                et_uuid_5.setText("");
                et_uuid_5.requestFocus();
            }
        });

        et_uuid_1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (et_uuid_1.getText().length() >= 8) {
                    et_uuid_2.requestFocus();
                }
            }
        });

        et_uuid_2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (et_uuid_2.getText().length() >= 4) {
                    et_uuid_3.requestFocus();
                }
            }
        });

        et_uuid_3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (et_uuid_3.getText().length() >= 4) {
                    et_uuid_4.requestFocus();
                }
            }
        });

        et_uuid_4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (et_uuid_4.getText().length() >= 4) {
                    et_uuid_5.requestFocus();
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }
        if (! extras.containsKey("uuid")) {
            return;
        }
        String uuid_str = extras.getString("uuid");
        et_uuid_1.setText(uuid_str.substring(0, 8));
        et_uuid_2.setText(uuid_str.substring(9, 13));
        et_uuid_3.setText(uuid_str.substring(14, 18));
        et_uuid_4.setText(uuid_str.substring(19, 23));
        et_uuid_5.setText(uuid_str.substring(24, 36));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_setuuid, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveUuid();
                return true;
        }
        return false;
    }

    private void saveUuid() {
        String uuid_str = "";

        if (et_uuid_1.getText().toString().length() != 8) {
            et_uuid_1.setError("Invalid UUID segment.");
            et_uuid_1.requestFocus();
            return;
        }
        uuid_str += et_uuid_1.getText().toString() + "-";

        if (et_uuid_2.getText().toString().length() != 4) {
            et_uuid_2.setError("Invalid UUID segment.");
            et_uuid_2.requestFocus();
            return;
        }
        uuid_str += et_uuid_2.getText().toString() + "-";

        if (et_uuid_3.getText().toString().length() != 4) {
            et_uuid_3.setError("Invalid UUID segment.");
            et_uuid_3.requestFocus();
            return;
        }
        uuid_str += et_uuid_3.getText().toString() + "-";

        if (et_uuid_4.getText().toString().length() != 4) {
            et_uuid_4.setError("Invalid UUID segment.");
            et_uuid_4.requestFocus();
            return;
        }
        uuid_str += et_uuid_4.getText().toString() + "-";

        if (et_uuid_5.getText().toString().length() != 12) {
            et_uuid_5.setError("Invalid UUID segment.");
            et_uuid_5.requestFocus();
            return;
        }
        uuid_str += et_uuid_5.getText().toString() ;

        try {
            UUID tmp = UUID.fromString(uuid_str);
        }
        catch (IllegalArgumentException ex) {
            Toast.makeText(getApplicationContext(), "Invalid UUID.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent data = new Intent();
        data.putExtra("uuid", uuid_str);
        setResult(RESULT_OK, data);
        super.finish();
    }
}
