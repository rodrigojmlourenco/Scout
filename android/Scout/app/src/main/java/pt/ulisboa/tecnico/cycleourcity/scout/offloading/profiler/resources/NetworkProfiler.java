package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by rodrigo.jm.lourenco on 29/05/2015.
 */
public class NetworkProfiler extends ResourceProfiler{

    public static enum NetworkType {
        LTE,
        WIFI
    }

    private NetworkType type;
    private float linkSpeed;

    private final ConnectivityManager networkManager;
    private final WifiManager wifiManager;

    public NetworkProfiler(Context context) {
        this.networkManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager    = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }


    @Override
    public void profile() {

        NetworkInfo info = networkManager.getActiveNetworkInfo();
        switch (info.getType()){
            case ConnectivityManager.TYPE_WIFI:
                type = NetworkType.WIFI;
                linkSpeed = wifiManager.getConnectionInfo().getLinkSpeed();
                break;
            case ConnectivityManager.TYPE_MOBILE:
                switch (info.getSubtype()){
                    case 13:
                        type = NetworkType.LTE;
                        break;
                    default:
                        Log.e("???", String.valueOf(info.getSubtype()));
                        break;
                }
                break;
            default:
                Log.e("???", String.valueOf(info.getType()));
        }
    }

    @Override
    public String dumpInfo() {
        String info = "[NetworkProfiler]\n";

        switch (type){
            case WIFI:
                info += "\tNetwork Type: WiFi\n"+
                        "\tLink Speed: "+linkSpeed+"Mbps";
                break;
            case LTE:
                info += "\tNetwork Type: LTE";

        }

        return info;
    }
}
