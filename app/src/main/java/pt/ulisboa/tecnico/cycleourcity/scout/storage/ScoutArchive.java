package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import android.os.Environment;
import android.util.Log;

import java.io.File;

import edu.mit.media.funf.storage.FileArchive;

/**
 * Created by rodrigo.jm.lourenco on 31/03/2015.
 */
public class ScoutArchive implements FileArchive {

    private final static String NAME = "Scout";
    private final static String LOG_TAG = "ScoutArchive";

    private File getScoutStorageDir() {
        File file = new File(Environment.getExternalStorageDirectory(), NAME);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }

    @Override
    public boolean add(File file) {
        return false;
    }

    @Override
    public boolean remove(File file) {
        return false;
    }

    @Override
    public boolean contains(File file) {
        return false;
    }

    @Override
    public File[] getAll() {
        return new File[0];
    }
}
