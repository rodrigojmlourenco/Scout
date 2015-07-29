package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device;

import android.content.Context;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.RuleSetKeys;

/**
 * Created by rodrigo.jm.lourenco on 29/07/2015.
 */
public class DeviceStateProfiler {

    private final NetworkStateProfiler networkState;
    private final BatteryStateProfiler batteryState;
    private final MobileDataPlanProfiler mobileDataPlanState;

    public DeviceStateProfiler(Context context) {
        networkState = new NetworkStateProfiler(context);
        batteryState = new BatteryStateProfiler(context);
        mobileDataPlanState = new MobileDataPlanProfiler(context);

        mobileDataPlanState.startMonitoring();
    }

    public DeviceStateSnapshot getDeviceState(){
        return new DeviceStateSnapshot(
                networkState.getCurrentNetworkType(),
                batteryState.getCurrentBattery(),
                mobileDataPlanState.getDataPlan(),
                mobileDataPlanState.getDataPlanLimit(),
                mobileDataPlanState.getDataPlanConsumption());
    }


    public void teardown() {

        mobileDataPlanState.stopMonitoring();

        networkState.teardown();
        batteryState.teardown();
        mobileDataPlanState.teardown();

    }




    public static class DeviceStateSnapshot{

        public final RuleSetKeys.SupportedNetworkTypes networkType;

        public final int currentBattery;

        public final long dataPlan, dataPlanLimit, consumedDataPlan;

        public DeviceStateSnapshot(int networkType,
                                   int currentBattery,
                                   long dataPlan, long dataPlanLimit, long consumedDataPlan){

            this.networkType        = parseNetworkType(networkType);
            this.currentBattery     = currentBattery;
            this.dataPlan           = dataPlan;
            this.dataPlanLimit      = dataPlanLimit;
            this.consumedDataPlan   = consumedDataPlan;
        }

        private RuleSetKeys.SupportedNetworkTypes parseNetworkType(int networkType){
            switch (networkType){
                case NetworkStateProfiler.NetworkTypes.TYPE_WIFI:
                    return RuleSetKeys.SupportedNetworkTypes.NETWORK_WIFI;
                case NetworkStateProfiler.NetworkTypes.TYPE_4G:
                    return RuleSetKeys.SupportedNetworkTypes.NETWORK_4G;
                case NetworkStateProfiler.NetworkTypes.TYPE_3G:
                    return RuleSetKeys.SupportedNetworkTypes.NETWORK_3G;
                case NetworkStateProfiler.NetworkTypes.TYPE_2G:
                    return RuleSetKeys.SupportedNetworkTypes.NETWORK_2G;
                case NetworkStateProfiler.NetworkTypes.TYPE_GPRS:
                    return RuleSetKeys.SupportedNetworkTypes.NETWORK_GPRS;
                default:
                    return RuleSetKeys.SupportedNetworkTypes.NETWORK_UNDEFINED;
            }
        }
    }
}
