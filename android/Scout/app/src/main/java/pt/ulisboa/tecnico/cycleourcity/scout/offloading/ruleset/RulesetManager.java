package pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cycleourcity.scout.R;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleException;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleSetException;

public class RuleSetManager {

    private boolean VERBOSE = true;
    public final static String LOG_TAG = "RuleSet";

    private final Context appContext;
    private final JsonParser jsonParser = new JsonParser();

    //Rule Set
    private final List<Rule> ruleSet;
    private Rule defaultRule    = null;
    private Rule enforcedRule   = null;

    public RuleSetManager(Context context)
            throws InvalidRuleSetException {

        appContext = context;
        ruleSet = new ArrayList<>();

        fetchAndValidateRuleSet();
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

    public Rule selectRuleToEnforce(DeviceStateProfiler.DeviceStateSnapshot deviceState){

        if(deviceState.isCharging){
            if(VERBOSE) Log.d(LOG_TAG, "Device is charging so the default rule will be employed.");
            return defaultRule;
        }

        for(Rule rule : ruleSet)
            if(rule.ruleMatches(deviceState)) {
                enforcedRule = rule;
                return rule;
            }

        enforcedRule = defaultRule;
        return defaultRule;
    }
}
