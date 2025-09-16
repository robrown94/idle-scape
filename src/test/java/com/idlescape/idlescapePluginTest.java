package com.idlescape;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class idlescapePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(idlescapePlugin.class);
		RuneLite.main(args);
	}
}