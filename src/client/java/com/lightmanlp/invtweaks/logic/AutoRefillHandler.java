package com.lightmanlp.invtweaks.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import com.lightmanlp.invtweaks.config.SortingRule;
import com.lightmanlp.invtweaks.library.ContainerManager;
import com.lightmanlp.invtweaks.library.ContainerSectionManager;
import com.lightmanlp.invtweaks.library.Obfuscation;
import com.lightmanlp.invtweaks.tree.ItemTree;
import com.lightmanlp.invtweaks.tree.ItemTreeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.item.ItemStack;

public class AutoRefillHandler extends Obfuscation {
  private static final Logger log = Logger.getLogger("InvTweaks");

  private InvTweaksConfig config = null;

  public AutoRefillHandler(Minecraft mc, InvTweaksConfig config) {
    super(mc);
    setConfig(config);
  }

  public void setConfig(InvTweaksConfig config) {
    this.config = config;
  }

  public void autoRefillSlot(int slot, int wantedId, int wantedDamage) throws Exception {
    ContainerSectionManager container = new ContainerSectionManager(this.mc, ContainerManager.ContainerSection.INVENTORY);
    ItemStack replacementStack = null;
    int replacementStackSlot = -1;
    List<SortingRule> matchingRules = new ArrayList<SortingRule>();
    List<SortingRule> rules = this.config.getRules();
    ItemTree tree = this.config.getTree();
    List<ItemTreeItem> items = tree.getItems(wantedId, wantedDamage);
    for (ItemTreeItem item : items)
      matchingRules.add(new SortingRule(tree, "D" + (slot - 27), item.getName(), 36, 9));
    for (SortingRule rule : rules) {
      if (rule.getType() == SortingRule.RuleType.TILE || rule.getType() == SortingRule.RuleType.COLUMN)
        for (int preferredSlot : rule.getPreferredSlots()) {
          if (slot == preferredSlot) {
            matchingRules.add(rule);
            break;
          }
        }
    }
    for (SortingRule rule : matchingRules) {
      for (int i = 0; i < 36; i++) {
        ItemStack candidateStack = container.getItemStack(i);
        if (candidateStack != null) {
          List<ItemTreeItem> candidateItems = tree.getItems(getItemID(candidateStack), getItemDamage(candidateStack));
          if (tree.matches(candidateItems, rule.getKeyword()))
            if (replacementStack == null || getStackSize(replacementStack) > getStackSize(candidateStack) || (getStackSize(replacementStack) == getStackSize(candidateStack) && getMaxStackSize(replacementStack) == 1 && getItemDamage(replacementStack) < getItemDamage(candidateStack))) {
              replacementStack = candidateStack;
              replacementStackSlot = i;
            }
        }
      }
      if (replacementStack != null)
        break;
    }
    if (replacementStack != null) {
      log.info("Automatic stack replacement.");
      (new Thread((new Runnable() {
            private ContainerSectionManager containerMgr;

            private int targetedSlot;

            private int i;

            private int expectedItemId;

            public Runnable init(Minecraft mc, int i, int currentItem) throws Exception {
              this.containerMgr = new ContainerSectionManager(mc, ContainerManager.ContainerSection.INVENTORY);
              this.targetedSlot = currentItem;
              this.expectedItemId = AutoRefillHandler.this.getItemID(this.containerMgr.getItemStack(i));
              this.i = i;
              return this;
            }

            public void run() {
              if (AutoRefillHandler.this.isMultiplayerWorld()) {
                int pollingTime = 0;
                AutoRefillHandler.this.setHasInventoryChanged(false);
                while (!AutoRefillHandler.this.hasInventoryChanged() && pollingTime < 1500)
                  AutoRefillHandler.trySleep(3);
                if (pollingTime < 200)
                  AutoRefillHandler.trySleep(200 - pollingTime);
                if (pollingTime >= 1500)
                  AutoRefillHandler.log.warning("Autoreplace timout");
              } else {
                AutoRefillHandler.trySleep(200);
              }
              try {
                ItemStack stack = this.containerMgr.getItemStack(this.i);
                if (stack != null && AutoRefillHandler.this.getItemID(stack) == this.expectedItemId)
                  if (this.containerMgr.move(this.i, this.targetedSlot)) {
                    if (!AutoRefillHandler.this.config.getProperty("enableAutoRefillSound").equals("false"))
                      AutoRefillHandler.this.mc.theWorld.playSoundAtEntity((Entity)AutoRefillHandler.this.getThePlayer(), "mob.chickenplop", 0.15F, 0.2F);
                    if (this.containerMgr.getItemStack(this.i) != null && this.i >= 27)
                      for (int j = 0; j < 36; j++) {
                        if (this.containerMgr.getItemStack(j) == null) {
                          this.containerMgr.move(this.i, j);
                          break;
                        }
                      }
                  } else {
                    AutoRefillHandler.log.warning("Failed to move stack for autoreplace, despite of prior tests.");
                  }
              } catch (NullPointerException e) {

              } catch (TimeoutException e) {
                AutoRefillHandler.log.severe("Failed to trigger autoreplace: " + e.getMessage());
              }
            }
          }).init(this.mc, replacementStackSlot, slot))).start();
    }
  }

  private static void trySleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {}
  }
}
