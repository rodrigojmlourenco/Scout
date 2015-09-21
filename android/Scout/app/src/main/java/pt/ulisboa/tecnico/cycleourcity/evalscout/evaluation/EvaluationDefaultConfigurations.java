package pt.ulisboa.tecnico.cycleourcity.evalscout.evaluation;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.evalscout.offloading.stages.ConfigurationTaggingStage;
import pt.ulisboa.tecnico.cycleourcity.evalscout.pipeline.RoadConditionMonitoringPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensing;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;


public abstract class EvaluationDefaultConfigurations {

    public static List<PipelineConfiguration> generateConfigurationA(){
        List<PipelineConfiguration> configurations = new ArrayList<>();
        PipelineConfiguration p1 = new PipelineConfiguration();

        p1.addStage(new TestOffloadingStage("p11", new TestOffloadStage(1000),      1000,    800, 800));
        p1.addStage(new TestOffloadingStage("p12", new TestOffloadStage(10000),     10000,   800, 600));
        p1.addStage(new TestOffloadingStage("p13", new TestOffloadStage(100000),    100000,  600, 400));
        p1.addStage(new TestOffloadingStage("p14", new TestOffloadStage(1000000),   1000000, 400, 200));
        p1.addStage(new TestOffloadingStage("p15", new TestOffloadStage(2000000),   2000000, 200, 50));

        p1.addFinalStage(new ConfigurationTaggingStage());

        configurations.add(p1);
        return configurations;
    }


    /**
     * Simulates the RoadConditionMonitoringPipeline but with increased computation costs.
     */
    public static List<PipelineConfiguration> generateConfigurationB(){
        List<PipelineConfiguration> configurations = new ArrayList<>();
        PipelineConfiguration p1 = new PipelineConfiguration();

        p1.addStage(new TestOffloadingStage("p21", new TestOffloadStage(40000),      40000,    1200, 1200));
        p1.addStage(new TestOffloadingStage("p22", new TestOffloadStage(30000),      30000,    1200, 1200));
        //p1.addStage(new TestOffloadingStage("p23", new TestOffloadStage(100000),    100000,    1200, 1200));
        //p1.addStage(new TestOffloadingStage("p24", new TestOffloadStage(1000000),  1000000,    1200,  100));
        //p1.addStage(new TestOffloadingStage("p24", new TestOffloadStage(10000),      10000,     100,   50));



        configurations.add(p1);
        return configurations;
    }

    public static List<PipelineConfiguration> generateConfigurationC(){

        List<PipelineConfiguration> configurations = new ArrayList<>();
        PipelineConfiguration p1 = new PipelineConfiguration(),
                p2 = new PipelineConfiguration();

        p1.addStage(new TestOffloadingStage("p11", new TestStages.Test100Stage(), 100, 800, 800));
        p1.addStage(new TestOffloadingStage("p12", new TestStages.Test200Stage(), 200, 800, 700));
        p1.addStage(new TestOffloadingStage("p13", new TestStages.Test300Stage(), 300, 700, 600));
        p1.addStage(new TestOffloadingStage("p14", new TestStages.Test400Stage(), 400, 600, 400));
        p1.addStage(new TestOffloadingStage("p15", new TestStages.Test500Stage(), 500, 400, 200));
        p1.addStage(new TestOffloadingStage("p16", new TestStages.Test600Stage(), 200, 200, 50));

        p2.addStage(new TestOffloadingStage("p21", new TestStages.Test50Stage(), 50, 600, 600));
        p2.addStage(new TestOffloadingStage("p22", new TestStages.Test200Stage(), 200, 600, 600));
        p2.addStage(new TestOffloadingStage("p23", new TestStages.Test600Stage(), 600, 600, 10));
        p2.addStage(new TestOffloadingStage("p24", new TestStages.Test150Stage(), 150, 50, 50));
        p2.addStage(new TestOffloadingStage("p25", new TestStages.Test10Stage(), 10, 50, 50));

        configurations.add(p1);
        configurations.add(p2);

        return configurations;
    }

