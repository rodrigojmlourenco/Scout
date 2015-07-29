package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
@Deprecated
public class NetworkProfiler {

    private MobileDataPlanProfiler dataPlanProfiler;
    private NetworkStateProfiler networkStateProfiler;

    public NetworkProfiler(Context context){
        this.networkStateProfiler   = new NetworkStateProfiler(context);
        this.dataPlanProfiler       = new MobileDataPlanProfiler(context);

        dataPlanProfiler.startMonitoring();

        //Logging
        networkStateProfiler.VERBOSE = true;
    }


    public void teardown(){

        dataPlanProfiler.stopMonitoring();

        networkStateProfiler.teardown();
        dataPlanProfiler.teardown();

    }
}
