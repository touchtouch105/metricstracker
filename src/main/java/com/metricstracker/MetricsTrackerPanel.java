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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MetricsTrackerPanel extends PluginPanel
{
    @Inject
    private Client client;
    private final MetricsTrackerPlugin plugin;
    private final String PANEL_KEY_STRING = "PanelStringMasterKey";
    private final JPanel overallPanel = new JPanel();
    private final JLabel monstersKilled = new JLabel( "Killed" );
    private final JLabel monstersPerHour = new JLabel( "Per hour" );
    private final Map< String, MetricsInfoBox > infoBoxes = new HashMap<>();
    private final MetricsManager metricsManager = new MetricsManager();
    private MetricsTrackerConfig config;
    JComponent infoBoxPanel;

    public MetricsTrackerPanel( MetricsTrackerPlugin metricsTrackerPlugin, MetricsTrackerConfig config, Client client )
    {
        super();
        this.plugin = metricsTrackerPlugin;
        this.config = config;
        this.client = client;

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

        popupMenu.addPopupMenuListener( new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible( PopupMenuEvent popupMenuEvent )
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible( PopupMenuEvent popupMenuEvent )
            {
            }

            @Override
            public void popupMenuCanceled( PopupMenuEvent popupMenuEvent )
            {
            }
        });
        overallPanel.setComponentPopupMenu( popupMenu );

        final JLabel overallIcon = new JLabel( new ImageIcon( ImageUtil.loadImageResource(metricsTrackerPlugin.getClass(), "/metrics_tracker_icon.png" ) ) );

        final JPanel overallInfo = new JPanel();
        overallInfo.setBackground( ColorScheme.DARKER_GRAY_COLOR );
        overallInfo.setLayout( new GridLayout( 2, 1 ) );
        overallInfo.setBorder( new EmptyBorder( 0, 10, 0, 0) );

        monstersKilled.setFont( FontManager.getRunescapeSmallFont() );
        monstersPerHour.setFont( FontManager.getRunescapeSmallFont() );

        overallInfo.add( monstersKilled );
        overallInfo.add( monstersPerHour );

        overallPanel.add( overallIcon, BorderLayout.WEST );
        overallPanel.add( overallInfo, BorderLayout.CENTER );

        infoBoxPanel = new DragAndDropReorderPane();

        layoutPanel.add( overallPanel );
        layoutPanel.add( infoBoxPanel );

    }

    public void addEvent( Event event )
    {
        metricsManager.addDataPoint( event );

        if ( !infoBoxes.containsKey( event.getName() ) )
        {
            infoBoxes.put( event.getName(), new MetricsInfoBox( plugin, infoBoxPanel, event.getName() ) );
        }

        infoBoxes.get( event.getName() ).update( infoBoxPanel, event.getName(), metricsManager.getCumulativeQuantity( event.getName() ), metricsManager.getQuantityPerHour( event.getName() ) );
        updateOverallTrackerText();
    }

    public void resetAllInfoBoxes()
    {
        metricsManager.resetAll();

        for ( MetricsInfoBox box : infoBoxes.values() )
        {
            box.reset( infoBoxPanel );
        }

        infoBoxes.clear();

        monstersKilled.setText( "Total Killed:" );
        monstersPerHour.setText( "Total Per hour:" );
    }

    public void removeInfoBox( String name )
    {
        if ( infoBoxes.containsKey( name ) )
        {
            infoBoxes.get( name ).reset( infoBoxPanel );
            infoBoxes.remove( name );
        }

        metricsManager.reset( name );
    }

    public void removeOthers( String name )
    {
        int sz = infoBoxes.keySet().size() - 1;
        if ( sz >= 0 )
        {
            String keys[] = infoBoxes.keySet().toArray( new String[0] );
            for ( int i = sz; i >= 0; --i )
            {
                if ( !( keys[ i ].equals( name ) ) )
                {
                    removeInfoBox( keys[ i ] );
                }
            }
        }
        metricsManager.resetOthers( name );
    }

    private void updateOverallTrackerText()
    {
        final String killed = "Total Killed:" + metricsManager.getOverallCumulativeQuantity();
        final String kph = "Total Per hour:" + metricsManager.getOverallPerHour();

        monstersKilled.setText( killed );
        monstersPerHour.setText( kph );

    }

}
