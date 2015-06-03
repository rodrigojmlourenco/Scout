package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources;

import android.util.Log;

/**
 * Because during debugging the device must be connected via USB, the device's battery never decreases
 * as the device is being charged.
 * <br>
 * In order to enable testing a mockup battery was created, as to allow offloading decisions even
 * if the device is being charged.
 */
public class MockupBatteryProfiler extends ResourceProfiler {

    private int currentBattery;

    private static MockupBatteryProfiler BATTERY = null;
    public static MockupBatteryProfiler activate(){
        if(BATTERY == null)
            synchronized (MockupBatteryProfiler.class){
                if(BATTERY == null) BATTERY = new MockupBatteryProfiler();
            }

        return BATTERY;
    }


    public static MockupBatteryProfiler activate(int initialBattery){
        activate().setCurrentBattery(initialBattery);
        return activate();
    }

    public static boolean isActive(){
        return BATTERY!=null;
    }

    public static void decrementBattery(){
        try{
            BATTERY.decreaseBattery();
        }catch (NullPointerException e){
            Log.e(MockupBatteryProfiler.class.getSimpleName(),
                    "MockupBattery is not active, unable to decrease its value.");
        }
    }

    private MockupBatteryProfiler(){}

    @Override
    protected void profile() {

    }

    @Override
    public String dumpInfo() {
        return null;
    }

    public void setCurrentBattery(int capacity){
        currentBattery = capacity;
    }

    public int getCurrentBattery(){
        return currentBattery;
    }

    private void decreaseBattery(){
        currentBattery--;
    }
}
