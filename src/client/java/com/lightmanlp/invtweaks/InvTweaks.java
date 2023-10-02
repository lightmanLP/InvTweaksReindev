package com.lightmanlp.invtweaks;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import com.lightmanlp.invtweaks.config.InvTweaksConfigManager;
import com.lightmanlp.invtweaks.config.SortingRule;
import com.lightmanlp.invtweaks.gui.GuiInventorySettingsButton;
import com.lightmanlp.invtweaks.gui.GuiSortingButton;
import com.lightmanlp.invtweaks.library.ContainerManager;
import com.lightmanlp.invtweaks.library.ContainerSectionManager;
import com.lightmanlp.invtweaks.library.Obfuscation;
import com.lightmanlp.invtweaks.logic.SortingHandler;
import com.lightmanlp.invtweaks.mixins.MixinGuiContainer;
import com.lightmanlp.invtweaks.mixins.MixinGuiScreen;
import com.lightmanlp.invtweaks.tree.ItemTree;
import com.lightmanlp.invtweaks.tree.ItemTreeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.Container;
import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiContainer;
import net.minecraft.src.client.gui.GuiContainerInventory;
import net.minecraft.src.client.gui.GuiEditSign;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.Slot;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.item.ItemStack;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class InvTweaks extends Obfuscation {
  private static final Logger log = Logger.getLogger("InvTweaks");

  private static InvTweaks instance;

  private InvTweaksConfigManager cfgManager = null;

  private int chestAlgorithm = 0;

  private long chestAlgorithmClickTimestamp = 0L;

  private boolean chestAlgorithmButtonDown = false;

  private long sortingKeyPressedDate = 0L;

  private int storedStackId = 0;

  private int storedStackDamage = -1;

  private int storedFocusedSlot = -1;

  private ItemStack[] hotbarClone = new ItemStack[9];

  private boolean mouseWasInWindow = true;

  private boolean mouseWasDown = false;

  private int tickNumber = 0, lastPollingTickNumber = -3;

  public InvTweaks(Minecraft mc) {
    super(mc);
    log.setLevel(Const.DEFAULT_LOG_LEVEL);
    instance = this;
    this.cfgManager = new InvTweaksConfigManager(mc);
    if (this.cfgManager.makeSureConfigurationIsLoaded()) {
      log.info("Mod initialized");
    } else {
      log.severe("Mod failed to initialize!");
    }
  }

  public final void onSortingKeyPressed() {
    synchronized (this) {
      if (!this.cfgManager.makeSureConfigurationIsLoaded())
        return;
      GuiScreen guiScreen = getCurrentScreen();
      if (guiScreen != null && !(guiScreen instanceof GuiContainer))
        return;
      handleSorting(guiScreen);
    }
  }

  public void onItemPickup() {
    if (!this.cfgManager.makeSureConfigurationIsLoaded())
      return;
    InvTweaksConfig config = this.cfgManager.getConfig();
    if (this.cfgManager.getConfig().getProperty("enableSortingOnPickup").equals("false"))
      return;
    try {
      ContainerSectionManager containerMgr = new ContainerSectionManager(this.mc, ContainerManager.ContainerSection.INVENTORY);
      int currentSlot = -1;
      do {
        if (isMultiplayerWorld() && currentSlot == -1)
          try {
            Thread.sleep(3L);
          } catch (InterruptedException interruptedException) {}
        for (int i = 0; i < 9; i++) {
          ItemStack currentHotbarStack = containerMgr.getItemStack(i + 27);
          if (currentHotbarStack != null && currentHotbarStack.animationsToGo == 5 && this.hotbarClone[i] == null)
            currentSlot = i + 27;
        }
      } while (isMultiplayerWorld() && currentSlot == -1);
      if (currentSlot != -1) {
        List<Integer> prefferedPositions = new LinkedList<Integer>();
        ItemTree tree = config.getTree();
        ItemStack stack = containerMgr.getItemStack(currentSlot);
        List<ItemTreeItem> items = tree.getItems(getItemID(stack),
            getItemDamage(stack));
        for (SortingRule rule : config.getRules()) {
          if (tree.matches(items, rule.getKeyword()))
            for (int slot : rule.getPreferredSlots())
              prefferedPositions.add(Integer.valueOf(slot));
        }
        boolean hasToBeMoved = true;
        for (Iterator<Integer> iterator = prefferedPositions.iterator(); iterator.hasNext(); ) {
          int newSlot = ((Integer)iterator.next()).intValue();
          try {
            if (newSlot == currentSlot) {
              hasToBeMoved = false;
              break;
            }
            if (containerMgr.getItemStack(newSlot) == null)
              if (containerMgr.move(currentSlot, newSlot))
                break;
          } catch (TimeoutException e) {
            logInGameError("Failed to move picked up stack", e);
          }
        }
        if (hasToBeMoved)
          for (int i = 0; i < containerMgr.getSize() && (
            containerMgr.getItemStack(i) != null ||
            !containerMgr.move(currentSlot, i)); i++);
      }
    } catch (Exception e) {
      logInGameError("Failed to move picked up stack", e);
    }
  }

  public void onTickInGame() {
    synchronized (this) {
      if (!onTick())
        return;
      handleAutoRefill();
    }
  }

  public void onTickInGUI(GuiScreen guiScreen) {
    synchronized (this) {
      if (!onTick())
        return;
      if (isTimeForPolling())
        unlockKeysIfNecessary();
      handleGUILayout(guiScreen);
      handleMiddleClick(guiScreen);
      handleShortcuts(guiScreen);
    }
  }

  public void logInGame(String message) {
    String formattedMsg = buildlogString(Level.INFO, message);
    addChatMessage(formattedMsg);
    log.info(formattedMsg);
  }

  public void logInGameError(String message, Exception e) {
    String formattedMsg = buildlogString(Level.SEVERE, message, e);
    addChatMessage(formattedMsg);
    log.severe(formattedMsg);
  }

  public static void logInGameStatic(String message) {
    getInstance().logInGame(message);
  }

  public static void logInGameErrorStatic(String message, Exception e) {
    getInstance().logInGameError(message, e);
  }

  public static InvTweaks getInstance() {
    return instance;
  }

  public static boolean getIsMouseOverSlot(GuiContainer guiContainer, Slot slot, int i, int j) {
    int k = (guiContainer.width - ((MixinGuiContainer) guiContainer).getXSize()) / 2;
    int l = (guiContainer.height - ((MixinGuiContainer) guiContainer).getYSize()) / 2;
    i -= k;
    j -= l;
    return (i >= slot.xDisplayPosition - 1 && i < slot.xDisplayPosition + 16 + 1 && j >= slot.yDisplayPosition - 1 && j < slot.yDisplayPosition + 16 + 1);
  }

  private boolean onTick() {
    this.tickNumber++;
    InvTweaksConfig config = this.cfgManager.getConfig();
    if (config == null)
      return false;
    GuiScreen currentScreen = getCurrentScreen();
    if (currentScreen == null || currentScreen instanceof GuiContainerInventory)
      cloneHotbar();
    if (Keyboard.isKeyDown(getKeycode(Const.SORT_KEY_BINDING))) {
      long currentTime = System.currentTimeMillis();
      if (this.sortingKeyPressedDate == 0L) {
        this.sortingKeyPressedDate = currentTime;
      } else if (currentTime - this.sortingKeyPressedDate > 1000L) {
        String previousRuleset = config.getCurrentRulesetName();
        String newRuleset = config.switchConfig();
        if (newRuleset == null) {
          logInGameError("Failed to switch the configuration", (Exception)null);
        } else if (!previousRuleset.equals(newRuleset)) {
          logInGame("'" + newRuleset + "' enabled");
          handleSorting(currentScreen);
        }
        this.sortingKeyPressedDate = currentTime;
      }
    } else {
      this.sortingKeyPressedDate = 0L;
    }
    return true;
  }

  private void handleSorting(GuiScreen guiScreen) {
    ItemStack selectedItem = getMainInventory()[getFocusedSlot()];
    InvTweaksConfig config = this.cfgManager.getConfig();
    Vector<Integer> downKeys = this.cfgManager.getShortcutsHandler().getDownShortcutKeys();
    if (Keyboard.isKeyDown(Const.SORT_KEY_BINDING.keyCode))
      for (Iterator<Integer> iterator = downKeys.iterator(); iterator.hasNext(); ) {
        int downKey = ((Integer)iterator.next()).intValue();
        String newRuleset = null;
        switch (downKey) {
          case 2:
          case 79:
            newRuleset = config.switchConfig(0);
            break;
          case 3:
          case 80:
            newRuleset = config.switchConfig(1);
            break;
          case 4:
          case 81:
            newRuleset = config.switchConfig(2);
            break;
          case 5:
          case 75:
            newRuleset = config.switchConfig(3);
            break;
          case 6:
          case 76:
            newRuleset = config.switchConfig(4);
            break;
          case 7:
          case 77:
            newRuleset = config.switchConfig(5);
            break;
          case 8:
          case 71:
            newRuleset = config.switchConfig(6);
            break;
          case 9:
          case 72:
            newRuleset = config.switchConfig(7);
            break;
          case 10:
          case 73:
            newRuleset = config.switchConfig(8);
            break;
        }
        if (newRuleset != null)
          logInGame("'" + newRuleset + "' enabled");
      }
    try {
      (new SortingHandler(this.mc, this.cfgManager.getConfig(),
          ContainerManager.ContainerSection.INVENTORY,
          3)).sort();
    } catch (Exception e) {
      logInGame("Failed to sort inventory: " + e.getMessage());
    }
    playClick();
    if (selectedItem != null && getMainInventory()[getFocusedSlot()] == null)
      this.storedStackId = 0;
  }

  private void handleAutoRefill() {
    ItemStack currentStack = getFocusedStack();
    int currentStackId = (currentStack == null) ? 0 : getItemID(currentStack);
    int currentStackDamage = (currentStack == null) ? 0 : getItemDamage(currentStack);
    int focusedSlot = getFocusedSlot() + 27;
    InvTweaksConfig config = this.cfgManager.getConfig();
    if (currentStackId != this.storedStackId || currentStackDamage != this.storedStackDamage)
      if (this.storedFocusedSlot != focusedSlot) {
        this.storedFocusedSlot = focusedSlot;
      } else if ((currentStack == null || (getItemID(currentStack) == 281 && this.storedStackId == 282)) && (
        getCurrentScreen() == null ||
        getCurrentScreen() instanceof GuiEditSign)) {
        if (config.isAutoRefillEnabled(this.storedStackId, this.storedStackId))
          try {
            this.cfgManager.getAutoRefillHandler().autoRefillSlot(focusedSlot, this.storedStackId, this.storedStackDamage);
          } catch (Exception e) {
            logInGameError("Failed to trigger auto-refill", e);
          }
      }
    this.storedStackId = currentStackId;
    this.storedStackDamage = currentStackDamage;
  }

  private void handleMiddleClick(GuiScreen guiScreen) {
    if (Mouse.isButtonDown(2)) {
      if (!this.cfgManager.makeSureConfigurationIsLoaded())
        return;
      InvTweaksConfig config = this.cfgManager.getConfig();
      if (config.getProperty("enableMiddleClick")
        .equals("true"))
        if (!this.chestAlgorithmButtonDown) {
          this.chestAlgorithmButtonDown = true;
          if (isChestOrDispenser(guiScreen)) {
            GuiContainer guiContainer = (GuiContainer)guiScreen;
            Container container = getContainer((GuiContainer)guiScreen);
            int slotCount = getSlots(container).size();
            int mouseX = Mouse.getEventX() * guiContainer.width / this.mc.displayWidth;
            int mouseY = guiContainer.height - Mouse.getEventY() * guiContainer.height / this.mc.displayHeight - 1;
            int target = 0;
            for (int i = 0; i < slotCount; i++) {
              Slot slot = getSlot(container, i);
              int k = (guiContainer.width - ((MixinGuiContainer) guiContainer).getXSize()) / 2;
              int l = (guiContainer.height - ((MixinGuiContainer) guiContainer).getYSize()) / 2;
              if (mouseX - k >= slot.xDisplayPosition - 1 &&
                mouseX - k < slot.xDisplayPosition + 16 + 1 &&
                mouseY - l >= slot.yDisplayPosition - 1 &&
                mouseY - l < slot.yDisplayPosition + 16 + 1) {
                target = (i < slotCount - 36) ? 1 : 2;
                break;
              }
            }
            if (target == 1) {
              this.mc.theWorld.playSoundAtEntity((Entity)getThePlayer(), "random.click", 0.2F, 1.8F);
              long timestamp = System.currentTimeMillis();
              if (timestamp - this.chestAlgorithmClickTimestamp >
                3000L)
                this.chestAlgorithm = 0;
              try {
                (new SortingHandler(this.mc, this.cfgManager.getConfig(),
                    ContainerManager.ContainerSection.CHEST, this.chestAlgorithm)).sort();
              } catch (Exception e) {
                logInGameError("Failed to sort container", e);
              }
              this.chestAlgorithm = (this.chestAlgorithm + 1) % 3;
              this.chestAlgorithmClickTimestamp = timestamp;
            } else if (target == 2) {
              handleSorting(guiScreen);
            }
          } else {
            handleSorting(guiScreen);
          }
        }
    } else {
      this.chestAlgorithmButtonDown = false;
    }
  }

  private void handleGUILayout(GuiScreen guiScreen) {
    InvTweaksConfig config = this.cfgManager.getConfig();
    boolean isContainer = isChestOrDispenser(guiScreen);
    if (isContainer || guiScreen instanceof GuiContainerInventory ||
      guiScreen.getClass().getSimpleName()
      .equals("GuiInventoryMoreSlots")) {
      int w = 10, h = 10;
      boolean customButtonsAdded = false;
      for (Object o : ((MixinGuiScreen) guiScreen).getControlList()) {
        GuiButton button = (GuiButton)o;
        if (button.id == 54696386) {
          customButtonsAdded = true;
          break;
        }
      }
      if (!customButtonsAdded)
        if (!isContainer) {
          ((MixinGuiScreen) guiScreen).getControlList().add(new GuiInventorySettingsButton(
                this.cfgManager, 54696386,
                guiScreen.width / 2 + 73, guiScreen.height / 2 - 78,
                w, h, "...", "Inventory settings"));
        } else {
          GuiContainer guiContainer = (GuiContainer)guiScreen;
          MixinGuiScreen guiScreenAccessor = (MixinGuiScreen)guiScreen;
          int id = 54696386;
          int x = ((MixinGuiContainer) guiContainer).getXSize() / 2 + guiContainer.width / 2 - 17;
          int y = (guiContainer.height - ((MixinGuiContainer) guiContainer).getYSize()) / 2 + 5;
          guiScreenAccessor.getControlList().add(new GuiInventorySettingsButton(
                this.cfgManager, id++,
                x - 1, y, w, h, "...", "Inventory settings"));
          if (!config.getProperty("showChestButtons").equals("false")) {
            GuiSortingButton guiSortingButton = new GuiSortingButton(
                this.cfgManager, id++,
                x - 13, y, w, h, "h", "Sort in rows",
                2);
            guiScreenAccessor.getControlList().add(guiSortingButton);
            guiSortingButton = new GuiSortingButton(
                this.cfgManager, id++,
                x - 25, y, w, h, "v", "Sort in columns",
                1);
            guiScreenAccessor.getControlList().add(guiSortingButton);
            guiSortingButton = new GuiSortingButton(
                this.cfgManager, id++,
                x - 37, y, w, h, "s", "Default sorting",
                0);
            guiScreenAccessor.getControlList().add(guiSortingButton);
          }
        }
    }
  }

  private void handleShortcuts(GuiScreen guiScreen) {
    if (!(guiScreen instanceof GuiContainer) ||
      guiScreen.getClass().getSimpleName().equals("MLGuiChestBuilding"))
      return;
    if (Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
      if (!this.mouseWasDown) {
        this.mouseWasDown = true;
        if (this.cfgManager.getConfig().getProperty(
            "enableShortcuts").equals("true"))
          this.cfgManager.getShortcutsHandler().handleShortcut(
              (GuiContainer)guiScreen);
      }
    } else {
      this.mouseWasDown = false;
    }
  }

  private boolean isTimeForPolling() {
    if (this.tickNumber - this.lastPollingTickNumber >= 3)
      this.lastPollingTickNumber = this.tickNumber;
    return (this.tickNumber - this.lastPollingTickNumber == 0);
  }

  private void unlockKeysIfNecessary() {
    boolean mouseInWindow = Mouse.isInsideWindow();
    if (!this.mouseWasInWindow && mouseInWindow) {
      Keyboard.destroy();
      boolean firstTry = true;
      while (!Keyboard.isCreated()) {
        try {
          Keyboard.create();
        } catch (LWJGLException e) {
          if (firstTry) {
            logInGameError("I'm having troubles with the keyboard: ", (Exception)e);
            firstTry = false;
          }
        }
      }
      if (!firstTry)
        logInGame("Ok it's repaired, sorry about that.");
    }
    this.mouseWasInWindow = mouseInWindow;
  }

  private void cloneHotbar() {
    ItemStack[] arrayOfItemStack = getMainInventory();
    for (int i = 0; i < 9; i++) {
      if (arrayOfItemStack[i] != null) {
        this.hotbarClone[i] = arrayOfItemStack[i].copy();
      } else {
        this.hotbarClone[i] = null;
      }
    }
  }

  private void playClick() {
    if (!this.cfgManager.getConfig().getProperty("enableSortingSound").equals("false"))
      this.mc.theWorld.playSoundAtEntity((Entity)getThePlayer(), "random.click", 0.2F, 1.8F);
  }

  private String buildlogString(Level level, String message, Exception e) {
    if (e != null)
      return String.valueOf(buildlogString(level, message)) + ": " + e.getMessage();
    return String.valueOf(buildlogString(level, message)) + ": (unknown error)";
  }

  private String buildlogString(Level level, String message) {
    return "InvTweaks: " + (level.equals(Level.SEVERE) ? "[ERROR] " : "") + message;
  }
}


/* Location:              D:\soft\game_related\RetroMCP\minecraft\jars\deobfuscated.jar!\InvTweaks.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */
