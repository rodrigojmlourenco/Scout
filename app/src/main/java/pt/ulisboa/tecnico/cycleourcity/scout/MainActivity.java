package pt.ulisboa.tecnico.cycleourcity.scout;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.BasicPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline;

public class MainActivity extends ActionBarActivity {

    private Handler handler;

    //UI
    private Button startSession, stopSession, saveSession;

    //Funf
    public static final String PIPELINE_NAME = "default";
    private FunfManager funfManager;
    private ScoutPipeline pipeline;

    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            funfManager = ((FunfManager.LocalBinder)service).getManager();
            pipeline = (ScoutPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

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

        // Used to make interface changes on main thread
        handler = new Handler();

        // Runs a save action if pipeline is enabled
        saveSession = (Button) findViewById(R.id.archive);
        saveSession.setEnabled(false);
        saveSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);

                /*
                if (pipeline.isEnabled()) {
                    pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);

                    // Wait 1 second for archive to finish, then refresh the UI
                    // (Note: this is kind of a hack since archiving is seamless and there are no messages when it occurs)
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
                        }
                    }, 1000L);
                } else {
                    Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
                }
                */
            }
        });


        // Forces the pipeline to scan now
        startSession = (Button) findViewById(R.id.startSession);
        stopSession = (Button) findViewById(R.id.stopSession);
        startSession.setEnabled(false);
        stopSession.setEnabled(false);

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
    }
}
