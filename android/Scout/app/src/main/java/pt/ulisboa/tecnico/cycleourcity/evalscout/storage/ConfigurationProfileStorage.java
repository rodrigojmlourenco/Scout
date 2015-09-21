package pt.ulisboa.tecnico.cycleourcity.evalscout.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

//FOR EVALUATION PURPOSES ONLY
public class ConfigurationProfileStorage {

    private final File BASE_DIR;
    public final static String BASE_DIR_NAME = "stage";

    private static ConfigurationProfileStorage STORAGE = null;

    private ConfigurationProfileStorage(){
        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString() + "/" + BASE_DIR_NAME);
        if (!BASE_DIR.exists()) BASE_DIR.mkdirs();
    }

    public static ConfigurationProfileStorage getInstance(){
        synchronized (ConfigurationProfileStorage.class){
            if(STORAGE == null)
                STORAGE = new ConfigurationProfileStorage();
        }

        return STORAGE;
    }



    public void storeConfigurationProfile(JsonObject profile, String tag){

        Gson gson = new Gson();
        File output = new File(BASE_DIR, tag+".txt");

        try {
            FileWriter writer = new FileWriter(output);
            writer.write(gson.toJson(profile));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
