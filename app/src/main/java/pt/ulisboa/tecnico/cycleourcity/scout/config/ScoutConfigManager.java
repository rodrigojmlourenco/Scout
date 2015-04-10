package pt.ulisboa.tecnico.cycleourcity.scout.config;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.FunfManager;
import pt.ulisboa.tecnico.cycleourcity.scout.R;
import pt.ulisboa.tecnico.cycleourcity.scout.config.exceptions.NotInitializedException;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;

/**
 * @version 1.0
 * @author rodrigo.jm.lourenco
 *
 */
public class ScoutConfigManager {

    private final static String LOG_TAG = "ConfigManager";

    public final static String PIPELINE_NAME = "default";

    private static ScoutConfigManager CONFIG_MANAGER = new ScoutConfigManager();

    private Context context = null;
    private FunfManager funfManager = null;

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private ScoutConfigManager(){}

    public static ScoutConfigManager getInstance(){ return CONFIG_MANAGER; }

    public void init(Context context, FunfManager manager){
        this.context = context;
        this.funfManager = manager;
    }

    /**
     *
     * @throws NotInitializedException
     */
    public void reloadDefaultConfig() throws NotInitializedException{

        if(funfManager==null) throw new NotInitializedException();

        String defaultConfig = context.getString(R.string.scout_pipeline);
        JsonObject config = new JsonParser().parse(defaultConfig).getAsJsonObject();
        funfManager.saveAndReload(PIPELINE_NAME, config);

        //Logging
        logger.log(ScoutLogger.INFO, LOG_TAG, "Reloading default configuration...");

        //TODO: throw exception
        if(!isCurrentConfig(config))
            logger.log(ScoutLogger.ERR, LOG_TAG, "Failed to reload default "+PIPELINE_NAME+" configuration.");
        else
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, "Default configuration successfully reloaded.");

    }

    public void loadConfig(JsonObject config) throws NotInitializedException {
        if (funfManager == null) throw new NotInitializedException();

        funfManager.saveAndReload(PIPELINE_NAME, config);

        //TODO: throw exception
        if(!isCurrentConfig(config))
            logger.log(ScoutLogger.ERR, LOG_TAG, "Failed to load new configuration.");
        else
            logger.log(ScoutLogger.VERBOSE, LOG_TAG, "New configuration successfully reloaded.");
    }

    public void loadConfig(String config) throws NotInitializedException{
        JsonObject configuration = new JsonParser().parse(config).getAsJsonObject();
        loadConfig(configuration);
    }

    /**
     * Returns the currently active pipeline configuration.
     * @return Active configuration as a JsonObject
     *
     * @see com.google.gson.JsonObject
     */
    public JsonObject getCurrentConfig(){
        return funfManager.getPipelineConfig(PIPELINE_NAME);
    }

    /**
     * Checks if a given configuration is the currently active one.
     * @param config Pipeline configuration
     * @return True if the configuration is active, false otherwise.
     */
    public boolean isCurrentConfig(JsonObject config){
        return config.equals(funfManager.getPipelineConfig(PIPELINE_NAME));
    }
}
