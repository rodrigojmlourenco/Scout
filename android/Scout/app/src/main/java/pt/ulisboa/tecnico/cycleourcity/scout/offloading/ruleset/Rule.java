package pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset;

import android.util.Log;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.offloading.profiling.device.DeviceStateProfiler;
import pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset.exceptions.InvalidRuleException;

public class Rule {

    //Id
    private final String ruleName;

    //Enforced Rules
    private boolean
            hasBatteryRule          = false,
            hasNetworkTypeRule      = false,
            hasRemainingDataPlanRule= false,
            hasOverDataPlanLimitRule= false,
            hasScoutBudgetRule      = false;


    //Rules
    private int
            batteryRule,
            networkTypeRule,
            remainingDataPlanRule;

    private String networkTypeAsString;

    private boolean overDataLimitRule, scoutBudgetRule;

    //Weights
    private final float
            timeWeight,
            transmissionWeight,
            mobileCostWeight;


    public Rule(JsonObject rule) throws InvalidRuleException {

        //Name
        try {
            ruleName = rule.get(RuleSetKeys.NAME).getAsString();
        }catch (Exception e){
            throw new InvalidRuleException(e.getMessage());
        }

        //Rules
        if(ruleName != RuleSetKeys.DEFAULT) {

            JsonObject rules = (JsonObject) rule.get(RuleSetKeys.RULES);
            setBatteryRule(rules);
            setNetworkTypeRule(rules);
            setRemainingDataPlanRule(rules);
            setOverDataPlanLimitRule(rules);
            setScoutBudgetRule(rules);

            if (!hasBatteryRule)
                throw new InvalidRuleException("The " + RuleSetKeys.BATTERY_RULE + " rule is mandatory in rule "+ruleName);

            if(!hasNetworkTypeRule)
                throw new InvalidRuleException("The " + RuleSetKeys.NETWORK_TYPE_RULE + " rule is mandatory in rule"+ruleName);

            if(networkTypeRule == RuleSetKeys.NETWORK_UNDEFINED)
                throw new InvalidRuleException("Unknown "+networkTypeAsString+" network type in rule "+ruleName);

            if (!hasAtLeastOneRule())
                throw new InvalidRuleException("A rule must enforce at least one restriction");
        }

        //Weights
        JsonObject weights  = (JsonObject) rule.get(RuleSetKeys.WEIGHTS);
        timeWeight          = weights.get(RuleSetKeys.TIME_WEIGHT).getAsFloat();
        transmissionWeight  = weights.get(RuleSetKeys.TRANSMISSION_WEIGHT).getAsFloat();
        mobileCostWeight    = weights.get(RuleSetKeys.MOBILE_COST_WEIGHT).getAsFloat();

        float sums =timeWeight+transmissionWeight+mobileCostWeight;
        if(sums != 1 && (1-sums) > 0.1f)
            throw new InvalidRuleException("The weights sums ("+sums+") must be equal to 1.");
    }


    public String getRuleName() { return ruleName; }

    public float getTimeWeight() {
        return timeWeight;
    }

    public float getTransmissionWeight() {
        return transmissionWeight;
    }

    public float getMobileCostWeight() {
        return mobileCostWeight;
    }

    public boolean ruleMatches(DeviceStateProfiler.DeviceStateSnapshot deviceState){

        // MANDATORY
        //1. Check is current battery is lower or equal than the batteryRule
        if(deviceState.currentBattery > batteryRule)
            return false;

        // MANDATORY
        //2. Check if the current network type is lower or equal to the network type rule
        //  The network types are ordered from most restrictive (wifi) to least restrictive (GPRS)
        if(parseNetworkType(deviceState.networkType) > networkTypeRule)
            return false;

        // OPTIONAL
        //3. Check if the currently remaining data in the mobile data plan is lower than
        // the one specified by the rule.
        long remainingDataInPlan = deviceState.dataPlan -deviceState.consumedDataPlan;
        if(hasRemainingDataPlanRule && remainingDataInPlan > remainingDataPlanRule)
            return false;

        //TODO: serious doubts
        boolean dataBelowPlanLimit = deviceState.consumedDataPlan > deviceState.dataPlanLimit;
        if(hasOverDataPlanLimitRule && dataBelowPlanLimit != overDataLimitRule)
            return false;

        //TODO: scout budget rule

        return true;
    }

