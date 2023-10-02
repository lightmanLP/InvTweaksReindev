package com.lightmanlp.invtweaks.config;

import com.lightmanlp.invtweaks.InvTweaks;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.lightmanlp.invtweaks.Const;
import com.lightmanlp.invtweaks.logic.AutoRefillHandler;
import com.lightmanlp.invtweaks.logic.ShortcutsHandler;

import lombok.Cleanup;
import net.minecraft.client.Minecraft;

public class InvTweaksConfigManager {
  private static final Logger log = Logger.getLogger("InvTweaks");

  private Minecraft mc;

  private InvTweaksConfig config = null;

  private long storedConfigLastModified = 0L;

  private AutoRefillHandler autoRefillHandler = null;

  private ShortcutsHandler shortcutsHandler = null;

  public InvTweaksConfigManager(Minecraft mc) {
    this.mc = mc;
  }

  public boolean makeSureConfigurationIsLoaded() {
    try {
      if (this.config != null && this.config.refreshProperties()) {
        this.shortcutsHandler = new ShortcutsHandler(this.mc, this.config);
        InvTweaks.logInGameStatic("Mod properties loaded");
      }
    } catch (IOException e) {
      InvTweaks.logInGameErrorStatic("Failed to refresh properties from file", e);
    }
    long configLastModified = computeConfigLastModified();
    if (this.config != null) {
      if (this.storedConfigLastModified != configLastModified)
        return loadConfig();
      return true;
    }
    this.storedConfigLastModified = configLastModified;
    if (loadConfig())
      return true;
    return false;
  }

  public InvTweaksConfig getConfig() {
    return this.config;
  }

  public AutoRefillHandler getAutoRefillHandler() {
    return this.autoRefillHandler;
  }

  public ShortcutsHandler getShortcutsHandler() {
    return this.shortcutsHandler;
  }

  private long computeConfigLastModified() {
    return (new File(Const.CONFIG_RULES_FILE)).lastModified() + (new File(Const.CONFIG_TREE_FILE)).lastModified();
  }

  private boolean loadConfig() {
    if ((new File(Const.OLDER_CONFIG_RULES_FILE)).exists()) {
      if ((new File(Const.CONFIG_RULES_FILE)).exists())
        backupFile(new File(Const.CONFIG_RULES_FILE), Const.CONFIG_RULES_FILE);
      (new File(Const.OLDER_CONFIG_RULES_FILE)).renameTo(new File(Const.CONFIG_RULES_FILE));
    }
    if ((new File(Const.OLDER_CONFIG_TREE_FILE)).exists())
      backupFile(new File(Const.OLDER_CONFIG_TREE_FILE), Const.CONFIG_TREE_FILE);
    if ((new File(Const.OLD_CONFIG_TREE_FILE)).exists())
      (new File(Const.OLD_CONFIG_TREE_FILE)).renameTo(new File(Const.CONFIG_TREE_FILE));
    if (!(new File(Const.CONFIG_RULES_FILE)).exists() && extractFile("/com/lightmanlp/invtweaks/DefaultConfig.dat", Const.CONFIG_RULES_FILE))
      InvTweaks.logInGameStatic(Const.CONFIG_RULES_FILE + " missing, creating default one.");
    if (!(new File(Const.CONFIG_TREE_FILE)).exists() && extractFile("/com/lightmanlp/invtweaks/DefaultTree.dat", Const.CONFIG_TREE_FILE))
      InvTweaks.logInGameStatic(Const.CONFIG_TREE_FILE + " missing, creating default one.");
    this.storedConfigLastModified = computeConfigLastModified();
    String error = null;
    try {
      if (this.config == null) {
        this.config = new InvTweaksConfig(Const.CONFIG_RULES_FILE, Const.CONFIG_TREE_FILE);
        this.autoRefillHandler = new AutoRefillHandler(this.mc, this.config);
        this.shortcutsHandler = new ShortcutsHandler(this.mc, this.config);
      }
      this.config.load();
      this.shortcutsHandler.reset();
      log.setLevel(this.config.getLogLevel());
      InvTweaks.logInGameStatic("Configuration loaded");
      showConfigErrors(this.config);
    } catch (FileNotFoundException e) {
      error = "Config file not found";
    } catch (Exception e) {
      error = "Error while loading config: " + e.getMessage();
    }
    if (error != null) {
      InvTweaks.logInGameStatic(error);
      log.severe(error);
      this.config = null;
      return false;
    }
    return true;
  }

  private void backupFile(File file, String baseName) {
    String newFileName;
    if ((new File(baseName + ".bak")).exists()) {
      int i = 1;
      while ((new File(baseName + ".bak" + i)).exists())
        i++;
      newFileName = baseName + ".bak" + i;
    } else {
      newFileName = baseName + ".bak";
    }
    file.renameTo(new File(newFileName));
  }

  private boolean extractFile(String resource, String destination) {
    String resourceContents = "";
    URL resourceUrl = InvTweaks.class.getResource(resource);
    if (resourceUrl != null)
      try {
        Object o = resourceUrl.getContent();
        if (o instanceof InputStream) {
          @Cleanup InputStream content = (InputStream)o;
          while (content.available() > 0) {
            byte[] bytes = new byte[content.available()];
            content.read(bytes);
            resourceContents = resourceContents + new String(bytes);
          }
        }
      } catch (IOException e) {
        resourceUrl = null;
      }
    if (resourceUrl == null) {
      File modFolder = new File(Const.MINECRAFT_DIR + File.separatorChar + "mods/");
      File[] zips = modFolder.listFiles();
      if (zips != null && zips.length > 0)
        for (File zip : zips) {
          // TODO: smarter check maybe
          if (!zip.isFile()) { continue; }
          try {
            @Cleanup ZipFile invTweaksZip = new ZipFile(zip);
            ZipEntry zipResource = invTweaksZip.getEntry(resource);
            if (zipResource != null) {
              InputStream content = invTweaksZip.getInputStream(zipResource);
              while (content.available() > 0) {
                byte[] bytes = new byte[content.available()];
                content.read(bytes);
                resourceContents = resourceContents + new String(bytes);
              }
              break;
            }
          } catch (Exception e) {
            log.warning("Failed to extract " + resource + " from mod: " + e.getMessage());
          }
        }
    }
    if (!resourceContents.isEmpty())
      try {
        FileWriter f = new FileWriter(destination);
        f.write(resourceContents);
        f.close();
        return true;
      } catch (IOException e) {
        InvTweaks.logInGameStatic("The mod won't work, because " + destination + " creation failed!");
        log.severe("Cannot create " + destination + " file: " + e.getMessage());
        return false;
      }
    InvTweaks.logInGameStatic("The mod won't work, because " + resource + " could not be found!");
    log.severe("Cannot create " + destination + " file: " + resource + " not found");
    return false;
  }

  private void showConfigErrors(InvTweaksConfig config) {
    Vector<String> invalid = config.getInvalidKeywords();
    if (invalid.size() > 0) {
      String error = "Invalid keywords found: ";
      for (String keyword : config.getInvalidKeywords())
        error = error + keyword + " ";
      InvTweaks.logInGameStatic(error);
    }
  }
}
