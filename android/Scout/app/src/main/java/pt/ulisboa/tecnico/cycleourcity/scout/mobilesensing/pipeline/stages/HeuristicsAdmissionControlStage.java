package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.Error;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data.Location;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * The information captured by the location sensors varies in quality. In order to assure the
 * application's robustness the AdmissionControl stage removes samples that may undermine the
 * quality of the system, for example samples with lower quality.
 * <br>
 * This admission control process is based on heuristics, which define rules that specify which
 * samples are acceptable, and is designed as a pipeline. This design allows the admission control
 * process to be more flexible, and easier to modify.
 *
 * @version 1.0
 * @see com.ideaimpl.patterns.pipeline.Stage
 */
public class HeuristicsAdmissionControlStage implements Stage {

    public final String LOG_TAG = this.getClass().getSimpleName();
    public final String TAG = "[AdmissionControl]: ";

    private ScoutLogger logger = ScoutLogger.getInstance();

    private SensorPipeline admissionPipeline;

    public HeuristicsAdmissionControlStage() {
        admissionPipeline = new SensorPipeline();
        admissionPipeline.addStage(new AccuracyOutlier());
        admissionPipeline.addStage(new SatellitesOutlier());
        admissionPipeline.addStage(new GPSSpeedOutlier());
        admissionPipeline.addStage(new HighTravelSpeedOutlier());
        admissionPipeline.addStage(new OverlappingLocationsOutlier());
        admissionPipeline.addErrorStage(new InvalidateSample());
    }


    @Override
    public void execute(PipelineContext pipelineContext) {

        //Logging
        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "admission control.");

        JsonObject[] input = ((SensorPipelineContext) pipelineContext).getInput();

        int discarded = 0;
        HeuristicAdmissionControlContext ctx;
        Queue<JsonObject> aux = new LinkedList<>();

        for (JsonObject sample : input) {

            if (sample == null)
                continue;

            //Set the Heuristics Pipeline with the new sample
            ctx = new HeuristicAdmissionControlContext();
            ctx.setInput(sample);

            //Execute the Heuristics Pipeline
            admissionPipeline.execute(ctx);

            //Check if the sample is valid
            if (!ctx.isValidSample()) {
                discarded++;
                for (Error error : ctx.getErrors())
                    logger.log(ScoutLogger.DEBUG, LOG_TAG, TAG + error.toString());
            } else {
                //If none of the heuristics has failed then the sample is accepted.
                aux.add(sample);
            }
        }

        logger.log(ScoutLogger.INFO, LOG_TAG, TAG + discarded + " samples out of " + input.length + " were discarded.");

        JsonObject[] output = new JsonObject[aux.size()];
        aux.toArray(output);

