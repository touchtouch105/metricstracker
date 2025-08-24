package com.metricstracker;

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.DragAndDropReorderPane;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import java.util.HashMap;
import java.util.Map;

public class MetricsTrackerPanel extends PluginPanel
{
    @Inject
    private Client client;
    private final MetricsTrackerPlugin plugin;
    private final MetricsSnapshot snapshotManager;
    private final JPanel overallPanel = new JPanel();
    private final JLabel totalQuantity = new JLabel( "Killed:" );
    private final JLabel totalRate = new JLabel( "KPH:" );
    private final JLabel altQuantity = new JLabel( "Damage:" );
    private final JLabel altRate = new JLabel( "DPS:" );
    private final Map< MetricsInfoBox.infoBoxType, Map< String, MetricsInfoBox > > infoBoxes = new HashMap<>();
    private final Map< MetricsInfoBox.infoBoxType, MetricsManager > metrics = new HashMap<>();
    private MetricsInfoBox.infoBoxType currentDisplayType = MetricsInfoBox.infoBoxType.MONSTERS;
    
    JComponent infoBoxPanel;

    public MetricsTrackerPanel( MetricsTrackerPlugin metricsTrackerPlugin, MetricsSnapshot snapshotManager, Client client )
    {
        super();
        this.plugin = metricsTrackerPlugin;
        this.client = client;
        this.snapshotManager = snapshotManager;

        setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
        setBackground( ColorScheme.DARK_GRAY_COLOR );
        setLayout( new BorderLayout() );

        final JPanel layoutPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout( layoutPanel, BoxLayout.Y_AXIS );
        layoutPanel.setLayout( boxLayout );
        add( layoutPanel, BorderLayout.NORTH );

        overallPanel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
        overallPanel.setBackground( ColorScheme.DARKER_GRAY_COLOR );
        overallPanel.setLayout( new BorderLayout() );
        overallPanel.setVisible( true ); // this will only become visible when the player gets exp

        // Create reset all menu
        final JMenuItem reset = new JMenuItem( "Reset All" );
        reset.addActionListener( e -> plugin.resetState() );

        // Create popup menu
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        popupMenu.add( reset );

        overallPanel.setComponentPopupMenu( popupMenu );

        final JLabel overallIcon = new JLabel( new ImageIcon( ImageUtil.loadImageResource(metricsTrackerPlugin.getClass(), "/metrics_tracker_icon.png" ) ) );
        final JPanel overallInfo = new JPanel();

        overallInfo.setBackground( ColorScheme.DARKER_GRAY_COLOR );
        overallInfo.setLayout( new GridLayout( 2, 2 ) );
        overallInfo.setBorder( new EmptyBorder( 0, 10, 0, 0) );

        totalQuantity.setFont( FontManager.getRunescapeSmallFont() );
        totalRate.setFont( FontManager.getRunescapeSmallFont() );
        altQuantity.setFont( FontManager.getRunescapeSmallFont() );
        altRate.setFont( FontManager.getRunescapeSmallFont() );

        overallInfo.add( totalQuantity );
        overallInfo.add( totalRate );
        overallInfo.add( altQuantity );
        overallInfo.add( altRate );

        overallPanel.add( overallIcon, BorderLayout.WEST );
        overallPanel.add( overallInfo, BorderLayout.CENTER );

        infoBoxPanel = new DragAndDropReorderPane();

        layoutPanel.add( overallPanel );
        layoutPanel.add( infoBoxPanel );
    }

    public void addEvent( MetricEvent metricEvent )
    {
        MetricsInfoBox.infoBoxType type = getInfoBoxType( metricEvent.getType() );
        String eventOriginalName = metricEvent.getName();
        boolean isSecondaryMetric = false;

        if ( metricEvent.getType() == MetricEvent.eventType.DAMAGE_DEALT )
        {
            isSecondaryMetric = true;
            metricEvent.name = ( "DPS_" + eventOriginalName );
        }

        if ( metrics.containsKey( type ) )
        {
            metrics.get( type ).addDataPoint(metricEvent, isSecondaryMetric, eventOriginalName );
        }
        else
        {
            metrics.put( type, new MetricsManager() );
            metrics.get( type ).addDataPoint(metricEvent, isSecondaryMetric, eventOriginalName );
        }

        if ( type == currentDisplayType )
        {
            updateInfoBox( type, metricEvent, eventOriginalName );
        }

        updateOverallTrackerText();
    }

