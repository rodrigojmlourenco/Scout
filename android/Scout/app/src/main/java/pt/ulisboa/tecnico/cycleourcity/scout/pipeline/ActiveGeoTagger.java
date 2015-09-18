package pt.ulisboa.tecnico.cycleourcity.scout.pipeline;


import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils.RotationVectorKeys;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.location.LocationUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.PipelineConfiguration;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.sensor.location.LocationSensorPipeline;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.stages.CommonStages;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.LocationState;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.state.data.Location;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.RouteStorage;

/**
 * Created by rodrigo.jm.lourenco on 25/06/2015.
 */
public class ActiveGeoTagger {

    //Logging
    private final String LOG_TAG = this.getClass().getSimpleName();

    private final History geoHistory;


    //Geo-tagging Pipelines
    private final LocationSensorPipeline locationPipeline;
    private final RotationVectorSensorPipeline rotationVectorPipeline;

    private static ActiveGeoTagger GEO_TAGGER = null;

    private ActiveGeoTagger(){
        this.geoHistory = new History();

        //Location Sensor Pipeline
        //ConfigurationCaretaker locationCaretaker = new ConfigurationCaretaker();
        //locationCaretaker.setOriginalPipelineConfiguration(geoTaggingLocationConfiguration());
        //locationPipeline = new LocationSensorPipeline(locationCaretaker);
        locationPipeline = new LocationSensorPipeline(geoTaggingLocationConfiguration());

        //Orientation Sensor Pipeline
        //ConfigurationCaretaker rotationVectorCaretaker = new ConfigurationCaretaker();
        //rotationVectorCaretaker.setOriginalPipelineConfiguration(getRotationVectorConfiguration());
        //rotationVectorPipeline = new RotationVectorSensorPipeline(rotationVectorCaretaker);
        rotationVectorPipeline = new RotationVectorSensorPipeline(getRotationVectorConfiguration());
    }

    public static ActiveGeoTagger getInstance(){
        if(GEO_TAGGER == null){
            synchronized (ActiveGeoTagger.class){
                if(GEO_TAGGER == null)
                    GEO_TAGGER = new ActiveGeoTagger();
            }
        }

        return GEO_TAGGER;
    }


    public void pushLocation(final JsonObject locationSample){

        if(!geoHistory.isEmpty())
            locationPipeline.pushSample(geoHistory.getLastKnownLocation());

        locationPipeline.pushSample(locationSample);

        new Thread(locationPipeline).start();
        new Thread(rotationVectorPipeline).start();
    }

    public void pushOrientation(JsonObject orientationSample){
        rotationVectorPipeline.pushSample(orientationSample);
    }

    private PipelineConfiguration geoTaggingLocationConfiguration(){
        PipelineConfiguration locationConfiguration = new PipelineConfiguration();

        locationConfiguration.addStage(new LocationStages.SortLocationsStage());
        locationConfiguration.addStage(new LocationStages.TrimStage());
        locationConfiguration.addStage(new LocationStages.CheckMovementStage());
        locationConfiguration.addStage(new RouteStorage.RouteStorageStage("noisyGPS", RouteStorage.GPS_BASED_ALTITUDE));
        locationConfiguration.addStage(new LocationStages.AdmissionControlStage());
        locationConfiguration.addStage(new LocationStages.UpdateGeoTaggerStage(this.geoHistory));
        locationConfiguration.addStage(new RouteStorage.RouteStorageStage("parsedGPS", RouteStorage.GPS_BASED_ALTITUDE));

        return locationConfiguration;
    }

    private PipelineConfiguration getRotationVectorConfiguration(){
        PipelineConfiguration rotationConfiguration = new PipelineConfiguration();
        rotationConfiguration.addStage(new RotationStages.AdmissionControlStage());
        rotationConfiguration.addStage(new RotationStages.MergeStage());
        rotationConfiguration.addStage(new RotationStages.GenerateRotationMatrix());
        rotationConfiguration.addStage(new RotationStages.UpdateGeoTaggerHistory(this.geoHistory));
        return rotationConfiguration;
    }


