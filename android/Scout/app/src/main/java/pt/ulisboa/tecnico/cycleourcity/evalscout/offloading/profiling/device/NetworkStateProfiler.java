package pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.profiling.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.ulisboa.tecnico.cycleourcity.evalscout.ScoutApplication;

public class NetworkStateProfiler extends BroadcastReceiver {

    protected boolean VERBOSE= true; //TODO: remover
    private final String LOG_TAG = this.getClass().getSimpleName();

    public static interface NetworkTypes{
        public static final int
                TYPE_UNKNOWN    = 0,
                TYPE_WIFI       = 1,
                TYPE_BLUETOOTH  = 2,
                TYPE_VPN        = 3,
                TYPE_GPRS       = 4,
                TYPE_2G         = 5,
                TYPE_3G         = 6,
                TYPE_4G         = 7;
    }

    private final Context appContext;
    private final ConnectivityManager connectivityManager;
    private final TelephonyManager telephonyManager;

    //Internal State
    private boolean isConnected;
    private boolean isMobile;
    private int currentNetworkType;


    protected NetworkStateProfiler(Context context){
        appContext = context;

        connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager    = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        appContext.registerReceiver(this, filter);

        runUpdate();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION))
            runUpdate();
    }



    private int getMobileType(int type){
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return NetworkTypes.TYPE_GPRS;
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkTypes.TYPE_2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NetworkTypes.TYPE_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NetworkTypes.TYPE_4G;
            default:
                return NetworkTypes.TYPE_UNKNOWN;
        }
    }


    private String getNetworkTypeAsString(int type){
        switch (type){
            case NetworkTypes.TYPE_UNKNOWN:
                return "unknown";
            case NetworkTypes.TYPE_WIFI:
                return "WiFi";
            case NetworkTypes.TYPE_BLUETOOTH:
                return "Bluetooth";
            case NetworkTypes.TYPE_VPN:
                return "VPN";
            case NetworkTypes.TYPE_GPRS:
                return "GPRS";
            case NetworkTypes.TYPE_2G:
                return "2G";
            case NetworkTypes.TYPE_3G:
                return "3G";
            case NetworkTypes.TYPE_4G:
                return "4G";
            default:
                return "undefined";
        }
    }

    private String getTelTypeAsString(int type){
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    private void runUpdate(){
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if(info!=null && info.isConnected()){ //Check if connected

            isConnected = true;

            if(info.getType() == ConnectivityManager.TYPE_MOBILE) {

                int mobileType = telephonyManager.getNetworkType();
                this.currentNetworkType = getMobileType(mobileType);
                this.isMobile = true;

            }else {

                switch (info.getType()){
                    case ConnectivityManager.TYPE_WIFI:
                        this.currentNetworkType = NetworkTypes.TYPE_WIFI;
                        break;
                    case ConnectivityManager.TYPE_BLUETOOTH:
                        this.currentNetworkType = NetworkTypes.TYPE_BLUETOOTH;
                        break;
                    case ConnectivityManager.TYPE_VPN:
                        this.currentNetworkType = NetworkTypes.TYPE_VPN;
                        break;
                }

                this.isMobile = false;
            }

        }else
            isConnected=false;

        if(VERBOSE) Log.d(DeviceStateProfiler.LOG_TAG, dumpState());
    }

    private String dumpState(){
        DateFormat format = new SimpleDateFormat("hh:mm:ss");
        return "[ "+LOG_TAG+" - "+format.format(new Date())+" ], "+
                (isConnected ? "Connected to" : "Disconnected") +" "+
                (isMobile ? "Mobile" : "")+ getNetworkTypeAsString(currentNetworkType);
    }



    public boolean isConnected(){
        return isConnected;
    }

    public boolean isMobileNetwork(){
        return isMobile;
    }

    public int getCurrentNetworkType() {
        return currentNetworkType;
    }

    public void teardown() {
        ScoutApplication.getContext().unregisterReceiver(this);
    }
}
