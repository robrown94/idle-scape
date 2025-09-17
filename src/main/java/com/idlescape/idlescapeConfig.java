package com.idlescape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("idlescape")
public interface idlescapeConfig extends Config
{
    @ConfigItem(
            keyName = "gold",
            name = "Gold",
            description = "Total gold saved"
    )
    default long gold() { return 0L; }

    // CSV of generator counts (length = number of generators)
    @ConfigItem(
            keyName = "gensCsv",
            name = "Generators CSV",
            description = "Internal storage (do not edit)"
    )
    default String gensCsv() { return ""; }

    // CSV of upgrade flags (0/1 for each upgrade)
    @ConfigItem(
            keyName = "upsCsv",
            name = "Upgrades CSV",
            description = "Internal storage (do not edit)"
    )
    default String upsCsv() { return ""; }

    // Reset checkbox trigger (handled in plugin, then flipped back false)
    @ConfigItem(
            keyName = "reset_now",
            name = "Reset all progress",
            description = "Check to trigger a full reset (you'll be asked to confirm).",
            position = 99
    )
    default boolean reset_now() { return false; }
}
