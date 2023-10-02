package com.lightmanlp.invtweaks.gui;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import com.lightmanlp.invtweaks.Const;
import com.lightmanlp.invtweaks.InvTweaks;
import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiScreen;

import org.lwjgl.util.Point;

public class GuiInventorySettings extends GuiScreen {
  private static final Logger log = Logger.getLogger("InvTweaks");

  private static final String SCREEN_TITLE = "Inventory and chests settings";

  private static final String MIDDLE_CLICK = "Middle click";

  private static final String CHEST_BUTTONS = "Chest buttons";

  private static final String SORT_ON_PICKUP = "Sort on pickup";

  private static final String SHORTCUTS = "Shortcuts";

  private static final String ON = ": ON";

  private static final String OFF = ": OFF";

  private static final String DISABLE_CI = ": Disable CI";

  private static final String SP_ONLY = ": Only in SP";

  private static final int ID_MIDDLE_CLICK = 1;

  private static final int ID_CHESTS_BUTTONS = 2;

  private static final int ID_SORT_ON_PICKUP = 3;

  private static final int ID_SHORTCUTS = 4;

  private static final int ID_SHORTCUTS_HELP = 5;

  private static final int ID_EDITRULES = 100;

  private static final int ID_EDITTREE = 101;

  private static final int ID_HELP = 102;

  private static final int ID_DONE = 200;

  private Minecraft b;

  private GuiScreen parentScreen;

  private InvTweaksConfig config;

  public GuiInventorySettings(Minecraft mc, GuiScreen parentScreen, InvTweaksConfig config) {
    this.b = mc;
    this.parentScreen = parentScreen;
    this.config = config;
  }

  public void initGui() {
    List<GuiButton> controlList = new LinkedList<GuiButton>();
    Point p = new Point();
    int i = 0;
    moveToButtonCoords(1, p);
    controlList.add(new GuiButton(100, p.getX() + 55, this.height / 6 + 96, "Open the sorting rules file..."));
    controlList.add(new GuiButton(101, p.getX() + 55, this.height / 6 + 120, "Open the item tree file..."));
    controlList.add(new GuiButton(102, p.getX() + 55, this.height / 6 + 144, "Open help in browser..."));
    controlList.add(new GuiButton(200, p.getX() + 55, this.height / 6 + 168, "Done"));
    String middleClick = this.config.getProperty("enableMiddleClick");
    moveToButtonCoords(i++, p);
    GuiTooltipButton middleClickBtn = new GuiTooltipButton(1, p.getX(), p.getY(), computeBooleanButtonLabel("enableMiddleClick", "Middle click"), "To sort using the middle click");
    controlList.add(middleClickBtn);
    if (middleClick.equals("convenientInventoryCompatibility")) {
      middleClickBtn.enabled = false;
      middleClickBtn.setTooltip(middleClickBtn.getTooltip() + "\n(In conflict with Convenient Inventory)");
    }
    moveToButtonCoords(i++, p);
    controlList.add(new GuiTooltipButton(5, p.getX() + 130, p.getY(), 20, 20, "?", "Shortcuts help"));
    String shortcuts = this.config.getProperty("enableShortcuts");
    GuiTooltipButton shortcutsBtn = new GuiTooltipButton(4, p.getX(), p.getY(), 130, 20, computeBooleanButtonLabel("enableShortcuts", "Shortcuts"), "Enables various shortcuts\nto move items around");
    controlList.add(shortcutsBtn);
    if (shortcuts.equals("convenientInventoryCompatibility")) {
      shortcutsBtn.enabled = false;
      shortcutsBtn.setTooltip(shortcutsBtn.getTooltip() + "\n(In conflict with Convenient Inventory)");
    }
    moveToButtonCoords(i++, p);
    GuiTooltipButton sortOnPickupBtn = new GuiTooltipButton(3, p.getX(), p.getY(), computeBooleanButtonLabel("enableSortingOnPickup", "Sort on pickup"), "Moves picked up items\nto the right slots");
    controlList.add(sortOnPickupBtn);
    if (this.b.isMultiplayerWorld()) {
      sortOnPickupBtn.enabled = false;
      sortOnPickupBtn.displayString = "Sort on pickup: Only in SP";
      sortOnPickupBtn.setTooltip(sortOnPickupBtn.getTooltip() + "\n(Single player only)");
    }
    moveToButtonCoords(i++, p);
    controlList.add(new GuiTooltipButton(2, p.getX(), p.getY(), computeBooleanButtonLabel("showChestButtons", "Chest buttons"), "Adds three buttons\non chests to sort them"));
    if (!Desktop.isDesktopSupported())
      for (GuiButton o : controlList) {
        GuiButton button = o;
        if (button.id == 100 || button.id < 101)
          button.enabled = false;
      }
    this.controlList = controlList;
  }

  public void drawScreen(int i, int j, float f) {
    drawDefaultBackground();
    drawCenteredString(this.fontRenderer, "Inventory and chests settings", this.width / 2, 20, 16777215);
    super.drawScreen(i, j, f);
  }

  protected void actionPerformed(GuiButton guibutton) {
    switch (guibutton.id) {
      case 1:
        toggleBooleanButton(guibutton, "enableMiddleClick", "Middle click");
        break;
      case 2:
        toggleBooleanButton(guibutton, "showChestButtons", "Chest buttons");
        break;
      case 3:
        toggleBooleanButton(guibutton, "enableSortingOnPickup", "Sort on pickup");
        break;
      case 4:
        toggleBooleanButton(guibutton, "enableShortcuts", "Shortcuts");
        break;
      case 5:
        this.b.displayGuiScreen(new GuiShortcutsHelp(this.b, this, this.config));
        break;
      case 100:
        try {
          Desktop.getDesktop().open(new File(Const.CONFIG_RULES_FILE));
        } catch (Exception e) {
          InvTweaks.logInGameErrorStatic("Failed to open rules file", e);
        }
        break;
      case 101:
        try {
          Desktop.getDesktop().open(new File(Const.CONFIG_TREE_FILE));
        } catch (Exception e) {
          InvTweaks.logInGameErrorStatic("Failed to open tree file", e);
        }
        break;
      case 102:
        try {
          Desktop.getDesktop().browse((new URL("http://wan.ka.free.fr/?invtweaks")).toURI());
        } catch (Exception e) {
          InvTweaks.logInGameErrorStatic("Failed to open help", e);
        }
        break;
      case 200:
        this.b.displayGuiScreen(this.parentScreen);
        break;
    }
  }

  private void moveToButtonCoords(int buttonOrder, Point p) {
    p.setX(this.width / 2 - 155 + (buttonOrder + 1) % 2 * 160);
    p.setY(this.height / 6 + buttonOrder / 2 * 24);
  }

  private void toggleBooleanButton(GuiButton guibutton, String property, String label) {
    Boolean enabled = Boolean.valueOf(!(Boolean.valueOf(this.config.getProperty(property))).booleanValue());
    this.config.setProperty(property, enabled.toString());
    guibutton.displayString = computeBooleanButtonLabel(property, label);
  }

  private String computeBooleanButtonLabel(String property, String label) {
    String propertyValue = this.config.getProperty(property);
    if (propertyValue.equals("convenientInventoryCompatibility"))
      return label + ": Disable CI";
    Boolean enabled = Boolean.valueOf(propertyValue);
    return label + (enabled.booleanValue() ? ": ON" : ": OFF");
  }
}
