package com.metricstracker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Experience;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.SkillColor;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;

import javax.swing.*;
public class MetricsTrackerOverlay extends OverlayPanel
{
    private final MetricsTrackerPlugin plugin;
    private final MetricsSnapshot snapshotManager;
    private static final PanelComponent splitPanelComponent = new PanelComponent();
    private static final int BORDER_SIZE = 2;
    private static final int XP_AND_PROGRESS_BAR_GAP = 2;
    private static final int XP_AND_ICON_GAP = 4;
    private static final Rectangle XP_AND_ICON_COMPONENT_BORDER = new Rectangle(2, 1, 4, 0);

    @Getter
    private final String name;

    MetricsTrackerOverlay(
            MetricsTrackerPlugin plugin,
            MetricsSnapshot snapshotManager,
            String name )
    {
        super( plugin );
        this.plugin = plugin;
        this.name = name;
        this.snapshotManager = snapshotManager;

        panelComponent.setBorder(new Rectangle(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        panelComponent.setGap(new Point(0, XP_AND_PROGRESS_BAR_GAP));
        splitPanelComponent.setBorder(XP_AND_ICON_COMPONENT_BORDER);
        splitPanelComponent.setBackgroundColor(null);
    }


    @Override
    public Dimension render(Graphics2D graphics)
    {
        splitPanelComponent.getChildren().clear();

        //Setting the font to rs small font so that the overlay isn't huge
        graphics.setFont(FontManager.getRunescapeSmallFont());

        final LineComponent nameLineComponent = LineComponent.builder().left(name).build();
        final LineComponent topLineComponent = LineComponent.builder().left(snapshotManager.getPrimaryQuantity( name )).right(snapshotManager.getPrimaryRate( name )).build();
        final LineComponent bottomLineComponent =  LineComponent.builder().left(snapshotManager.getAlternateQuantity( name )).right(snapshotManager.getAlternateRate( name )).build();

        splitPanelComponent.getChildren().add(nameLineComponent);
        splitPanelComponent.getChildren().add(topLineComponent);
        splitPanelComponent.getChildren().add(bottomLineComponent);

        panelComponent.getChildren().add(splitPanelComponent);
        return super.render(graphics);
    }
}
