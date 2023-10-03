package com.lightmanlp.invtweaks.config;

import com.lightmanlp.invtweaks.InvTweaks;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import com.lightmanlp.invtweaks.Const;
import com.lightmanlp.invtweaks.tree.ItemTree;
import com.lightmanlp.invtweaks.tree.ItemTreeItem;
import com.lightmanlp.invtweaks.tree.ItemTreeLoader;

public class InvTweaksConfig {
  public static final String PROP_ENABLE_MIDDLE_CLICK = "enableMiddleClick";

  public static final String PROP_SHOW_CHEST_BUTTONS = "showChestButtons";

  public static final String PROP_ENABLE_SORTING_ON_PICKUP = "enableSortingOnPickup";

  public static final String PROP_ENABLE_SHORTCUTS = "enableShortcuts";

  public static final String PROP_SHORTCUT_PREFIX = "shortcutKey";

  public static final String PROP_SHORTCUT_ONE_ITEM = "shortcutKeyOneItem";

  public static final String PROP_SHORTCUT_ONE_STACK = "shortcutKeyOneStack";

  public static final String PROP_SHORTCUT_ALL_ITEMS = "shortcutKeyAllItems";

  public static final String PROP_SHORTCUT_DROP = "shortcutKeyDrop";

  public static final String PROP_SHORTCUT_UP = "shortcutKeyToUpperSection";

  public static final String PROP_SHORTCUT_DOWN = "shortcutKeyToLowerSection";

  public static final String PROP_ENABLE_SORTING_SOUND = "enableSortingSound";

  public static final String PROP_ENABLE_AUTO_REFILL_SOUND = "enableAutoRefillSound";

  public static final String VALUE_TRUE = "true";

  public static final String VALUE_FALSE = "false";

  public static final Object VALUE_DEFAULT = "DEFAULT";

  public static final String VALUE_CI_COMPATIBILITY = "convenientInventoryCompatibility";

  public static final String LOCKED = "LOCKED";

  public static final String FROZEN = "FROZEN";

  public static final String AUTOREPLACE = "AUTOREPLACE";

  public static final String AUTOREPLACE_NOTHING = "nothing";

  public static final String DEBUG = "DEBUG";

  public static final boolean DEFAULT_AUTO_REFILL_BEHAVIOUR = true;

  private String rulesFile;

  private String treeFile;

  private InvTweaksProperties properties;

  private ItemTree tree;

  private Vector<InventoryConfigRuleset> rulesets;

  private int currentRuleset = 0;

  private String currentRulesetName = null;

  private Vector<String> invalidKeywords;

  private long storedConfigLastModified;

  public InvTweaksConfig(String rulesFile, String treeFile) {
    this.rulesFile = rulesFile;
    this.treeFile = treeFile;
    reset();
  }

  public void load() throws Exception {
    synchronized (this) {
      reset();
      loadProperties();
      saveProperties();
      this.tree = (new ItemTreeLoader()).load(this.treeFile);
      File f = new File(this.rulesFile);
      char[] bytes = new char[(int)f.length()];
      FileReader reader = new FileReader(f);
      reader.read(bytes);
      String[] configLines = String.valueOf(bytes).replace("\r\n", "\n").replace('\r', '\n').split("\n");
      InventoryConfigRuleset activeRuleset = new InventoryConfigRuleset(this.tree, "Default");
      boolean defaultRuleset = true, defaultRulesetEmpty = true;
      for (String line : configLines) {
        if (line.matches("^[\\w]*\\:$")) {
          if (!defaultRuleset || !defaultRulesetEmpty) {
            activeRuleset.finalize();
            this.rulesets.add(activeRuleset);
          }
          activeRuleset = new InventoryConfigRuleset(this.tree, line.substring(0, line.length() - 1));
        }
        try {
          String invalidKeyword = activeRuleset.registerLine(line);
          if (defaultRuleset)
            defaultRulesetEmpty = false;
          if (invalidKeyword != null)
            this.invalidKeywords.add(invalidKeyword);
        } catch (InvalidParameterException e) {}
      }
      activeRuleset.finalize();
      this.rulesets.add(activeRuleset);
      this.currentRuleset = 0;
      if (this.currentRulesetName != null) {
        int rulesetIndex = 0;
        for (InventoryConfigRuleset ruleset : this.rulesets) {
          if (ruleset.getName().equals(this.currentRulesetName)) {
            this.currentRuleset = rulesetIndex;
            break;
          }
          rulesetIndex++;
        }
      }
      if (this.currentRuleset == 0)
        if (!this.rulesets.isEmpty()) {
          this.currentRulesetName = ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getName();
        } else {
          this.currentRulesetName = null;
        }
    }
  }

