package pt.ulisboa.tecnico.cycleourcity.scout.storage.gpx;

import com.google.gson.JsonObject;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exceptions.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.storage.exceptions.GPXBuilderException;

/**
 * @version 1.0
 * @author rodrigo.jm.lourenco
 *
 * GPX stands for GPS Exchange Format, and is an XML Schema designed as a common GPS data format
 * for software applications.
 */
public class GPXBuilder {

    private static GPXBuilder GPX_PARSER = new GPXBuilder();

    public final static String DEFAULT_FOLDER = "/tracks";
    public final static String FILE_EXTENSION = ".gpx";

    public final static int GPS_BASED_ALTITUDE = 0;
    public final static int GPS_AVERAGED_ALTITUDE = 1;
    public final static int PRESSURE_BASED_ALTITUDE = 2;

    private ScoutLogger logger = ScoutLogger.getInstance();

    private GPX gpx;
    private GPXParser parser = new GPXParser();

    private HashMap<Integer, ArrayList<Waypoint>> tracks;

    private GPXBuilder(){
        this.gpx = new GPX();
        this.tracks = new HashMap<>();
    }
    public static GPXBuilder getInstance() { return GPX_PARSER;}

    private ArrayList<Waypoint> retrieveTrackByTypeId(Integer type){

        if(!tracks.containsKey(type))
            tracks.put(type, new ArrayList<Waypoint>());

        return tracks.get(type);

    }

    private boolean trackExists(Integer type){
        return this.tracks.containsKey(type);
    }

    public void addTrackPoint(Integer type, JsonObject location) throws GPXBuilderException {

        ArrayList<Waypoint> track;

        //Determine the type of track to store
        switch (type){
            case GPS_BASED_ALTITUDE:
            case GPS_AVERAGED_ALTITUDE:
            case PRESSURE_BASED_ALTITUDE:
                track = retrieveTrackByTypeId(type);
                break;
            default:
                throw new GPXBuilderException();
        }

        //TODO: location está hardcoded, isto deve ser corrigido
        if(location == null && !SensingUtils.LocationSampleAccessor.isLocationSample(location))
            throw new GPXBuilderException();

        Waypoint waypoint = new Waypoint();

        Date time;
        double lat, lon, ele;
        time = new Date(SensingUtils.LocationSampleAccessor.getTimestamp(location));
        lat = SensingUtils.LocationSampleAccessor.getLatitude(location);
        lon = SensingUtils.LocationSampleAccessor.getLongitude(location);

        try {
            switch (type){
                case GPS_BASED_ALTITUDE:
                    ele = SensingUtils.LocationSampleAccessor.getAltitude(location);
                    break;
                case PRESSURE_BASED_ALTITUDE:
                    ele = SensingUtils.LocationSampleAccessor.getBarometricAltitude(location);
                    break;
                default:
                    throw new GPXBuilderException();
            }
        } catch (NoSuchDataFieldException e) {
            ele = 0;
        }

        waypoint.setLatitude(lat);
        waypoint.setLongitude(lon);
        waypoint.setElevation(ele);
        waypoint.setTime(time);

        track.add(waypoint);
    }


    public void storeAllGPXTracks(String path, String filename){

    	if(tracks.isEmpty())
    		System.out.println("WTF!!! - No track points");
    	
        for(Map.Entry<Integer, ArrayList<Waypoint>> trackEntry : tracks.entrySet()){
            switch (trackEntry.getKey()){
                case GPS_BASED_ALTITUDE:
                    storeGPXTrack(GPS_BASED_ALTITUDE, path, filename+"_justGPSAltitudes");
                    break;
                case GPS_AVERAGED_ALTITUDE:
                    storeGPXTrack(GPS_AVERAGED_ALTITUDE, path, filename+"_meanGPSAltitudes");
                    break;
                case PRESSURE_BASED_ALTITUDE:
                    storeGPXTrack(PRESSURE_BASED_ALTITUDE, path, filename+"_barometricAltitudes");
            }
        }

        tracks.clear();

    }
    public void storeGPXTrack(Integer type, String path, String filename){

        ArrayList<Waypoint> builtTrack = retrieveTrackByTypeId(type);

        if(builtTrack.isEmpty()){
            logger.log(ScoutLogger.ERR, "GPX", "empty");
            return;
        }else
        	logger.log(ScoutLogger.VERBOSE, "GPX", "NOT empty");

        File gpxFile = new File(path, filename+FILE_EXTENSION);

        GPX gpx = new GPX();
        Track track = new Track();

        track.setTrackPoints(builtTrack);
        gpx.addTrack(track);

        try {
            FileOutputStream out = new FileOutputStream(gpxFile);
            parser.writeGPX(gpx, out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
