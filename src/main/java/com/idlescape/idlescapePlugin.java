package com.idlescape;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
        name = "idle-scape",
        description = "idle-scape, play an idle game within a game while idling!",
        tags = {"idle", "scape", "game", "clicker", "panel"}
)
public class idlescapePlugin extends Plugin
{
    @Inject private ClientToolbar clientToolbar;
    @Inject private idlescapeConfig config;
    @Inject private ConfigManager configManager;

    private idlescapePanel panel;
    private NavigationButton navButton;
    private Timer timer; // 1-second tick

    private long gold;
    private int miners;
    private int woodcutters;
    private int fishers;

    // ==== RuneLite wiring ====
    @Provides
    idlescapeConfig provideConfig(ConfigManager cm) { return cm.getConfig(idlescapeConfig.class); }

    @Override
    protected void startUp()
    {
        // Load saved state
        gold = config.gold();
        miners = config.miners();
        woodcutters = config.woodcutters();
        fishers = config.fishers();

        // Build panel with actions
        panel = new idlescapePanel(
                this::buyMiner,
                this::buyWoodcutter,
                this::buyFisher,
                () -> addGold(1)
        );

        // Load icon (optional, safe if missing)
        BufferedImage icon = null;
        try
        {
            // Looks for /com/idlescape/icon.png (same package as this class)
            icon = ImageUtil.loadImageResource(idlescapePlugin.class, "icon.png");
        }
        catch (IllegalArgumentException e)
        {
            log.warn("Icon missing, continuing without one");
        }

        NavigationButton.NavigationButtonBuilder nb = NavigationButton.builder()
                .tooltip("idlescape")
                .priority(5)
                .panel(panel);
        if (icon != null) nb.icon(icon);
        navButton = nb.build();

        clientToolbar.addNavigation(navButton);

        // Initial render
        refreshPanel();

        // Start game loop (1s)
        timer = new Timer(1000, e -> tick());
        timer.start();
    }

    @Override
    protected void shutDown()
    {
        if (timer != null) timer.stop();
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        save();
        panel = null;
    }

    // ==== Game Logic ====

    private void tick()
    {
        long delta = calcGps();
        if (delta > 0)
        {
            gold += delta;
            SwingUtilities.invokeLater(this::refreshPanel);
            // light autosave
            if ((gold % 50) == 0) save();
        }
    }

    private int calcGps()
    {
        // Simple baseline: M=1, W=3, F=10 gps each
        return miners * 1 + woodcutters * 3 + fishers * 10;
    }

    private long costFor(int owned, long base)
    {
        // Exponential price curve: base * 1.15^owned
        return Math.round(base * Math.pow(1.15, owned));
    }

    private void buyMiner()
    {
        long cost = costFor(miners, 50);
        if (gold < cost) return;
        gold -= cost;
        miners++;
        refreshAndSave();
    }

    private void buyWoodcutter()
    {
        long cost = costFor(woodcutters, 200);
        if (gold < cost) return;
        gold -= cost;
        woodcutters++;
        refreshAndSave();
    }

    private void buyFisher()
    {
        long cost = costFor(fishers, 1000);
        if (gold < cost) return;
        gold -= cost;
        fishers++;
        refreshAndSave();
    }

    private void addGold(long amount)
    {
        gold += amount;
        refreshAndSave();
    }

    private void refreshPanel()
    {
        long minerCost = costFor(miners, 50);
        long woodCost  = costFor(woodcutters, 200);
        long fishCost  = costFor(fishers, 1000);
        if (panel != null)
        {
            panel.updateState(
                    gold, miners, woodcutters, fishers, calcGps(),
                    minerCost, woodCost, fishCost
            );
        }
    }

    private void refreshAndSave()
    {
        SwingUtilities.invokeLater(this::refreshPanel);
        save();
    }

    private void save()
    {
        set("gold", Long.toString(gold));
        set("miners", Integer.toString(miners));
        set("woodcutters", Integer.toString(woodcutters));
        set("fishers", Integer.toString(fishers));
    }

    private void set(String key, String value)
    {
        // Persist via config group "idlescape"
        configManager.setConfiguration("idlescape", key, value);
    }
}
