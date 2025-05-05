package com.metricstracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Metrics Tracker",
        description = "Trackers miscellaneous player metrics"
)
public class MetricsTrackerPlugin extends Plugin
{
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ConfigManager configManager;
    @Inject
    private EventBus eventBus;
    @Inject
    private NpcUtil npcUtil;
    @Inject
    private MetricsTrackerConfig config;
    @Inject
    private ClientToolbar clientToolbar;
    private EventConsumer consumer;
    private final DamageHandler damageHandler = new DamageHandler();
    private MetricsTrackerPanel loggerPanel;
    private static final String ICON_FILE = "/metrics_tracker_icon.png";
    private static final String PLUGIN_NAME = "Metrics Tracker";
    @Override
    protected void startUp() throws Exception
    {
         loggerPanel = new MetricsTrackerPanel( this , config, client );
         final BufferedImage icon = ImageUtil.loadImageResource(getClass(), ICON_FILE);
         NavigationButton navigationButton = NavigationButton.builder()
            .tooltip(PLUGIN_NAME)
            .icon(icon)
            .priority(6)
            .panel(loggerPanel)
            .build();
        clientToolbar.addNavigation(navigationButton);
        consumer = new EventConsumer(loggerPanel);
    }
    @Provides
    MetricsTrackerConfig provideConfig(ConfigManager configManager )
    {
        return configManager.getConfig( MetricsTrackerConfig.class );
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if ( !config.monstersKilled() )
        {
            return;
        }

        damageHandler.tick( consumer, npcUtil );
        consumer.consumePendingEvents();
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if ( !config.monstersKilled() )
        {
            return;
        }
        Actor actor = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        if ( damageHandler.isMonsterKilledEvent( hitsplat, actor, npcUtil )
        &&   config.monstersKilled() )
        {
            damageHandler.emitMonsterKilledEvent( actor, consumer );
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        if ( config.monstersKilled() )
        {
//            damageHandler.emitNPCDespawnEvent(event.getActor(), consumer);
        }
    }

    public void resetState()
    {
        loggerPanel.resetAllInfoBoxes();
    }

    public void resetSingleMetric(MetricsManager metric)
    {
        loggerPanel.removeInfoBox(metric);
    }

    void resetOthers(MetricsManager metric)
    {
        loggerPanel.removeOthers(metric);
    }


}