    private void updateInfoBox( MetricsInfoBox.infoBoxType type, MetricEvent metricEvent, String eventOriginalName )
    {
        Map< String, MetricsInfoBox > map;
        MetricsManager metric;

        if ( infoBoxes.containsKey( type ) )
        {
            map = infoBoxes.get( type );
        }
        else
        {
            map = new HashMap<>();
        }

        if ( !map.containsKey( eventOriginalName ) )
        {
            map.put( eventOriginalName, new MetricsInfoBox( plugin, infoBoxPanel, eventOriginalName, type ) );
            infoBoxes.put( type, map );
        }

        metric = metrics.get( type );
        MetricsInfoBox infoBox = infoBoxes.get( type ).get( eventOriginalName );

        final long quant = metric.getCumulativeQuantity( eventOriginalName );
        final float rate = metric.getQuantityPerHour( eventOriginalName );
        final long altQuant = metric.getCumulativeQuantity( metricEvent.getName() );
        final float altRate = metric.getQuantityPerSecond( metricEvent.getName() );

        if ( type == MetricsInfoBox.infoBoxType.MONSTERS
        &&   metricEvent.getType() == MetricEvent.eventType.DAMAGE_DEALT )
        {
            infoBox.update( infoBoxPanel,
                            eventOriginalName,
                            quant,
                            rate,
                            altQuant,
                            altRate );

            snapshotManager.updateSnapshot( eventOriginalName, quant, rate, altQuant, altRate );
        }
        else
        {
            infoBox.update( infoBoxPanel,
                            eventOriginalName,
                            quant,
                            rate );

            snapshotManager.updateSnapshot( eventOriginalName, quant, rate );
        }
    }

    private MetricsInfoBox.infoBoxType getInfoBoxType( MetricEvent.eventType eventType )
    {
        MetricsInfoBox.infoBoxType type = MetricsInfoBox.infoBoxType.NONE;
        switch ( eventType )
        {
            case MONSTERS_KILLED:
            case DAMAGE_DEALT:
                type = MetricsInfoBox.infoBoxType.MONSTERS;
                break;
            default:
                break;
        }

        return type;
    }

    public void resetAllInfoBoxes()
    {
        for ( MetricsManager metricsManager : metrics.values() )
        {
            metricsManager.resetAll();
        }

        for ( Map< String, MetricsInfoBox > map : infoBoxes.values() )
        {
            for ( MetricsInfoBox box : map.values() )
            {
                box.reset( infoBoxPanel );
            }
        }

        infoBoxes.clear();
        updateOverallTrackerText();
    }

    public void removeInfoBox( MetricsInfoBox.infoBoxType type, String name )
    {
        if ( infoBoxes.containsKey( type ) )
        {
            if ( infoBoxes.get( type ).containsKey( name ) )
            {
                infoBoxes.get( type ).get( name ).reset( infoBoxPanel );
                infoBoxes.get( type ).remove( name );
            }
        }

        if ( metrics.containsKey( type ) )
        {
            metrics.get( type ).reset( name );
            metrics.remove( type, name );
        }
    }

    public void removeOthers( MetricsInfoBox.infoBoxType type, String name )
    {
        Map< String, MetricsInfoBox > infoBoxesLocal;
        if ( !infoBoxes.containsKey( type ) )
        {
            return;
        }

        infoBoxesLocal = infoBoxes.get( type );
        int sz = infoBoxesLocal.keySet().size() - 1;
        if ( sz >= 0 )
        {
            String keys[] = infoBoxesLocal.keySet().toArray( new String[ 0 ] );
            for ( int i = sz; i >= 0; --i )
            {
                if ( !( keys[ i ].equals( name ) ) )
                {
                    removeInfoBox( type, keys[ i ] );
                }
            }
        }

        if ( metrics.containsKey( type ) )
        {
            metrics.get( type ).resetOthers( name );
        }
    }

    private void updateOverallTrackerText()
    {
        String quantity;
        String rate;
        String altQ;
        String altR;

        switch ( currentDisplayType )
        {
            case MONSTERS:
            default:
                quantity = "Killed:";
                rate = "KPH:";
                altQ = "Damage:";
                altR = "DPS:";
                break;
        }

        if ( metrics.containsKey( currentDisplayType ) )
        {
            switch ( currentDisplayType )
            {
                case MONSTERS:
                default:
                    quantity += metrics.get( currentDisplayType ).getOverallCumulativeQuantity( false );
                    rate += metrics.get( currentDisplayType ).getOverallPerHour( false );
                    altQ += metrics.get( currentDisplayType ).getOverallCumulativeQuantity( true );
                    altR += metrics.get( currentDisplayType ).getOverallPerSecond( true );
                    break;
            }
        }

        totalQuantity.setText( quantity );
        totalRate.setText( rate );
        altQuantity.setText( altQ );
        altRate.setText( altR );
    }

    public void refreshActive()
    {
        MetricEvent metricEvent;
        String infoBoxKey;
        for ( MetricsInfoBox.infoBoxType type : metrics.keySet() )
        {
            if ( type == currentDisplayType )
            {
                for ( String key : metrics.get( type ).getKeys() )
                {
                    infoBoxKey = metrics.get( type ).getMetricKey( key );
                    metricEvent = metrics.get( type ).lastEvent.get( key );
                    metricEvent.quantity = 0;

                    updateInfoBox( type, metricEvent, infoBoxKey );
                }
            }
        }

        updateOverallTrackerText();
    }

    public void loadInfoBoxes( MetricsInfoBox.infoBoxType type )
    {
        currentDisplayType = type;

        for ( MetricsInfoBox.infoBoxType t : infoBoxes.keySet() )
        {
            for ( String name : infoBoxes.get( t ).keySet() )
            {
                infoBoxes.get( t ).get( name ).reset( infoBoxPanel );
            }
        }

        refreshActive();
    }
}
