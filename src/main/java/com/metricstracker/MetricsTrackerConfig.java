package com.metricstracker;

import net.runelite.client.config.*;

@ConfigGroup( "metricstracker" )
public interface MetricsTrackerConfig extends Config
{
    @ConfigItem(
            keyName = "refreshRate",
            name = "Passive Refresh Rate",
            description = "Number of ticks per passive refresh, 0 to disable",
            position = 1
    )
    default int refreshRate() { return 5; }

    @ConfigItem(
            keyName = "blacklistedNPCs",
            name = "NPC Blacklist",
            description = "Comma Separated list of blacklisted npcs",
            position = 2
    )
    default String blacklistedNPCs() { return ""; }
}
