package com.lightmanlp.invtweaks.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.FontRenderer;
import net.minecraft.src.client.gui.GuiButton;

public class GuiTooltipButton extends GuiButton {
  public static final int DEFAULT_BUTTON_WIDTH = 200;

  public static final int LINE_HEIGHT = 11;

  private int hoverTime = 0;

  private long prevSystemTime = 0L;

  private String tooltip = null;

  private String[] tooltipLines = null;

  private int tooltipWidth = -1;

  public GuiTooltipButton(int id, int x, int y, String displayString) {
    this(id, x, y, 150, 20, displayString, (String)null);
  }

  public GuiTooltipButton(int id, int x, int y, String displayString, String tooltip) {
    this(id, x, y, 150, 20, displayString, tooltip);
  }

  public GuiTooltipButton(int id, int x, int y, int w, int h, String displayString) {
    this(id, x, y, w, h, displayString, (String)null);
  }

  public GuiTooltipButton(int id, int x, int y, int w, int h, String displayString, String tooltip) {
    super(id, x, y, w, h, displayString);
    if (tooltip != null)
      setTooltip(tooltip);
  }

  public void drawButton(Minecraft minecraft, int i, int j) {
    super.drawButton(minecraft, i, j);
    if (!this.enabled)
      return;
    if (this.tooltipLines != null) {
      if (isMouseOverButton(i, j)) {
        long systemTime = System.currentTimeMillis();
        if (this.prevSystemTime != 0L)
          this.hoverTime = (int)(this.hoverTime + systemTime - this.prevSystemTime);
        this.prevSystemTime = systemTime;
      } else {
        this.hoverTime = 0;
        this.prevSystemTime = 0L;
      }
      if (this.hoverTime > 1000 && this.tooltipLines != null) {
        FontRenderer fontRenderer = minecraft.fontRenderer;
        int x = i + 12, y = j - 11 * this.tooltipLines.length;
        if (this.tooltipWidth == -1)
          for (String line : this.tooltipLines)
            this.tooltipWidth = Math.max(fontRenderer.getStringWidth(line), this.tooltipWidth);
        if (x + this.tooltipWidth > minecraft.currentScreen.width)
          x = minecraft.currentScreen.width - this.tooltipWidth;
        drawGradientRect(x - 3, y - 3, x + this.tooltipWidth + 3, y + 11 * this.tooltipLines.length, -1073741824, -1073741824);
        int lineCount = 0;
        for (String line : this.tooltipLines)
          minecraft.fontRenderer.drawStringWithShadow(line, x, y + lineCount++ * 11, -1);
      }
    }
  }

  protected boolean isMouseOverButton(int i, int j) {
    return (i >= this.xPosition && j >= this.yPosition && i < this.xPosition + this.width && j < this.yPosition + this.height);
  }

  protected int getTextColor(int i, int j) {
    int textColor = -2039584;
    if (!this.enabled) {
      textColor = -6250336;
    } else if (isMouseOverButton(i, j)) {
      textColor = -96;
    }
    return textColor;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
    this.tooltipLines = tooltip.split("\n");
  }

  public String getTooltip() {
    return this.tooltip;
  }
}
