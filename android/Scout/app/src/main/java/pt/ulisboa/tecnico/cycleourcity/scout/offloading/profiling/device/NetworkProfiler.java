package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device;

import android.content.Context;

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
