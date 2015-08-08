package pt.ulisboa.tecnico.cycleourcity.evalscout.network;

import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import pt.ulisboa.tecnico.cycleourcity.evalscout.network.exceptions.UnableToInitiateSessionException;

/**
 * Created by rodrigo.jm.lourenco on 27/07/2015.
 */
public class ScoutRemoteClient {

    private final Webb webb;
    private Gson gson = new Gson();

    private int sessionID = INVALID_SESSION;
    private final Object sessionLock = new Object();

    //Singleton
    private static ScoutRemoteClient CLIENT = null;

    public static final int INVALID_SESSION = -1;

    interface ScoutProxyEndpoints{
        String  ROOT                = "",
                REQUEST_SESSION     = "/initiate-session",
                TERMINATE_SESSION   = "/terminate-session",
                PUBLISH_FEATURES    = "/publish-features";
    }

    interface ScoutProxyEndpointFields {
        String  SESSION     = "_session",
                DATA        = "_data",
                TIMESTAMP   = "_timestamp";
    }

    private ScoutRemoteClient(){
        webb = Webb.create();
        webb.setBaseUri(ScoutProxyEndpoints.ROOT);
        //webb.setDefaultHeader(Webb.HDR_USER_AGENT, ); ? value ?
    }

    public static ScoutRemoteClient getInstance(){

        synchronized (ScoutRemoteClient.class){
            if(CLIENT == null)
                CLIENT = new ScoutRemoteClient();
        }

        return CLIENT;
    }

    //TODO: rever o output e tratamento de erros
    public int requestSession() throws UnableToInitiateSessionException {

        JSONObject response = webb
                .get(ScoutProxyEndpoints.REQUEST_SESSION)
                .ensureSuccess()
                .asJsonObject()
                .getBody();

        try {
            return response.getInt(ScoutProxyEndpointFields.SESSION);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        throw new UnableToInitiateSessionException();
    }

    //TODO: rever o output e tratamento de erros
    public int terminateSession(int sessionID){
        JSONObject response = webb
                .post(ScoutProxyEndpoints.TERMINATE_SESSION)
                .param(ScoutProxyEndpointFields.SESSION, sessionID)
                .ensureSuccess()
                .asJsonObject()
                .getBody();


        sessionID = -1;
        return 0;
    }


    //TODO: rever o output e tratamento de erros
    public int publishProcessedSamples(int sessionID, long timestamp, JsonObject[] processedSamples){

        JsonArray samplesAsJSONArray = new JsonArray();
        for(JsonObject sample : processedSamples)
            samplesAsJSONArray.add(sample);

        Response<JSONObject> response = webb
                .post(ScoutProxyEndpoints.PUBLISH_FEATURES)
                .param(ScoutProxyEndpointFields.SESSION, sessionID)
                .param(ScoutProxyEndpointFields.TIMESTAMP, timestamp)
                .param(ScoutProxyEndpointFields.DATA, gson.toJson(samplesAsJSONArray))
                .ensureSuccess()
                .asJsonObject();

        JSONObject apiResult = response.getBody();

        return 0;
    }

    public int publishProcessedSamples(int sessionID, long timestamp, JsonArray processedSamples){

        Response<JSONObject> response = webb
                .post(ScoutProxyEndpoints.PUBLISH_FEATURES)
                .param(ScoutProxyEndpointFields.SESSION, sessionID)
                .param(ScoutProxyEndpointFields.TIMESTAMP, timestamp)
                .param(ScoutProxyEndpointFields.DATA, gson.toJson(processedSamples))
                .ensureSuccess()
                .asJsonObject();

        JSONObject apiResult = response.getBody();

        return 0;
    }


    public int getCurrentSessionID() throws UnableToInitiateSessionException {

        if(sessionID == INVALID_SESSION)
            synchronized (sessionLock) {
                if (sessionID == INVALID_SESSION)
                    sessionID = requestSession();
            }

        return sessionID;
    }
}