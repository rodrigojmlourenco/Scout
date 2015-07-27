package pt.ulisboa.tecnico.cycleourcity.scout.network;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class ScoutSyncService extends Service {

    private static ScoutSyncAdapter scoutSyncAdapter=null;
    private final static Object adapterLock = new Object();

    public ScoutSyncService(){
        super();
    }

    @Override
    public void onCreate() {
        synchronized (adapterLock){
            if(scoutSyncAdapter == null)
                scoutSyncAdapter = new ScoutSyncAdapter(getApplicationContext(), true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return scoutSyncAdapter.getSyncAdapterBinder();
    }


}
