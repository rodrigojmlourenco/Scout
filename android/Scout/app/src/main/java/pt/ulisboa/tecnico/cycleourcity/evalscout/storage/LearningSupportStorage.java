package pt.ulisboa.tecnico.cycleourcity.evalscout.storage;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * Created by rodrigo.jm.lourenco on 17/07/2015.
 */
public class LearningSupportStorage {

    //WEKA
    private final static String RELATION = "pavement";
    public final static String CLASS = "CLASS";

    public final static String FILE_EXTENTION = ".txt";

    private final File BASE_DIR;
    public final static String BASE_DIR_NAME = "weka";


    //Storage
    private File output;
    private FileWriter writer;

    private static LearningSupportStorage INSTANCE = new LearningSupportStorage();

    private LearningSupportStorage() {

        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString() + "/" + BASE_DIR_NAME);
        if (!BASE_DIR.exists()) BASE_DIR.mkdirs();

    }

    public static LearningSupportStorage getInstance(){return INSTANCE;}

    private String dumpHeader() {
        String header = "";

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        header += "% 1. Title Road Condition Monitoring\n";
        header += "%\n";
        header += "% 2. Source:\n";
        header += "%\tData gathered through cycling, using an Android smartphone device.\n";
        header += "%\n";
        header += "% 3. Date: " + dateFormat.format(date)+"\n";
        header += "@RELATION " + RELATION + "\n";
        header += "\n";
        header += "@ATTRIBUTE mean NUMERIC\n";
        header += "@ATTRIBUTE median NUMERIC\n";
        header += "@ATTRIBUTE variance NUMERIC\n";
        header += "@ATTRIBUTE stdDev NUMERIC\n";
        header += "@ATTRIBUTE range NUMERIC\n";
        header += "@ATTRIBUTE max NUMERIC\n";
        header += "@ATTRIBUTE min NUMERIC\n";
        header += "@ATTRIBUTE rms NUMERIC\n";
        header += "@ATTRIBUTE zeroCrossings NUMERIC\n";
        header += "@ATTRIBUTE meanCrossings NUMERIC\n";
        header += "@ATTRIBUTE medianCrossings NUMERIC\n";
        header += "@ATTRIBUTE rangeCrossings NUMERIC\n";
        header += "@ATTRIBUTE numSamples NUMERIC\n";
        header += "@ATTRIBUTE speed NUMERIC\n";
        header += "@ATTRIBUTE class { asphalt, cobblestone, gravel }\n";
        header += "\n";
        header += "@DATA\n";

        return header;
    }

    public void prepareStorage(){
        try{
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){}

        String filename = "weka_" + System.currentTimeMillis() + FILE_EXTENTION;
        File outputFile = new File(BASE_DIR, filename);

        try {

            writer = new FileWriter(outputFile);
            writer.write(dumpHeader());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void storeFeatureVector(JsonObject featureVector) {

        String dataLine = "";

        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.MEAN).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.MEDIAN).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.VARIANCE).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.STDEV).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.RANGE).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.MAX).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.MIN).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.RMS).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.ZERO_CROSSING).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.MEAN_CROSSING).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.MEDIAN_CROSSING).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.FeatureVectorKeys.RANGE_CROSSING).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.MotionKeys.SAMPLES).getAsString() + ",";
        dataLine += featureVector.get(SensingUtils.LocationKeys.SPEED).getAsString() + ",";
        dataLine += featureVector.get(CLASS).getAsString()+"\n";

        try {
            writer.write(dataLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void teardown(){
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NullPointerException e){}
    }
}

