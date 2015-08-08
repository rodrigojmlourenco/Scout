package pt.ulisboa.tecnico.cycleourcity.evalscout.network.stages;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import pt.ulisboa.tecnico.cycleourcity.evalscout.ScoutApplication;
import pt.ulisboa.tecnico.cycleourcity.evalscout.storage.provider.ScoutProvider;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class UploadStage implements Stage {

    private ContentResolver resolver;
    private Uri mURI;

    public UploadStage(){
        resolver = ScoutApplication.getContext().getContentResolver();
        /*mURI = new Uri.Builder()
                .scheme(ScoutProvider.SCHEME)
                .authority(ScoutProvider.AUTHORITY)
                .path(ScoutProvider.MAIN_TABLE)
                .build();*/
    }

    @Override
    public void execute(PipelineContext pipelineContext) {

        ContentValues mNewValues = new ContentValues();
        mNewValues.put("name", 1);

        resolver.insert(ScoutProvider.CONTENT_URI, mNewValues);

    }
}
