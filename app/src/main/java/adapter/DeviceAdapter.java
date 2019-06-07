package adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import tn.insat.nodebox.Device;
import tn.insat.nodebox.R;

public class DeviceAdapter extends FirestoreRecyclerAdapter<Device, DeviceAdapter.DeviceHolder> {

    private OnItemClickListener listener;
    private ArrayList<Device> devices;


    public DeviceAdapter(@NonNull FirestoreRecyclerOptions<Device> options, OnItemClickListener listener) {
        super(options);
        this.listener = listener;
        devices = new ArrayList<Device>();
    }

    @Override
    protected void onBindViewHolder(@NonNull final DeviceHolder deviceHolder, final int i, @NonNull final Device device) {
        if (device!=null && device.getName()!=null && device.getState()!=null) {

            setDevice(device,deviceHolder);

            //add device to the list if it does not exist
            if(!contains(getSnapshots().getSnapshot(i).toObject(Device.class))) {
                devices.add(getSnapshots().getSnapshot(i).toObject(Device.class));
            }

            deviceHolder.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (device.getIsDevice()){
                        toggleDevice(deviceHolder,device,i);
                        listener.onItemClick(device);
                    }
                }
            });
        }

    }

    @NonNull
    @Override
    public DeviceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item,parent,false);
        return new DeviceHolder(view);
    }

    public void delteItem(int position){
        getSnapshots().getSnapshot(position).getReference().delete();

    }

    public ArrayList<Device> getItems() {
        return devices;
    }

    public class DeviceHolder extends RecyclerView.ViewHolder {

        TextView deviceName;
        TextView devicState;
        ImageView deviceImg;
        RelativeLayout relativeLayout;

        public DeviceHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.view_device_name);
            devicState = itemView.findViewById(R.id.view_device_state);
            deviceImg = itemView.findViewById(R.id.view_device_img);
            relativeLayout = itemView.findViewById(R.id.relative);

        }

    }

    public interface OnItemClickListener {
        void onItemClick(Device device);

    }

    private boolean contains(Device device){
        for(Device d : devices){
            if(d.getTopic().contains(device.getTopic()))
                return true;
        }
        return false;
    }

    private void deviceImg (int imgOn,int imgOff,String state, DeviceHolder deviceHolder) {
                if (state.equalsIgnoreCase("on") || state.equals("1")) {
                    deviceHolder.deviceImg.setImageResource(imgOn);
                    deviceHolder.relativeLayout.setBackgroundResource(R.color.saumaon);
                    Log.d("img","on");
                } else if (state.equalsIgnoreCase("off") || state.equals("0"))  {
                    deviceHolder.deviceImg.setImageResource(imgOff);
                    deviceHolder.relativeLayout.setBackgroundResource(R.color.gray);
                    Log.d("img","off");
                }
    }

    private void setDeviceImg (String topic,String state,DeviceHolder deviceHolder) {
        if (topic.contains("lamp")) {
            deviceImg(R.drawable.on,R.drawable.off,state,deviceHolder);
        } else if (topic.contains("door")) {
            deviceImg(R.drawable.ic_door_open,R.drawable.ic_door_closed,state,deviceHolder);
        } else if (topic.contains("coffeeMaker")) {
            deviceImg(R.drawable.ic_coffe,R.drawable.ic_coffe,state,deviceHolder);
        }
    }

    private void setDevice (Device device, DeviceHolder deviceHolder) {
        deviceHolder.deviceName.setText(device.getName());
        deviceHolder.devicState.setText(device.getState());
        if (device.getIsDevice()) {
            setDeviceImg(device.getTopic(),device.getState(),deviceHolder);
        } else {
            deviceHolder.relativeLayout.setBackgroundResource(R.color.iris);
            if (device.getTopic().contains("temp")) {
                if (Integer.valueOf(device.getState())>20)
                    deviceHolder.deviceImg.setImageResource(R.drawable.ic_warm);
                else
                    deviceHolder.deviceImg.setImageResource(R.drawable.ic_cold);
            } else if (device.getTopic().contains("humidity"))
                deviceHolder.deviceImg.setImageResource(R.drawable.ic_humidity);
        }
    }

    private void toggleDevice(DeviceHolder deviceHolder,Device device,  int i) {
        if (device.getState().equalsIgnoreCase("on") || device.getState().equals("1")) {
            deviceHolder.devicState.setText("off");
            getSnapshots().getSnapshot(i).getReference().update("state","off");
            setDeviceImg(device.getTopic(),"off",deviceHolder);
            device.setState("off");
        } else if (device.getState().equalsIgnoreCase("off")|| device.getState().equals("0")) {
            deviceHolder.devicState.setText("on");
            getSnapshots().getSnapshot(i).getReference().update("state","on");
            setDeviceImg(device.getTopic(),"on",deviceHolder);
            device.setState("on");
        }
    }

}