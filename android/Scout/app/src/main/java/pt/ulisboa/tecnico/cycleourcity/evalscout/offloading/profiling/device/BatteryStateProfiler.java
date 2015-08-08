package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BatteryStateProfiler extends BroadcastReceiver{

    //Logging
    protected boolean VERBOSE = true;
    private final String LOG_TAG = this.getClass().getSimpleName();

    private int currentBattery;
    private boolean isCharging, isFull;

    private final Context appContext;
    private BatteryManager batteryManager;

    private boolean isCleanedUp = false;
    protected BatteryStateProfiler(Context context){
        appContext = context;
        batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        //Force Battery State Update
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        currentBattery  = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        isCharging      = (batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0 ? false : true);
        isFull          = (currentBattery == scale ? true : false);


        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
    }

    public int getCurrentBattery(){return currentBattery;}

    public boolean isCharging(){return isCharging;}

    public boolean isFull(){return isFull;}

    public void teardown(){
        if(!isCleanedUp) {
            appContext.unregisterReceiver(this);
            isCleanedUp = true;
        }
    }

    private void updateInternalState(@Nullable Intent batteryStatus){

        if(batteryStatus!=null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isFull = (status == BatteryManager.BATTERY_STATUS_FULL);
            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);
        }else{
            isFull = false;
            isCharging = false;
        }

        currentBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if(VERBOSE) Log.d(DeviceStateProfiler.LOG_TAG, dumpState());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action.equals(Intent.ACTION_BATTERY_CHANGED))
            updateInternalState(intent);
    }

    //Logging
    private String dumpState(){

        DateFormat format = new SimpleDateFormat("hh:mm:ss");

        return "["+LOG_TAG+" - "+format.format(new Date())+"]: "+
                (isCharging ? "Charging" : "Not Charging")+", "+
                (isFull ? "Full Capacity": ""+currentBattery+"%");
    }

    /*
     ************************************************************************
     * MockUp Function - For testing purposes only                          *
     ************************************************************************
     */
    protected void forceMockUpMode(){
        teardown();
    }

    protected void forceBatteryUpdate(int batteryLevel){
        this.currentBattery = batteryLevel;
        this.isCharging = true;
        this.isFull = batteryLevel == 100;
    }
}

