package pt.ulisboa.tecnico.cycleourcity.scout;

import android.app.Application;
import android.content.Context;

/**
 * Created by rodrigo.jm.lourenco on 30/03/2015.
 */
public class ScoutApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        ScoutApplication.context = getApplicationContext();
    }

    public static Context getContext(){
        return ScoutApplication.context;
    }
}
