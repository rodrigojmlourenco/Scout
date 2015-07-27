package pt.ulisboa.tecnico.cycleourcity.scout.network.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/** http://developer.android.com/training/sync-adapters/creating-authenticator.html#CreateAuthenticatorService */
public class ScoutAuthenticatorService extends Service{

    private ScoutAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new ScoutAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
