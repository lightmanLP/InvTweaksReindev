package com.lightmanlp.invtweaks.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import com.lightmanlp.invtweaks.Const;
import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import com.lightmanlp.invtweaks.config.SortingRule;
import com.lightmanlp.invtweaks.library.ContainerManager;
import com.lightmanlp.invtweaks.library.ContainerSectionManager;
import com.lightmanlp.invtweaks.library.Obfuscation;
import com.lightmanlp.invtweaks.tree.ItemTree;
import com.lightmanlp.invtweaks.tree.ItemTreeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.Slot;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemArmor;
import net.minecraft.src.game.item.ItemStack;

public class SortingHandler extends Obfuscation {
  private static final Logger log = Logger.getLogger("InvTweaks");

  public static final boolean STACK_NOT_EMPTIED = true;

  public static final boolean STACK_EMPTIED = false;

  private static int[] DEFAULT_LOCK_PRIORITIES = null;

  private static boolean[] DEFAULT_FROZEN_SLOTS = null;

  private static final int MAX_CONTAINER_SIZE = 100;

  public static final int ALGORITHM_DEFAULT = 0;

  public static final int ALGORITHM_VERTICAL = 1;

  public static final int ALGORITHM_HORIZONTAL = 2;

  public static final int ALGORITHM_INVENTORY = 3;

  private ContainerSectionManager containerMgr;

  private int algorithm;

  private int size;

  private ItemTree tree;

  private Vector<SortingRule> rules;

  private int[] rulePriority;

  private int[] keywordOrder;

  private int[] lockPriorities;

  private boolean[] frozenSlots;

  public SortingHandler(Minecraft mc, InvTweaksConfig config, ContainerManager.ContainerSection section, int algorithm) throws Exception {
    super(mc);
    if (DEFAULT_LOCK_PRIORITIES == null) {
      DEFAULT_LOCK_PRIORITIES = new int[100];
      for (int j = 0; j < 100; j++)
        DEFAULT_LOCK_PRIORITIES[j] = 0;
    }
    if (DEFAULT_FROZEN_SLOTS == null) {
      DEFAULT_FROZEN_SLOTS = new boolean[100];
      for (int j = 0; j < 100; j++)
        DEFAULT_FROZEN_SLOTS[j] = false;
    }
    this.containerMgr = new ContainerSectionManager(mc, section);
    this.size = this.containerMgr.getSize();
    this.rules = config.getRules();
    this.tree = config.getTree();
    if (section == ContainerManager.ContainerSection.INVENTORY) {
      this.lockPriorities = config.getLockPriorities();
      this.frozenSlots = config.getFrozenSlots();
      this.algorithm = 3;
    } else {
      this.lockPriorities = DEFAULT_LOCK_PRIORITIES;
      this.frozenSlots = DEFAULT_FROZEN_SLOTS;
      this.algorithm = algorithm;
      if (algorithm != 0)
        computeLineSortingRules(9, (algorithm == 2));
    }
    this.rulePriority = new int[this.size];
    this.keywordOrder = new int[this.size];
    for (int i = 0; i < this.size; i++) {
      this.rulePriority[i] = -1;
      ItemStack stack = this.containerMgr.getItemStack(i);
      if (stack != null) {
        this.keywordOrder[i] = getItemOrder(stack);
      } else {
        this.keywordOrder[i] = -1;
      }
    }
  }