  public boolean refreshProperties() throws IOException {
    long configLastModified = (new File(Const.CONFIG_PROPS_FILE)).lastModified();
    if (this.storedConfigLastModified != configLastModified) {
      this.storedConfigLastModified = configLastModified;
      loadProperties();
      return true;
    }
    return false;
  }

  public void saveProperties() {
    File configPropsFile = getPropertyFile();
    if (configPropsFile.exists())
      try {
        FileOutputStream fos = new FileOutputStream(configPropsFile);
        this.properties.store(fos, "Inventory Tweaks Configuration\n(Regarding shortcuts, all key names can be found at: http://www.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html)");
        fos.flush();
        fos.close();
        this.storedConfigLastModified = (new File(Const.CONFIG_PROPS_FILE)).lastModified();
      } catch (IOException e) {
        InvTweaks.logInGameStatic("Failed to save config file " + Const.CONFIG_PROPS_FILE);
      }
  }

  public Map<String, String> getProperties(String prefix) {
    Map<String, String> result = new HashMap<String, String>();
    for (Object o : this.properties.keySet()) {
      String key = (String)o;
      if (key.startsWith(prefix))
        result.put(key, this.properties.getProperty(key));
    }
    return result;
  }

  public String getProperty(String key) {
    return this.properties.getProperty(key);
  }

  public void setProperty(String key, String value) {
    this.properties.put(key, value);
    saveProperties();
    if (key.equals("enableMiddleClick"))
      resolveConvenientInventoryConflicts();
  }

  public ItemTree getTree() {
    return this.tree;
  }

  public String getCurrentRulesetName() {
    return this.currentRulesetName;
  }

  public String switchConfig(int i) {
    if (!this.rulesets.isEmpty() && i < this.rulesets.size()) {
      this.currentRuleset = i;
      this.currentRulesetName = ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getName();
      return this.currentRulesetName;
    }
    return null;
  }

  public String switchConfig() {
    if (this.currentRuleset == -1)
      return switchConfig(0);
    return switchConfig((this.currentRuleset + 1) % this.rulesets.size());
  }

  public Vector<SortingRule> getRules() {
    return ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getRules();
  }

  public Vector<String> getInvalidKeywords() {
    return this.invalidKeywords;
  }

  public int[] getLockPriorities() {
    return ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getLockPriorities();
  }

  public boolean[] getFrozenSlots() {
    return ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getFrozenSlots();
  }

  public Vector<Integer> getLockedSlots() {
    return ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getLockedSlots();
  }

