package com.lightmanlp.invtweaks.gui;

import java.util.LinkedList;
import java.util.List;
import com.lightmanlp.invtweaks.Const;
import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

public class GuiShortcutsHelp extends GuiScreen {
  private static final String SCREEN_TITLE = "Shortcuts help";

  private static final int ID_DONE = 0;

  private Minecraft b;

  private GuiScreen parentScreen;

  private InvTweaksConfig config;

  public GuiShortcutsHelp(Minecraft mc, GuiScreen parentScreen, InvTweaksConfig config) {
    this.b = mc;
    this.parentScreen = parentScreen;
    this.config = config;
  }

  public void initGui() {
    List<GuiButton> controlList = new LinkedList<GuiButton>();
    controlList.add(new GuiButton(0, this.width / 2 - 100, this.height / 6 + 168, "Done"));
    this.controlList = controlList;
  }

  public void drawScreen(int i, int j, float f) {
    drawDefaultBackground();
    drawCenteredString(this.fontRenderer, "Shortcuts help", this.width / 2, 20, 16777215);
    int y = this.height / 6;
    drawShortcutLine("Move", "Left click", 61183, y);
    y += 12;
    drawShortcutLine("Move to empty slot", "Right click", 61183, y);
    y += 20;
    drawShortcutLine("Move one stack", this.config.getProperty("shortcutKeyOneStack") + " + Click", 16776960, y);
    y += 12;
    drawShortcutLine("Move one item only", this.config.getProperty("shortcutKeyOneItem") + " + Click", 16776960, y);
    y += 12;
    drawShortcutLine("Move all items of same type", this.config.getProperty("shortcutKeyAllItems") + " + Click", 16776960, y);
    y += 20;
    drawShortcutLine("Move to upper section", this.config.getProperty("shortcutKeyToUpperSection") + " + Click", 65331, y);
    y += 12;
    drawShortcutLine("Move to lower section", this.config.getProperty("shortcutKeyToLowerSection") + " + Click", 65331, y);
    y += 12;
    drawShortcutLine("Move to hotbar", "0-9 + Click", 65331, y);
    y += 20;
    drawShortcutLine("Drop", this.config.getProperty("shortcutKeyDrop") + " + Click", 16746496, y);
    y += 12;
    drawShortcutLine("Craft all", "LSHIFT, RSHIFT + Click", 16746496, y);
    y += 12;
    drawShortcutLine("Select sorting configuration", "0-9 + " + Keyboard.getKeyName(Const.SORT_KEY_BINDING.keyCode), 16746496, y);
    y += 12;
    super.drawScreen(i, j, f);
  }

  private void drawShortcutLine(String label, String value, int color, int y) {
    drawString(this.fontRenderer, label, 50, y, -1);
    drawString(this.fontRenderer, value.contains("DEFAULT") ? "-" : value, this.width / 2 + 40, y, color);
  }

  protected void actionPerformed(GuiButton guibutton) {
    switch (guibutton.id) {
      case 0:
        this.b.displayGuiScreen(this.parentScreen);
        break;
    }
  }
}