    public static List<PipelineConfiguration> generateConfigurationD(){

        List<PipelineConfiguration> configurations = new ArrayList<>();
        PipelineConfiguration
                p1 = new PipelineConfiguration(),
                p2 = new PipelineConfiguration(),
                p3 = new PipelineConfiguration(),
                p4 = new PipelineConfiguration(),
                p5 = new PipelineConfiguration();

        p1.addStage(new TestOffloadingStage("p11", new TestStages.Test100Stage(), 100, 800, 800));
        p1.addStage(new TestOffloadingStage("p12", new TestStages.Test200Stage(), 200, 800, 700));
        p1.addStage(new TestOffloadingStage("p13", new TestStages.Test300Stage(), 300, 700, 600));
        p1.addStage(new TestOffloadingStage("p14", new TestStages.Test400Stage(), 400, 600, 400));
        p1.addStage(new TestOffloadingStage("p15", new TestStages.Test500Stage(), 500, 400, 200));
        p1.addStage(new TestOffloadingStage("p16", new TestStages.Test600Stage(), 200, 200, 50));

        p2.addStage(new TestOffloadingStage("p21", new TestStages.Test50Stage(), 50, 600, 600));
        p2.addStage(new TestOffloadingStage("p22", new TestStages.Test200Stage(), 200, 600, 600));
        p2.addStage(new TestOffloadingStage("p23", new TestStages.Test600Stage(), 600, 600, 10));
        p2.addStage(new TestOffloadingStage("p24", new TestStages.Test150Stage(), 150, 50, 50));
        p2.addStage(new TestOffloadingStage("p25", new TestStages.Test10Stage(), 10, 50, 50));

        p3.addStage(new TestOffloadingStage("p31", new TestStages.Test50Stage(),  50,  600, 600));
        p3.addStage(new TestOffloadingStage("p32", new TestStages.Test100Stage(), 100, 600, 600));
        p3.addStage(new TestOffloadingStage("p33", new TestStages.Test600Stage(), 600, 600, 10));
        p3.addStage(new TestOffloadingStage("p34", new TestStages.Test150Stage(), 150, 10,  50));
        p3.addStage(new TestOffloadingStage("p35", new TestStages.Test10Stage(),  10,  50,  100));
        p3.addStage(new TestOffloadingStage("p36", new TestStages.Test10Stage(),  10,  100, 150));
        p3.addStage(new TestOffloadingStage("p37", new TestStages.Test10Stage(),  10,  150, 200));
        p3.addStage(new TestOffloadingStage("p38", new TestStages.Test10Stage(),  10,  200, 250));

        p4.addStage(new TestOffloadingStage("p41", new TestStages.Test50Stage(), 50, 600, 600));
        p4.addStage(new TestOffloadingStage("p42", new TestStages.Test100Stage(), 100, 600, 600));
        p4.addStage(new TestOffloadingStage("p43", new TestStages.Test600Stage(), 600, 600, 10));
        p4.addStage(new TestOffloadingStage("p44", new TestStages.Test150Stage(), 150, 10, 50));

        p5.addStage(new TestOffloadingStage("p51", new TestStages.Test50Stage(), 50, 600, 600));
        p5.addStage(new TestOffloadingStage("p52", new TestStages.Test10Stage(), 10, 600, 50));
        p5.addStage(new TestOffloadingStage("p53", new TestStages.Test600Stage(), 600, 50, 10));
        p5.addStage(new TestOffloadingStage("p54", new TestStages.Test200Stage(), 200, 10, 700));
        p5.addStage(new TestOffloadingStage("p55", new TestStages.Test10Stage(), 10, 700, 50));



        configurations.add(p1);
        configurations.add(p2);

        return configurations;
    }
}
