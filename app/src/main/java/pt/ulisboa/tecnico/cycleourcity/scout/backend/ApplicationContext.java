package pt.ulisboa.tecnico.cycleourcity.scout.backend;

import android.app.Application;
import android.content.Context;

/**
 * Created by rodrigo.jm.lourenco on 11/03/2015.
 *
 * ApplicationContext is designed to allow access to the application's context, outside of the
 * application's activity flow.
 *
 * WARNING: in order for this to work the following must be added to the AndroidManifest.xml file:
 * <pre>
 * {@code
 * <application
 *      ...
 *      android:name=".backend.ApplicationContext>
 *      ...
 * </application>
 * }
 * </pre>
 */
public class ApplicationContext extends Application {

    private static ApplicationContext instance;

    public ApplicationContext(){
        instance = this;
    }

    public static Context getContext(){
        return  instance;
    }
}
