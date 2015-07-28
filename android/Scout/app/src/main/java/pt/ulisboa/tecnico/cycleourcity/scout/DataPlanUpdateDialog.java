package pt.ulisboa.tecnico.cycleourcity.scout;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources.MobileDataPlanProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler.resources.ScoutProfiling;

public class DataPlanUpdateDialog extends DialogFragment{

    private SharedPreferences dataPlanPrefs;

    private TextView errorAlert;
    private EditText dataPlanTotal, dataPlanLimit, dataPlanUsed;

    private float MB_2_B = 1048576f;
    private float GB_2_B = 1073741824f;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setMessage(getString(R.string.data_plan));

        final View dialogView = inflater.inflate(R.layout.data_plan_settings,null);

        errorAlert      = (TextView) dialogView.findViewById(R.id.errorDataPlan);
        dataPlanTotal   = (EditText) dialogView.findViewById(R.id.dataPlanTotal);
        dataPlanLimit   = (EditText) dialogView.findViewById(R.id.dataPlanLimit);
        dataPlanUsed    = (EditText) dialogView.findViewById(R.id.dataPlanUsed);

        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                );


        final AlertDialog d = builder.create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = d.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long total = 0, limit = 0, used=0;

                        try {
                            total   = (long)(Float.parseFloat(dataPlanTotal.getText().toString())*GB_2_B);
                            limit   = (long)(Float.parseFloat(dataPlanLimit.getText().toString())*GB_2_B);
                            used    = (long)(Float.parseFloat(dataPlanUsed.getText().toString())*GB_2_B);
                        } catch (NumberFormatException e) { //Error
                            errorAlert.setText(getString(R.string.mobile_plan_void));
                            return;
                        }

                        if( total <= 0 || limit <= 0){
                            errorAlert.setText(getString(R.string.mobile_plan_zero));
                            return;
                        }

                        if (limit > total || used > total) {//Error
                            errorAlert.setText(getString(R.string.mobile_plan_error));
                            return;
                        }

                        save(total,limit,used);

                        d.dismiss();
                    }
                });
            }
        });

        return d;
    }

    private void save(long total, long limit, long used){

        dataPlanPrefs = ScoutApplication.getContext()
                .getSharedPreferences(ScoutProfiling.PREFERENCES, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = dataPlanPrefs.edit();

        Gson gson = new Gson();

        JsonObject state = new JsonObject();

        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.NAME, MobileDataPlanProfiler.DataPlanStateFields.PREFS_NAME);
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.TIME, System.nanoTime());

        //Immutable Fields
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.DATA_PLAN, total);
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.DATA_LIMIT, limit);

        //Pre-Shutdown
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.PRE_DOWNLOAD, 0);
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.PRE_UPLOAD, 0);
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.PRE_USAGE, used);

        //Post-Shutdown
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.POST_DOWNLOAD, 0);
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.POST_UPLOAD, 0);
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.POST_USAGE, 0);

        //Pseudo-Real Consumed Data
        state.addProperty(MobileDataPlanProfiler.DataPlanStateFields.CONSUMED_DATA, used);

        Log.d("DataPlan", String.valueOf(state));

        editor.putString(ScoutProfiling.DATA_PLAN_PREFS, gson.toJson(state));
        editor.commit();
    }

}
