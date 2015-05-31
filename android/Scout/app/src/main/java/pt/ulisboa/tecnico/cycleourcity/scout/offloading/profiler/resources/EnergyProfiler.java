package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources;

import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.log4j.helpers.CyclicBuffer;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain.StatisticalMetrics;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.exceptions.EnergyPropertyNotSupportedException;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
public class EnergyProfiler extends ResourceProfiler{

    public String LOG_TAG = "EnergyProfiler";

    private final BatteryManager batteryStats;

    private int capacity;
    private int currentInstant;
    private int currentAverage;
    private int chargeCounter;

    private long energyCounter;

    private boolean isCharging;

    private List<Integer> supportedProperties;
    private final SharedPreferences settings;


    private CircularFifoQueue<Integer> instantCurrents = new CircularFifoQueue(25);

    public static interface EnergyProfilerSettings {
        public static String
            CONFIGURED = "configured",
            SUPPORTS_BATTERY_CAPACITY = "battery_supported",
            SUPPORTS_CHARGE_COUNTER     = "charge_supported",
            SUPPORTS_CURRENT_AVG        = "avg_current_supported",
            SUPPORTS_CURRENT_NOW        = "inst_current_supported",
            SUPPORTS_ENERGY_COUNTER     = "energy_supported";
    }

    public final static String PREFS_NAME = "EnergySettings";

    public EnergyProfiler(BatteryManager batteryStats, SharedPreferences settings){
        this.settings = settings;
        this.batteryStats = batteryStats;
        supportedProperties = new ArrayList<>();

        //Checks if the energy profiler has been executed before
        if(!settings.getBoolean(EnergyProfilerSettings.CONFIGURED, false)) {
            Log.d(LOG_TAG, "First time executing the profiler, running its configuration");
            SharedPreferences.Editor editor = settings.edit();

            editor.putBoolean(
                    EnergyProfilerSettings.SUPPORTS_BATTERY_CAPACITY,
                    batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) != Integer.MIN_VALUE);

            editor.putBoolean(
                    EnergyProfilerSettings.SUPPORTS_CHARGE_COUNTER,
                    batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) != Integer.MIN_VALUE);

            editor.putBoolean(
                    EnergyProfilerSettings.SUPPORTS_CURRENT_AVG,
                    batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) != Integer.MIN_VALUE);

            editor.putBoolean(
                    EnergyProfilerSettings.SUPPORTS_CURRENT_NOW,
                    batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) != Integer.MIN_VALUE);

            editor.putBoolean(
                    EnergyProfilerSettings.SUPPORTS_ENERGY_COUNTER,
                    batteryStats.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) != Long.MIN_VALUE);

            editor.putBoolean(EnergyProfilerSettings.CONFIGURED, true);

            editor.commit();
        }
    }



    @Override
    public void profile() {

        capacity = batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        chargeCounter = batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        energyCounter = batteryStats.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);

        currentAverage = batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        currentInstant = batteryStats.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        instantCurrents.add(currentInstant);

        isCharging = BatteryManager.BATTERY_STATUS_CHARGING == 1 ? true : false;

    }

    @Override
    public String dumpInfo() {
        String info = "["+LOG_TAG+":]\n";

        try {
            info += "\tCapacity: "+getCapacity()+"%\n";
        } catch (EnergyPropertyNotSupportedException e) {
            info += "\tCapacity: Not supported\n";
        }

        try {
            info += "\tRemaining Charge: "+getChargeCounter()+"mA/h\n";
        } catch (EnergyPropertyNotSupportedException e) {
            info += "\tRemaining Charge: Not supported\n";
        }

        try {
            info += "\tAverage Current: "+getCurrentAverage()+"mA\n";
        } catch (EnergyPropertyNotSupportedException e) {
            info += "\tAverage Current: Not supported\n";
        }
        try {
            info += "\tInstantaneous Current: "+getCurrentInstant()+"mA\n";
        } catch (EnergyPropertyNotSupportedException e) {
            info += "\tInstantaneous Current: Not supported\n";
        }
        try {
            info += "\tRemaining Energy: "+getEnergyCounter()+"nW/h\n";
        } catch (EnergyPropertyNotSupportedException e) {
            info += "\tRemaining Energy: Not supported\n";
        }

        info +="\tCharging: "+isCharging();

        return info;
    }

    /**
     * Remaining battery capacity as an integer percentage of total capacity (with no fractional part).
     * @return remaining battery
     */
    public int getCapacity() throws EnergyPropertyNotSupportedException {

        if(!settings.getBoolean(EnergyProfilerSettings.SUPPORTS_BATTERY_CAPACITY, false))
            throw new EnergyPropertyNotSupportedException("Battery Capacity");

        return capacity;
    }

    /**
     * Battery capacity in microampere-hours, as an integer.
     * @return battery capacity
     */
    public int getCurrentAverage() throws EnergyPropertyNotSupportedException {

        if(!settings.getBoolean(EnergyProfilerSettings.SUPPORTS_CURRENT_AVG, false))
            throw new EnergyPropertyNotSupportedException("Current Average");

        int total = 0;
        if(currentAverage==0){
            for(Integer val : instantCurrents)
                total += val;

            return  total / instantCurrents.size();
        }



        return currentAverage;
    }

    /**
     *
     * @return
     */
    public int getCurrentInstant() throws EnergyPropertyNotSupportedException {

        if(!settings.getBoolean(EnergyProfilerSettings.SUPPORTS_CURRENT_NOW, false))
            throw new EnergyPropertyNotSupportedException("Instantaneous Current");

        return currentInstant;
    }

    /**
     * @return
     */
    public int getChargeCounter() throws EnergyPropertyNotSupportedException {

        if(!settings.getBoolean(EnergyProfilerSettings.SUPPORTS_CHARGE_COUNTER, false))
            throw new EnergyPropertyNotSupportedException("Charge Count");

        return chargeCounter;
    }

    /**
     * Instantaneous battery current in microamperes, as an integer.
     * @return
     */
    public long getEnergyCounter() throws EnergyPropertyNotSupportedException {

        if(!settings.getBoolean(EnergyProfilerSettings.SUPPORTS_ENERGY_COUNTER, false))
            throw new EnergyPropertyNotSupportedException("Energy Count");


        return energyCounter;
    }

    /**
     * Battery remaining energy in nanowatt-hours, as a long integer.
     * @return remaining battery in nanowatts-hour
     */
    public boolean isCharging() {
        return isCharging;
    }

    public static class IdleEnergyProfiler extends EnergyProfiler {

        public final String LOG_TAG = this.getClass().getSimpleName();

        public IdleEnergyProfiler(BatteryManager batteryStats, SharedPreferences settings) {
            super(batteryStats, settings);
        }

        @Override
        public void profile() {
            super.profile();
        }
    }

    public static class SensingEnergyProfiler extends EnergyProfiler {

        public final String LOG_TAG = this.getClass().getSimpleName();

        public SensingEnergyProfiler(BatteryManager batteryStats, SharedPreferences settings) {
            super(batteryStats, settings);
        }

        @Override
        public void profile() {
            super.profile();
        }
    }
}
