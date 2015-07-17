package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 17/07/2015.
 */
public class GraphValuesStorage {

    public final static String FILE_EXTENTION = ".txt";

    private final File BASE_DIR;
    public final static String BASE_DIR_NAME = "tests";

    public final File ACC_BASE_DIR;
    public final static String ACC_BASE_DIR_NAME = "accelerometer";

    public final File PRESSURE_BASE_DIR;
    public final static String PRESSURE_BASE_DIR_NAME = "pressure";

    private HashMap<String, AccelerometerValues> accelerometerTests;
    private HashMap<String, FileWriter> testFileWriters;

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy hh:mm:ss");

    private static GraphValuesStorage INSTANCE = new GraphValuesStorage();


    private GraphValuesStorage(){
        this.accelerometerTests = new HashMap<>();
        this.testFileWriters = new HashMap<>();

        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString()+"/"+BASE_DIR_NAME);
        if(!BASE_DIR.exists()) BASE_DIR.mkdirs();

        ACC_BASE_DIR = new File(BASE_DIR.toString()+"/"+ACC_BASE_DIR_NAME);
        if(!ACC_BASE_DIR.exists()) ACC_BASE_DIR.mkdirs();

        PRESSURE_BASE_DIR = new File(BASE_DIR.toString()+"/"+PRESSURE_BASE_DIR_NAME);
        if(!PRESSURE_BASE_DIR.exists()) ACC_BASE_DIR.mkdirs();
    }

    public static GraphValuesStorage getInstance(){ return  INSTANCE; }

    private String printHeader(String testID){
        return "Test["+testID+"] - "+dateFormat.format(new Date())+"\n\n"+
                "time || x || y || z\n";
    }

    private void registerNewAccelerometerTest(String testID){

        //accelerometerTests.put(testID, new AccelerometerValues()); DEPRECATED

        String filename = "test_"+testID+"_"+System.currentTimeMillis()+FILE_EXTENTION;
        File testFile = new File(ACC_BASE_DIR, filename);

        try {
            FileWriter writer = new FileWriter(testFile);
            writer.write(printHeader(testID));
            testFileWriters.put(testID, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseAccelerometerSample(JsonObject sample){

        return  sample.get(SensingUtils.SCOUT_TIME).getAsString()+" || "+
                sample.get(SensingUtils.MotionKeys.X).getAsString()+" || "+
                sample.get(SensingUtils.MotionKeys.Y).getAsString()+" || "+
                sample.get(SensingUtils.MotionKeys.Z).getAsString()+"\n";

    }

    public void storeAccelerometerTestValue(String testID, JsonObject value){

        /*if(!accelerometerTests.containsKey(testID))
            registerNewAccelerometerTest(testID);*/

        // accelerometerTests.get(testID).addSample(value); DEPRECATED

        if(!testFileWriters.containsKey(testID))
            registerNewAccelerometerTest(testID);

        try {
            testFileWriters.get(testID).write(parseAccelerometerSample(value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    private String dumpAccelerometerTest(String testID){

        String info = "";

        if(!accelerometerTests.containsKey(testID))
            info += "ERROR: unknown test ["+testID+"]";
        else {
            info += "Test[" + testID + "]\n";
            info += accelerometerTests.get(testID).dumpValues();
        }

        return info;
    }
    */


    public void teardown(){
        for(String testID : testFileWriters.keySet()){
            try {

                FileWriter writer = testFileWriters.get(testID);
                writer.flush();
                writer.close();

                testFileWriters.remove(testID);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    public void clearAllTests(){

        if(!accelerometerTests.isEmpty())
            for (String testID : accelerometerTests.keySet())
                accelerometerTests.get(testID).clear();

    }
    */

    /*
    public void archive(String tag){

        if(!accelerometerTests.isEmpty()){
            String filename;
            File outputFile;
            FileWriter writer;
            for(String testID : accelerometerTests.keySet()) {
                filename = tag + "_" + testID + "_" + System.currentTimeMillis() + FILE_EXTENTION;
                outputFile = new File(ACC_BASE_DIR, filename);

                try {

                    writer = new FileWriter(outputFile);
                    writer.write(dumpAccelerometerTest(testID));

                    writer.flush();
                    writer.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //TODO: pressure sensor test values
    }
    */


    class AccelerometerValues{

        private List<String> timestamps, x, y, z;

        public  AccelerometerValues(){
            timestamps = new ArrayList<>();
            x = new ArrayList<>();
            y = new ArrayList<>();
            z = new ArrayList<>();
        }

        public void addSample(JsonObject sample){
            timestamps.add(sample.get(SensingUtils.SCOUT_TIME).getAsString());
            x.add(sample.get(SensingUtils.MotionKeys.X).getAsString());
            y.add(sample.get(SensingUtils.MotionKeys.Y).getAsString());
            z.add(sample.get(SensingUtils.MotionKeys.Z).getAsString());
        }

        public void clear(){
            this.timestamps.clear();
            this.x.clear();
            this.y.clear();
            this.z.clear();
        }

        public String dumpValues(){

            String info = "============== X ==============\n";
            for(String v : x)
                info += v + "\n";

            info += "============== Y ==============\n";
            for(String v : y)
                info += v + "\n";

            info += "============== Z ==============\n";
            for(String v : z)
                info += v + "\n";

            return info;
        }
    }
}