  public void sort() throws TimeoutException {
    long timer = System.nanoTime();
    ContainerManager globalContainer = new ContainerManager(this.mc);
    if (isMultiplayerWorld())
      putHoldItemDown();
    if (this.algorithm != 0) {
      if (this.algorithm == 3) {
        log.info("Handling crafting slots.");
        if (globalContainer.hasSection(ContainerManager.ContainerSection.CRAFTING_IN)) {
          List<Slot> craftingSlots = globalContainer.getSlots(ContainerManager.ContainerSection.CRAFTING_IN);
          int emptyIndex = globalContainer.getFirstEmptyIndex(ContainerManager.ContainerSection.INVENTORY);
          if (emptyIndex != -1)
            for (Slot craftingSlot : craftingSlots) {
              if (craftingSlot.getHasStack()) {
                globalContainer.move(ContainerManager.ContainerSection.CRAFTING_IN, globalContainer.getSlotIndex(craftingSlot.slotNumber), ContainerManager.ContainerSection.INVENTORY, emptyIndex);
                emptyIndex = globalContainer.getFirstEmptyIndex(ContainerManager.ContainerSection.INVENTORY);
                if (emptyIndex == -1)
                  break;
              }
            }
        }
        log.info("Merging stacks.");
        for (int j = this.size - 1; j >= 0; j--) {
          ItemStack from = this.containerMgr.getItemStack(j);
          if (from != null) {
            Item fromItem = from.getItem();
            if (fromItem.isDamagable()) {
              if (fromItem instanceof ItemArmor) {
                ItemArmor fromItemArmor = (ItemArmor)fromItem;
                List<Slot> armorSlots = globalContainer.getSlots(ContainerManager.ContainerSection.ARMOR);
                for (Slot slot : armorSlots) {
                  // TODO: not sure about armorMaterial same as armorLevel
                  if (slot.isItemValid(from) && (!slot.getHasStack() || fromItemArmor.armorMaterial > ((ItemArmor)slot.getStack().getItem()).armorMaterial))
                    globalContainer.move(ContainerManager.ContainerSection.INVENTORY, j, ContainerManager.ContainerSection.ARMOR, globalContainer.getSlotIndex(slot.slotNumber));
                }
              }
            } else {
              int k = 0;
              for (int arr$[] = this.lockPriorities, len$ = arr$.length, i$ = 0; i$ < len$; ) {
                Integer lockPriority = Integer.valueOf(arr$[i$]);
                if (lockPriority.intValue() > 0) {
                  ItemStack to = this.containerMgr.getItemStack(k);
                  if (to != null && from.isItemEqual(to)) {
                    move(j, k, Integer.MAX_VALUE);
                    markAsNotMoved(k);
                    if (this.containerMgr.getItemStack(j) == null)
                      break;
                  }
                }
                k++;
                i$++;
              }
            }
          }
        }
      }
      log.info("Applying rules.");
      Iterator<SortingRule> rulesIt = this.rules.iterator();
      while (rulesIt.hasNext()) {
        SortingRule rule = rulesIt.next();
        int rulePriority = rule.getPriority();
        if (log.getLevel() == Const.DEBUG)
          log.info("Rule : " + rule.getKeyword() + "(" + rulePriority + ")");
        for (int j = 0; j < this.size; j++) {
          ItemStack from = this.containerMgr.getItemStack(j);
          if (hasToBeMoved(j) && this.lockPriorities[j] < rulePriority) {
            List<ItemTreeItem> fromItems = this.tree.getItems(getItemID(from), getItemDamage(from));
            if (this.tree.matches(fromItems, rule.getKeyword())) {
              int[] preferredSlots = rule.getPreferredSlots();
              int stackToMove = j;
              for (int k = 0; k < preferredSlots.length; k++) {
                int m = preferredSlots[k];
                int moveResult = move(stackToMove, m, rulePriority);
                if (moveResult != -1) {
                  if (moveResult == m)
                    break;
                  from = this.containerMgr.getItemStack(moveResult);
                  fromItems = this.tree.getItems(getItemID(from), getItemDamage(from));
                  if (!this.tree.matches(fromItems, rule.getKeyword()))
                    break;
                  stackToMove = moveResult;
                  k = -1;
                }
              }
            }
          }
        }
      }
      log.info("Locking stacks.");
      for (int i = 0; i < this.size; i++) {
        if (hasToBeMoved(i) && this.lockPriorities[i] > 0)
          markAsMoved(i, 1);
      }
    }
    defaultSorting();
    if (log.getLevel() == Const.DEBUG) {
      timer = System.nanoTime() - timer;
      log.info("Sorting done in " + timer + "ns");
    }
  }

  private void defaultSorting() throws TimeoutException {
    log.info("Default sorting.");
    Vector<Integer> remaining = new Vector<Integer>(), nextRemaining = new Vector<Integer>();
    for (int i = 0; i < this.size; i++) {
      if (hasToBeMoved(i)) {
        remaining.add(Integer.valueOf(i));
        nextRemaining.add(Integer.valueOf(i));
      }
    }
    int iterations = 0;
    while (remaining.size() > 0 && iterations++ < 50) {
      for (Iterator<Integer> i$ = remaining.iterator(); i$.hasNext(); ) {
        int j = ((Integer)i$.next()).intValue();
        if (hasToBeMoved(j)) {
          for (int k = 0; k < this.size; k++) {
            if (move(j, k, 1) != -1) {
              nextRemaining.remove(Integer.valueOf(k));
              break;
            }
          }
          continue;
        }
        nextRemaining.remove(Integer.valueOf(j));
      }
      remaining.clear();
      remaining.addAll(nextRemaining);
    }
    if (iterations == 50)
      log.info("Sorting takes too long, aborting.");
  }

  private int putHoldItemDown() throws TimeoutException {
    ItemStack holdStack = getHoldStack();
    if (holdStack != null) {
      for (int step = 1; step <= 2; step++) {
        for (int i = this.size - 1; i >= 0; i--) {
          if ((this.containerMgr.getItemStack(i) == null && this.lockPriorities[i] == 0 && !this.frozenSlots[i]) || step == 2) {
            this.containerMgr.leftClick(i);
            return i;
          }
        }
      }
      return -1;
    }
    return -1;
  }

