package pt.ulisboa.tecnico.cycleourcity.scout;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.config.ScoutConfigManager;
import pt.ulisboa.tecnico.cycleourcity.scout.config.exceptions.NotInitializedException;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensingPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline;

public class MainActivity extends ActionBarActivity {

    private final String LOG_TAG = "MainActivity";


    //UI
    private Button startSession, stopSession, saveSession;
    private EditText tagText;

    private TextView
            locationView,
            speedView,
            slopeView,
            altitudeView,
            travelStateView;

    //Funf
    public static final String PIPELINE_NAME = "default";
    private FunfManager funfManager;
    private ScoutPipeline pipeline;

    //Backgound UI updating
    private Timer timer;
    private TimerTask uiUpdater;
    private ScoutStateUpdaterTask updateStateTask;

    //Configuration Management
    private ScoutConfigManager configManager = ScoutConfigManager.getInstance();

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            funfManager = ((FunfManager.LocalBinder)service).getManager();
            pipeline = (ScoutPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            //Initialize Configuration Manager
            // & reload default configuration
            configManager.init(MainActivity.this, funfManager);
            try {
                configManager.reloadDefaultConfig();
                //configManager.loadConfig(ScoutConfigManager.ONLY_LOCATION_CONFIG); //TESTING
                Log.w("CONFIG", String.valueOf(configManager.getCurrentConfig()));
            } catch (NotInitializedException e) {
                e.printStackTrace();
            }

            //Pipeline starts as disabled
            funfManager.disablePipeline(PIPELINE_NAME);

            startSession.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast.makeText(MainActivity.this, "Starting sensing session...", Toast.LENGTH_SHORT).show();

                    if (!pipeline.isEnabled()) {
                        funfManager.enablePipeline(PIPELINE_NAME);
                        pipeline = (ScoutPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

                        if(pipeline.isEnabled()) {
                            startSession.setEnabled(false);
                            stopSession.setEnabled(true);
                        }else
                            Toast.makeText(MainActivity.this, "Unable to start sensing pipeline.", Toast.LENGTH_SHORT).show();

                    }else
                        Toast.makeText(MainActivity.this, "Pipeline is already enabled", Toast.LENGTH_SHORT).show();

                }
            });

            stopSession.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast.makeText(MainActivity.this, "Stopping sensing session!", Toast.LENGTH_SHORT).show();

                    if(pipeline.isEnabled()) {
                        funfManager.disablePipeline(PIPELINE_NAME);

                        if(!pipeline.isEnabled()) {
                            startSession.setEnabled(true);
                            stopSession.setEnabled(false);
                        }else
                            Toast.makeText(MainActivity.this, "Unable to stop sensing pipeline.", Toast.LENGTH_SHORT).show();

                    }else
                        Toast.makeText(MainActivity.this, "Pipeline is not enabled", Toast.LENGTH_SHORT).show();
                }
            });


            // Set UI ready to use, by enabling buttons
            startSession.setEnabled(true);
            stopSession.setEnabled(true);
            saveSession.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Runs a save action if pipeline is enabled
        saveSession = (Button) findViewById(R.id.archive);
        saveSession.setEnabled(false);
        saveSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String tag = tagText.getText().toString();
                Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");
                Matcher m = pattern.matcher(tag);

                if(tag.length()<=10 && m.matches()) {
                    logger.log(ScoutLogger.DEBUG, LOG_TAG, tag);
                    pipeline.setSamplingTag(tag);
                    pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                }else{
                    Toast.makeText(MainActivity.this,
                            "Only characters are acceptable\n(max length 10 chars).",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Forces the pipeline to scan now
        startSession = (Button) findViewById(R.id.startSession);
        stopSession = (Button) findViewById(R.id.stopSession);
        startSession.setEnabled(false);
        stopSession.setEnabled(false);

        tagText = (EditText) findViewById(R.id.tag);

        //Background UI updating
        locationView = (TextView) findViewById(R.id.locationValue);
        speedView = (TextView) findViewById(R.id.speedValue);
        slopeView = (TextView) findViewById(R.id.slopeValue);
        altitudeView = (TextView) findViewById(R.id.altitudeValue);
        travelStateView = (TextView) findViewById(R.id.travelStateValue);

        //TODO: optimizar
        timer = new Timer();
        uiUpdater = new TimerTask() {
            @Override
            public void run() {
                new ScoutStateUpdaterTask().execute();
            }
        };
        timer.scheduleAtFixedRate(uiUpdater, 17000, MobileSensingPipeline.WINDOW_SIZE);


        // Bind to the service, to create the connection with FunfManager
        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MAIN", "Disabling pipeline, and unbinding service connection.");
        funfManager.disablePipeline(PIPELINE_NAME);
        unbindService(funfManagerConn);

        //TODO//Cancel UI Threads
    }

    private void publishScoutState(String travelState){
        ScoutState state = ScoutState.getInstance();
        travelStateView.setText(state.getMotionState().getTravelState());
    }


    /*********************************************************************************
     *
     *********************************************************************************/
    private class ScoutStateUpdaterTask extends AsyncTask{

        private ScoutState scoutState = ScoutState.getInstance();
        private Geocoder geocoder = new Geocoder(ScoutApplication.getContext());

        @Override
        protected Object doInBackground(Object[] params) {

            travelStateView.post(new Runnable() {
                @Override
                public void run() {

                    double lat, lon;
                    lat = scoutState.getLocationState().getLatitude();
                    lon = scoutState.getLocationState().getLongitude();

                    locationView.setText("Lat:"+lat+"; Lon:"+lon);
                    travelStateView.setText(scoutState.getMotionState().getTravelState());
                    speedView.setText(String.valueOf(scoutState.getMotionState().getSpeed()));
                    slopeView.setText(String.valueOf(scoutState.getLocationState().getSlope()));
                    altitudeView.setText(String.valueOf(scoutState.getLocationState().getAltitude()));
                }
            });

            return null;
        }
    }
}
