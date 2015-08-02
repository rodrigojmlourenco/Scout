package pt.ulisboa.tecnico.cycleourcity.scout;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import pt.ulisboa.tecnico.cycleourcity.scout.calibration.SensorCalibrator;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.ScoutProfiling;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;


public class SettingsActivity extends ActionBarActivity {

    private Button clearDataBtn;
    private RadioButton hasStageModel;

    int energyProgressValue, dataProgressValue;

    private AdaptiveOffloadingManager offloadingManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        hasStageModel = (RadioButton) findViewById(R.id.hasStageModelRadio);

        try {
            offloadingManager = AdaptiveOffloadingManager.getInstance(ScoutApplication.getContext());
            hasStageModel.setChecked(offloadingManager.isStageModelComplete());
        } catch (InvalidRuleSetException e) {
            e.printStackTrace();
        }

        clearDataBtn = (Button) findViewById(R.id.clearDataBtn);
        clearDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getSharedPreferences(SensorCalibrator.PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit();

                getSharedPreferences(ScoutProfiling.PREFERENCES,Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit();

                /*
                getSharedPreferences(ScoutProfiling.DATA_PLAN_PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit();
                */

                Toast.makeText(SettingsActivity.this, getString(R.string.cleared_data_settings), Toast.LENGTH_SHORT).show();

                MainActivity.ON_INITS_CHECKED = false;
            }
        });
    }

    private String getWeightAsText(float weight){
        return ""+((int)weight)+"%";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
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
}
