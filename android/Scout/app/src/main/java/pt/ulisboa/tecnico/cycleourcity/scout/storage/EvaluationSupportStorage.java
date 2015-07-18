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
public class EvaluationSupportStorage {

    public final static String FILE_EXTENTION = ".txt";

    private final File BASE_DIR;
    public final static String BASE_DIR_NAME = "tests";

    //Accelerometer
    public final File ACC_BASE_DIR;
    public final static String ACC_BASE_DIR_NAME = "accelerometer";
    private HashMap<String, FileWriter> accelerometerFileWriters;

    //Pressure
    public final File PRESSURE_BASE_DIR;
    public final static String PRESSURE_BASE_DIR_NAME = "pressure";
    private HashMap<String, FileWriter> pressureFileWriters;


    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy hh:mm:ss");

    private static EvaluationSupportStorage INSTANCE = new EvaluationSupportStorage();


    private EvaluationSupportStorage(){
        this.accelerometerFileWriters = new HashMap<>();
        this.pressureFileWriters = new HashMap<>();

        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString()+"/"+BASE_DIR_NAME);
        if(!BASE_DIR.exists()) BASE_DIR.mkdirs();

        ACC_BASE_DIR = new File(BASE_DIR.toString()+"/"+ACC_BASE_DIR_NAME);
        if(!ACC_BASE_DIR.exists()) ACC_BASE_DIR.mkdirs();

        PRESSURE_BASE_DIR = new File(BASE_DIR.toString()+"/"+PRESSURE_BASE_DIR_NAME);
        if(!PRESSURE_BASE_DIR.exists()) ACC_BASE_DIR.mkdirs();
    }

    public static EvaluationSupportStorage getInstance(){ return  INSTANCE; }

    private String printHeader(String testID){
        return "Test["+testID+"] - "+dateFormat.format(new Date())+"\n\n"+
                "time || x || y || z\n";
    }

    private void registerNewAccelerometerTest(String testID){

        String filename = "test_"+testID+"_"+System.currentTimeMillis()+FILE_EXTENTION;
        File testFile = new File(ACC_BASE_DIR, filename);

        try {
            FileWriter writer = new FileWriter(testFile);
            writer.write(printHeader(testID));
            accelerometerFileWriters.put(testID, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerNewPressureTest(String testID){

        String filename = "test_"+testID+"_"+System.currentTimeMillis()+FILE_EXTENTION;
        File testFile = new File(PRESSURE_BASE_DIR, filename);

        try {
            FileWriter writer = new FileWriter(testFile);
            writer.write(printHeader(testID));
            pressureFileWriters.put(testID, writer);
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

    private String parseSimplePressureSample(JsonObject sample){
        return "";
    }

    private String parseComplexPressureSample(JsonObject sample){
        return "";
    }

    public void storeAccelerometerTestValue(String testID, JsonObject value){

        if(!accelerometerFileWriters.containsKey(testID))
            registerNewAccelerometerTest(testID);

        try {
            accelerometerFileWriters.get(testID).write(parseAccelerometerSample(value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeSimplePressureTestValue(String testID, JsonObject value){

        if(!pressureFileWriters.containsKey(testID))
            registerNewPressureTest(testID);

        try {
            pressureFileWriters.get(testID).write(parseSimplePressureSample(value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeComplexPressureTestValue(String testID, JsonObject value){

        if(!pressureFileWriters.containsKey(testID))
            registerNewPressureTest(testID);

        try {
            pressureFileWriters.get(testID).write(parseComplexPressureSample(value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void teardownAccelerometerTests(){
        for(String testID : accelerometerFileWriters.keySet()){
            try {

                FileWriter writer = accelerometerFileWriters.get(testID);
                writer.flush();
                writer.close();

                accelerometerFileWriters.remove(testID);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void teardownPressureTests(){
        for(String testID : pressureFileWriters.keySet()){
            try {

                FileWriter writer = pressureFileWriters.get(testID);
                writer.flush();
                writer.close();

                pressureFileWriters.remove(testID);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void teardown(){
        teardownAccelerometerTests();
        teardownPressureTests();
    }
}
