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
            description = "Total gold saved",
            secret = false
    )
    default long gold() { return 0L; }

    @ConfigItem(
            keyName = "miners",
            name = "Miners",
            description = "Number of miners owned"
    )
    default int miners() { return 0; }

    @ConfigItem(
            keyName = "woodcutters",
            name = "Woodcutters",
            description = "Number of woodcutters owned"
    )
    default int woodcutters() { return 0; }

    @ConfigItem(
            keyName = "fishers",
            name = "Fishers",
            description = "Number of fishers owned"
    )
    default int fishers() { return 0; }
}

