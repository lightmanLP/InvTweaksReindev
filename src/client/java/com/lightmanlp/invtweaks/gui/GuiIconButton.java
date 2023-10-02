package com.lightmanlp.invtweaks.gui;

import com.lightmanlp.invtweaks.config.InvTweaksConfigManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class GuiIconButton extends GuiTooltipButton {
  protected InvTweaksConfigManager cfgManager;

  public GuiIconButton(InvTweaksConfigManager cfgManager, int id, int x, int y, int w, int h, String displayString, String tooltip) {
    super(id, x, y, w, h, displayString, tooltip);
    this.cfgManager = cfgManager;
  }

  public void drawButton(Minecraft minecraft, int i, int j) {
    super.drawButton(minecraft, i, j);
    if (!this.enabled)
      return;
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.renderEngine.getTexture("/gui/gui.png"));
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    int k = getHoverState(isMouseOverButton(i, j));
    drawTexturedModalRect(this.xPosition, this.yPosition, 1, 46 + k * 20 + 1, this.width / 2, this.height / 2);
    drawTexturedModalRect(this.xPosition, this.yPosition + this.height / 2, 1, 46 + k * 20 + 20 - this.height / 2 - 1, this.width / 2, this.height / 2);
    drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2 - 1, 46 + k * 20 + 1, this.width / 2, this.height / 2);
    drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition + this.height / 2, 200 - this.width / 2 - 1, 46 + k * 20 + 19 - this.height / 2, this.width / 2, this.height / 2);
  }
}