        //Pass results onto the next stage
        ((SensorPipelineContext) pipelineContext).setInput(output); //For the next Stage
    }

    public static class HeuristicAdmissionControlContext implements PipelineContext {

        JsonObject inputSample;
        boolean validSample = true;
        List<Error> errors;

        public JsonObject getInput() {
            return this.inputSample;
        }

        public void setInput(JsonObject sample) {
            this.inputSample = sample;
        }

        public boolean isValidSample() {
            return this.validSample;
        }

        public void invalidateSample(final String errorMsg) {

            if (errors == null)
                errors = new LinkedList<>();

            errors.add(new Error() {
                @Override
                public String toString() {
                    return errorMsg;
                }
            });

            validSample = false;
        }

        @Override
        public List<Error> getErrors() {
            return errors;
        }
    }

    public static class InvalidateSample implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {
        }
    }

    /**
     * Heuristic AccuracyOutlier removes where the error margin is too high.
     * <br>
     * More specifically, a location must have an error margin, defined by the accuracy field, lower
     * than a predefined threshold, set by LocationState.MIN_ACCURACY.
     */
    public static class AccuracyOutlier implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {

            HeuristicAdmissionControlContext ctx = (HeuristicAdmissionControlContext) pipelineContext;
            JsonObject input = ctx.getInput();

            if (input == null) {
                ctx.invalidateSample("Sample is null, nothing to validate.");
                return;
            }

            float errorMargin = input.get(SensingUtils.LocationKeys.ACCURACY).getAsFloat();

            if (errorMargin > LocationState.MIN_ACCURACY)
                ctx.invalidateSample("Discarded, not enough accuracy (" + errorMargin + ").");
        }
    }


    /**
     * Heuristic SatellitesOutlier removes samples that are not fixed to enough satellites.
     * <br>
     * In order for the GPS receiver to perform triangulation and thus determine the device's
     * location it must be fixed to at least 3 satellites.
     * <br>
     * Besides this admission control, given the number of fixed satellites the following
     * corrections are performed:
     * <ol>
     * With 3 satellites although triangulation is possible, it is not enough to determine
     * the device's elevation and speed, and so the location takes on the mean values
     * defined by the LocationState for both altitude and speed.
     * </ol>
     * <ol>
     * With 4 satellites elevation may be accepted by speed must still be corrected.
     * </ol>
     */
    public static class SatellitesOutlier implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {
            HeuristicAdmissionControlContext ctx = (HeuristicAdmissionControlContext) pipelineContext;
            JsonObject input = ctx.getInput();

            if (input == null) {
                ctx.invalidateSample("Sample is null, nothing to validate.");
                return;
            }

            int numSatellites;

            try {
                numSatellites = input.get(SensingUtils.LocationKeys.SATTELITES).getAsInt();
            } catch (NullPointerException e) {
                ctx.invalidateSample("Sample does not specify satellites number, impossible to determine accuracy");
                return;
            }


            LocationState locationState = ScoutState.getInstance().getLocationState();
            float meanAlt = locationState.getAverageAltitude(),
                    meanSpeed = locationState.getAverageSpeed();

            //Heuristic3: Not fixed to enough satellites
            if (numSatellites < LocationState.MIN_FIXED_SATELLITES) {
                ctx.invalidateSample("Discarded, not fixed to enough satellites (" + numSatellites + ").");
                return;
            } else {
                switch (numSatellites) {
                    case LocationState.MIN_FIXED_SATELLITES:
                        //Correction1:
                        // Minimum acceptable for lat and lon, alt and speed must be corrected
                        input.remove(SensingUtils.LocationKeys.ALTITUDE);
                        input.remove(SensingUtils.LocationKeys.SPEED);
                        input.addProperty(SensingUtils.LocationKeys.ALTITUDE, meanAlt);
                        input.addProperty(SensingUtils.LocationKeys.SPEED, meanSpeed);
                        break;
                    case LocationState.MIN_FIXED_SATELLITES + 1:
                        //Correction2:
                        //Minimum acceptable for lat, lon and altitude, speed must be corrected
                        input.remove(SensingUtils.LocationKeys.SPEED);
                        input.addProperty(SensingUtils.LocationKeys.SPEED, meanSpeed);
                        break;
                    default:
                        //Correction3: Nothing to correct
                        break;
                }
            }

            ctx.setInput(input);

        }
    }


    /**
     * Heuristic GPSSpeedOutlier removes samples where the registered speed is too high
     * <br>
     * More specifically, the speed registered by the GPS, for a given location, must not exceed a
     * predefined value, set by LocationState.MAX_SPEED.
     */
    public static class GPSSpeedOutlier implements Stage {

        @Override
        public void execute(PipelineContext pipelineContext) {

            HeuristicAdmissionControlContext ctx = (HeuristicAdmissionControlContext) pipelineContext;
            JsonObject sample = ctx.getInput();

            if (sample == null) {
                ctx.invalidateSample("Sample is null, nothing to validate.");
                return;
            }

            float speed;

            try {
                speed = sample.get(SensingUtils.LocationKeys.SPEED).getAsFloat();
            } catch (NullPointerException e) {
                speed = 0;
            }


            if (speed >= LocationState.MAX_SPEED)
                ctx.invalidateSample("Discarded, too much speed (" + speed + ").");
        }
    }

    /**
     * Heuristic HighAltitudeOutlier removes samples where there is a high elevation variance.
     * <br>
     * More specifically, if the difference between the previously recorded altitude and the
     * registered by the GPS receiver is too high, that is crosses a given threshold set by
     * LocationState.ALTITUDE_VARIANCE_THRESHOLD, then the location is discarded.
     * <br>
     * Due to this implementation there is the possibility of a sequence of wrong altitudes
     * compromising the state. So, if a given number of locations have been discarded due
     * to this heuristic, set by LocationState.POISONED_BUFFER_THRESHOLD, then the location
     * state is restarted.
     * <p/>
     * TODO: caso esta heuristica seja usada novamente é preciso voltar a activar o mecanismo de anti-poison
     */
    public static class HighAltitudeOutlier implements Stage {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            HeuristicAdmissionControlContext ctx = (HeuristicAdmissionControlContext) pipelineContext;
            JsonObject sample = ctx.getInput();

            float altitude, meanAltitude;

            try {
                altitude = sample.get(SensingUtils.LocationKeys.ALTITUDE).getAsFloat();
            } catch (NullPointerException e) {
                logger.log(ScoutLogger.WARN, LOG_TAG, "Sample has no altitude, skipping this heuristic.");
                return;
            }

            LocationState locationState = ScoutState.getInstance().getLocationState();
            meanAltitude = locationState.getAverageAltitude();

            float deltaAltitude = Math.abs(meanAltitude - altitude);
            if (meanAltitude > 0 && Math.abs(deltaAltitude) > LocationState.ALTITUDE_VARIANCE_THRESHOLD) {
                ctx.invalidateSample("Discarded, huge elevation variance (" + deltaAltitude + ").");


                /*
                discarded++;
                discardedByAlt++;

                //If too many samples were rejected by height differences then it can be
                //assumed that the locations queue is "poisoned".
                //TODO: isto deveria ter em conta o tempo passado
                if(discardedByAlt >= LocationState.POISONED_BUFFER_THRESHOLD){
                    logger.log(ScoutLogger.ERR, LOG_TAG, TAG+"Location state is poisoned, clearing it's state...");
                    discardedByAlt = 0;
                    locationState.clearLocationBuffer();
                }
                */
            }
        }
    }

    /**
     * Heuristic HighTravelSpeedOutlier removes samples where the calculated travel speed to high.
     * <br>
     * More specifically, given to consecutive location A and B, if the time it took to reach B
     * from A is too low, which means the travelling speed is to high, thus making B an outlier.
     */
    public static class HighTravelSpeedOutlier implements Stage {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            HeuristicAdmissionControlContext ctx = (HeuristicAdmissionControlContext) pipelineContext;
            JsonObject sample = ctx.getInput();

            ScoutState scoutState = ScoutState.getInstance();
            LocationState locationState = scoutState.getLocationState();

            if (!scoutState.isReady()) {
                logger.log(ScoutLogger.WARN, LOG_TAG, "Scout state is not ready yet, skipping this heuristic.");
                return;
            }

            Location location = new Location(sample),
                    previousLocation = locationState.getLastLocation();


            float calculatedSpeed = previousLocation.getTraveledSpeed(location);

            if (calculatedSpeed > LocationState.MAX_SPEED)
                ctx.invalidateSample("Discarded, calculated speed is too high (" + calculatedSpeed + "m//s).");

        }
    }

    /**
     * Heuristic OverlappingLocationsOutlier removes the overlapping location with lower accuracy.
     * <br>
     * More specifically, given two consecutive locations A and B, if their uncertainty areas
     * overlap and B's accuracy is lower than A's, then B is discarded.
     *
     * @see <a href="http://www.thinkmind.org/download.php?articleid=lifsci_v4_n34_2012_6">Move Better with Tripzoom</a>
     */
    public static class OverlappingLocationsOutlier implements Stage {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private ScoutLogger logger = ScoutLogger.getInstance();

        @Override
        public void execute(PipelineContext pipelineContext) {

            HeuristicAdmissionControlContext ctx = (HeuristicAdmissionControlContext) pipelineContext;
            JsonObject sample = ctx.getInput();

            if (sample == null) {
                ctx.invalidateSample("Sample is null, nothing to validate");
                return;
            }

            ScoutState scoutState = ScoutState.getInstance();
            LocationState locationState = scoutState.getLocationState();

            if (!scoutState.isReady()) {
                logger.log(ScoutLogger.WARN, LOG_TAG, "Scout state is not ready yet, skipping this heuristic.");
                return;
            }

            Location location = new Location(sample),
                    previousLocation = locationState.getLastLocation();

            if (previousLocation.isOverlapping(location)
                    && location.getErrorMargin() > previousLocation.getErrorMargin())
                ctx.invalidateSample("Discarded, new location overlaps the previous and has a lower accuracy.");
        }
    }
}