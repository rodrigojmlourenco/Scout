package pt.ulisboa.tecnico.cycleourcity.scout.storage.provider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class ScoutProviderObserver extends ContentObserver {

    private Account account;

    public ScoutProviderObserver(Account account) {
        super(null); //TODO: should be receiving an handler
        this.account = account;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d("CHANGE", "change");
        ContentResolver.requestSync(account, ScoutProvider.AUTHORITY, new Bundle());
    }
}
