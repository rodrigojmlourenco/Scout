package pt.ulisboa.tecnico.cycleourcity.scout.parser.gpx;

import android.util.Log;

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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.exception.NoSuchDataFieldException;
import pt.ulisboa.tecnico.cycleourcity.scout.parser.SensingUtils;

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

    private GPX gpx;
    private GPXParser parser = new GPXParser();

    private ArrayList<Waypoint> trackPoints;

    private GPXBuilder(){
        this.trackPoints = new ArrayList<>();
        this.gpx = new GPX();
    }
    public static GPXBuilder getInstance() { return GPX_PARSER;}

    public void addTrackPoint(JsonObject location){

        //TODO: location está hardcoded, isto deve ser corrigido
        if(!location.get(SensingUtils.SENSOR_TYPE).getAsString().equals("Location"))
            return;

        Waypoint waypoint = new Waypoint();

        Date time;
        double lat, lon, ele;
        time = new Date(SensingUtils.LocationSampleAccessor.getTimestamp(location));
        lat = SensingUtils.LocationSampleAccessor.getLatitude(location);
        lon = SensingUtils.LocationSampleAccessor.getLongitude(location);

        try {
            ele = SensingUtils.LocationSampleAccessor.getAltitude(location);
        } catch (NoSuchDataFieldException e) {
            ele = 0;
        }

        waypoint.setLatitude(lat);
        waypoint.setLongitude(lon);
        waypoint.setElevation(ele);
        waypoint.setTime(time);

        trackPoints.add(waypoint);
    }

    public void storeGPXTrack(String path, String filename){

        if(trackPoints.isEmpty()){
            Log.e("GPX", "empty");
            return;
        }

        File gpxFile = new File(path, filename+FILE_EXTENSION);

        GPX gpx = new GPX();
        Track track = new Track();

        track.setTrackPoints(trackPoints);
        gpx.addTrack(track);

        try {
            FileOutputStream out = new FileOutputStream(gpxFile);
            parser.writeGPX(gpx, out);
            out.close();

            trackPoints.clear();

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