  private int move(int i, int j, int priority) throws TimeoutException {
    ItemStack from = this.containerMgr.getItemStack(i);
    ItemStack to = this.containerMgr.getItemStack(j);
    if (from == null || this.frozenSlots[j] || this.frozenSlots[i])
      return -1;
    if (this.lockPriorities[i] <= priority) {
      if (i == j) {
        markAsMoved(i, priority);
        return j;
      }
      if (to == null && this.lockPriorities[j] <= priority && !this.frozenSlots[j]) {
        this.rulePriority[i] = -1;
        this.keywordOrder[i] = -1;
        this.rulePriority[j] = priority;
        this.keywordOrder[j] = getItemOrder(from);
        this.containerMgr.move(i, j);
        return j;
      }
      if (to != null) {
        boolean canBeSwappedOrMerged = false;
        if (this.lockPriorities[j] <= priority)
          if (this.rulePriority[j] < priority) {
            canBeSwappedOrMerged = true;
          } else if (this.rulePriority[j] == priority &&
            isOrderedBefore(i, j)) {
            canBeSwappedOrMerged = true;
          }
        if (!canBeSwappedOrMerged && from.isItemEqual(to) && getStackSize(to) < to.getMaxStackSize())
          canBeSwappedOrMerged = true;
        if (canBeSwappedOrMerged) {
          this.keywordOrder[j] = this.keywordOrder[i];
          this.rulePriority[j] = priority;
          this.rulePriority[i] = -1;
          this.rulePriority[i] = -1;
          this.containerMgr.move(i, j);
          ItemStack remains = this.containerMgr.getItemStack(i);
          if (remains != null) {
            int dropSlot = i;
            if (this.lockPriorities[j] > this.lockPriorities[i])
              for (int k = 0; k < this.size; k++) {
                if (this.containerMgr.getItemStack(k) == null && this.lockPriorities[k] == 0) {
                  dropSlot = k;
                  break;
                }
              }
            if (dropSlot != i)
              this.containerMgr.move(i, dropSlot);
            this.rulePriority[dropSlot] = -1;
            this.keywordOrder[dropSlot] = getItemOrder(remains);
            return dropSlot;
          }
          return j;
        }
      }
    }
    return -1;
  }

  private void markAsMoved(int i, int priority) {
    this.rulePriority[i] = priority;
  }

  private void markAsNotMoved(int i) {
    this.rulePriority[i] = -1;
  }

  private boolean hasToBeMoved(int slot) {
    return (this.containerMgr.getItemStack(slot) != null && this.rulePriority[slot] == -1);
  }

  private boolean isOrderedBefore(int i, int j) {
    ItemStack iStack = this.containerMgr.getItemStack(i);
    ItemStack jStack = this.containerMgr.getItemStack(j);
    if (jStack == null)
      return true;
    if (iStack == null || this.keywordOrder[i] == -1)
      return false;
    if (this.keywordOrder[i] == this.keywordOrder[j]) {
      if (getItemID(iStack) == getItemID(jStack)) {
        if (getStackSize(iStack) == getStackSize(jStack)) {
          int damageDiff = getItemDamage(iStack) - getItemDamage(jStack);
          return ((damageDiff < 0 && !iStack.isItemStackDamageable()) || (damageDiff > 0 && iStack.isItemStackDamageable()));
        }
        return (getStackSize(iStack) > getStackSize(jStack));
      }
      return (getItemID(iStack) > getItemID(jStack));
    }
    return (this.keywordOrder[i] < this.keywordOrder[j]);
  }

  private int getItemOrder(ItemStack item) {
    List<ItemTreeItem> items = this.tree.getItems(getItemID(item), getItemDamage(item));
    return (items != null && items.size() > 0) ? ((ItemTreeItem)items.get(0)).getOrder() : Integer.MAX_VALUE;
  }

