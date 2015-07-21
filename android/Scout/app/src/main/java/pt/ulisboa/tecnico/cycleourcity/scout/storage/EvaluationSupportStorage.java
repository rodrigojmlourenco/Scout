package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 17/07/2015.
 */
public class EvaluationSupportStorage {

    public final static String FILE_EXTENSION = ".txt";

    private final File BASE_DIR;
    public final static String BASE_DIR_NAME = "tests";

    private FileWriter writer;
    private Object writerLock = new Object();

    //Accelerometer
    public final File ACC_BASE_DIR;
    public final static String ACC_BASE_DIR_NAME = "accelerometer";
    private ConcurrentHashMap<String, File> accelerometerFileWriters;


    //Pressure
    public final File PRESSURE_BASE_DIR;
    public final static String PRESSURE_BASE_DIR_NAME = "pressure";
    private ConcurrentHashMap<String, File> pressureFileWriters;

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy hh:mm:ss");

    private static EvaluationSupportStorage INSTANCE = new EvaluationSupportStorage();



    private EvaluationSupportStorage(){
        this.accelerometerFileWriters = new ConcurrentHashMap<>();
        this.pressureFileWriters = new ConcurrentHashMap<>();

        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString()+"/"+BASE_DIR_NAME);
        if(!BASE_DIR.exists()) BASE_DIR.mkdirs();

        ACC_BASE_DIR = new File(BASE_DIR.toString()+"/"+ACC_BASE_DIR_NAME);
        if(!ACC_BASE_DIR.exists()) ACC_BASE_DIR.mkdirs();

        PRESSURE_BASE_DIR = new File(BASE_DIR.toString()+"/"+PRESSURE_BASE_DIR_NAME);
        if(!PRESSURE_BASE_DIR.exists()) PRESSURE_BASE_DIR.mkdirs();
    }

    public static EvaluationSupportStorage getInstance(){ return  INSTANCE; }

    private String printAccelerometerHeader(String testID){
        return "Test["+testID+"] - "+dateFormat.format(new Date())+"\n\n"+
                "time || x || y || z\n";
    }

    private String printSimplePressureHeader(String testID){
        return "Test["+testID+"] - "+dateFormat.format(new Date())+"\n\n"+
                "time || pressure || variance || stdDev || altitude\n";
    }

    private String printComplexPressureHeader(String testID){
        return "Test["+testID+"] - "+dateFormat.format(new Date())+"\n\n"+
                "time || pressure || altitude || distance (delta) || slope\n";
    }

    private void registerNewAccelerometerTest(String testID){

        String filename = "test_"+testID+"_"+System.currentTimeMillis()+ FILE_EXTENSION;
        File testFile = new File(ACC_BASE_DIR, filename);

        try {
            synchronized (writerLock) {
                writer = new FileWriter(testFile);
                writer.write(printAccelerometerHeader(testID));
                writer.close();
                accelerometerFileWriters.put(testID, testFile);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerNewPressureTest(String testID, boolean isComplex){

        String filename = "test_"+testID+"_"+System.currentTimeMillis()+ FILE_EXTENSION;
        File testFile = new File(PRESSURE_BASE_DIR, filename);

        try {

            synchronized (writerLock) {
                writer = new FileWriter(testFile);

                if (isComplex)
                    writer.write(printComplexPressureHeader(printAccelerometerHeader(testID)));
                else writer.write(printSimplePressureHeader(testID));

                writer.close();
            }

            pressureFileWriters.put(testID, testFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseAccelerometerSample(JsonObject sample){

        return  sample.get(SensingUtils.GeneralFields.SCOUT_TIME).getAsString()+" || "+
                sample.get(SensingUtils.MotionKeys.X).getAsString()+" || "+
                sample.get(SensingUtils.MotionKeys.Y).getAsString()+" || "+
                sample.get(SensingUtils.MotionKeys.Z).getAsString()+"\n";

    }

    private String parseSimplePressureSample(JsonObject sample){

        return  sample.get(SensingUtils.GeneralFields.SCOUT_TIME).getAsString()+" || "+
                sample.get(SensingUtils.PressureKeys.PRESSURE).getAsString()+" || "+
                (sample.has(SensingUtils.PressureKeys.VARIANCE) ?
                        sample.get(SensingUtils.PressureKeys.VARIANCE).getAsString(): "n/a")+" || "+
                (sample.has(SensingUtils.PressureKeys.STDEV) ?
                        sample.get(SensingUtils.PressureKeys.STDEV).getAsString(): "n/a")+ " || " +
                (sample.has(SensingUtils.PressureKeys.ALTITUDE) ?
                        sample.get(SensingUtils.PressureKeys.ALTITUDE).getAsString(): "n/a")+ "\n";
    }

    private String parseComplexPressureSample(JsonObject sample){

        return  sample.get(SensingUtils.GeneralFields.SCOUT_TIME).getAsString()+" || "+
                sample.get(SensingUtils.PressureKeys.PRESSURE).getAsString()+" || "+
                sample.get(SensingUtils.PressureKeys.ALTITUDE).getAsString()+" || "+
                (sample.has(SensingUtils.PressureKeys.TRAVELLED_DISTANCE) ?
                        sample.get(SensingUtils.PressureKeys.TRAVELLED_DISTANCE).getAsString(): "n/a")+ " || " +
                (sample.has(SensingUtils.PressureKeys.SLOPE) ?
                        sample.get(SensingUtils.PressureKeys.SLOPE).getAsString(): "n/a")+ "\n";
    }

    public void storeAccelerometerTestValue(String testID, JsonObject value){

        synchronized (writerLock) {
            if(!accelerometerFileWriters.containsKey(testID))
                registerNewAccelerometerTest(testID);

            try {

                writer = new FileWriter(accelerometerFileWriters.get(testID), true);
                writer.write(parseAccelerometerSample(value));
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void storeSimplePressureTestValue(String testID, JsonObject value){
        if(!pressureFileWriters.containsKey(testID))
            registerNewPressureTest(testID, false);

        try {
            synchronized (writerLock) {
                writer = new FileWriter(pressureFileWriters.get(testID), true);
                writer.write(parseSimplePressureSample(value));
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeComplexPressureTestValue(String testID, JsonObject value){

        if (!pressureFileWriters.containsKey(testID))
            registerNewPressureTest(testID, true);

        try {
            synchronized (writerLock) {
                writer = new FileWriter(pressureFileWriters.get(testID), true);
                writer.write(parseComplexPressureSample(value));
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void teardownAccelerometerTests(){
        synchronized (writerLock) {
            for (String testID : accelerometerFileWriters.keySet()) {
                accelerometerFileWriters.remove(testID);
            }
        }
    }

    private void teardownPressureTests(){
        synchronized (writerLock) {
            for (String testID : pressureFileWriters.keySet()) {
                pressureFileWriters.remove(testID);
            }
        }
    }

    public void teardown(){
        teardownAccelerometerTests();
        teardownPressureTests();
    }
}
