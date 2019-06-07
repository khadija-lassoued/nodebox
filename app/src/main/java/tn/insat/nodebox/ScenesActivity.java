package tn.insat.nodebox;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import prtocol.MQTTHelper;

public class ScenesActivity extends AppCompatActivity {

    private ArrayList<Device> nodes, devices;


    BottomNavigationView bottomNavigationView;
    private Spinner condition, conditionState, result, resultState;
    private ToggleButton add;
    private LinearLayout morning, nigh, movie, leaving;
    Toolbar toolbar;

    MQTTHelper mqttHelper;

    Boolean customRule = false;
    String conditionTopic, conditionValue, resultTopic, resultValue;

    SharedPreferences sharedPreferences;
    public static final String mypreference = "mypref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenes);

        //get nodes list from Home
        Intent intent = getIntent();
        nodes = intent.getParcelableArrayListExtra("list");
        devices = devices(nodes);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                return navigation(menuItem.getItemId());
            }
        });

        sharedPreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        condition = findViewById(R.id.condition);
        conditionState = findViewById(R.id.condition_state);
        result = findViewById(R.id.result);
        resultState = findViewById(R.id.result_state);
        add = findViewById(R.id.rule);
        morning = findViewById(R.id.morning_rule);
        nigh = findViewById(R.id.night_rule);
        movie = findViewById(R.id.movie_rule);
        leaving = findViewById(R.id.leaving_rule);

        //display the Back Button
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addItem(condition, listNames(nodes));
        addItem(result, listNames(devices));
        nodeState(condition, nodes);
        addItem(resultState, new ArrayList<String>(Arrays.asList("on", "off")));

        if (sharedPreferences.contains("rule")) {
            add.setChecked(sharedPreferences.getBoolean("rule",false));
        }
        add.setOnCheckedChangeListener(new CustomRuleClick());
        morning.setOnClickListener(new RuleClick());
        nigh.setOnClickListener(new RuleClick());
        movie.setOnClickListener(new RuleClick());
        leaving.setOnClickListener(new RuleClick());

    }

    @Override
    protected void onStart() {
        super.onStart();
        startMqtt();
        condition.setSelection(sharedPreferences.getInt("condition", 0));
        result.setSelection(sharedPreferences.getInt("result", 0));
        resultState.setSelection(sharedPreferences.getInt("resultState", 0));
    }


    public void addItem(Spinner spinner, ArrayList<String> list) {
        //delete item if exists
        spinner.setAdapter(null);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(adapter);
    }

    public ArrayList<String> listNames(ArrayList<Device> list) {
        ArrayList<String> listname = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            listname.add(list.get(i).toString());
        }
        return listname;
    }

    public ArrayList<Device> devices(ArrayList<Device> nodes) {
        ArrayList<Device> list = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getIsDevice())
                list.add(nodes.get(i));
        }
        return list;
    }

    //populate the state spinner depending on the device/sensor
    public void nodeState(Spinner spinner, final ArrayList<Device> list) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("test", list.get(i).getState());
                // is a device
                if ((list.get(i).getIsDevice())) {
                    addItem(conditionState, new ArrayList<String>(Arrays.asList("on", "off")));
                } else { //is a sensor
                    ArrayList<String> list1 = new ArrayList<String>();
                    for (int j = -10; j < 100; j++) {
                        list1.add(Integer.toString(j));
                    }
                    addItem(conditionState, list1);
                }
                conditionState.setSelection(sharedPreferences.getInt("conditionState", 0));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void startMqtt() {

        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean b, String s) {
                Log.d("TAG MQTT", "connection complete");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d("TAG MQTT", "connection lost");
            }

            @Override
            public void messageArrived(String topic, final MqttMessage mqttMessage) throws Exception {
                if (customRule) {
                    if (conditionTopic.equalsIgnoreCase(topic) && conditionValue.equalsIgnoreCase(mqttMessage.toString())) {
                        mqttHelper.publishMessage(resultTopic, resultValue);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            }
        });
    }

    // bottom navigation manager
    private Boolean navigation(Integer integer) {
        switch (integer) {
            case R.id.action_devices:
                saveSpinnersState();
                Intent intent = new Intent();
                intent.putExtra("custom_rule", conditionTopic + "," + conditionValue + "," + resultTopic + "," + resultValue);
                finish();
                break;
            case R.id.action_scenes:
                break;
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveSpinnersState();
                Intent intent = new Intent();
                intent.putExtra("custom_rule", conditionTopic + "," + conditionValue + "," + resultTopic + "," + resultValue);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class RuleClick implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            //change background for 1 seconds when pressed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.saumaon));
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        view.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.gray));
                    }
                }, 1000);
            }
            //publish MQTT message
            switch (view.getId()) {
                case R.id.morning_rule:
                    try {
                        for (Device d : devices) {
                            if (d.getTopic().contains("coffeeMaker") || d.getTopic().contains("radio") || d.getTopic().contains("window"))
                                mqttHelper.publishMessage(d.getTopic(), "on");
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                case R.id.night_rule:
                    try {
                        for (Device d : devices) {
                            if (d.getTopic().contains("lamp") || d.getTopic().contains("tv"))
                                mqttHelper.publishMessage(d.getTopic(), "off");
                        }
                        Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.movie_rule:
                    try {
                        for (Device d : devices) {
                            if (d.getTopic().contains("lamp"))
                                mqttHelper.publishMessage(d.getTopic(), "off");
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                case R.id.leaving_rule:
                    try {
                        for (Device d : devices) {
                            if (d.getTopic().contains("door") || d.getTopic().contains("window"))
                                mqttHelper.publishMessage(d.getTopic(), "off");
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    class CustomRuleClick implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isChecked) {
                customRule = true;
                conditionTopic = nodes.get(condition.getSelectedItemPosition()).getTopic();
                conditionValue = conditionState.getSelectedItem().toString();
                resultTopic = devices.get(result.getSelectedItemPosition()).getTopic();
                resultValue = resultState.getSelectedItem().toString();
                Log.d("rule", conditionTopic + resultTopic);
                //condition is already verified
                if (isVerified(conditionTopic, conditionValue)) {
                    try {
                        mqttHelper.publishMessage(resultTopic, resultValue);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                customRule = false;
                conditionTopic = "";
                conditionValue = "";
                resultTopic = "";
                resultValue = "";
            }
            editor.putBoolean("rule", isChecked).commit();
        }
    }

    private void saveSpinnersState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("condition", condition.getSelectedItemPosition());
        editor.putInt("conditionState", conditionState.getSelectedItemPosition());
        editor.putInt("result", result.getSelectedItemPosition());
        editor.putInt("resultState", resultState.getSelectedItemPosition());
        editor.commit();
    }

    private boolean isVerified(String topic, String state) {
        for (Device d : nodes) {
            if (d.getTopic().contains(topic) && d.getState().contains(state))
                return true;
        }
        return false;
    }

}
