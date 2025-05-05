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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsTrackerPanel extends PluginPanel
{
    private final MetricsTrackerPlugin plugin;
    private MetricsTrackerConfig config;
    @Inject
    private Client client;
    private final JPanel overallPanel = new JPanel();
    private final JLabel monstersKilled = new JLabel("Killed");
    private final JLabel monstersPerHour = new JLabel("Per hour");
    private final Map<MetricsManager, MetricsInfoBox> infoBoxes = new HashMap<>();
    private List<MetricsManager> metrics = new ArrayList<>();
    private MetricsManager overallMetrics = null;
    private final String PANEL_KEY_STRING = "PanelStringMasterKey";
    JComponent infoBoxPanel;
    public MetricsTrackerPanel(MetricsTrackerPlugin metricsTrackerPlugin, MetricsTrackerConfig config, Client client)
    {
        super();
        this.plugin = metricsTrackerPlugin;
        this.config = config;
        this.client = client;

        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        final JPanel layoutPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(layoutPanel, BoxLayout.Y_AXIS);
        layoutPanel.setLayout(boxLayout);
        add(layoutPanel, BorderLayout.NORTH);

        overallPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        overallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overallPanel.setLayout(new BorderLayout());
        overallPanel.setVisible(true); // this will only become visible when the player gets exp

        // Create reset all menu
        final JMenuItem reset = new JMenuItem("Reset All");
        reset.addActionListener(e -> plugin.resetState());

        // Create popup menu
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
        popupMenu.add(reset);

        popupMenu.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent)
            {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent)
            {
            }
        });
        overallPanel.setComponentPopupMenu(popupMenu);

        final JLabel overallIcon = new JLabel(new ImageIcon(ImageUtil.loadImageResource(metricsTrackerPlugin.getClass(), "/metrics_tracker_icon.png")));

        final JPanel overallInfo = new JPanel();
        overallInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overallInfo.setLayout(new GridLayout(2, 1));
        overallInfo.setBorder(new EmptyBorder(0, 10, 0, 0));

        monstersKilled.setFont(FontManager.getRunescapeSmallFont());
        monstersPerHour.setFont(FontManager.getRunescapeSmallFont());

        overallInfo.add(monstersKilled);
        overallInfo.add(monstersPerHour);

        overallPanel.add(overallIcon, BorderLayout.WEST);
        overallPanel.add(overallInfo, BorderLayout.CENTER);

        infoBoxPanel = new DragAndDropReorderPane();

        layoutPanel.add(overallPanel);
        layoutPanel.add(infoBoxPanel);

    }

    public void addEvent(Event event)
    {
        if ( this.overallMetrics == null )
        {
            this.overallMetrics = new MetricsManager( PANEL_KEY_STRING, event );
        }
        else
        {
            this.overallMetrics.addDataPoint( PANEL_KEY_STRING, event );
        }

        boolean found = false;
        for ( MetricsManager m : metrics )
        {
            if ( m.containsAnyKeyFrom(event.getInformation()))
            {
                found = true;
                m.addDataPoint( event );
                infoBoxes.get( m ).update( infoBoxPanel, m );
            }
        }

        if ( !found )
        {
            addMetric(event);
        }

        updateOverallTrackerText();
    }
    public void addMetric(Event event)
    {
        MetricsManager metric = new MetricsManager( event );
        metrics.add( metric );
        infoBoxes.put( metric, new MetricsInfoBox( plugin, infoBoxPanel, metric ) );
        infoBoxes.get( metric ).update( infoBoxPanel, metric );

    }
    public void resetAllInfoBoxes()
    {
        int sz = metrics.size();
        for ( int i = sz - 1; i >= 0; --i )
        {
            MetricsManager m = metrics.get(i);
            if ( infoBoxes.containsKey( m ) )
            {
                infoBoxes.get(m).reset( infoBoxPanel );
                infoBoxes.remove(m);
            }
            m.reset();
            metrics.remove(i);
        }

        this.overallMetrics = new MetricsManager( PANEL_KEY_STRING );
        monstersKilled.setText( "Total Killed:" );
        monstersPerHour.setText( "Total Per hour:" );
    }

    public void removeInfoBox(MetricsManager metric)
    {
        if ( infoBoxes.containsKey( metric ) )
        {
            infoBoxes.get(metric).reset( infoBoxPanel );
            infoBoxes.remove( metric );
        }

        if ( metrics.contains( metric ) )
        {
            metric.reset();
            metrics.remove( metric );
        }
    }

    public void removeOthers(MetricsManager metric)
    {
        for ( MetricsManager m : metrics )
        {
            if ( metric == m )
            {
                continue;
            }

            if ( infoBoxes.containsKey( m ) )
            {
                infoBoxes.get(m).reset( infoBoxPanel );
                infoBoxes.remove(m);
            }
            m.reset();
            metrics.remove(m);
        }
    }

    private void updateOverallTrackerText()
    {
        final String killed = "Total Killed:" + overallMetrics.getCumulativeQuantity( PANEL_KEY_STRING );
        final String kph = "Total Per hour:" + overallMetrics.getQuantityPerHour( PANEL_KEY_STRING );

        monstersKilled.setText( killed );
        monstersPerHour.setText( kph );

    }

}
