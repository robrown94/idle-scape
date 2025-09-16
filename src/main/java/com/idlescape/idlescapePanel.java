package com.idlescape;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

public class idlescapePanel extends PluginPanel
{
    private final JLabel titleLbl = new JLabel("idlescape");
    private final JLabel goldLbl = new JLabel();
    private final JLabel gpsLbl  = new JLabel();
    private final JLabel minersLbl = new JLabel();
    private final JLabel woodcuttersLbl = new JLabel();
    private final JLabel fishersLbl = new JLabel();

    private final JButton minerBtn = new JButton("Buy Miner");
    private final JButton woodBtn  = new JButton("Buy Woodcutter");
    private final JButton fishBtn  = new JButton("Buy Fisher");
    private final JButton clickBtn = new JButton("Click for +1 gold");

    private final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);

    public idlescapePanel(Runnable buyMiner, Runnable buyWoodcutter, Runnable buyFisher, Runnable manualClick)
    {
        setLayout(new BorderLayout(6, 6));

        JPanel title = new JPanel(new GridLayout(2, 1, 4, 4));
        title.add(titleLbl);
        add(title, BorderLayout.NORTH);

        JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
        header.add(goldLbl);
        header.add(gpsLbl);
        add(header, BorderLayout.NORTH);

        JPanel shop = new JPanel(new GridLayout(3, 2, 4, 4));
        minerBtn.addActionListener(e -> buyMiner.run());
        woodBtn.addActionListener(e -> buyWoodcutter.run());
        fishBtn.addActionListener(e -> buyFisher.run());

        shop.add(minerBtn);        shop.add(minersLbl);
        shop.add(woodBtn);         shop.add(woodcuttersLbl);
        shop.add(fishBtn);         shop.add(fishersLbl);

        add(shop, BorderLayout.CENTER);

        clickBtn.addActionListener(e -> manualClick.run());
        add(clickBtn, BorderLayout.SOUTH);
    }

    /** Now shows costs in the right-hand labels and tooltips on the buttons. */
    public void updateState(long gold, int miners, int woodcutters, int fishers, int gps,
                            long nextMinerCost, long nextWoodCost, long nextFishCost)
    {
        goldLbl.setText("Gold: " + nf.format(gold));
        gpsLbl.setText("Gold/sec: " + nf.format(gps));

        minersLbl.setText("Miners: " + miners);
        woodcuttersLbl.setText("Woodcutters: " + woodcutters);
        fishersLbl.setText("Fishers: " + fishers);

        minerBtn.setEnabled(gold >= nextMinerCost);
        woodBtn.setEnabled(gold >= nextWoodCost);
        fishBtn.setEnabled(gold >= nextFishCost);

        minerBtn.setToolTipText("Cost: " + nf.format(nextMinerCost));
        woodBtn.setToolTipText("Cost: " + nf.format(nextWoodCost));
        fishBtn.setToolTipText("Cost: " + nf.format(nextFishCost));

        revalidate();
        repaint();
    }
}
