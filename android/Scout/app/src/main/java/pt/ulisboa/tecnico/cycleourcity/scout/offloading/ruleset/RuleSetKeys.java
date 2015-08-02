package pt.ulisboa.tecnico.cycleourcity.scout.offloading.ruleset;

public interface RuleSetKeys {

    public static final String
            RULESET             = "@rule-set",
            NAME                = "@name",
            DEFAULT             = "default-rule",
            RULES               = "@rules",
            BATTERY_RULE        = "@battery",
            NETWORK_TYPE_RULE   = "@network-type",
            REMAINING_DATA_PLAN = "@remaining-data-plan",
            OVER_DATA_LIMIT     = "@over-data-limit",
            SCOUT_BUDGET        = "@over-scout-budget",
            WEIGHTS             = "@weights",
            TIME_WEIGHT         = "@time-weight",
            TRANSMISSION_WEIGHT = "@transmission-weight",
            MOBILE_COST_WEIGHT  = "@mobile-cost-weight";

    public static final int
            NETWORK_WIFI       = 0, //Best-case (most restrictive)
            NETWORK_BLUETOOTH  = 1,
            NETWORK_4G         = 2, //Mobile best-case
            NETWORK_3G         = 3,
            NETWORK_2G         = 4,
            NETWORK_GPRS       = 5, //least-restrictive
            NETWORK_UNDEFINED  = -1; //Mobile worst-case

    enum SupportedNetworkTypes{
        NETWORK_WIFI,       //Best-case (most restrictive)
        NETWORK_BLUETOOTH,
        NETWORK_4G,         //Mobile best-case
        NETWORK_3G,
        NETWORK_2G,
        NETWORK_GPRS,       //least-restrictive
        NETWORK_UNDEFINED;
    }
}
