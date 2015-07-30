package pt.ulisboa.tecnico.cycleourcity.scout;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.ScoutCalibrationManager;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.SensorCalibrator;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.exceptions.NotYetCalibratedException;
import pt.ulisboa.tecnico.cycleourcity.scout.config.ScoutConfigManager;
import pt.ulisboa.tecnico.cycleourcity.scout.config.exceptions.NotInitializedException;
import pt.ulisboa.tecnico.cycleourcity.scout.learning.PavementType;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.network.ScoutRemoteClient;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.exceptions.OverearlyOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NoAdaptivePipelineValidatedException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.exceptions.NothingToOffloadException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.ScoutProfiling;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.Rule;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.RuleSetManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.provider.ScoutProvider;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.provider.ScoutProviderObserver;

public class MainActivity extends ActionBarActivity {

    private final String LOG_TAG = "MainActivity";


    //UI
    private Button startSession, stopSession, saveSession;
    private EditText tagText;

    private Button offloadBtn; //testing
    private Button netTestBtn;

    //Pavement Type
    private RadioGroup pavementTypeGroup;


    //Funf
    public static final String PIPELINE_NAME = "default";
    private FunfManager funfManager;
    private ScoutPipeline pipeline;

    //Configuration Management
    private ScoutConfigManager configManager = ScoutConfigManager.getInstance();

    //Logging
    private ScoutLogger logger = ScoutLogger.getInstance();

    //Adaptive Offloading
    private AdaptiveOffloadingManager offloadingManager;


    private boolean isSensing = false;


    //BEGIN TESTING - SyncAdapters
    Account mAccount;
    ContentResolver mContentResolver;
    public static final String ACCOUNT = "scout";
    //END TESTING - SyncAdapters

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
                            isSensing = true;
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
                            isSensing = false;
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


        onInitChecks();

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

        //Pavement Type Group
        final PavementType pavementType = PavementType.getInstance();
        pavementTypeGroup = (RadioGroup) findViewById(R.id.pavementTypeGroup);
        pavementTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                PavementType.Pavements pavement;

                switch (checkedId) {
                    case R.id.isAsphalt:
                        pavement = PavementType.Pavements.asphalt;
                        break;
                    case R.id.isCobblestone:
                        pavement = PavementType.Pavements.cobblestone;
                        break;
                    case R.id.isGravel:
                        pavement = PavementType.Pavements.gravel;
                        break;
                    default:
                        pavement = PavementType.Pavements.undefined;
                }

                pavementType.setPavementType(pavement);
            }
        });

        //BEGIN TESTING
        offloadBtn = (Button) findViewById(R.id.offloadBtn);
        offloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean error = false;
                String errorMessage = "";
                try {
                    offloadingManager.forceOffloading();
                    error = false;
                } catch (NothingToOffloadException | NoAdaptivePipelineValidatedException | OverearlyOffloadException e) {
                    errorMessage = e.getMessage();
                    error = true;
                }

                if (error)
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

            }
        });
        //END TESTING

        tagText = (EditText) findViewById(R.id.tag);




        //Profiling
        try {
            offloadingManager = AdaptiveOffloadingManager.getInstance(getApplicationContext());
        } catch (InvalidRuleSetException e) {
            e.printStackTrace();
        }

        //BEGIN TESTING - SyncAdapters
        mAccount = CreateSyncAccount(this);
        mContentResolver = getContentResolver();
        ScoutProviderObserver observer = new ScoutProviderObserver(mAccount);
        mContentResolver.registerContentObserver(ScoutProvider.CONTENT_URI, true, observer);

        ContentResolver.setIsSyncable(mAccount, ScoutProvider.AUTHORITY, 1);
        //TODO: solve as this invalidates the performance improvements inherent from the use of SyncAdapters
        ContentResolver.setSyncAutomatically(mAccount, ScoutProvider.AUTHORITY, true);

        /* This method actually works
        ContentResolver.addPeriodicSync(
                mAccount,
                ScoutProvider.AUTHORITY,
                Bundle.EMPTY,
                1); //Minutos
       */

        //END TESTING - SyncAdapters


        // Bind to the service, to create the connection with FunfManager
        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        onInitChecks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(isSensing){
            Toast.makeText(MainActivity.this,
                    "Please terminate the sensing session first.", Toast.LENGTH_SHORT).show();

            return true;
        }

        Intent intent;
        switch (id){
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_calibrate:
                intent = new Intent(this, CalibrateActivity.class);
                startActivity(intent);
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

        offloadingManager.onDestroy();
    }

    //BEGIN TESTING - SyncAdapters
    public static Account CreateSyncAccount(Context context){
        Account account = new Account(ACCOUNT, ScoutProvider.ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

        if(accountManager.addAccountExplicitly(account, null, null))
            return account;
        else {
            Account[] accounts = accountManager.getAccountsByType(ScoutProvider.ACCOUNT_TYPE);
            if(accounts.length == 1)
                return accounts[0];
            else
                throw new RuntimeException("There are "+accounts.length+" registered.");
        }


    }
    //END TESTING - SyncAdapters


    /*
     ********************************************************
     *  Overall Settings Check                              *
     ********************************************************
     */
    private boolean hasDataPlanSettings(){
        SharedPreferences profPrefs = getSharedPreferences(ScoutProfiling.PREFERENCES,Context.MODE_PRIVATE);

        if(profPrefs.getString(ScoutProfiling.DATA_PLAN_PREFS,null)!=null)
            return true;
        else
            return false;
    }

    private void onInitChecks(){

        boolean hasDataPlan = hasDataPlanSettings();

        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        if(!hasDataPlan) settingsIntent.putExtra(SettingsActivity.SettingsExtras.EXTRA_DATA_PLAN, true);

        if(!hasDataPlan)
            startActivity(settingsIntent);

        //Check if calibrated
        boolean isCalibrated = true;
        try {
            ScoutCalibrationManager.initScoutCalibrationManager(
                    getSharedPreferences(SensorCalibrator.PREFERENCES_NAME,MODE_PRIVATE));
        } catch (NotYetCalibratedException e) {
            Log.e(getClass().getSimpleName(), "The application must be calibrated.");
            isCalibrated = false;
        }

        if(!isCalibrated) {
            Intent intent = new Intent(this, CalibrateActivity.class);
            startActivity(intent);
        }
    }
}
