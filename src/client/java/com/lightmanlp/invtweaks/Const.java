package com.lightmanlp.invtweaks;

import java.io.File;
import java.util.logging.Level;
import com.lightmanlp.invtweaks.library.Obfuscation;

import net.minecraft.src.client.KeyBinding;

public class Const {
  public static final String MOD_VERSION = "1.31 (1.7.3)";

  public static final int RULESET_SWAP_DELAY = 1000;

  public static final int AUTO_REFILL_DELAY = 200;

  public static final int POLLING_DELAY = 3;

  public static final int POLLING_TIMEOUT = 1500;

  public static final int SORTING_TIMEOUT = 2999;

  public static final int CHEST_ALGORITHM_SWAP_MAX_INTERVAL = 3000;

  public static final int TOOLTIP_DELAY = 1000;

  public static final String MINECRAFT_DIR = Obfuscation.getMinecraftDir();

  public static final String MINECRAFT_CONFIG_DIR = MINECRAFT_DIR + "config" + File.separatorChar;

  public static final String CONFIG_PROPS_FILE = MINECRAFT_CONFIG_DIR + "InvTweaks.cfg";

  public static final String CONFIG_RULES_FILE = MINECRAFT_CONFIG_DIR + "InvTweaksRules.txt";

  public static final String CONFIG_TREE_FILE = MINECRAFT_CONFIG_DIR + "InvTweaksTree.txt";

  public static final String OLD_CONFIG_TREE_FILE = MINECRAFT_CONFIG_DIR + "InvTweaksTree.xml";

  public static final String OLDER_CONFIG_RULES_FILE = MINECRAFT_DIR + "InvTweaksRules.txt";

  public static final String OLDER_CONFIG_TREE_FILE = MINECRAFT_DIR + "InvTweaksTree.txt";

  public static final String DEFAULT_CONFIG_FILE = "/com/lightmanlp/invtweaks/DefaultConfig.dat";

  public static final String DEFAULT_CONFIG_TREE_FILE = "/com/lightmanlp/invtweaks/DefaultTree.dat";

  public static final String HELP_URL = "http://wan.ka.free.fr/?invtweaks";

  public static final String INGAME_LOG_PREFIX = "InvTweaks: ";

  public static final Level DEFAULT_LOG_LEVEL = Level.WARNING;

  public static final Level DEBUG = Level.INFO;

  public static final int JIMEOWAN_ID = 54696386;

  public static final int INVENTORY_SIZE = 36;

  public static final int INVENTORY_ROW_SIZE = 9;

  public static final int INVENTORY_HOTBAR_SIZE = 9;

  public static final int PLAYER_INVENTORY_WINDOW_ID = 0;

  public static final KeyBinding SORT_KEY_BINDING = new KeyBinding("Sort inventory", 19);
}
