package com.metricstracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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
    @Inject
    private OverlayManager overlayManager;

    private static final String ICON_FILE = "/metrics_tracker_icon.png";
    private static final String PLUGIN_NAME = "Metrics Tracker";
    private final DamageHandler damageHandler = new DamageHandler();
    private MetricsTrackerPanel loggerPanel;
    private NavigationButton navigationButton;
    private int tickCounter = 0;
    private List< String > blacklist = new ArrayList<>();
    private static boolean bUpdateConfig = false;
    private final MetricsSnapshot snapshotManager = new MetricsSnapshot();

    @Override
    protected void startUp() throws Exception
    {
         loggerPanel = new MetricsTrackerPanel( this, snapshotManager, client );
         final BufferedImage icon = ImageUtil.loadImageResource( getClass(), ICON_FILE );
         navigationButton = NavigationButton.builder()
											.tooltip( PLUGIN_NAME )
											.icon( icon )
											.priority( 6 )
											.panel( loggerPanel )
											.build();
        clientToolbar.addNavigation( navigationButton );

        blacklist = Text.fromCSV( config.blacklistedNPCs().toLowerCase() );
    }

    @Override
    protected void shutDown() throws Exception
    {
        resetState();
        clientToolbar.removeNavigation( navigationButton );
    }

    @Provides
    MetricsTrackerConfig provideConfig( ConfigManager configManager )
    {
        return configManager.getConfig( MetricsTrackerConfig.class );
    }

    @Subscribe
    public void onConfigChanged( ConfigChanged configChanged )
    {
        if ( configChanged.getKey().equals( "blacklistedNPCs" ) )
        {
            blacklist = Text.fromCSV( config.blacklistedNPCs() );
        }
    }

    @Subscribe
    public void onMetricEvent( MetricEvent metricEvent )
    {
        if ( !( blacklist.contains( metricEvent.getName().toLowerCase() ) ) )
        {
            loggerPanel.addEvent( metricEvent );
        }
    }

    @Subscribe
    public void onGameTick( GameTick gameTick )
    {
        if ( bUpdateConfig )
        {
            blacklist = Text.fromCSV( config.blacklistedNPCs().toLowerCase() );
            bUpdateConfig = false;
        }
        
        if ( config.refreshRate() > 0 )
        {
            tickCounter = ( tickCounter + 1 ) % config.refreshRate();
            if ( tickCounter == 0 )
            {
                loggerPanel.refreshActive();
            }
        }

        damageHandler.tick( npcUtil, eventBus, client.getLocalPlayer().getLocalLocation() );
        loggerPanel.tick();
    }

    @Subscribe
    public void onAnimationChanged( AnimationChanged animationChanged )
    {
        if ( animationChanged.getActor() instanceof NPC )
        {
            damageHandler.emitAnimationChange( animationChanged.getActor(), eventBus );
        }
    }

    @Subscribe
    public void onHitsplatApplied( HitsplatApplied event )
    {
        damageHandler.hitsplatApplied( event, npcUtil, eventBus );
    }

    public void resetState()
    {
        loggerPanel.resetAllInfoBoxes();
		overlayManager.removeIf( e -> e instanceof MetricsTrackerOverlay );
    }

    public void resetSingleMetric( MetricsInfoBox.infoBoxType type, String name )
    {
        loggerPanel.removeInfoBox( type, name );
    }

    void resetOthers( MetricsInfoBox.infoBoxType type, String name )
    {
        loggerPanel.removeOthers( type, name );
    }

    void addToCanvas( String name )
    {
        overlayManager.add( new MetricsTrackerOverlay( this, snapshotManager, name ) );
    }
	
    void removeFromCanvas( String name )
    {
        overlayManager.removeIf( e -> e instanceof MetricsTrackerOverlay && ((MetricsTrackerOverlay) e).getName() == name );
    }

    boolean hasOverlay( String name )
    {
        return overlayManager.anyMatch(o -> o instanceof MetricsTrackerOverlay && ((MetricsTrackerOverlay) o).getName() == name);
    }
	
    void blacklistNPC( MetricsInfoBox.infoBoxType type, String npcName )
    {
        List< String > vals = new ArrayList<>();
        vals.addAll( Text.fromCSV( configManager.getConfiguration( "metricstracker", "blacklistedNPCs" ) ) );

        if ( !vals.contains( npcName ) )
        {
            vals.add( npcName );
            configManager.setConfiguration( "metricstracker", "blacklistedNPCs", Text.toCSV( vals ) );
            bUpdateConfig = true;
			removeFromCanvas( npcName );
        }

        resetSingleMetric( type, npcName );
    }
}

