package com.idlescape;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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
    // ===== Definitions =====
    @AllArgsConstructor @Getter
    private static class GenDef {
        String name;
        long baseCost;
        double growth; // cost growth per-owned
        int gps;       // base gps per unit
    }
    @AllArgsConstructor @Getter
    private static class UpDef {
        String name;
        long cost;
        int clickAdd;     // +click power
        double gpsMult;   // x GPS multiplier (1.0 = none)
    }

    private static GenDef g(String name, int idx, int gps) {
        long base = (long)Math.round(50 * Math.pow(1.9, idx));
        return new GenDef(name, base, 1.15, gps);
    }
    // 30 generators
    private static final GenDef[] GEN = new GenDef[] {
            g("miner",0,1), g("woodcutter",1,3), g("fisher",2,10), g("farmer",3,25), g("smith",4,60),
            g("fletcher",5,120), g("hunter",6,250), g("runecrafter",7,500), g("herblorist",8,900), g("cook",9,1400),
            g("thief",10,2000), g("alchemist",11,3000), g("slayer",12,4500), g("crafter",13,6500), g("mage",14,9000),
            g("ranger",15,12000), g("warrior",16,16000), g("priest",17,21000), g("druid",18,27000), g("scribe",19,34000),
            g("artisan",20,42000), g("navigator",21,51000), g("cartographer",22,61000), g("inquisitor",23,72000), g("archmage",24,84000),
            g("high priest",25,97000), g("grandmaster",26,111000), g("guildmaster",27,126000), g("warden",28,142000), g("king's hand",29,159000)
    };

    private static UpDef u(String n, long c, int ca, double gm) { return new UpDef(n, c, ca, gm); }
    // 30 upgrades
    private static final UpDef[] UP = new UpDef[] {
            u("leather gloves",          500,        1, 1.0),
            u("bronze gloves",           1_250,      1, 1.0),
            u("iron gloves",             3_000,      2, 1.0),
            u("steel gloves",            6_000,      2, 1.0),
            u("mithril gloves",          12_000,     3, 1.0),
            u("adamant gloves",          25_000,     3, 1.0),
            u("rune gloves",             50_000,     4, 1.0),
            u("dragon gloves",           100_000,    5, 1.0),
            u("barrows gloves",          200_000,    6, 1.0),
            u("ferocious gloves",        400_000,    8, 1.0),
            u("ring of wealth",          800_000,    10,1.0),
            u("expert clicks I",         1_600_000,  5, 1.0),
            u("expert clicks II",        3_200_000,  6, 1.0),
            u("expert clicks III",       6_400_000,  8, 1.0),
            u("expert clicks IV",        12_800_000, 10,1.0),

            u("guild charter",           2_000,      0, 1.05),
            u("blessed symbol",          6_000,      0, 1.10),
            u("artisan tools",           20_000,     0, 1.15),
            u("ancient tome",            60_000,     0, 1.20),
            u("royal edict",             180_000,    0, 1.25),
            u("scrying orb",             500_000,    0, 1.30),
            u("gilded altar",            1_500_000,  0, 1.35),
            u("arcane focus",            4_000_000,  0, 1.40),
            u("masterwork anvils",       10_000_000, 0, 1.45),
            u("king's decree",           25_000_000, 0, 1.50),

            u("divine gloves",           2_500_000,  12, 1.0),
            u("shadow gloves",           5_000_000,  15, 1.0),
            u("celestial gloves",        10_000_000, 18, 1.0),
            u("soulbound gloves",        20_000_000, 22, 1.0),
            u("elder gloves",            40_000_000, 28, 1.0)
    };

    // ===== DI / UI =====
    @Inject private ClientToolbar clientToolbar;
    @Inject private idlescapeConfig config;
    @Inject private ConfigManager configManager;

    private idlescapePanel panel;
    private NavigationButton navButton;
    private Timer timer;

    // ===== State =====
    private long gold;
    private int[] gensOwned = new int[GEN.length];
    private boolean[] upOwned = new boolean[UP.length];

    // ===== Wiring =====
    @Provides idlescapeConfig provideConfig(ConfigManager cm) { return cm.getConfig(idlescapeConfig.class); }

    @Override
    protected void startUp()
    {
        loadState();

        panel = new idlescapePanel(
                this::buyGenerator,
                this::buyUpgrade,
                () -> addGold(getClickPower()),
                Arrays.stream(GEN).map(GenDef::getName).toArray(String[]::new),
                Arrays.stream(UP).map(UpDef::getName).toArray(String[]::new)
        );

        BufferedImage icon = null;
        try { icon = ImageUtil.loadImageResource(idlescapePlugin.class, "icon.png"); }
        catch (IllegalArgumentException e) { log.warn("icon.png not found; continuing without an icon"); }

        NavigationButton.NavigationButtonBuilder nb = NavigationButton.builder()
                .tooltip("idle-scape")
                .priority(5)
                .panel(panel);
        if (icon != null) nb.icon(icon);
        navButton = nb.build();
        clientToolbar.addNavigation(navButton);

        refreshPanel();

        timer = new Timer(1000, e -> tick());
        timer.start();
    }

    @Override
    protected void shutDown()
    {
        if (timer != null) timer.stop();
        if (navButton != null) { clientToolbar.removeNavigation(navButton); navButton = null; }
        saveState();
        panel = null;
    }

    // ===== Loop =====
    private void tick()
    {
        long delta = calcGps();
        if (delta > 0)
        {
            gold += delta;
            SwingUtilities.invokeLater(this::refreshPanel);
            if ((gold % 50) == 0) saveState();
        }
    }

    // ===== Buy actions =====
    private void buyGenerator(int idx)
    {
        long cost = nextGenCost(idx);
        if (gold < cost) return;
        gold -= cost;
        gensOwned[idx]++;
        refreshAndSave();
    }

    private void buyUpgrade(int idx)
    {
        if (upOwned[idx]) return;
        long cost = UP[idx].cost;
        if (gold < cost) return;
        gold -= cost;
        upOwned[idx] = true;
        refreshAndSave();
    }

    private void addGold(long amount)
    {
        gold += amount;
        refreshAndSave();
    }

    // ===== Numbers =====
    private long nextGenCost(int idx)
    {
        GenDef d = GEN[idx];
        int owned = gensOwned[idx];
        return Math.round(d.baseCost * Math.pow(d.growth, owned));
    }

    private long[] allNextGenCosts()
    {
        long[] c = new long[GEN.length];
        for (int i = 0; i < GEN.length; i++) c[i] = nextGenCost(i);
        return c;
    }

    private long getClickPower()
    {
        long base = 1;
        for (int i = 0; i < UP.length; i++) if (upOwned[i]) base += Math.max(0, UP[i].clickAdd);
        return base;
    }

    private double currentGpsMultiplier()
    {
        double m = 1.0;
        for (int i = 0; i < UP.length; i++)
            if (upOwned[i] && UP[i].gpsMult > 0) m *= UP[i].gpsMult;
        return m;
    }

    private int calcGps()
    {
        double gps = 0;
        for (int i = 0; i < GEN.length; i++) gps += gensOwned[i] * GEN[i].gps;
        gps *= currentGpsMultiplier();
        return (gps > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)Math.round(gps);
    }

    private long[] genGpsPerUnitEffective()
    {
        long[] out = new long[GEN.length];
        double mult = currentGpsMultiplier();
        for (int i = 0; i < GEN.length; i++)
            out[i] = Math.round(GEN[i].gps * mult);
        return out;
    }

    // ===== UI glue =====
    private void refreshPanel()
    {
        if (panel == null) return;

        long[] genCosts = allNextGenCosts();
        long[] upCosts  = Arrays.stream(UP).mapToLong(u -> u.cost).toArray();
        String[] upTips = new String[UP.length];
        for (int i = 0; i < UP.length; i++) {
            String eff = (UP[i].clickAdd > 0 ? ("+" + UP[i].clickAdd + " click") : "") +
                    (UP[i].gpsMult > 1.0 ? ((UP[i].clickAdd > 0 ? ", " : "") + "x" + trimMult(UP[i].gpsMult) + " GPS") : "");
            if (eff.isEmpty()) eff = "No effect";
            upTips[i] = eff;
        }

        panel.updateState(
                gold,
                calcGps(),
                getClickPower(),
                Arrays.copyOf(gensOwned, gensOwned.length),
                Arrays.copyOf(upOwned, upOwned.length),
                genCosts,
                upCosts,
                upTips,
                genGpsPerUnitEffective() // NEW: per-unit effective GPS for tooltips
        );
    }

    private static String trimMult(double m)
    {
        String s = String.format(java.util.Locale.US, "%.2f", m);
        if (s.endsWith("0")) s = s.substring(0, s.length()-1);
        if (s.endsWith("0")) s = s.substring(0, s.length()-1);
        if (s.endsWith(".")) s = s.substring(0, s.length()-1);
        return s;
    }

    private void refreshAndSave()
    {
        SwingUtilities.invokeLater(this::refreshPanel);
        saveState();
    }

    // ===== Persistence =====
    private void loadState()
    {
        gold = config.gold();
        gensOwned = parseCsvInt(config.gensCsv(), GEN.length);
        upOwned = parseCsvBool(config.upsCsv(), UP.length);
    }

    private void saveState()
    {
        configManager.setConfiguration("idlescape", "gold", Long.toString(gold));
        configManager.setConfiguration("idlescape", "gensCsv", joinCsv(gensOwned));
        configManager.setConfiguration("idlescape", "upsCsv", joinCsv(upOwned));
    }

    private static int[] parseCsvInt(String csv, int len)
    {
        int[] out = new int[len];
        if (csv == null || csv.isEmpty()) return out;
        String[] parts = csv.split(",");
        for (int i = 0; i < Math.min(len, parts.length); i++) {
            try { out[i] = Integer.parseInt(parts[i].trim()); } catch (Exception ignored) {}
        }
        return out;
    }
    private static boolean[] parseCsvBool(String csv, int len)
    {
        boolean[] out = new boolean[len];
        if (csv == null || csv.isEmpty()) return out;
        String[] parts = csv.split(",");
        for (int i = 0; i < Math.min(len, parts.length); i++) {
            String p = parts[i].trim();
            out[i] = "1".equals(p) || "true".equalsIgnoreCase(p);
        }
        return out;
    }
    private static String joinCsv(int[] a)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i>0) sb.append(','); sb.append(a[i]); }
        return sb.toString();
    }
    private static String joinCsv(boolean[] a)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i>0) sb.append(','); sb.append(a[i] ? '1' : '0'); }
        return sb.toString();
    }

    // ===== Reset via config checkbox =====
    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"idlescape".equals(e.getGroup())) return;
        if (!"reset_now".equals(e.getKey())) return;

        boolean requested = Boolean.parseBoolean(e.getNewValue());
        if (!requested) return;

        SwingUtilities.invokeLater(() -> {
            int res = JOptionPane.showConfirmDialog(
                    panel,
                    "Reset ALL idle-scape progress?\nThis cannot be undone.",
                    "Confirm reset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (res == JOptionPane.YES_OPTION) {
                performReset();
            }
            configManager.setConfiguration("idlescape", "reset_now", "false");
        });
    }

    private void performReset()
    {
        gold = 0;
        Arrays.fill(gensOwned, 0);
        Arrays.fill(upOwned, false);
        saveState();
        refreshPanel();
    }
}