  public Level getLogLevel() {
    return ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).isDebugEnabled() ? Level.INFO : Level.WARNING;
  }

  public boolean isAutoRefillEnabled(int itemID, int itemDamage) {
    List<ItemTreeItem> items = this.tree.getItems(itemID, itemDamage);
    Vector<String> autoReplaceRules = ((InventoryConfigRuleset)this.rulesets.get(this.currentRuleset)).getAutoReplaceRules();
    boolean found = false;
    for (String keyword : autoReplaceRules) {
      if (keyword.equals("nothing"))
        return false;
      if (this.tree.matches(items, keyword))
        found = true;
    }
    if (found)
      return true;
    if (autoReplaceRules.isEmpty())
      return true;
    return false;
  }

  public void resolveConvenientInventoryConflicts() {
    boolean convenientInventoryInstalled = false;
    boolean defaultCISortingShortcutEnabled = false;
    try {
      Class<?> convenientInventory = Class.forName("ConvenientInventory");
      convenientInventoryInstalled = true;
      Field middleClickField = null;
      try {
        middleClickField = convenientInventory.getDeclaredField("middleClickEnabled");
      } catch (NoSuchFieldException e) {}
      if (middleClickField != null) {
        boolean middleClickSorting = getProperty("enableMiddleClick").equals("true");
        middleClickField.setAccessible(true);
        middleClickField.setBoolean((Object)null, !middleClickSorting);
      } else {
        Field initializedField = convenientInventory.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        Boolean initialized = (Boolean)initializedField.get((Object)null);
        if (!initialized.booleanValue()) {
          Method initializeMethod = convenientInventory.getDeclaredMethod("initialize", new Class[0]);
          initializeMethod.setAccessible(true);
          initializeMethod.invoke((Object)null, new Object[0]);
        }
        Field actionMapField = convenientInventory.getDeclaredField("actionMap");
        actionMapField.setAccessible(true);
        List<Integer>[][] arrayOfList = (List<Integer>[][])actionMapField.get(null);
        if (arrayOfList != null && arrayOfList[7] != null)
          for (List<Integer> combo : arrayOfList[7]) {
            if (combo != null && combo.size() == 1 && ((Integer)combo.get(0)).intValue() == 2) {
              defaultCISortingShortcutEnabled = true;
              break;
            }
          }
      }
    } catch (ClassNotFoundException e) {

    } catch (Exception e) {
      InvTweaks.logInGameErrorStatic("Failed to manage Convenient Inventory compatibility", e);
    }
    String shortcutsProp = getProperty("enableShortcuts");
    if (convenientInventoryInstalled && !shortcutsProp.equals("convenientInventoryCompatibility")) {
      setProperty("enableShortcuts", "convenientInventoryCompatibility");
    } else if (!convenientInventoryInstalled && shortcutsProp.equals("convenientInventoryCompatibility")) {
      setProperty("enableShortcuts", "true");
    }
    String middleClickProp = getProperty("enableMiddleClick");
    if (defaultCISortingShortcutEnabled && !middleClickProp.equals("convenientInventoryCompatibility")) {
      setProperty("enableMiddleClick", "convenientInventoryCompatibility");
    } else if (!defaultCISortingShortcutEnabled && middleClickProp.equals("convenientInventoryCompatibility")) {
      setProperty("enableMiddleClick", "true");
    }
  }

  private void reset() {
    this.rulesets = new Vector<InventoryConfigRuleset>();
    this.currentRuleset = -1;
    this.properties = new InvTweaksProperties();
    this.properties.put("enableMiddleClick", "true");
    this.properties.put("showChestButtons", "true");
    this.properties.put("enableSortingOnPickup", "true");
    this.properties.put("enableAutoRefillSound", "true");
    this.properties.put("enableSortingSound", "true");
    this.properties.put("enableShortcuts", "false");
    this.properties.put("shortcutKeyAllItems", "LSHIFT, RSHIFT");
    this.properties.put("shortcutKeyOneItem", "LCONTROL, RCONTROL");
    this.properties.put("shortcutKeyOneStack", VALUE_DEFAULT);
    this.properties.put("shortcutKeyToUpperSection", "UP");
    this.properties.put("shortcutKeyToLowerSection", "DOWN");
    this.properties.put("shortcutKeyDrop", "LALT, RALT");
    this.invalidKeywords = new Vector<String>();
  }

  private void loadProperties() throws IOException {
    File configPropsFile = getPropertyFile();
    if (configPropsFile != null) {
      FileInputStream fis = new FileInputStream(configPropsFile);
      this.properties.load(fis);
      fis.close();
      resolveConvenientInventoryConflicts();
    }
    this.properties.sortKeys();
    if (((String)this.properties.get("shortcutKeyDrop")).contains("META"))
      this.properties.setProperty("shortcutKeyDrop", "LALT, RALT");
    if (((String)this.properties.get("shortcutKeyOneItem")).contains("CTRL"))
      this.properties.setProperty("shortcutKeyOneItem", "LCONTROL, RCONTROL");
    if (this.properties.contains("enableAutoreplaceSound")) {
      this.properties.put("enableAutoRefillSound", this.properties.get("enableAutoreplaceSound"));
      this.properties.remove("enableAutoreplaceSound");
    }
  }

  private File getPropertyFile() {
    File configPropsFile = new File(Const.CONFIG_PROPS_FILE);
    if (!configPropsFile.exists())
      try {
        configPropsFile.createNewFile();
      } catch (IOException e) {
        InvTweaks.logInGameStatic("Failed to create the config file " + Const.CONFIG_PROPS_FILE);
        return null;
      }
    return configPropsFile;
  }
}
