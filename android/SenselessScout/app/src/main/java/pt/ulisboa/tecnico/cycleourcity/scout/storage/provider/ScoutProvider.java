package pt.ulisboa.tecnico.cycleourcity.scout.storage.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.archive.ScoutArchive;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class ScoutProvider extends ContentProvider {


    public final static int DB_VERSION      = 1;
    public final static String MAIN_TABLE   = "ScoutContentProvider";
    public static final String SCHEME       = "content://";
    public static final String AUTHORITY    = "pt.ulisboa.tecnico.cycleourcity.scout.storage.provider";
    public static final String ACCOUNT_TYPE = "scout.cycleourcity.tecnico.ulisboa.pt";
    public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/" + MAIN_TABLE);


    public static final String JSON_TYPE = "application/json";


    private Context ctx;
    private ScoutArchive archive;
    private SQLiteDatabase database;
    private NameValueDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new NameValueDatabaseHelper(getContext(), MAIN_TABLE, DB_VERSION);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(NameValueDatabaseHelper.DATA_TABLE.name);

        Cursor cursor =
                queryBuilder.query(
                        dbHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return JSON_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        //if(dbHelper == null) dbHelper = new NameValueDatabaseHelper(getContext(), MAIN_TABLE, DB_VERSION);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insert(NameValueDatabaseHelper.DATA_TABLE.name, null, values);

        getContext().getContentResolver().notifyChange(CONTENT_URI,null);

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int rows = 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        rows = db.delete(NameValueDatabaseHelper.DATA_TABLE.name, null,null);

        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
