package pt.ulisboa.tecnico.cycleourcity.scout;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
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

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.config.ScoutConfigManager;
import pt.ulisboa.tecnico.cycleourcity.scout.config.exceptions.NotInitializedException;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data.Location;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline;

public class MainActivity extends ActionBarActivity {

    private final String LOG_TAG = "MainActivity";


    //UI
    private Button startSession, stopSession, saveSession;
    private EditText tagText;

    private TextView
            locationView,
            speedView;
            //slopeView,
            //altitudeView,
            //travelStateView;

    //Plotting
    private XYPlot elevationPlot;
    private SimpleXYSeries gpsElevationSeries = null, meanElevationSeries, pressureElevationSeries;



    //Funf
    public static final String PIPELINE_NAME = "default";
    private FunfManager funfManager;
    private ScoutPipeline pipeline;

    //Configuration Management
    private ScoutConfigManager configManager = ScoutConfigManager.getInstance();

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            funfManager = ((FunfManager.LocalBinder)service).getManager();
            pipeline = (ScoutPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            ((ScoutPipeline)pipeline).setDisplay(getWindowManager().getDefaultDisplay());

            //Initialize Configuration Manager
            // & reload default configuration
            configManager.init(MainActivity.this, funfManager);
            try {
                configManager.reloadDefaultConfig();
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
                            startRepeatingTask();
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
                            stopRepeatingTask();
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
        mHandler = new Handler();
        locationView = (TextView) findViewById(R.id.locationValue);
        speedView = (TextView) findViewById(R.id.speedValue);
        //slopeView = (TextView) findViewById(R.id.slopeValue);
        //altitudeView = (TextView) findViewById(R.id.altitudeValue);
        //travelStateView = (TextView) findViewById(R.id.travelStateValue);

        //Plotting
        elevationPlot = (XYPlot) findViewById(R.id.elevationPlot);

        gpsElevationSeries = new SimpleXYSeries("GPS Altitude");
        gpsElevationSeries.useImplicitXVals(); //Maybe not (Use index value as xVal, instead of explicit, user provided xVals.)
        meanElevationSeries = new SimpleXYSeries("Scout Mean Altitude");
        meanElevationSeries.useImplicitXVals();
        pressureElevationSeries = new SimpleXYSeries("Scout Pressure Altitude");
        pressureElevationSeries.useImplicitXVals();

        elevationPlot.setRangeBoundaries(100, 300, BoundaryMode.AUTO);
        elevationPlot.setDomainBoundaries(0, 60, BoundaryMode.FIXED);

        elevationPlot.addSeries(gpsElevationSeries, new LineAndPointFormatter(Color.BLUE, Color.TRANSPARENT, Color.TRANSPARENT, null));
        elevationPlot.addSeries(meanElevationSeries, new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null));
        elevationPlot.addSeries(pressureElevationSeries, new LineAndPointFormatter(Color.YELLOW, Color.TRANSPARENT, Color.TRANSPARENT, null));





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

        stopRepeatingTask();

    }

    /**********************************************************************************************
     * UI update Async
     **********************************************************************************************/
    private int mInterval = 1000; //millis
    private Handler mHandler;

    Runnable uiUpdate = new Runnable() {

        private ScoutState scoutState = ScoutState.getInstance();
        private Geocoder geoDecoder = new Geocoder(ScoutApplication.getContext());

        @Override
        public void run() {

            String address = "Unknown Location";
            double lat, lon;
            lat = scoutState.getLocationState().getLatitude();
            lon = scoutState.getLocationState().getLongitude();


            try {
                List<Address> addresses = geoDecoder.getFromLocation(lat, lon, 1);

                if(!addresses.isEmpty())

                    address = addresses.get(0).getThoroughfare();

            } catch (IOException e) {
                e.printStackTrace();
            }


            locationView.setText(address);
            speedView.setText(String.valueOf(scoutState.getLocationState().getPressureAltitude()));

            //travelStateView.setText(scoutState.getMotionState().getTravelState());
            //slopeView.setText(String.valueOf(scoutState.getLocationState().getSlope()));
            //altitudeView.setText(String.valueOf(scoutState.getLocationState().getAltitude()));

            Location last = scoutState.getLocationState().getLastLocation();
            if(scoutState.getLocationState().isReadyState()) {
                Date date = new Date(last.getTimestamp());

                Log.w("PLOT", date.toGMTString()+" : "+last.getAltitude());
                Log.e("PLOT", date.toGMTString()+" : "+scoutState.getLocationState().getAverageAltitude());

                if(pressureElevationSeries.size() > 60){
                    gpsElevationSeries.removeFirst();
                    meanElevationSeries.removeFirst();
                    pressureElevationSeries.removeFirst();
                }

                gpsElevationSeries.addLast(null, last.getAltitude());
                meanElevationSeries.addLast(null, scoutState.getLocationState().getAverageAltitude());
                pressureElevationSeries.addLast(null, scoutState.getLocationState().getPressureAltitude());

                elevationPlot.redraw();
            }else{
                if(pressureElevationSeries.size() > 60){
                    pressureElevationSeries.removeFirst();
                }

                pressureElevationSeries.addLast(null, scoutState.getLocationState().getPressureAltitude());

                elevationPlot.redraw();
            }

            mHandler.postDelayed(this, mInterval);
        }
    };

    void startRepeatingTask(){
        mHandler.postDelayed(uiUpdate,mInterval);
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(uiUpdate);
    }
}