  private void computeLineSortingRules(int rowSize, boolean horizontal) {
    int spaceWidth, spaceHeight;
    String defaultRule;
    this.rules = new Vector<SortingRule>();
    Map<ItemTreeItem, Integer> stats = computeContainerStats();
    List<ItemTreeItem> itemOrder = new ArrayList<ItemTreeItem>();
    int distinctItems = stats.size();
    int columnSize = getContainerColumnSize(rowSize);
    int availableSlots = this.size;
    int remainingStacks = 0;
    for (Integer stacks : stats.values())
      remainingStacks += stacks.intValue();
    if (distinctItems == 0)
      return;
    List<ItemTreeItem> unorderedItems = new ArrayList<ItemTreeItem>(stats.keySet());
    boolean hasStacksToOrderFirst = true;
    while (hasStacksToOrderFirst) {
      hasStacksToOrderFirst = false;
      for (ItemTreeItem item : unorderedItems) {
        Integer value = stats.get(item);
        if (value.intValue() > (horizontal ? rowSize : columnSize) && !itemOrder.contains(item)) {
          hasStacksToOrderFirst = true;
          itemOrder.add(item);
          unorderedItems.remove(item);
        }
      }
    }
    Collections.sort(unorderedItems, Collections.reverseOrder());
    itemOrder.addAll(unorderedItems);
    if (horizontal) {
      spaceHeight = 1;
      spaceWidth = rowSize / (distinctItems + columnSize - 1) / columnSize;
    } else {
      spaceWidth = 1;
      spaceHeight = columnSize / (distinctItems + rowSize - 1) / rowSize;
    }
    char row = 'a', maxRow = (char)(row - 1 + columnSize);
    char column = '1', maxColumn = (char)(column - 1 + rowSize);
    Iterator<ItemTreeItem> it = itemOrder.iterator();
    while (it.hasNext()) {
      ItemTreeItem item = it.next();
      int thisSpaceWidth = spaceWidth;
      int thisSpaceHeight = spaceHeight;
      while (((Integer)stats.get(item)).intValue() > thisSpaceHeight * thisSpaceWidth) {
        if (horizontal) {
          if (column + thisSpaceWidth < maxColumn) {
            thisSpaceWidth = maxColumn - column + 1;
            continue;
          }
          if (row + thisSpaceHeight < maxRow) {
            thisSpaceHeight++;
            continue;
          }
          break;
        }
        if (row + thisSpaceHeight < maxRow) {
          thisSpaceHeight = maxRow - row + 1;
          continue;
        }
        if (column + thisSpaceWidth < maxColumn)
          thisSpaceWidth++;
      }
      if (horizontal && column + thisSpaceWidth == maxColumn) {
        thisSpaceWidth++;
      } else if (!horizontal && row + thisSpaceHeight == maxRow) {
        thisSpaceHeight++;
      }
      String constraint = row + "" + column + "-" + (char)(row - 1 + thisSpaceHeight) + (char)(column - 1 + thisSpaceWidth);
      if (!horizontal)
        constraint = constraint + 'v';
      this.rules.add(new SortingRule(this.tree, constraint, item.getName(), this.size, rowSize));
      availableSlots -= thisSpaceHeight * thisSpaceWidth;
      remainingStacks -= ((Integer)stats.get(item)).intValue();
      if (availableSlots >= remainingStacks) {
        if (horizontal) {
          if (column + thisSpaceWidth + spaceWidth <= maxColumn + 1) {
            column = (char)(column + thisSpaceWidth);
          } else {
            column = '1';
            row = (char)(row + thisSpaceHeight);
          }
        } else if (row + thisSpaceHeight + spaceHeight <= maxRow + 1) {
          row = (char)(row + thisSpaceHeight);
        } else {
          row = 'a';
          column = (char)(column + thisSpaceWidth);
        }
        if (row > maxRow || column > maxColumn)
          break;
      }
    }
    if (horizontal) {
      defaultRule = maxRow + "1-a" + maxColumn;
    } else {
      defaultRule = "a" + maxColumn + "-" + maxRow + "1v";
    }
    this.rules.add(new SortingRule(this.tree, defaultRule, this.tree.getRootCategory().getName(), this.size, rowSize));
  }

  private Map<ItemTreeItem, Integer> computeContainerStats() {
    Map<ItemTreeItem, Integer> stats = new HashMap<ItemTreeItem, Integer>();
    Map<Integer, ItemTreeItem> itemSearch = new HashMap<Integer, ItemTreeItem>();
    for (int i = 0; i < this.size; i++) {
      ItemStack stack = this.containerMgr.getItemStack(i);
      if (stack != null) {
        int itemSearchKey = getItemID(stack) * 100000 + ((getMaxStackSize(stack) != 1) ? getItemDamage(stack) : 0);
        ItemTreeItem item = itemSearch.get(Integer.valueOf(itemSearchKey));
        if (item == null) {
          item = this.tree.getItems(getItemID(stack), getItemDamage(stack)).get(0);
          itemSearch.put(Integer.valueOf(itemSearchKey), item);
          stats.put(item, Integer.valueOf(1));
        } else {
          stats.put(item, Integer.valueOf(((Integer)stats.get(item)).intValue() + 1));
        }
      }
    }
    return stats;
  }

  private int getContainerColumnSize(int rowSize) {
    return this.size / rowSize;
  }
}
