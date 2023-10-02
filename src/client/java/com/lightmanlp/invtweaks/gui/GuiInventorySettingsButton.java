package com.lightmanlp.invtweaks.gui;

import java.util.concurrent.TimeoutException;

import com.lightmanlp.invtweaks.InvTweaks;
import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import com.lightmanlp.invtweaks.config.InvTweaksConfigManager;
import com.lightmanlp.invtweaks.library.ContainerManager;
import com.lightmanlp.invtweaks.library.ContainerSectionManager;
import com.lightmanlp.invtweaks.library.Obfuscation;
import net.minecraft.client.Minecraft;

public class GuiInventorySettingsButton extends GuiIconButton {
  public GuiInventorySettingsButton(InvTweaksConfigManager cfgManager, int id, int x, int y, int w, int h, String displayString, String tooltip) {
    super(cfgManager, id, x, y, w, h, displayString, tooltip);
  }

  public void drawButton(Minecraft minecraft, int i, int j) {
    super.drawButton(minecraft, i, j);
    if (!this.enabled)
      return;
    drawCenteredString(minecraft.fontRenderer, this.displayString, this.xPosition + 5, this.yPosition - 1, getTextColor(i, j));
  }

  public boolean mousePressed(Minecraft minecraft, int i, int j) {
    InvTweaksConfig config = this.cfgManager.getConfig();
    if (super.mousePressed(minecraft, i, j)) {
      try {
        ContainerSectionManager containerMgr = new ContainerSectionManager(minecraft, ContainerManager.ContainerSection.INVENTORY);
        if (Obfuscation.getHoldStackStatic(minecraft) != null)
          try {
            for (int k = containerMgr.getSize() - 1; k >= 0; k--) {
              if (containerMgr.getItemStack(k) == null) {
                containerMgr.leftClick(k);
                break;
              }
            }
          } catch (TimeoutException e) {
            InvTweaks.logInGameErrorStatic("Failed to put item down", e);
          }
      } catch (Exception e) {
        InvTweaks.logInGameErrorStatic("Failed to set up settings button", e);
      }
      this.cfgManager.makeSureConfigurationIsLoaded();
      minecraft.displayGuiScreen(new GuiInventorySettings(minecraft, Obfuscation.getCurrentScreenStatic(minecraft), config));
      return true;
    }
    return false;
  }
}