    /*
     ************************************************************************
     * Geo-Tagging Methods                                                  *
     ************************************************************************
     */
    private final Gson gson = new Gson();

    public void tagSample(JsonObject sample){
        geoTagSample(sample);
        rotationTagSample(sample);
    }

    private JsonObject cleanLocation(JsonObject location){

        if(location==null) return null;

        JsonObject cleaned = new JsonObject();

        try {
            cleaned.addProperty(
                    SensingUtils.GeneralFields.TIMESTAMP,
                    location.get(SensingUtils.GeneralFields.TIMESTAMP).getAsString());

            cleaned.addProperty(
                    SensingUtils.GeneralFields.SCOUT_TIME,
                    location.get(SensingUtils.GeneralFields.SCOUT_TIME).getAsString());

            cleaned.addProperty(
                    SensingUtils.LocationKeys.LATITUDE,
                    location.get(SensingUtils.LocationKeys.LATITUDE).getAsString());

            cleaned.addProperty(
                    SensingUtils.LocationKeys.LONGITUDE,
                    location.get(SensingUtils.LocationKeys.LONGITUDE).getAsString());

            cleaned.addProperty(
                    SensingUtils.LocationKeys.ALTITUDE,
                    location.get(SensingUtils.LocationKeys.ALTITUDE).getAsString());

            cleaned.addProperty(
                    SensingUtils.LocationKeys.SPEED,
                    location.get(SensingUtils.LocationKeys.SPEED).getAsString());

            if(location.has(SensingUtils.LocationKeys.IS_STATIONARY))
                cleaned.addProperty(
                        SensingUtils.LocationKeys.IS_STATIONARY,
                        location.get(SensingUtils.LocationKeys.IS_STATIONARY).getAsString());

        }catch (NullPointerException | UnsupportedOperationException e){
            Log.e("ERROR", e.getMessage()+" in "+String.valueOf(location));
            e.printStackTrace();
        }


        return cleaned;
    }

    public void geoTagSample(JsonObject sample){
        JsonObject location =  cleanLocation(geoHistory.getLastKnownLocation());
        sample.add(SensingUtils.MotionKeys.LOCATION, location);
    }

    public void rotationTagSample(JsonObject sample){
        JsonObject rotation = geoHistory.getLastKnownRotation();
        sample.add(SensingUtils.MotionKeys.ROTATION, rotation);
    }
    /*
     ************************************************************************
     * Geo-Tagging Internal State                                           *
     ************************************************************************
     */
    public static class History {

        public static int MIN_2_FIXED = 5;

        //Sync
        private final Object lock = new Object();

        private CircularFifoQueue<JsonObject> geoTags;
        private CircularFifoQueue<JsonObject> rotationTags;

        protected History(){
            geoTags = new CircularFifoQueue<>(5);
            rotationTags = new CircularFifoQueue<>(5);
        }

        public boolean isEmpty(){
            return geoTags.isEmpty();
        }

        public void registerLocation(JsonObject location){
            synchronized (lock) {
                this.geoTags.add(location);
            }
        }

        public JsonObject getLastKnownLocation(){
            synchronized (lock) {
                return geoTags.peek();
            }
        }

        public void registerRotation(JsonObject rotation){
            synchronized (lock) {
                this.rotationTags.add(rotation);
            }
        }

        public JsonObject getLastKnownRotation(){
            synchronized (lock) {
                return rotationTags.peek();
            }
        }

        public void clearHistory(){
            geoTags.clear();
            rotationTags.clear();
        }

        public boolean hasFixedLocation(){
            return geoTags.size() > MIN_2_FIXED;
        }
    }

    /*
     ************************************************************************
     * Geo-Tagging Support Stages                                           *
     ************************************************************************
     */
    public interface LocationStages{

        class SortLocationsStage implements Stage {

