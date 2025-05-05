package com.metricstracker;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.MouseDragEventForwarder;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.ColorUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MetricsInfoBox extends JPanel
{
	static final DecimalFormat TWO_DECIMAL_FORMAT = new DecimalFormat("0.00");

	static
	{
		TWO_DECIMAL_FORMAT.setRoundingMode(RoundingMode.DOWN);
	}

	// Templates
	private static final String HTML_LABEL_TEMPLATE =
		"<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";

	private static final EmptyBorder DEFAULT_PROGRESS_WRAPPER_BORDER = new EmptyBorder(0, 7, 7, 7);
	private static final EmptyBorder COMPACT_PROGRESS_WRAPPER_BORDER = new EmptyBorder(5, 1, 5, 5);

	@Getter(AccessLevel.PACKAGE)
	private final MetricsManager metric;

	/* The tracker's wrapping container */
	private final JPanel container = new JPanel();

	/* Contains the skill icon and the stats panel */
	private final JPanel headerPanel = new JPanel();

	/* Contains all the skill information (exp gained, per hour, etc) */
	private final JPanel statsPanel = new JPanel();

	// Contains progress bar and compact-view icon
	private final JPanel progressWrapper = new JPanel();

	private final ProgressBar progressBar = new ProgressBar();

	private final JLabel topLeftStat = new JLabel();
	private final JLabel bottomLeftStat = new JLabel();
	private final JLabel topRightStat = new JLabel();
	private final JLabel bottomRightStat = new JLabel();
	private JComponent panel;

	MetricsInfoBox(MetricsTrackerPlugin plugin, JComponent panel, MetricsManager metric )
	{
		this.metric = metric;
		this.panel = panel;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(5, 0, 0, 0));

		container.setLayout(new BorderLayout());
		container.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Create reset menu
		final JMenuItem reset = new JMenuItem("Reset");
		reset.addActionListener(e -> plugin.resetSingleMetric(metric) );

		final JMenuItem resetOthers = new JMenuItem("Reset Others");
		resetOthers.addActionListener(e -> plugin.resetOthers(metric) );

		// Create popup menu
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));

		popupMenu.add(reset);
		popupMenu.add(resetOthers);
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

		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setLayout(new BorderLayout());

		statsPanel.setLayout(new DynamicGridLayout(2, 2));
		statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statsPanel.setBorder(new EmptyBorder(9, 2, 9, 2));

		topLeftStat.setFont(FontManager.getRunescapeSmallFont());
		bottomLeftStat.setFont(FontManager.getRunescapeSmallFont());
		topRightStat.setFont(FontManager.getRunescapeSmallFont());
		bottomRightStat.setFont(FontManager.getRunescapeSmallFont());

		statsPanel.add(topLeftStat);     // top left
		statsPanel.add(topRightStat);    // top right
		statsPanel.add(bottomLeftStat);  // bottom left
		statsPanel.add(bottomRightStat); // bottom right

		headerPanel.add(statsPanel, BorderLayout.CENTER);
		container.add(headerPanel, BorderLayout.NORTH);

		container.setComponentPopupMenu(popupMenu);
		progressBar.setComponentPopupMenu(popupMenu);

		// forward mouse drag events to parent panel for drag and drop reordering
		MouseDragEventForwarder mouseDragEventForwarder = new MouseDragEventForwarder(panel);
		container.addMouseListener(mouseDragEventForwarder);
		container.addMouseMotionListener(mouseDragEventForwarder);
		progressBar.addMouseListener(mouseDragEventForwarder);
		progressBar.addMouseMotionListener(mouseDragEventForwarder);

		add(container, BorderLayout.NORTH);
	}

	public void reset( JComponent panel )
	{
		panel.remove(this);
		panel.revalidate();
	}

	void update( JComponent panel, MetricsManager metricsSnapshotSingle)
	{
		SwingUtilities.invokeLater(() -> rebuildAsync(panel, metricsSnapshotSingle));
	}

	private void toggleCompactView()
	{
		final boolean isCompact = !headerPanel.isVisible();
		setCompactView(!isCompact);
	}

	private void setCompactView(final boolean compact)
	{
		progressWrapper.setBorder(compact ? COMPACT_PROGRESS_WRAPPER_BORDER : DEFAULT_PROGRESS_WRAPPER_BORDER);
		headerPanel.setVisible(!compact);
	}

	private void rebuildAsync( JComponent panel, MetricsManager metricsSnapshotSingle )
	{
		if (getParent() != panel)
		{
			panel.add(this);
			panel.revalidate();
		}

		String key = metricsSnapshotSingle.getLastEvent().getInformation().get(0);

		topLeftStat.setText(htmlLabel("Monster:",  key ) );
		topRightStat.setText(htmlLabel("Total Kills:",  metricsSnapshotSingle.getCumulativeQuantity( key ) ));
		bottomLeftStat.setText(htmlLabel("Kills Per hour:",  metricsSnapshotSingle.getQuantityPerHour( key ) ) );

	}
	static String htmlLabel(String key, float value)
	{
		String valueStr = Float.toString( value );
		return htmlLabel(key, valueStr);
	}
	static String htmlLabel(String key, long value)
	{
		String valueStr = Integer.toString( (int) value );
		return htmlLabel(key, valueStr);
	}
	static String htmlLabel(String key, String valueStr)
	{
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, valueStr );
	}
}
