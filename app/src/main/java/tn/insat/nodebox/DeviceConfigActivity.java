package tn.insat.nodebox;

import androidx.appcompat.app.AppCompatActivity;
import prtocol.DeviceConfigProtocol;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceConfigActivity extends AppCompatActivity {

    Button submit;
    EditText name,SSID,password;

    Device item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_config);

        submit = (Button) findViewById(R.id.add);
        name = (EditText) findViewById(R.id.device_name);
        SSID= (EditText) findViewById(R.id.wifiSSID);
        password = (EditText) findViewById(R.id.wifiPass);

        Bundle data = getIntent().getExtras();
        item = (Device) data.getParcelable("item");

        submit.setOnClickListener(new SubmitButtonClick());
    }

    private void saveDevice(){

    }
    class SubmitButtonClick implements  View.OnClickListener {

        @Override
        public void onClick(View view) {
            Log.d("config_dev","dending SSID & pass");
            new Thread() {
                public void run () {
                    DeviceConfigProtocol deviceConfig = new DeviceConfigProtocol();
                    deviceConfig.configurationMessage(item.getIP(),SSID.getText().toString(),password.getText().toString());
                }
            }.start();

            Intent intent = new Intent();
            intent.putExtra("device_name",name.getText().toString());
            intent.putExtra("device",item);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
