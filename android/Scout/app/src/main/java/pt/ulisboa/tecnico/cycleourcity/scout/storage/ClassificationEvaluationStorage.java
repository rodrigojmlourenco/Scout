package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import pt.ulisboa.tecnico.cycleourcity.scout.classification.PavementType;

/**
 * Created by rodrigo.jm.lourenco on 24/08/2015.
 */
public class ClassificationEvaluationStorage {

    private static final String FILE_EXTENTION = ".txt";
    private final File BASE_DIR;
    private final String BASE_DIR_NAME = "classifications";
    private PavementType humanClassification;
    private HashMap<String, ClassificationInstance> classificationInstanceMap;

    //Sync
    private final Object lock = new Object();

    protected ClassificationEvaluationStorage(){
        humanClassification = PavementType.getInstance();
        this.classificationInstanceMap = new HashMap<>();

        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString() + "/" + BASE_DIR_NAME);
        if (!BASE_DIR.exists()) BASE_DIR.mkdirs();
    }

    private static ClassificationEvaluationStorage INSTANCE;

    public static ClassificationEvaluationStorage getInstance(){
        synchronized (ClassificationInstance.class){
            if(INSTANCE == null)
                INSTANCE = new ClassificationEvaluationStorage();
        }

        return INSTANCE;
    }

    private void registerClassifier(String classifier){
        classificationInstanceMap.put(classifier, new ClassificationInstance(classifier));
    }

    public void registerClassification(String classifier, String pavement){

        synchronized (lock) {
            if (!classificationInstanceMap.containsKey(classifier))
                registerClassifier(classifier);

            //Ignore the following instances of pavement types
            if(pavement.equals(String.valueOf(PavementType.Pavements.GravelBad))
                || pavement.equals(String.valueOf(PavementType.Pavements.GravelGood))
                || pavement.equals(String.valueOf(PavementType.Pavements.undefined)))
                return;

            if (humanClassification.getPavementType().equals(pavement))
                classificationInstanceMap.get(classifier).registerTruePositive();
            else
                classificationInstanceMap.get(classifier).registerFalseNegative();
        }
    }

    public void export(){

        if(classificationInstanceMap.isEmpty()) return; //Avoid NullPointerException

        FileWriter writer;
        String filename = "classification_"+ + System.currentTimeMillis() + FILE_EXTENTION;
        File outputFile = new File(BASE_DIR, filename);

        try {
            writer = new FileWriter(outputFile);

            synchronized (lock) {
                for (String key : classificationInstanceMap.keySet())
                    writer.write(classificationInstanceMap.get(key).dumpInfo());
            }

            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClassificationInstance {
        private String name;
        private int classifications, falseNegatives, truePositives;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

        public ClassificationInstance(String name){
            this.name = name;
            classifications = 0;
            falseNegatives = 0;
            truePositives   = 0;
        }

        public void registerTruePositive(){
            classifications ++;
            truePositives++;
        }

        public void registerFalseNegative(){
            classifications++;
            falseNegatives++;
        }

        public String getName(){return  this.name; }

        private float getPrecision(){
            return truePositives/classifications;
        }

        private float getFNRate(){
            return falseNegatives /classifications;
        }

        public String dumpHeader(){
            return name + " - " + dateFormat.format(new Date())+"\n\n"+
                    "#Classifications | Precision | FN Rate"+"\n";
        }

        public String dumpInfo(){
            return  dumpHeader()+
                    classifications + " | " + getPrecision() + " | " + getFNRate()+"\n";
        }
    }
}
