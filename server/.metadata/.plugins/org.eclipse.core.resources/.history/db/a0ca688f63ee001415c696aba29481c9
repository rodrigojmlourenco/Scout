package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline;

import com.ideaimpl.patterns.pipeline.Pipeline;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rodrigo.jm.lourenco on 13/03/2015.
 */
public class SensorPipeline implements Pipeline {

    private List<Stage> stages;
    private List<Stage> errorStages;
    private List<Stage> finalStages;

    public SensorPipeline(){
        this.stages = new ArrayList<>();
        this.errorStages = new ArrayList<>();
        this.finalStages = new ArrayList<>();
    }

    public void addStage(Stage stage){
        stages.add(stage);
    }

    public void addErrorStage(Stage stage){
        errorStages.add(stage);
    }

    public void addFinalStage(Stage stage){
        finalStages.add(stage);

    }

    public void removeStage(Stage stage) {
        stages.remove(stage);

    }

    public void removeErrorStage(Stage stage) {
        errorStages.remove(stage);

    }

     public void removeFinalStage(Stage stage){
         finalStages.remove(stage);
     }

    public void execute(PipelineContext context){
        /* execute the stages */
        for (Stage stage: stages){

            stage.execute(context);

            if (context.getErrors()!= null && !context.getErrors().isEmpty()){
                break;
            }

        }
		/* if any error occurred, execute the error stages*/
        if (context.getErrors()!= null && !context.getErrors().isEmpty()){
            for (Stage errorStage: errorStages){
                errorStage.execute(context);
            }
        }
        //execute the final stages
        for (Stage finalStage: finalStages){
            finalStage.execute(context);
        }
    }
}
