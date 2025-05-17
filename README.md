# Metrics Tracker Features
- Tracks total NPCs killed + killed per hour in side panel
- Tracks specific NPC names, total kills, and kills per hour in side panel
- Ability to reset any trackers, either specific npc, all but specific npc, and all
- Ability to blacklist any npc from the list of npcs tracked

# User guide
After installation, a new side panel will be created for Metrics Tracker. Ensure that the "NPC Kill Tracker" setting is checked within the Metrics Tracker plugin. After that, all you have to do is kill some monsters and they will be tracked. Currently, it tracks monsters based off the player dealing damage to them, so it will not track party monsters killed, and may have difficulty tracking if someone other than you deals the final hit to the monster.

Metrics will update either when a new data point comes in, or the background refresh interval elapses. The default value for the background refresh is 5 ticks, but can be configured to any value in the settings tab. Set the passive refresh value to 0 if you want to disable background refreshing.

In order to blacklist an npc, you can either enter the name manually in the "Blacklisted NPCs" field, or right click on a metric that has popped up and click blacklist NPC.
The format for the Blacklisted NPCs field is a comma separated list of npc names. This field is not case sensitive. Example of a blacklist: imp, monkey, black demon