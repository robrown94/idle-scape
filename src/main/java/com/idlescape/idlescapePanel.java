package com.idlescape;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.IntConsumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.PluginPanel;

public class idlescapePanel extends PluginPanel
{
    // Header
    private final JLabel goldLbl = new JLabel();
    private final JLabel gpsLbl  = new JLabel();
    private final JLabel clickLbl = new JLabel();

    // Generators
    private final JButton[] genBtn;
    private final JLabel[]  genLbl;

    // Upgrades
    private final JButton[] upBtn;
    private final JLabel[]  upLbl;

    private final JButton clickBtn = new JButton("Click");

    public idlescapePanel(
            IntConsumer onBuyGen,
            IntConsumer onBuyUp,
            Runnable onClick,
            String[] genNames,
            String[] upNames
    )
    {
        setLayout(new BorderLayout(6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(3, 1, 2, 2));
        header.add(goldLbl);
        header.add(gpsLbl);
        header.add(clickLbl);
        add(header, BorderLayout.NORTH);

        // Body (scrollable)
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // Generators section
        body.add(sectionLabel("Generators"));
        JPanel gList = new JPanel(new GridLayout(0, 2, 4, 4));
        genBtn = new JButton[genNames.length];
        genLbl = new JLabel[genNames.length];
        for (int i = 0; i < genNames.length; i++)
        {
            genBtn[i] = new JButton("Buy " + genNames[i]);
            final int idx = i;
            genBtn[i].addActionListener(e -> onBuyGen.accept(idx));
            genLbl[i] = new JLabel("Owned: 0");
            gList.add(genBtn[i]); gList.add(genLbl[i]);
        }
        body.add(gList);
        body.add(Box.createVerticalStrut(8));

        // Upgrades section
        body.add(sectionLabel("Upgrades (one-time)"));
        JPanel uList = new JPanel(new GridLayout(0, 2, 4, 4));
        upBtn = new JButton[upNames.length];
        upLbl = new JLabel[upNames.length];
        for (int i = 0; i < upNames.length; i++)
        {
            upBtn[i] = new JButton("Buy " + upNames[i]);
            final int idx = i;
            upBtn[i].addActionListener(e -> onBuyUp.accept(idx));
            upLbl[i] = new JLabel("Not purchased");
            uList.add(upBtn[i]); uList.add(upLbl[i]);
        }
        body.add(uList);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(250, 420));
        add(scroll, BorderLayout.CENTER);

        // Click button
        clickBtn.addActionListener(e -> onClick.run());
        add(clickBtn, BorderLayout.SOUTH);
    }

    private JLabel sectionLabel(String txt)
    {
        JLabel l = new JLabel(txt);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    /**
     * Costs in tooltips only; buttons auto-enable/disable.
     * genGpsPerUnit[i] = effective GPS gained if you buy one more of generator i.
     */
    public void updateState(
            long gold,
            int gps,
            long clickPower,
            int[] gensOwned,
            boolean[] upsOwned,
            long[] genCosts,
            long[] upCosts,
            String[] upDescs,
            long[] genGpsPerUnit
    )
    {
        goldLbl.setText("Gold: " + String.format("%,d", gold));
        gpsLbl.setText("Gold/sec: " + String.format("%,d", gps));
        clickLbl.setText("Click power: +" + clickPower);
        clickBtn.setText("Click +" + clickPower);

        // Generators
        for (int i = 0; i < genBtn.length; i++)
        {
            boolean can = gold >= genCosts[i];
            genBtn[i].setEnabled(can);
            long current = (long)gensOwned[i] * genGpsPerUnit[i];
            genBtn[i].setToolTipText(
                    "Cost: " + String.format("%,d", genCosts[i]) +
                            "   •   +" + String.format("%,d", genGpsPerUnit[i]) + " gps per unit" +
                            "   •   current: " + String.format("%,d", current) + " gps"
            );
            genLbl[i].setText("Owned: " + gensOwned[i]);
        }

        // Upgrades
        for (int i = 0; i < upBtn.length; i++)
        {
            boolean purchased = upsOwned[i];
            upBtn[i].setEnabled(!purchased && gold >= upCosts[i]);
            upBtn[i].setToolTipText((purchased ? "[Purchased]  " : "") +
                    "Cost: " + String.format("%,d", upCosts[i]) +
                    "   •   " + upDescs[i]);
            upLbl[i].setText(purchased ? "Purchased" : "Not purchased");
        }

        revalidate();
        repaint();
    }
}