    /*
     ****************************************************************
     * Support Functions                                            *
     ****************************************************************
     */
    private void setBatteryRule(JsonObject rules){
        try {
            batteryRule = rules.get(RuleSetKeys.BATTERY_RULE).getAsInt();
            hasBatteryRule = true;
        }catch (Exception e){
            Log.w(RuleSetManager.LOG_TAG, "Unable to set battery rule because "+e.getMessage());
        }
    }

    private void setNetworkTypeRule(JsonObject rules){
        try {
            networkTypeAsString = rules.get(RuleSetKeys.NETWORK_TYPE_RULE).getAsString();
            networkTypeRule = parseNetworkType(networkTypeAsString);
            hasNetworkTypeRule = true;
        }catch (Exception e ){
            Log.w(RuleSetManager.LOG_TAG, "Unable to set network type rule rule because "+e.getMessage());
        }
    }

    private void setRemainingDataPlanRule(JsonObject rules){
        try{
            remainingDataPlanRule   = rules.get(RuleSetKeys.REMAINING_DATA_PLAN).getAsInt();
            hasRemainingDataPlanRule= true;
        }catch (Exception e ){
            Log.w(RuleSetManager.LOG_TAG, "Unable to set remaining data plan rule rule because "+e.getMessage());
        }
    }

    private void setOverDataPlanLimitRule(JsonObject rules){
        try{
            overDataLimitRule       = rules.get(RuleSetKeys.OVER_DATA_LIMIT).getAsBoolean();
            hasOverDataPlanLimitRule= true;
        }catch (Exception e){
            Log.w(RuleSetManager.LOG_TAG, "Unable to set over data plan rule rule because "+e.getMessage());
        }
    }

    private void setScoutBudgetRule(JsonObject rules){
        try{
            scoutBudgetRule     = rules.get(RuleSetKeys.SCOUT_BUDGET).getAsBoolean();
            hasScoutBudgetRule  = true;
        }catch (Exception e){
            Log.w(RuleSetManager.LOG_TAG, "Unable to set scout budget rule rule because "+e.getMessage());
        }
    }

    private boolean hasAtLeastOneRule(){
        return hasBatteryRule || hasRemainingDataPlanRule
                || hasNetworkTypeRule || hasOverDataPlanLimitRule || hasScoutBudgetRule;

    }

    @Override
    public boolean equals(Object rule) {

        Rule comparable = null;

        if(!(rule instanceof Rule))
            return false;
        else comparable = (Rule) rule;

        return batteryRule == comparable.batteryRule
                && hasNetworkTypeRule       == comparable.hasNetworkTypeRule
                && networkTypeRule          == comparable.networkTypeRule
                && hasRemainingDataPlanRule == comparable.hasRemainingDataPlanRule
                && remainingDataPlanRule    == comparable.remainingDataPlanRule
                && hasOverDataPlanLimitRule == comparable.hasOverDataPlanLimitRule
                && overDataLimitRule        == comparable.overDataLimitRule
                && hasScoutBudgetRule       == comparable.hasScoutBudgetRule
                && scoutBudgetRule          == comparable.scoutBudgetRule;
    }

    private int parseNetworkType(RuleSetKeys.SupportedNetworkTypes networkType){
        switch (networkType){
            case NETWORK_WIFI:
                return RuleSetKeys.NETWORK_WIFI;
            case NETWORK_BLUETOOTH:
                return RuleSetKeys.NETWORK_BLUETOOTH;
            case NETWORK_4G:
                return RuleSetKeys.NETWORK_4G;
            case NETWORK_3G:
                return RuleSetKeys.NETWORK_3G;
            case NETWORK_2G:
                return RuleSetKeys.NETWORK_2G;
            case NETWORK_GPRS:
                return RuleSetKeys.NETWORK_GPRS;
            default :
                return RuleSetKeys.NETWORK_UNDEFINED;
        }
    }

    private int parseNetworkType(String networkType){
        switch (networkType){
            case "wifi":
                return RuleSetKeys.NETWORK_WIFI;
            case "bluetooth":
                return RuleSetKeys.NETWORK_BLUETOOTH;
            case "4G":
            case "LTE":
            case "lte":
                return RuleSetKeys.NETWORK_4G;
            case "3G":
            case "UMTS":
            case "umts":
                return RuleSetKeys.NETWORK_3G;
            case "2G":
            case "edge":
            case "EDGE":
            case "CDMA":
            case "cdma":
                return RuleSetKeys.NETWORK_2G;
            case "GPRS":
            case "gprs":
                return RuleSetKeys.NETWORK_GPRS;
            default :
                return RuleSetKeys.NETWORK_UNDEFINED;
        }
    }
}
