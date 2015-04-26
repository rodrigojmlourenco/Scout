package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.location.stages;

import android.util.Log;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import java.util.LinkedList;
import java.util.Queue;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.sensorpipeline.SensorPipeLineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.ScoutState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data.Location;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

/**
 * The information captured by the location sensors varies in quality. In order to assure the
 * application's robustness the AdmissionControl stage removes samples that may undermine the
 * quality of the system, for example samples with lower quality.
 * <p/>
 * In this class admission control is performed based on a set of heuristics:
 * <ul>
 * <li>
 * <h3>Heuristic 1 : Error margin is too high</h3>
 * <p>
 * A location must have an error margin, defined by the accuracy field, lower than
 * a predefined threshold, set by LocationState.MIN_ACCURACY.
 * </p>
 * </li>
 * <li>
 * <h3>Heuristic 2 : Registered speed is too high</h3>
 * <p>
 * The speed registered by the GPS, for a given location must not exceed a predefined
 * value, set by LocationState.MAX_SPEED.
 * </p>
 * </li>
 * <li>
 * <h3>Heuristic 3 : Not fixed to enough satellites</h3>
 * <p>
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
 * </p>
 * </li>
 * <li>
 * <h3>Heuristic 4 : High elevation variance</h3>
 * <p>
 * If the difference between the previously recorded altitude and the registed by the
 * GPS receiver is too high, that is crosses a given threshold set by
 * LocationState.ALTITUDE_VARIANCE_THRESHOLD, then the location is discarded.
 * <br>
 * Due to this implementation there is the possibility of a sequence of wrong altitudes
 * compromising the state. So, if a given number of locations have been discarded due
 * to this heuristic, set by LocationState.POISONED_BUFFER_THRESHOLD, then the location
 * state is restarted.
 * </p>
 * </li>
 * <li>
 * <h3>Heuristic 5 : Travel speed to high</h3>
 * Given to consecutive location A and B, if the time it took to reach B from A is too low,
 * which means the travelling speed is to high, then B is discarded.
 * </li>
 * <li>
 * <h3>Heuristic 6 : Outlier out of 3 [Tripzoom]</h3>
 * <p>
 * Given three consecutive locations A, B and C, if the time it took to reach C from A,
 * through B is too low, which means the speed is too high, then B is discarded.
 * </p>
 * TODO: Esta heuristica não é possivel implementar enquanto o pipeline executar-se de segundo a segundo.
 * </li>
 * <li>
 * <h3>Heuristic 7 : Overlapping location with lower accuracy [Tripzoom]</h3>
 * <p>
 * Given two consecutive locations A and B, if their uncertainty areas overlap and
 * B's accuracy is lower than A's, then B is discarded.
 * </p>
 * </li>
 * </ul>
 *
 * @version 1.0
 * @see com.ideaimpl.patterns.pipeline.Stage
 */
public class HeuristicsAdmissionControlStage implements Stage {

    public final String LOG_TAG = this.getClass().getSimpleName();
    public final String TAG = "[AdmissionControl]: ";

    private int discardedByAlt = 0;
    private ScoutLogger logger = ScoutLogger.getInstance();

    @Override
    public void execute(PipelineContext pipelineContext) {

        //Logging
        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "admission control.");

        int discarded = 0;
        JsonObject[] input = ((SensorPipeLineContext) pipelineContext).getInput();
        Queue<JsonObject> aux = new LinkedList<>();

        //Aux
        boolean noise;
        float errorMargin, speed, altitude;
        int numSatellites;

        //ScoutState base values
        LocationState locationState = ScoutState.getInstance().getLocationState();
        float meanAlt = locationState.getAverageAltitude(),
                meanSpeed = locationState.getAverageSpeed();
        Location previousLocation = locationState.getLastLocation();