            //Logging
            private final String LOG_TAG = "ActiveGeoTagger";
            private final String STAGE_TAG = "[SortLocationsStage]: ";

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                Arrays.sort(input, new Comparator<JsonObject>() {
                            @Override
                            public int compare(JsonObject l1, JsonObject l2) {

                                String s1 = "0", s2= "0";
                                try {
                                    s1 = l1.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString();
                                    s2 = l2.get(SensingUtils.LocationKeys.ELAPSED_TIME_NANOS).getAsString();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                                BigInteger scoutTime1 = new BigInteger(s1),
                                        scoutTime2 = new BigInteger(s2);

                                return scoutTime2.compareTo(scoutTime1);
                            }
                        }

                );

                ctx.setInput(input);
            }
        }

        class TrimStage extends LocationSensorPipeline.TrimStage{

            //Logging
            private final String LOG_TAG = "ActiveGeoTagger";
            private final String STAGE_TAG = "[TrimStage]: ";

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                JsonObject currentLocation, previousLocation;

                //Unexpected Scenario
                if(input.length > 2 || input.length < 1) {
                    Log.e(LOG_TAG, STAGE_TAG+"too many location samples...");
                    return;
                }

                JsonObject[] output;
                //First Time - no last known locations
                if(input.length == 1) {
                    output = new JsonObject[1];
                }else{
                    output = new JsonObject[2];
                    output[1] = input[1];
                }

                output[0] = trimLocationSample(input[0]);
                ctx.setInput(output);
            }
        }

        class CheckMovementStage implements Stage {

            public void addTravelledDistance(JsonObject baseLocation, JsonObject location){
                double  speed   = location.get(SensingUtils.LocationKeys.SPEED).getAsDouble(),
                        fromLat = baseLocation.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble(),
                        fromLon = baseLocation.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble(),
                        toLat   = location.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble(),
                        toLon   = location.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble(),
                        distance= LocationUtils.calculateDistance(fromLat, fromLon, toLat, toLon);

                boolean isStationary = LocationUtils.isStationary(distance, speed);

                //location.addProperty(SensingUtils.LocationKeys.TRAVELLED_DISTANCE, distance);
                location.addProperty(SensingUtils.LocationKeys.IS_STATIONARY, isStationary);

            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                if(input == null || input.length < 2) return;

                JsonObject baseLocation = input[0];
                for(int i=1; i < input.length; i++)
                    addTravelledDistance(baseLocation, input[i]);


            }
        }

        class AdmissionControlStage implements Stage {

            //Logging
            private final String LOG_TAG = "ActiveGeoTagger";
            private final String STAGE_TAG = "[TrimStage]: ";

            private PipelineConfiguration admissionPipeline;

            public AdmissionControlStage(){
                admissionPipeline = new PipelineConfiguration();
                admissionPipeline.addStage(new CommonStages.HeuristicsAdmissionControlStage.AccuracyOutlier());
                admissionPipeline.addStage(new CommonStages.HeuristicsAdmissionControlStage.SatellitesOutlier());
                admissionPipeline.addStage(new CommonStages.HeuristicsAdmissionControlStage.GPSSpeedOutlier());
                admissionPipeline.addStage(new HighTravelSpeedOutlier());
                //admissionPipeline.addStage(new OverlappingLocationsOutlier()); TODO: DECOMMENT

                admissionPipeline.addErrorStage(new CommonStages.HeuristicsAdmissionControlStage.InvalidateSample());
            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                JsonObject currentLocation = input[0];
                JsonObject previousLocation = (input.length == 2 ? input[1] : null);

                CommonStages.HeuristicsAdmissionControlStage.HeuristicAdmissionControlContext
                        mediationContext = new CommonStages.HeuristicsAdmissionControlStage.HeuristicAdmissionControlContext();

                mediationContext.setCurrentLocation(currentLocation);
                mediationContext.setPreviousLocation(previousLocation);

                admissionPipeline.execute(mediationContext);

                JsonObject[] validatedLocation = new JsonObject[1];
                if(!mediationContext.isValidSample()){
                    for(com.ideaimpl.patterns.pipeline.Error error : mediationContext.getErrors())
                        Log.w(LOG_TAG, STAGE_TAG+error.toString()+"{"+currentLocation+"}");
                    validatedLocation[0] = null;
                }else{
                    validatedLocation[0] = currentLocation;
                }

                ctx.setInput(validatedLocation);
            }

