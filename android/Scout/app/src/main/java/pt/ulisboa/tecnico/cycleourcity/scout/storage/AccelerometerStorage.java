package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.ScoutApplication;

/**
 * Created by rodrigo.jm.lourenco on 30/06/2015.
 */
public class AccelerometerStorage {

    final String type;
    List<Float> x, y, z;

    public final static String NATIVE = "native";
    public final static String PROJECTED = "projected";

    public AccelerometerStorage(String type){
        this.type = type;
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
    }

    public void storeValues(float[] values){
        x.add(values[0]);
        y.add(values[1]);
        z.add(values[2]);
    }

    public String dump(){
        String info = "============== X ==============\n";
        for(Float v : x)
            info += v + "\n";

        info += "============== Y ==============\n";
        for(Float v : y)
            info += v + "\n";

        info += "============== Z ==============\n";
        for(Float v : z)
            info += v + "\n";

        return info;
    }

    public void clear(){
        x.clear();
        y.clear();
        z.clear();
    }

    /*
    public void exportAsFile(String tag){
        String data = dump();
        Context context = ScoutApplication.getContext();
        clear();

        Log.d("AccStore", String.valueOf(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)));

        try {
            File out = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), type + tag + "_" + System.nanoTime() + ".txt");
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    */
}