        JsonObject auxS;
        try { //TODO: remover try-catch
            for (JsonObject sample : input) {

                if (sample == null)
                    continue;

                //Assuming the location is acceptable...
                noise = false;

                //PHASE 1
                //Get the necessary values
                errorMargin = SensingUtils.LocationSampleAccessor.getAccuracy(sample);

                try {
                    speed = SensingUtils.LocationSampleAccessor.getSpeed(sample);
                } catch (NoSuchDataFieldException e) {
                    speed = 0;
                }

                try {
                    altitude = SensingUtils.LocationSampleAccessor.getAltitude(sample);
                } catch (NoSuchDataFieldException e) {
                    altitude = -1;
                }

                try {
                    numSatellites = SensingUtils.LocationSampleAccessor.getNumSatellites(sample);
                } catch (NoSuchDataFieldException e) {
                    numSatellites = 0;
                }

                //Heuristic1 : Not enough accuracy
                if (errorMargin > LocationState.MIN_ACCURACY) {
                    logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "Discarded, not enough accuracy (" + errorMargin + ").");
                    discarded++;
                    continue;
                }

                //Heuristic2 : Too much speed
                if (speed >= LocationState.MAX_SPEED) {
                    logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "Discarded, too much speed (" + speed + ").");
                    discarded++;
                    continue;
                }

                //Heuristic3: Not fixed to enough satellites
                if (numSatellites < LocationState.MIN_FIXED_SATELLITES) {
                    logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "Discarded, not fixed to enough satellites (" + numSatellites + ").");
                    discarded++;
                    continue;
                } else {
                    switch (numSatellites) {
                        case LocationState.MIN_FIXED_SATELLITES:
                            //Correction1:
                            // Minimum acceptable for lat and lon, alt and speed must be corrected
                            sample.remove(SensingUtils.LocationKeys.ALTITUDE);
                            sample.remove(SensingUtils.LocationKeys.SPEED);
                            sample.addProperty(SensingUtils.LocationKeys.ALTITUDE, meanAlt);
                            sample.addProperty(SensingUtils.LocationKeys.SPEED, meanSpeed);
                            break;
                        case LocationState.MIN_FIXED_SATELLITES + 1:
                            //Correction2:
                            //Minimum acceptable for lat, lon and altitude, speed must be corrected
                            sample.remove(SensingUtils.LocationKeys.SPEED);
                            sample.addProperty(SensingUtils.LocationKeys.SPEED, meanSpeed);
                            break;
                        default:
                            //Correction3: Nothing to correct
                            break;
                    }
                }

            /* TODO: dados que até mais ver a altitude usada passa a ser a do barometro esta heurística é desnecessária.
            //Heuristic4 : Huge variance in height
            float deltaAltitude = Math.abs(meanAlt - altitude);
            if(meanAlt > 0 && Math.abs(deltaAltitude) > LocationState.ALTITUDE_VARIANCE_THRESHOLD){
                logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG+"Discarded, huge elevation variance ("+deltaAltitude+").");

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

                continue;
            }
            */

                //The following heuristics depend on the LocationState
                if (locationState.isReadyState()) {
                    Location location = new Location(sample);


                    //Heuristic5 : Traveled speed to high
                    float calculatedSpeed = previousLocation.getTraveledSpeed(location);
                    if (calculatedSpeed > LocationState.MAX_SPEED) {
                        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "Discarded, calculated speed is too high (" + calculatedSpeed + "m//s).");
                        discarded++;
                        continue;
                    }


                    //Heuristic6:
                    // TODO: esta heuristica não vai funcionar para o pipeline a executar-se de segundo a segundo.

                    //Heuristic7: Overlapping location with lower precision
                    if (previousLocation.isOverlapping(location)
                            && location.getErrorMargin() > previousLocation.getErrorMargin()) {
                        logger.log(ScoutLogger.VERBOSE, LOG_TAG, TAG + "Discarded, new location overlaps the previous and has a lower accuracy.");
                        discarded++;
                        continue;
                    }
                }

                //If none of the heuristics has failed then the sample is accepted.
                aux.add(sample);
            }
        } catch (NullPointerException e) {
            Log.e("WTF", "WTF");
            e.printStackTrace();
        }

        logger.log(ScoutLogger.INFO, LOG_TAG, TAG + discarded + " samples out of " + input.length + " were discarded.");

        JsonObject[] output = new JsonObject[aux.size()];
        aux.toArray(output);

        //Pass results onto the next stage
        ((SensorPipeLineContext) pipelineContext).setInput(output); //For the next Stage
    }
}