package pt.ulisboa.tecnico.cycleourcity.scout.offloading.stages;

/**
 * Created by rodrigo.jm.lourenco on 09/07/2015.
 */
public interface TestStages {

    public static class Test1000Stage extends TestOffloadStage {
        public Test1000Stage() {
            super(1000);
        }
    }

    public static class Test2000Stage extends TestOffloadStage {
        public Test2000Stage() {
            super(2000);
        }
    }

    public static class Test3000Stage extends TestOffloadStage {
        public Test3000Stage() {
            super(3000);
        }
    }

    public static class Test4000Stage extends TestOffloadStage {
        public Test4000Stage() {
            super(4000);
        }
    }

    public static class Test5000Stage extends TestOffloadStage {
        public Test5000Stage() {
            super(5000);
        }
    }

    public static class Test6000Stage extends TestOffloadStage {
        public Test6000Stage() {
            super(6000);
        }
    }

    public static class Test7000Stage extends TestOffloadStage {
        public Test7000Stage() {
            super(7000);
        }
    }
}