            protected static class HighTravelSpeedOutlier implements Stage{

                @Override
                public void execute(PipelineContext pipelineContext) {
                    CommonStages.HeuristicsAdmissionControlStage.HeuristicAdmissionControlContext
                            mediationContext = (CommonStages.HeuristicsAdmissionControlStage.HeuristicAdmissionControlContext) pipelineContext;

                    JsonObject currentLocation = mediationContext.getCurrentLocation();
                    JsonObject previousLocation = mediationContext.getPreviousLocation();

                    if(previousLocation == null) return; //Impossible to calculate

                    Location currLoc = new Location(currentLocation),
                            prevLoc = new Location(previousLocation);

                    float calculatedSpeed = prevLoc.getTraveledSpeed(currLoc);

                    if(calculatedSpeed > LocationState.MAX_SPEED)
                        mediationContext.invalidateSample(
                                "Discarded, calculated speed is too high ("+
                                        calculatedSpeed+")");
                }
            }

            protected static class OverlappingLocationsOutlier implements Stage {

                @Override
                public void execute(PipelineContext pipelineContext) {
                    CommonStages.HeuristicsAdmissionControlStage.HeuristicAdmissionControlContext
                            mediationContext = (CommonStages.HeuristicsAdmissionControlStage.HeuristicAdmissionControlContext) pipelineContext;

                    JsonObject currentLocation = mediationContext.getCurrentLocation();
                    JsonObject previousLocation = mediationContext.getPreviousLocation();

                    if(previousLocation == null) return;

                    Location currLoc = new Location(currentLocation),
                            prevLoc = new Location(previousLocation);

                    if(prevLoc.isOverlapping(currLoc) && currLoc.getErrorMargin() > prevLoc.getErrorMargin())
                        mediationContext.invalidateSample("Discarded, new location overlaps the previous, with lower accuracy.");
                }
            }
        }

        class UpdateGeoTaggerStage implements  Stage {

            //Logging
            private final String LOG_TAG = "ActiveGeoTagger";
            private final String STAGE_TAG = "[UpdateGeoTaggerStage]: ";

            private final History geoHistory;

            public UpdateGeoTaggerStage(History geoHistory){
                this.geoHistory = geoHistory;
            }
            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                if(input[0]!=null)
                    geoHistory.registerLocation(input[0]);

