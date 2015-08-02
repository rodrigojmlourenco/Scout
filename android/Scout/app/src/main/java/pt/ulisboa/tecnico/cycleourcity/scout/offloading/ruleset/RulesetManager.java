package pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cycleourcity.scout.R;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;

public class RuleSetManager extends Observable{

    private boolean VERBOSE = true;
    public final static String LOG_TAG = "RuleSet";

    private final Context appContext;
    private final JsonParser jsonParser = new JsonParser();

    //Rule Set
    private final List<Rule> ruleSet;
    private Rule defaultRule    = null;
    private Rule enforcedRule   = null;

    //Device State
    private final DeviceStateProfiler deviceState;

    public RuleSetManager(Context context, DeviceStateProfiler deviceState)
            throws InvalidRuleSetException {

        appContext = context;
        ruleSet = new ArrayList<>();

        this.deviceState = deviceState;

        fetchAndValidateRuleSet();
        selectRuleToEnforce(deviceState.getDeviceState());

        initiateRuleSetEnforcer();
    }

    /**
     * Returns the currently active rule, specified in the rule set, given
     * the device's current state.
     * @return
     */
    public Rule getEnforcedRule(){
        return enforcedRule;
    }

    public float getEnforcedTimeWeight(){ return  enforcedRule.getTimeWeight(); }

    public float getEnforcedTransmissionWeight() { return  enforcedRule.getTransmissionWeight(); }

    public float getEnforcedMobileCostWeight() { return enforcedRule.getMobileCostWeight(); }


    /*
     ****************************************************
     * Support Functions                                *
     ****************************************************
     */
    private void fetchAndValidateRuleSet() throws InvalidRuleSetException{

        JsonArray jsonRuleSet = null;
        String ruleSetString = appContext.getString(R.string.rule_set);

        try {
            JsonObject aux = (JsonObject) jsonParser.parse(ruleSetString);
            jsonRuleSet = (JsonArray) aux.get(RuleSetKeys.RULESET);
        }catch (JsonSyntaxException e){
            throw new InvalidRuleSetException(e.getMessage());
        }

        if(jsonRuleSet == null || jsonRuleSet.size() <= 0)
            throw new InvalidRuleSetException("The rule set should not be empty.");

        Rule rule;
        JsonObject jsonRule;
        for(int i=0; i < jsonRuleSet.size(); i++){

            try {
                jsonRule = (JsonObject) jsonRuleSet.get(i);
                rule = new Rule(jsonRule);

                //Set the default rule
                if(rule.getRuleName().equals(RuleSetKeys.DEFAULT)){
                    if(defaultRule==null)
                        defaultRule = rule;
                    else
                        throw new InvalidRuleSetException("Only one default rule per rule set.");
                }

                ruleSet.add(rule);

            } catch (InvalidRuleException | ClassCastException e) {
                ruleSet.clear();
                throw  new InvalidRuleSetException(e.getMessage());
            }
        }

        if(defaultRule == null)
            throw new InvalidRuleSetException("A 'default' rule always must be defined.");

    }

    private void selectRuleToEnforce(DeviceStateProfiler.DeviceStateSnapshot deviceState){

        if(deviceState.isCharging){
            if(VERBOSE) Log.d(LOG_TAG, "Device is charging so the default rule will be employed.");
            enforcedRule = defaultRule;
        }

        for(Rule rule : ruleSet)
            if(rule.ruleMatches(deviceState))
                enforcedRule = rule;

        enforcedRule = defaultRule;
    }

    public void enforceRule(Rule rule){
        enforcedRule = rule;
        this.setChanged();
        notifyObservers(rule);
    }

    public void teardown(){
        terminateRuleSetEnforcer();
    }

    /*
     ****************************************************
     * Rule Set Enforcer                                *
     ****************************************************
     */
    private ScheduledFuture enforcerTask;
    private ScheduledExecutorService schedule;


    private void initiateRuleSetEnforcer(){
        schedule = Executors.newSingleThreadScheduledExecutor();
        enforcerTask = schedule.scheduleAtFixedRate(new
                RuleSetEnforcer(),
                10,
                10, TimeUnit.SECONDS);
    }

    private void terminateRuleSetEnforcer(){
        if(enforcerTask!=null){
            schedule.shutdown();
            try{
                if (!schedule.awaitTermination(60, TimeUnit.SECONDS)) {
                    schedule.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!schedule.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                schedule.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }


    private class RuleSetEnforcer implements Runnable {

        private DeviceStateProfiler.DeviceStateSnapshot lastRegisteredState;

        private RuleSetEnforcer(){
            this.lastRegisteredState = deviceState.getDeviceState();
        }

        private boolean hasDeviceStateChanged(DeviceStateProfiler.DeviceStateSnapshot currentState){
            return !lastRegisteredState.equals(currentState);
        }

        private Rule selectRuleToEnforce(DeviceStateProfiler.DeviceStateSnapshot deviceState){

            for(Rule rule : ruleSet)
                if(rule.ruleMatches(deviceState))
                    return  rule;

            return defaultRule;
        }


        @Override
        public void run() {

            DeviceStateProfiler.DeviceStateSnapshot currentState = deviceState.getDeviceState();

            if (hasDeviceStateChanged(currentState)){


                lastRegisteredState = currentState;

                Rule nRule = selectRuleToEnforce(lastRegisteredState);

                if(!enforcedRule.equals(nRule)){ //New rule to be enforced

                    if(VERBOSE) Log.d(LOG_TAG, "The device state has changed. Now enforcing rule "+nRule.getRuleName());

                    enforceRule(nRule);
                }else if(VERBOSE)
                    Log.v(LOG_TAG, "The device state has changed, however the rule still holds.");

            }else if(VERBOSE)
                Log.v(LOG_TAG, "The device state has not changed.");

        }
    }

}
