package tn.insat.nodebox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.wang.avi.AVLoadingIndicatorView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import adapter.DeviceAdapter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import manager.AccessPointManager;
import prtocol.DeviceConfigProtocol;
import prtocol.MQTTHelper;

public class HomeActivity extends AppCompatActivity {

    private static final int ADD_DEVICE = 1;

    FirebaseAuth auth;
    FirebaseAuth.AuthStateListener authListener;

    FirebaseFirestore db;

    FloatingActionButton scan_btn;
    Toolbar toolbar;
    BottomNavigationView bottomNavigationView;
    LinearLayout devicesList;
    FrameLayout content;
    ListView scanList;
    RecyclerView recyclerView;
    AVLoadingIndicatorView avi;
    TextView textView;

    String user;
    String remeberMe;
    Boolean fabOpen = false;

    ArrayList<Device> list;
    ArrayAdapter<Device> scanAdapter;
    DeviceAdapter deviceAdapter;

    private AccessPointManager accessPointManager;
    private MQTTHelper mqttHelper;

    static final String SSID = "iot";
    static final String password = "iotPPP2019";

    Intent intent;
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (avi.getVisibility() == View.VISIBLE) {
                textView.setVisibility(View.VISIBLE);
                avi.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        intent = getIntent();

        user = isLoggedIn();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        scan_btn = findViewById(R.id.scan);
        content = findViewById(R.id.hide);
        scanList = findViewById(R.id.scan_result);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        avi = findViewById(R.id.avi);
        textView = findViewById(R.id.add_devices);


        //bottomNavigationView.setOnNavigationItemSelectedListener(item -> navigation(item.getItemId())); }
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                return navigation(menuItem.getItemId());
            }
        });

        setSupportActionBar(toolbar);

        list = new ArrayList<>();

        accessPointManager = new AccessPointManager(getApplicationContext());
        scanAdapter = new ArrayAdapter<Device>(this, android.R.layout.simple_list_item_1, list);
        scanList.setAdapter(scanAdapter);

        displayName();

        setUpRecycleView();
        startMqtt();

        handler.postDelayed(runnable, 15000);

        deviceAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                avi.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            }
        });

        scan_btn.setOnClickListener(new ScanButtonClick());
        scanList.setOnItemClickListener(new ScannedItemClick());
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
        deviceAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
            deviceAdapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remeberMe == null)
            auth.signOut();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tollbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_out:
                auth.signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_DEVICE) {
            if (resultCode == RESULT_OK) {
                Device result = (Device) data.getParcelableExtra("device");
                result.setName(data.getStringExtra("device_name"));
                scanAdapter.remove(result);
                Log.d("tag", result.toString());
                addDevice(result);

            }
        }
    }

    private void setUpRecycleView() {
        Query query = db.collection("users").document(auth.getUid()).collection("devices").orderBy("timestamp");

        FirestoreRecyclerOptions<Device> response = new FirestoreRecyclerOptions.Builder<Device>()
                .setQuery(query, Device.class)
                .build();

        Log.d("tag", response.toString());

        deviceAdapter = new DeviceAdapter(response, new DeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Device device) {
                try {
                    mqttHelper.publishMessage(device.getTopic(), device.getState());
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.d("pub message", device.getTopic() + " " + device.getState());
            }
        });

        recyclerView = findViewById(R.id.devices_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(deviceAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                deviceAdapter.delteItem(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(recyclerView);
    }

    private String isLoggedIn() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.putExtra("return", "true");
                    startActivity(intent);
                    finish();
                }
            }
        };
        remeberMe = intent.getStringExtra("remeber_me");
        return user.getDisplayName();
    }

    private void displayName() {
        if (user == null) {
            getSupportActionBar().setTitle("Hello " + intent.getStringExtra("name"));
        } else {
            getSupportActionBar().setTitle("Hello " + user);
        }

    }

    public void getConnectedDevices(final ArrayAdapter<Device> connected) throws IOException {
        //get connected device's IP address
        final ArrayList<String> ipList = new ArrayList<String>();
        final ArrayList<Device> devices = new ArrayList<Device>();
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
                        String line;
                        while ((line = br.readLine()) != null) {
                            final String[] splitted = line.split(" +");

                            if ((splitted != null) && (splitted.length >= 4)) {
                                if (splitted[0].length() > 6) {
                                    if (!ipList.contains(splitted[0])) {
                                        ipList.add(splitted[0]);
                                        Log.d("config_dev", "device " + splitted[0]);
                                        new Thread() {
                                            public void run() {
                                                final DeviceConfigProtocol deviceConfig = new DeviceConfigProtocol();
                                                deviceConfig.helloMessage(splitted[0]);
                                                devices.add(deviceConfig.getDevice());
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        connected.add(deviceConfig.getDevice());
                                                    }
                                                });
                                            }
                                        }.start();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(this.getClass().toString(), e.toString());
                    }
                }
            }
        }.start();
    }

    private void addDevice(Device device) {
        // Create a new device
        Map<String, Object> newDevice = new HashMap<>();
        newDevice.put("timestamp", new Date());
        newDevice.put("name", device.getName());
        newDevice.put("state", device.getState());
        newDevice.put("isDevice", device.getIsDevice());
        newDevice.put("topic", device.getTopic());

        // Add a new document with a generated ID
        db.collection("users").document(auth.getUid()).collection("devices").add(newDevice)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "Error adding document", e);
                    }
                });
    }

    private Boolean navigation(Integer integer) {
        switch (integer) {
            case R.id.action_devices:
                break;
            case R.id.action_scenes:
                startActivity(new Intent(getApplicationContext(), ScenesActivity.class).putExtra("list", deviceAdapter.getItems()));
                break;
        }
        return true;
    }

    class ScanButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!fabOpen) {
                fabOpen = true;
                scan_btn.setImageResource(R.drawable.ic_close);
                content.setVisibility(View.VISIBLE);
                scanList.setVisibility(View.VISIBLE);
                CreateNewWifiApNetwork();
                scanAdapter.clear();
                Snackbar.make(v, "Searching for new devices", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                try {
                    getConnectedDevices(scanAdapter);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                content.setVisibility(View.GONE);
                scan_btn.setImageResource(R.drawable.ic_add);
                fabOpen = false;
                accessPointManager.turnWifiApOff();
                accessPointManager.turnWifiOn();
            }
        }
    }

    public void CreateNewWifiApNetwork() {
        accessPointManager.createNewNetwork(SSID, password);
    }

    private void startMqtt() {

        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean b, String s) {
                Log.d("TAG", "connection complete");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d("TAG", "connection lost");
            }

            @Override
            public void messageArrived(final String topic, final MqttMessage mqttMessage) throws Exception {
                Log.w("Debug", mqttMessage.toString());
                db.collection("users").document(auth.getUid()).collection("devices").whereEqualTo("topic", topic)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String message = mqttMessage.toString();
                                        //if (!topic.toString().contains("sensor"))
                                        if (message.equals("0"))
                                            message = "off";
                                        if (message.equals("1"))
                                            message = "on";
                                        document.getReference().update("state", message);
                                        Log.d("name", document.getId() + " => " + document.getData());
                                    }
                                } else {
                                    Log.d("TAG", "Error getting documents: ", task.getException());
                                }
                            }
                        });
                String rule = intent.getStringExtra("custom_rule");
                if (!rule.equals("")) {
                    String result[] = rule.split(",");
                    if (result[0].equalsIgnoreCase(topic) && result[1].equalsIgnoreCase(mqttMessage.toString())) {
                        mqttHelper.publishMessage(result[2], result[3]);
                        Log.d("RULE", result.toString());
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    class ScannedItemClick implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Device item = scanAdapter.getItem(i);
            Log.d("TAG", item.toString());
            Intent intent = new Intent(HomeActivity.this, DeviceConfigActivity.class).putExtra("item", item);
            startActivityForResult(intent, ADD_DEVICE);
        }
    }


}
