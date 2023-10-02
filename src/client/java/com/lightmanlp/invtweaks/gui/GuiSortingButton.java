package com.lightmanlp.invtweaks.gui;

import com.lightmanlp.invtweaks.InvTweaks;
import com.lightmanlp.invtweaks.config.InvTweaksConfigManager;
import com.lightmanlp.invtweaks.library.ContainerManager;
import com.lightmanlp.invtweaks.logic.SortingHandler;
import net.minecraft.client.Minecraft;

public class GuiSortingButton extends GuiIconButton {
  private final ContainerManager.ContainerSection section = ContainerManager.ContainerSection.CHEST;

  private int algorithm;

  public GuiSortingButton(InvTweaksConfigManager cfgManager, int id, int x, int y, int w, int h, String displayString, String tooltip, int algorithm) {
    super(cfgManager, id, x, y, w, h, displayString, tooltip);
    this.algorithm = algorithm;
  }

  public void drawButton(Minecraft minecraft, int i, int j) {
    super.drawButton(minecraft, i, j);
    if (!this.enabled)
      return;
    int textColor = getTextColor(i, j);
    if (this.displayString.equals("h")) {
      drawRect(this.xPosition + 3, this.yPosition + 3, this.xPosition + this.width - 3, this.yPosition + 4, textColor);
      drawRect(this.xPosition + 3, this.yPosition + 6, this.xPosition + this.width - 3, this.yPosition + 7, textColor);
    } else if (this.displayString.equals("v")) {
      drawRect(this.xPosition + 3, this.yPosition + 3, this.xPosition + 4, this.yPosition + this.height - 3, textColor);
      drawRect(this.xPosition + 6, this.yPosition + 3, this.xPosition + 7, this.yPosition + this.height - 3, textColor);
    } else {
      drawRect(this.xPosition + 3, this.yPosition + 3, this.xPosition + this.width - 3, this.yPosition + 4, textColor);
      drawRect(this.xPosition + 5, this.yPosition + 4, this.xPosition + 6, this.yPosition + 5, textColor);
      drawRect(this.xPosition + 4, this.yPosition + 5, this.xPosition + 5, this.yPosition + 6, textColor);
      drawRect(this.xPosition + 3, this.yPosition + 6, this.xPosition + this.width - 3, this.yPosition + 7, textColor);
    }
  }

  public boolean mousePressed(Minecraft minecraft, int i, int j) {
    if (super.mousePressed(minecraft, i, j)) {
      try {
        (new SortingHandler(minecraft, this.cfgManager.getConfig(), this.section, this.algorithm)).sort();
      } catch (Exception e) {
        InvTweaks.logInGameErrorStatic("Failed to sort container", e);
      }
      return true;
    }
    return false;
  }
}
