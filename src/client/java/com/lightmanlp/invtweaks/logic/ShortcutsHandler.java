package com.lightmanlp.invtweaks.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.lightmanlp.invtweaks.InvTweaks;
import com.lightmanlp.invtweaks.config.InvTweaksConfig;
import com.lightmanlp.invtweaks.library.ContainerManager;
import com.lightmanlp.invtweaks.library.Obfuscation;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.GuiContainer;
import net.minecraft.src.client.gui.Slot;
import net.minecraft.src.game.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ShortcutsHandler extends Obfuscation {
  private static final int DROP_SLOT = -999;

  private ShortcutType defaultAction = ShortcutType.MOVE_ONE_STACK;

  private ShortcutType defaultDestination = null;

  private InvTweaksConfig config;

  private ContainerManager container;

  private ContainerManager.ContainerSection fromSection;

  private int fromIndex;

  private ItemStack fromStack;

  private ContainerManager.ContainerSection toSection;

  private ShortcutType shortcutType;

  private Map<Integer, Boolean> shortcutKeysStatus;

  private Map<ShortcutType, List<Integer>> shortcuts;

  private enum ShortcutType {
    MOVE_TO_SPECIFIC_HOTBAR_SLOT, MOVE_ONE_STACK, MOVE_ONE_ITEM, MOVE_ALL_ITEMS, MOVE_UP, MOVE_DOWN, MOVE_TO_EMPTY_SLOT, DROP;
  }

  public ShortcutsHandler(Minecraft mc, InvTweaksConfig config) {
    super(mc);
    this.config = config;
    reset();
  }

  public void reset() {
    this.shortcutKeysStatus = new HashMap<Integer, Boolean>();
    this.shortcuts = new HashMap<ShortcutType, List<Integer>>();
    Map<String, String> keys = this.config.getProperties("shortcutKey");
    for (String key : keys.keySet()) {
      String value = keys.get(key);
      if (value.equals(InvTweaksConfig.VALUE_DEFAULT)) {
        ShortcutType newDefault = propNameToShortcutType(key);
        if (newDefault == ShortcutType.MOVE_ALL_ITEMS || newDefault == ShortcutType.MOVE_ONE_ITEM || newDefault == ShortcutType.MOVE_ONE_STACK) {
          this.defaultAction = newDefault;
          continue;
        }
        if (newDefault == ShortcutType.MOVE_DOWN || newDefault == ShortcutType.MOVE_UP)
          this.defaultDestination = newDefault;
        continue;
      }
      String[] keyNames = ((String)keys.get(key)).split("[ ]*,[ ]*");
      List<Integer> list = new LinkedList<Integer>();
      for (String keyName : keyNames)
        list.add(Integer.valueOf(Keyboard.getKeyIndex(keyName.replace("KEY_", "").replace("ALT", "MENU"))));
      ShortcutType shortcutType = propNameToShortcutType(key);
      if (shortcutType != null)
        this.shortcuts.put(shortcutType, list);
      for (Integer keyCode : list)
        this.shortcutKeysStatus.put(keyCode, Boolean.valueOf(false));
    }
    int upKeyCode = this.mc.gameSettings.keyBindForward.keyCode;
    int downKeyCode = this.mc.gameSettings.keyBindBack.keyCode;
    ((List<Integer>)this.shortcuts.get(ShortcutType.MOVE_UP)).add(Integer.valueOf(upKeyCode));
    ((List<Integer>)this.shortcuts.get(ShortcutType.MOVE_DOWN)).add(Integer.valueOf(downKeyCode));
    this.shortcutKeysStatus.put(Integer.valueOf(upKeyCode), Boolean.valueOf(false));
    this.shortcutKeysStatus.put(Integer.valueOf(downKeyCode), Boolean.valueOf(false));
    List<Integer> keyBindings = new LinkedList<Integer>();
    int[] hotbarKeys = {
        2, 3, 4, 5, 6, 7, 8, 9, 10, 79,
        80, 81, 75, 76, 77, 71, 72, 73 };
    for (int i : hotbarKeys) {
      keyBindings.add(Integer.valueOf(i));
      this.shortcutKeysStatus.put(Integer.valueOf(i), Boolean.valueOf(false));
    }
    this.shortcuts.put(ShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT, keyBindings);
  }

  public Vector<Integer> getDownShortcutKeys() {
    updateKeyStatuses();
    Vector<Integer> downShortcutKeys = new Vector<Integer>();
    for (Integer key : this.shortcutKeysStatus.keySet()) {
      if (((Boolean)this.shortcutKeysStatus.get(key)).booleanValue())
        downShortcutKeys.add(key);
    }
    return downShortcutKeys;
  }

  public void handleShortcut(GuiContainer guiScreen) {
    updateKeyStatuses();
    int ex = Mouse.getEventX(), ey = Mouse.getEventY();
    int x = ex * guiScreen.width / this.mc.displayWidth;
    int y = guiScreen.height - ey * guiScreen.height / this.mc.displayHeight - 1;
    boolean shortcutValid = false;
    Slot slot = getSlotAtPosition(guiScreen, x, y);
    if (slot != null && slot.getHasStack()) {
      ShortcutType shortcutType = this.defaultAction;
      if (isActive(ShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT) != -1) {
        shortcutType = ShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT;
        shortcutValid = true;
      }
      if (isActive(ShortcutType.MOVE_ALL_ITEMS) != -1) {
        shortcutType = ShortcutType.MOVE_ALL_ITEMS;
        shortcutValid = true;
      } else if (isActive(ShortcutType.MOVE_ONE_ITEM) != -1) {
        shortcutType = ShortcutType.MOVE_ONE_ITEM;
        shortcutValid = true;
      }
      try {
        ContainerManager container = new ContainerManager(this.mc);
        ContainerManager.ContainerSection srcSection = container.getSlotSection(slot.slotNumber);
        ContainerManager.ContainerSection destSection = null;
        Vector<ContainerManager.ContainerSection> availableSections = new Vector<ContainerManager.ContainerSection>();
        if (container.hasSection(ContainerManager.ContainerSection.CHEST)) {
          availableSections.add(ContainerManager.ContainerSection.CHEST);
        } else if (container.hasSection(ContainerManager.ContainerSection.CRAFTING_IN)) {
          availableSections.add(ContainerManager.ContainerSection.CRAFTING_IN);
        } else if (container.hasSection(ContainerManager.ContainerSection.FURNACE_IN)) {
          availableSections.add(ContainerManager.ContainerSection.FURNACE_IN);
        }
        availableSections.add(ContainerManager.ContainerSection.INVENTORY_NOT_HOTBAR);
        availableSections.add(ContainerManager.ContainerSection.INVENTORY_HOTBAR);
        int destinationModifier = 0;
        if (isActive(ShortcutType.MOVE_UP) != -1 || this.defaultDestination == ShortcutType.MOVE_UP) {
          destinationModifier = -1;
        } else if (isActive(ShortcutType.MOVE_DOWN) != -1 || this.defaultDestination == ShortcutType.MOVE_DOWN) {
          destinationModifier = 1;
        }
        if (destinationModifier == 0) {
          switch (srcSection) {
            case INVENTORY_HOTBAR:
              destSection = ContainerManager.ContainerSection.INVENTORY_NOT_HOTBAR;
              break;
            case CRAFTING_IN:
            case FURNACE_IN:
              destSection = ContainerManager.ContainerSection.INVENTORY_NOT_HOTBAR;
              break;
            default:
              destSection = ContainerManager.ContainerSection.INVENTORY_HOTBAR;
              break;
          }
        } else {
          shortcutValid = true;
          int srcSectionIndex = availableSections.indexOf(srcSection);
          if (srcSectionIndex != -1) {
            destSection = availableSections.get((availableSections.size() + srcSectionIndex + destinationModifier) % availableSections.size());
          } else {
            destSection = ContainerManager.ContainerSection.INVENTORY;
          }
        }
        if (srcSection == ContainerManager.ContainerSection.UNKNOWN)
          shortcutValid = false;
        if (shortcutValid || isActive(ShortcutType.DROP) != -1) {
          initAction(slot.slotNumber, shortcutType, destSection);
          if (shortcutType == ShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT) {
            String keyName = Keyboard.getKeyName(isActive(ShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT));
            int destIndex = -1 + Integer.parseInt(keyName.replace("NUMPAD", ""));
            container.move(this.fromSection, this.fromIndex, ContainerManager.ContainerSection.INVENTORY_HOTBAR, destIndex);
          } else if (srcSection == ContainerManager.ContainerSection.CRAFTING_OUT) {
            craftAll(Mouse.isButtonDown(1), (isActive(ShortcutType.DROP) != -1));
          } else {
            move(Mouse.isButtonDown(1), (isActive(ShortcutType.DROP) != -1));
          }
          Mouse.destroy();
          Mouse.create();
          Mouse.setCursorPosition(ex, ey);
        }
      } catch (Exception e) {
        InvTweaks.logInGameErrorStatic("Failed to trigger shortcut", e);
      }
    }
  }

  private void move(boolean separateStacks, boolean drop) throws Exception {
    int toIndex = -1;
    synchronized (this) {
      toIndex = getNextIndex(separateStacks, drop);
      if (toIndex != -1) {
        Slot slot;
        switch (this.shortcutType) {
          case MOVE_ONE_STACK:
            slot = this.container.getSlot(this.fromSection, this.fromIndex);
            while (slot.getHasStack() && toIndex != -1) {
              this.container.move(this.fromSection, this.fromIndex, this.toSection, toIndex);
              toIndex = getNextIndex(separateStacks, drop);
            }
            break;
          case MOVE_ONE_ITEM:
            this.container.moveSome(this.fromSection, this.fromIndex, this.toSection, toIndex, 1);
            break;
          case MOVE_ALL_ITEMS:
            for (Slot slot1 : this.container.getSlots(this.fromSection)) {
              if (slot1.getHasStack() && areSameItemType(this.fromStack, slot1.getStack())) {
                int fromIndex = this.container.getSlotIndex(slot1.slotNumber);
                while (slot1.getHasStack() && toIndex != -1 && (this.fromSection != this.toSection || fromIndex != toIndex)) {
                  boolean moveResult = this.container.move(this.fromSection, fromIndex, this.toSection, toIndex);
                  if (!moveResult)
                    break;
                  toIndex = getNextIndex(separateStacks, drop);
                }
              }
            }
            break;
        }
      }
    }
  }

  private void craftAll(boolean separateStacks, boolean drop) throws Exception {
    int toIndex = getNextIndex(separateStacks, drop);
    Slot slot = this.container.getSlot(this.fromSection, this.fromIndex);
    if (slot.getHasStack()) {
      int idToCraft = getItemID(slot.getStack());
      do {
        this.container.move(this.fromSection, this.fromIndex, this.toSection, toIndex);
        toIndex = getNextIndex(separateStacks, drop);
        if (getHoldStack() == null)
          continue;
        this.container.leftClick(this.toSection, toIndex);
        toIndex = getNextIndex(separateStacks, drop);
      } while (slot.getHasStack() && getItemID(slot.getStack()) == idToCraft && toIndex != -1);
    }
  }

  private boolean haveControlsChanged() {
    return (!this.shortcutKeysStatus.containsKey(Integer.valueOf(this.mc.gameSettings.keyBindForward.keyCode)) || !this.shortcutKeysStatus.containsKey(Integer.valueOf(this.mc.gameSettings.keyBindBack.keyCode)));
  }

  private void updateKeyStatuses() {
    if (haveControlsChanged())
      reset();
    for (Iterator<Integer> i$ = this.shortcutKeysStatus.keySet().iterator(); i$.hasNext(); ) {
      int keyCode = ((Integer)i$.next()).intValue();
      if (Keyboard.isKeyDown(keyCode)) {
        if (!((Boolean)this.shortcutKeysStatus.get(Integer.valueOf(keyCode))).booleanValue())
          this.shortcutKeysStatus.put(Integer.valueOf(keyCode), Boolean.valueOf(true));
        continue;
      }
      this.shortcutKeysStatus.put(Integer.valueOf(keyCode), Boolean.valueOf(false));
    }
  }

  private int getNextIndex(boolean emptySlotOnly, boolean drop) {
    if (drop)
      return -999;
    int result = -1;
    if (!emptySlotOnly) {
      int i = 0;
      for (Slot slot : this.container.getSlots(this.toSection)) {
        if (slot.getHasStack()) {
          ItemStack stack = slot.getStack();
          if (stack.isItemEqual(this.fromStack) && getStackSize(stack) < getMaxStackSize(stack)) {
            result = i;
            break;
          }
        }
        i++;
      }
    }
    if (result == -1)
      result = this.container.getFirstEmptyIndex(this.toSection);
    if (result == -1 && this.toSection == ContainerManager.ContainerSection.FURNACE_IN) {
      this.toSection = ContainerManager.ContainerSection.FURNACE_FUEL;
      result = this.container.getFirstEmptyIndex(this.toSection);
    }
    return result;
  }

  private int isActive(ShortcutType shortcutType) {
    for (Integer keyCode : this.shortcuts.get(shortcutType)) {
      if (((Boolean)this.shortcutKeysStatus.get(keyCode)).booleanValue() && (keyCode.intValue() != 29 || !Keyboard.isKeyDown(Keyboard.KEY_RMENU)))
        return keyCode.intValue();
    }
    return -1;
  }

  private void initAction(int fromSlot, ShortcutType shortcutType, ContainerManager.ContainerSection destSection) throws Exception {
    this.container = new ContainerManager(this.mc);
    this.fromSection = this.container.getSlotSection(fromSlot);
    this.fromIndex = this.container.getSlotIndex(fromSlot);
    this.fromStack = this.container.getItemStack(this.fromSection, this.fromIndex);
    this.shortcutType = shortcutType;
    this.toSection = destSection;
    if (getHoldStack() != null) {
      this.container.leftClick(this.fromSection, this.fromIndex);
      if (getHoldStack() != null) {
        int firstEmptyIndex = this.container.getFirstEmptyIndex(ContainerManager.ContainerSection.INVENTORY);
        if (firstEmptyIndex != -1) {
          this.fromSection = ContainerManager.ContainerSection.INVENTORY;
          fromSlot = firstEmptyIndex;
          this.container.leftClick(this.fromSection, fromSlot);
        } else {
          throw new Exception("Couldn't put hold item down");
        }
      }
    }
  }

  private Slot getSlotAtPosition(GuiContainer guiContainer, int i, int j) {
    for (int k = 0; k < guiContainer.inventorySlots.slots.size(); k++) {
      Slot slot = guiContainer.inventorySlots.slots.get(k);
      if (InvTweaks.getIsMouseOverSlot(guiContainer, slot, i, j))
        return slot;
    }
    return null;
  }

  private ShortcutType propNameToShortcutType(String property) {
    if (property.equals("shortcutKeyAllItems"))
      return ShortcutType.MOVE_ALL_ITEMS;
    if (property.equals("shortcutKeyToLowerSection"))
      return ShortcutType.MOVE_DOWN;
    if (property.equals("shortcutKeyDrop"))
      return ShortcutType.DROP;
    if (property.equals("shortcutKeyOneItem"))
      return ShortcutType.MOVE_ONE_ITEM;
    if (property.equals("shortcutKeyOneStack"))
      return ShortcutType.MOVE_ONE_STACK;
    if (property.equals("shortcutKeyToUpperSection"))
      return ShortcutType.MOVE_UP;
    return null;
  }
}
