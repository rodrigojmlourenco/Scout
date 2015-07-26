package pt.ulisboa.tecnico.cycleourcity.scout;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import pt.ulisboa.tecnico.cycleourcity.scout.calibration.SensorCalibrator;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.AdaptiveOffloadingManager;


public class SettingsActivity extends ActionBarActivity implements SeekBar.OnSeekBarChangeListener {

    private Button clearDataBtn;
    private SeekBar energySeekBar, dataSeekBar;
    private TextView energyProgressText, dataProgressText;

    int energyProgressValue, dataProgressValue;

    private AdaptiveOffloadingManager offloadingManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        offloadingManager = AdaptiveOffloadingManager.getInstance(ScoutApplication.getContext());

        clearDataBtn = (Button) findViewById(R.id.clearDataBtn);
        clearDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences =
                        getSharedPreferences(SensorCalibrator.PREFERENCES_NAME, Context.MODE_PRIVATE);

                preferences.edit().clear().commit();

                Toast.makeText(SettingsActivity.this, getString(R.string.cleared_data_settings), Toast.LENGTH_SHORT).show();
            }
        });

        energySeekBar   = (SeekBar) findViewById(R.id.energyWeightBar);
        dataSeekBar     = (SeekBar) findViewById(R.id.dataWeightBar);

        energyProgressText  = (TextView) findViewById(R.id.energyProgressText);
        dataProgressText   = (TextView) findViewById(R.id.dataProgressText);

        energySeekBar.setOnSeekBarChangeListener(this);
        dataSeekBar.setOnSeekBarChangeListener(this);

        energyProgressValue = (int) (offloadingManager.getEnergyUtilityWeight()*100);
        dataProgressValue   = (int) (offloadingManager.getDataUtilityWeight()*100);

        energySeekBar.setProgress(energyProgressValue);
        dataSeekBar.setProgress(dataProgressValue);

        energyProgressText.setText(getWeightAsText(energyProgressValue));
        dataProgressText.setText(getWeightAsText(dataProgressValue));

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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        switch (seekBar.getId()){
            case R.id.energyWeightBar:
                energyProgressValue = seekBar.getProgress();
                dataProgressValue = 100 - energyProgressValue;
                break;
            case R.id.dataWeightBar:
                dataProgressValue = seekBar.getProgress();
                energyProgressValue = 100 - dataProgressValue;
                break;
        }


        energySeekBar.setProgress(energyProgressValue);
        dataSeekBar.setProgress(dataProgressValue);

        energyProgressText.setText(getWeightAsText(energyProgressValue));
        dataProgressText.setText(getWeightAsText(dataProgressValue));

        offloadingManager.setTotalUtilityWeights((float)energyProgressValue / 100, (float)dataProgressValue / 100);
    }
}