                ctx.setOutput(input); //USELESS - throws exception if not used SOLVE THIS TODO
            }
        }

    }

    protected interface RotationStages {

        class AdmissionControlStage implements Stage{

            private final String LOG_TAG = "ActiveGeoTagger";
            private final String STAGE_TAG = "[AdmissionControlStage]: ";

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                int invalidSamples=0;
                LinkedList<JsonObject> validSamples = new LinkedList<>();

                int accuracy;
                for(JsonObject sample : input){
                    accuracy = sample.get(SensingUtils.GeneralFields.ACCURACY).getAsInt();
                    if(accuracy >= SensingUtils.GeneralFields.HIGH_ACCURACY)
                        validSamples.add(sample);
                    else
                        invalidSamples++;
                }

                JsonObject[] output = new JsonObject[validSamples.size()];
                validSamples.toArray(output);
                ctx.setInput(output);
            }
        }

        class RotationMergeStrategy implements CommonStages.MergeStage.MergeStrategy {

            @Override
            public JsonObject mergeSamples(Collection<JsonObject> collection) {
                float   cosTheta = 0,
                        xSinTheta = 0,
                        ySinTheta = 0,
                        zSinTheta = 0;

                long scoutTime = 0;

                for(JsonObject sample : collection){
                    cosTheta += sample.get(RotationVectorKeys.COS_THETA_OVER2).getAsFloat();
                    xSinTheta += sample.get(RotationVectorKeys.X_SIN_THETA_OVER2).getAsFloat();
                    ySinTheta += sample.get(RotationVectorKeys.Y_SIN_THETA_OVER2).getAsFloat();
                    zSinTheta += sample.get(RotationVectorKeys.Z_SIN_THETA_OVER2).getAsFloat();
                    scoutTime += sample.get(SensingUtils.GeneralFields.SCOUT_TIME).getAsLong();
                }

                cosTheta /= collection.size();
                xSinTheta /= collection.size();
                ySinTheta /= collection.size();
                zSinTheta /= collection.size();
                scoutTime /= collection.size();

                JsonObject mergedSample = new JsonObject();
                mergedSample.addProperty(SensingUtils.GeneralFields.SENSOR_TYPE, SensingUtils.Sensors.ROTATION_VECTOR);
                mergedSample.addProperty(RotationVectorKeys.COS_THETA_OVER2, cosTheta);
                mergedSample.addProperty(RotationVectorKeys.X_SIN_THETA_OVER2, xSinTheta);
                mergedSample.addProperty(RotationVectorKeys.Y_SIN_THETA_OVER2, ySinTheta);
                mergedSample.addProperty(RotationVectorKeys.Z_SIN_THETA_OVER2, zSinTheta);
                mergedSample.addProperty(SensingUtils.GeneralFields.SCOUT_TIME, scoutTime);

                return  mergedSample;
            }
        }

        class MergeStage extends CommonStages.MergeStage{

            public MergeStage() {
                super(new RotationMergeStrategy());
            }

            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                JsonObject[] output = new JsonObject[1];
                output[0] = this.mergingStrategy.mergeSamples(Arrays.asList(input));

                ctx.setInput(output);
            }
        }

        class UpdateGeoTaggerHistory implements Stage{

            private final History history;

            public UpdateGeoTaggerHistory(History history){
                this.history = history;
            }


            @Override
            public void execute(PipelineContext pipelineContext) {
                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                if(input.length > 0)
                    history.registerRotation(input[0]);

                ctx.setOutput(ctx.getInput());
            }
        }

        class GenerateRotationMatrix implements Stage {

            //Logging
            private final String LOG_TAG = "ActiveGeoTagger";
            private final String STAGE_TAG = "[GenerateRotationMatrix]: ";

            private final Gson gson = new Gson();

            @Override
            public void execute(PipelineContext pipelineContext) {

                SensorPipelineContext ctx = (SensorPipelineContext) pipelineContext;
                JsonObject[] input = ctx.getInput();

                //Logging
                Log.d(LOG_TAG, STAGE_TAG+"Samples : "+input.length);
                if(input.length == 1) Log.d(LOG_TAG, STAGE_TAG+"{"+input[0]+"}");

                //Rotation Matrix
                JsonObject rotation = input[0];
                float[] R = new float[16];
                float[] IR = new float[16];
                float[] rotationVector = new float[4];

                //Fill the rotationVector
                rotationVector[0] = rotation.get(RotationVectorKeys.X_SIN_THETA_OVER2).getAsFloat();
                rotationVector[1] = rotation.get(RotationVectorKeys.Y_SIN_THETA_OVER2).getAsFloat();
                rotationVector[2] = rotation.get(RotationVectorKeys.Z_SIN_THETA_OVER2).getAsFloat();
                rotationVector[3] = rotation.get(RotationVectorKeys.COS_THETA_OVER2).getAsFloat();

                try {
                    SensorManager.getRotationMatrixFromVector(R, rotationVector);
                    Matrix.invertM(IR, 0, R, 0);

                    rotation.addProperty(
                            RotationVectorKeys.ROTATION_MATRIX,
                            gson.toJson(R));

                    rotation.addProperty(
                            RotationVectorKeys.INV_ROTATION_MATRIX,
                            gson.toJson(IR));

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }
}
