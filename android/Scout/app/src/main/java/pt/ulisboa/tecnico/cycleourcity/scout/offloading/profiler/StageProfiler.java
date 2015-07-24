package pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiler;

import java.util.HashMap;
import java.util.UUID;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages.OffloadingStageWrapper;

/**
 * Created by rodrigo.jm.lourenco on 31/05/2015.
 */
public class StageProfiler {

    public static final Integer NUM_PROFILING_SAMPLES = 25;

    private static StageProfiler STAGE_PROFILER = null;

    private HashMap<UUID, OffloadingStageWrapper> stages;

    private StageProfiler(){
        stages = new HashMap<>();
    }

    public static StageProfiler getInstance(){

        if (STAGE_PROFILER == null ) {
            synchronized (StageProfiler.class) {
                if (STAGE_PROFILER == null) {
                    STAGE_PROFILER = new StageProfiler();
                }
            }
        }

        return STAGE_PROFILER;
    }

    public UUID registerStage(OffloadingStageWrapper stage){
        UUID id = UUID.randomUUID();
        this.stages.put(id, stage);
        return  id;
    }


}
