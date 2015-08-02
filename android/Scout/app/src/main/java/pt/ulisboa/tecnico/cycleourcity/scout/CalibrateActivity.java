package pt.ulisboa.tecnico.cycleourcity.scout;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.calibration.LinearAccelerationCalibrator;
import pt.ulisboa.tecnico.cycleourcity.scout.calibration.SensorCalibrator;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;


public class CalibrateActivity extends ActionBarActivity
        implements View.OnClickListener, SensorEventListener{

    //Logging
    private final String LOG_TAG = "Calibrate";
    private final boolean VERBOSE = true;

    // Indicates true if sampling is occurring
    private boolean sampling = false;
    // Indicates true if samples are trying to be taken or are being taken
    private boolean running = false;
    // Indicates true if samples have been recorded successfully
    private boolean finished = false;

    /*
     * UI Components
     */
    private Button      startButton;
    private TextView    textViewXAxis, textViewYAxis, textViewZAxis,
            rotationXAxis, rotationYAxis, rotationZAxis;
    private ImageView   imageViewPhone;

    //Sensing
    private SensorManager sensorManager;

    //User-Feedback
    private Vibrator vibe;




    private LinearAccelerationCalibrator accelerationCalibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        createInputView();

        startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(this);

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        accelerationCalibrator = new LinearAccelerationCalibrator();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_calibrate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent intent;
        switch (id) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }





    /*
     ********************************************************
     * OnClickListener                                      *
     ********************************************************
     */
    @Override
    public void onClick(View v) {

        /* AccelerationExplorer */
        if(v.equals(startButton)){ //Initiate Calibration
            if (!running && !finished)
            {
                CharSequence text = getString(R.string.step0_calibration);

                Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
                toast.show();

                startButton.setText(getString(R.string.stop_calibration));
                running = true;

                accelerationCalibrator = new LinearAccelerationCalibrator();
            }
            // If the user re-starts the samples
            else if (!running && finished)
            {
                createInputView();

                CharSequence text = getString(R.string.help_calibration);

                Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
                toast.show();


                startButton.setText(getString(R.string.stop_calibration));

                accelerationCalibrator.restart();
                running = true;
                finished = false;
            }
            // If the user stops the samples
            else
            {
                startButton.setText(getString(R.string.start_calibration));

                createInputView();

                accelerationCalibrator.restart();
                running = false;
                finished = false;
            }
        }
    }

    /*
     ********************************************************
     * SensorEventListener                                  *
     ********************************************************
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        int sensorType = event.sensor.getType();


        switch (sensorType){
            case Sensor.TYPE_LINEAR_ACCELERATION:
                handleLinearAccelerationEvent(event);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                handleRotationVector(event);
                break;
            default:
                Log.w(LOG_TAG, "Unknown sensor type " + sensorType);
        }
    }

    private float[] acceleration = new float[3];
    private void handleLinearAccelerationEvent(SensorEvent event){

        System.arraycopy(event.values, 0, acceleration, 0, event.values.length);

        textViewXAxis.setText(String.format("%.2f", acceleration[0]));
        textViewYAxis.setText(String.format("%.2f", acceleration[1]));
        textViewZAxis.setText(String.format("%.2f", acceleration[2]));

        if(running && ! finished){ //Calibrate

            JsonObject sample;

            if(!accelerationCalibrator.isComplete()){
                sample = new JsonObject();
                sample.addProperty(SensingUtils.MotionKeys.X, acceleration[0]);
                sample.addProperty(SensingUtils.MotionKeys.Y, acceleration[1]);
                sample.addProperty(SensingUtils.MotionKeys.Z, acceleration[2]);
                accelerationCalibrator.addSample(sample);
            }else{

                vibe.vibrate(1000);
                vibe.vibrate(1000);

                JsonObject offsets = accelerationCalibrator.getOffsets();

                if(accelerationCalibrator.successfulCalibration()) {

                    accelerationCalibrator.storeOffsets(getSharedPreferences(SensorCalibrator.PREFERENCES_NAME, Context.MODE_PRIVATE));
                    accelerationCalibrator.restart();

                    finished = true;
                    running = false;

                    startButton.setText(getString(R.string.start_calibration));
                    Toast.makeText(CalibrateActivity.this, getString(R.string.finished_calibration), Toast.LENGTH_SHORT).show();

                    Handler handler;
                    Runnable delayRunnable;

                    handler = new Handler();
                    delayRunnable = new Runnable() {

                        @Override
                        public void run() {
                            CalibrateActivity.this.finish();
                        }
                    };
                    handler.postDelayed(delayRunnable, Toast.LENGTH_SHORT);

                }else{

                    accelerationCalibrator.restart();
                    finished = false;
                    running = false;

                    startButton.setText(getString(R.string.start_calibration));
                    Toast.makeText(CalibrateActivity.this, getString(R.string.error_calibration), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final float[] rotation = new float[5];
    private void handleRotationVector(SensorEvent event){

        System.arraycopy(event.values, 0, rotation, 0, event.values.length);

        rotationXAxis.setText(String.format("%.2f", rotation[0]));
        rotationYAxis.setText(String.format("%.2f", rotation[1]));
        rotationZAxis.setText(String.format("%.2f", rotation[2]));

    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /*Do nothing*/  }


    /*
      ********************************************************
      * UI                                                   *
      ********************************************************
      */
    private void createInputView()
    {

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout_content);
        layout.removeAllViews();

        RelativeLayout inputLayout = (RelativeLayout) getLayoutInflater()
                .inflate(R.layout.layout_calibration_input, null);

        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        relativeParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        textViewXAxis = (TextView) inputLayout.findViewById(R.id.value_x_axis);
        textViewYAxis = (TextView) inputLayout.findViewById(R.id.value_y_axis);
        textViewZAxis = (TextView) inputLayout.findViewById(R.id.value_z_axis);

        rotationXAxis = (TextView) inputLayout.findViewById(R.id.rotationXAxis);
        rotationYAxis = (TextView) inputLayout.findViewById(R.id.rotationYAxis);
        rotationZAxis = (TextView) inputLayout.findViewById(R.id.rotationZAxis);


        imageViewPhone = (ImageView) inputLayout.findViewById(R.id.imageViewPhone);
        imageViewPhone.setImageResource(R.drawable.phone);

        layout.addView(inputLayout, relativeParams);
    }
}
