package pt.ulisboa.tecnico.cycleourcity.scout.storage;

import android.util.Log;

import com.google.gson.JsonObject;
import com.ideaimpl.patterns.pipeline.PipelineContext;
import com.ideaimpl.patterns.pipeline.Stage;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.pipeline.SensorPipelineContext;

/**
 * @version 1.0
 * @author rodrigo.jm.lourenco
 *
 * GPX stands for GPS Exchange Format, and is an XML Schema designed as a common GPS data format
 * for software applications.
 */
public class RouteStorage {

    //Logging
    private final String LOG_TAG = this.getClass().getSimpleName();

    private static RouteStorage GPX_PARSER = new RouteStorage();


    public final static String BASE_DIR_NAME = "/routes";
    private static File BASE_DIR;
    public final static String FILE_EXTENSION = ".gpx";

    public final static int GPS_BASED_ALTITUDE = 0;
    public final static int PRESSURE_BASED_ALTITUDE = 1;

    //AlternativeVision GPX Parser
    private GPXParser parser = new GPXParser();

    private HashMap<String, File> routeFiles;
    private ConcurrentHashMap<String, ArrayList<Waypoint>> tracks;

    //


    private RouteStorage(){
        this.tracks = new ConcurrentHashMap<>();

        routeFiles = new HashMap<>();

        BASE_DIR = new File(ScoutStorageManager.getApplicationFolder().toString()+"/"+BASE_DIR_NAME);
        if(!BASE_DIR.exists()) BASE_DIR.mkdirs();
    }

    public static RouteStorage getInstance() { return GPX_PARSER; }

    private ArrayList<Waypoint> retrieveTrackByTypeId(String routeID){

        if(!tracks.containsKey(routeID))
            tracks.put(routeID, new ArrayList<Waypoint>());

        return tracks.get(routeID);
    }

    private ArrayList<Waypoint> removeTrackByTypeID(String routeID){
        if(tracks.containsKey(routeID))
            return tracks.remove(routeID);
        else
            return null;
    }

    private Waypoint generatePressureBasedWaypoint(JsonObject pressureSample){

        Waypoint waypoint = new Waypoint();

        if(!pressureSample.has(SensingUtils.LocationKeys.LOCATION)
                || !pressureSample.has(SensingUtils.PressureKeys.ALTITUDE))
            return null;

        JsonObject location = (JsonObject) pressureSample.get(SensingUtils.LocationKeys.LOCATION);

        float slope = -1;
        if(pressureSample.has(SensingUtils.PressureKeys.SLOPE))
            slope = pressureSample.get(SensingUtils.PressureKeys.SLOPE).getAsFloat();

        try {
            waypoint.setLatitude(location.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble());
            waypoint.setLongitude(location.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble());
            waypoint.setElevation(pressureSample.get(SensingUtils.PressureKeys.ALTITUDE).getAsDouble());
            waypoint.setTime(new Date());
            waypoint.setComment("Slope = "+slope);
            waypoint.setDescription("Slope = "+slope);
        }catch (ClassCastException | NullPointerException e){
            e.printStackTrace();
            return null;
        }


        return waypoint;
    }

    private Waypoint generateGPSBasedLocation(JsonObject locationSample){
        Waypoint waypoint = new Waypoint();

        if(!locationSample.has(SensingUtils.LocationKeys.ALTITUDE)) return null;

        try {
            waypoint.setLatitude(locationSample.get(SensingUtils.LocationKeys.LATITUDE).getAsDouble());
            waypoint.setLongitude(locationSample.get(SensingUtils.LocationKeys.LONGITUDE).getAsDouble());
            waypoint.setElevation(locationSample.get(SensingUtils.LocationKeys.ALTITUDE).getAsDouble());
            waypoint.setTime(new Date());
        }catch (ClassCastException | NullPointerException e){
            e.printStackTrace();
            return null;
        }

        return waypoint;
    }

    //TODO: refractor all is incorrect
    public void addTrackPoint(String routeID, Integer type, JsonObject location){

        if(location==null) return;

        Waypoint waypoint = null;
        ArrayList<Waypoint> track = retrieveTrackByTypeId(routeID);

        switch (type){
            case PRESSURE_BASED_ALTITUDE:
                waypoint = generatePressureBasedWaypoint(location);
                break;
            case GPS_BASED_ALTITUDE:
                waypoint = generateGPSBasedLocation(location);
                break;
        }

        if(waypoint != null)
            track.add(waypoint);
    }


    private String generateRouteFilename(String routeID){
        return "route_"+routeID+"_"+System.currentTimeMillis()+ FILE_EXTENSION;
    }

    public void storeAllGPXTracks(){

        if(tracks.isEmpty()) return;

        try {
            for (String routeID : tracks.keySet())
                storeGPXTrack(routeID);
        }catch (Exception e){
            Log.e(LOG_TAG, "["+e.getClass().getSimpleName()+"]:"+e.getMessage());
        }

    }

    private void storeGPXTrack(final String routeID){

        final ArrayList<Waypoint> builtTrack = removeTrackByTypeID(routeID);

        if(builtTrack.isEmpty()) {
            Log.d(LOG_TAG, "Skipping "+routeID+" no waypoints found.");
            return;
        }else
            Log.d(LOG_TAG, "Saving "+routeID+" route, with "+builtTrack.size()+" waypoints.");

        File gpxFile = new File(BASE_DIR, generateRouteFilename(routeID));

        GPX gpx = new GPX();
        Track track = new Track();

        track.setTrackPoints(builtTrack);
        gpx.addTrack(track);

        try {
            FileOutputStream out = new FileOutputStream(gpxFile);
            parser.writeGPX(gpx, out);
            out.close();
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, routeID+"route storage failed, because "+e.getMessage());
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            Log.w(LOG_TAG, routeID+"route storage failed, because "+e.getMessage());
            e.printStackTrace();
        } catch (TransformerException e) {
            Log.w(LOG_TAG, routeID+"route storage failed, because "+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.w(LOG_TAG, routeID+"route storage failed, because "+e.getMessage());
            e.printStackTrace();
        }

        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
            }
        }).run();
        */
    }

    public static class RouteStorageStage implements Stage {

        protected int type;
        protected String routeID;
        private RouteStorage routeStorage = RouteStorage.getInstance();

        public RouteStorageStage(String routeID, int type){
            this.type   = type;
            this.routeID=routeID;
        }

        @Override
        public void execute(PipelineContext pipelineContext) {
            SensorPipelineContext ctx = (SensorPipelineContext)pipelineContext;
            JsonObject[] input = ctx.getInput();

            if(input == null) return;

            for(JsonObject sample : input){
                routeStorage.addTrackPoint(routeID, type, sample);
            }
        }
    }
}

