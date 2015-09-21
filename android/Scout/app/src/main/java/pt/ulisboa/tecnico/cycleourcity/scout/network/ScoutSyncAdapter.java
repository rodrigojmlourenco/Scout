package pt.ulisboa.tecnico.cycleourcity.scout.network;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import pt.ulisboa.tecnico.cycleourcity.scout.storage.provider.ScoutProvider;

@Deprecated
public class ScoutSyncAdapter extends AbstractThreadedSyncAdapter{


    ContentResolver scoutContentResolver;
    private final ScoutRemoteClient remoteClient;

    public ScoutSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        scoutContentResolver = context.getContentResolver();

        remoteClient = ScoutRemoteClient.getInstance();
    }

    public ScoutSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {

        super(context, autoInitialize, allowParallelSyncs);

        scoutContentResolver = context.getContentResolver();

        remoteClient = ScoutRemoteClient.getInstance();
    }

    private String cursorToString(Cursor c){

        int columns = c.getColumnCount();
        String result = "";
        for(int i=0; i< columns; i++ ){
            result += c.getColumnName(i) + " : " + c.getString(i)+" ";
        }

        return result;
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {



        /* 1. Query the results, and convert them to a JsonArray
        Cursor cursor = scoutContentResolver.query(ScoutProvider.CONTENT_URI, null, null, null, null);

        while (cursor.moveToNext()){
            Log.d("test", "name: " + cursorToString(cursor));
        }

        cursor.close();
        */

        /* 2. Upload the results by publishing them
        remoteClient.publishProcessedSamples()
        */


        // 3. Clear the local storage
    }
}
