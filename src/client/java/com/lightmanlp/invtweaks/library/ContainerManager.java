package com.lightmanlp.invtweaks.library;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.Container;
import net.minecraft.src.client.gui.ContainerChest;
import net.minecraft.src.client.gui.ContainerDispenser;
import net.minecraft.src.client.gui.ContainerFurnace;
import net.minecraft.src.client.gui.ContainerPlayer;
import net.minecraft.src.client.gui.ContainerWorkbench;
import net.minecraft.src.client.gui.GuiContainer;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.Slot;
import net.minecraft.src.game.item.ItemStack;

public class ContainerManager extends Obfuscation {
  public static final int DROP_SLOT = -999;

  public static final int INVENTORY_SIZE = 36;

  public static final int HOTBAR_SIZE = 9;

  public static final int ACTION_TIMEOUT = 500;

  public static final int POLLING_DELAY = 3;

  private Container container;

  public enum ContainerSection {
    INVENTORY, INVENTORY_HOTBAR, INVENTORY_NOT_HOTBAR, CHEST, CRAFTING_IN, CRAFTING_OUT, ARMOR, FURNACE_IN, FURNACE_OUT, FURNACE_FUEL, UNKNOWN;
  }

  private Map<ContainerSection, List<Slot>> slotRefs = new HashMap<ContainerSection, List<Slot>>();

  public ContainerManager(Minecraft mc) {
    super(mc);
    GuiScreen currentScreen = getCurrentScreen();
    if (currentScreen instanceof GuiContainer) {
      this.container = getContainer((GuiContainer)currentScreen);
    } else {
      this.container = (Container)getPlayerContainer();
    }
    List<Slot> slots = this.container.slots;
    int size = slots.size();
    boolean guiWithInventory = true;
    if (this.container instanceof ContainerPlayer) {
      this.slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(0, 1));
      this.slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(1, 5));
      this.slotRefs.put(ContainerSection.ARMOR, slots.subList(5, 9));
    } else if (this.container instanceof ContainerChest || this.container instanceof ContainerDispenser) {
      this.slotRefs.put(ContainerSection.CHEST, slots.subList(0, size - 36));
    } else if (this.container instanceof ContainerFurnace) {
      this.slotRefs.put(ContainerSection.FURNACE_IN, slots.subList(0, 1));
      this.slotRefs.put(ContainerSection.FURNACE_FUEL, slots.subList(1, 2));
      this.slotRefs.put(ContainerSection.FURNACE_OUT, slots.subList(2, 3));
    } else if (this.container instanceof ContainerWorkbench) {
      this.slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(0, 1));
      this.slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(1, 10));
    } else if (size >= 36) {
      this.slotRefs.put(ContainerSection.UNKNOWN, slots.subList(0, size - 36));
    } else {
      guiWithInventory = false;
      this.slotRefs.put(ContainerSection.UNKNOWN, slots.subList(0, size));
    }
    if (guiWithInventory) {
      this.slotRefs.put(ContainerSection.INVENTORY, slots.subList(size - 36, size));
      this.slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, slots.subList(size - 36, size - 9));
      this.slotRefs.put(ContainerSection.INVENTORY_HOTBAR, slots.subList(size - 9, size));
    }
  }

  public boolean move(ContainerSection srcSection, int srcIndex, ContainerSection destSection, int destIndex) throws TimeoutException {
    ItemStack srcStack = getItemStack(srcSection, srcIndex);
    ItemStack destStack = getItemStack(destSection, destIndex);
    if (srcStack == null)
      return false;
    if (srcSection == destSection && srcIndex == destIndex)
      return true;
    if (getHoldStack() != null) {
      int firstEmptyIndex = getFirstEmptyIndex(ContainerSection.INVENTORY);
      if (firstEmptyIndex != -1) {
        leftClick(ContainerSection.INVENTORY, firstEmptyIndex);
      } else {
        return false;
      }
    }
    boolean destinationEmpty = (getItemStack(destSection, destIndex) == null);
    if (destStack != null && getItemID(srcStack) == getItemID(destStack) && srcStack.getMaxStackSize() == 1) {
      int intermediateSlot = getFirstEmptyUsableSlotNumber();
      ContainerSection intermediateSection = getSlotSection(intermediateSlot);
      int intermediateIndex = getSlotIndex(intermediateSlot);
      if (intermediateIndex != -1) {
        leftClick(destSection, destIndex);
        leftClick(intermediateSection, intermediateIndex);
        leftClick(srcSection, srcIndex);
        leftClick(destSection, destIndex);
        leftClick(intermediateSection, intermediateIndex);
        leftClick(srcSection, srcIndex);
      } else {
        return false;
      }
    } else {
      leftClick(srcSection, srcIndex);
      leftClick(destSection, destIndex);
      if (!destinationEmpty)
        leftClick(srcSection, srcIndex);
    }
    return true;
  }

  public boolean moveSome(ContainerSection srcSection, int srcIndex, ContainerSection destSection, int destIndex, int amount) throws TimeoutException {
    ItemStack source = getItemStack(srcSection, srcIndex);
    if (source == null || (srcSection == destSection && srcIndex == destIndex))
      return true;
    ItemStack destination = getItemStack(srcSection, srcIndex);
    int sourceSize = getStackSize(source);
    int movedAmount = Math.min(amount, sourceSize);
    if (source != null && (destination == null || source.isItemEqual(destination))) {
      leftClick(srcSection, srcIndex);
      for (int i = 0; i < movedAmount; i++)
        rightClick(destSection, destIndex);
      if (movedAmount < sourceSize)
        leftClick(srcSection, srcIndex);
      return true;
    }
    return false;
  }

  public boolean drop(ContainerSection srcSection, int srcIndex) throws TimeoutException {
    return move(srcSection, srcIndex, (ContainerSection)null, -999);
  }

  public boolean dropSome(ContainerSection srcSection, int srcIndex, int amount) throws TimeoutException {
    return moveSome(srcSection, srcIndex, (ContainerSection)null, -999, amount);
  }

  public void leftClick(ContainerSection section, int index) throws TimeoutException {
    click(section, index, false);
  }

  public void rightClick(ContainerSection section, int index) throws TimeoutException {
    click(section, index, true);
  }

  public void click(ContainerSection section, int index, boolean rightClick) throws TimeoutException {
    int slot = indexToSlot(section, index);
    if (slot != -1)
      clickInventory(getPlayerController(), getWindowId(this.container), slot, rightClick ? 1 : 0, false, getThePlayer());
  }

  public boolean hasSection(ContainerSection section) {
    return this.slotRefs.containsKey(section);
  }

  public List<Slot> getSlots(ContainerSection section) {
    return this.slotRefs.get(section);
  }

  public int getSize() {
    int result = 0;
    for (List<Slot> slots : this.slotRefs.values())
      result += slots.size();
    return result;
  }

  public int getSize(ContainerSection section) {
    if (hasSection(section))
      return this.slotRefs.get(section).size();
    return 0;
  }

  public int getFirstEmptyIndex(ContainerSection section) {
    int i = 0;
    for (Slot slot : this.slotRefs.get(section)) {
      if (!slot.getHasStack())
        return i;
      i++;
    }
    return -1;
  }

  public boolean isSlotEmpty(ContainerSection section, int slot) {
    if (hasSection(section))
      return (getItemStack(section, slot) == null);
    return false;
  }

  public Slot getSlot(ContainerSection section, int index) {
    List<Slot> slots = this.slotRefs.get(section);
    if (slots != null)
      return slots.get(index);
    return null;
  }

  public int getSlotIndex(int slotNumber) {
    for (ContainerSection section : this.slotRefs.keySet()) {
      if (section != ContainerSection.INVENTORY) {
        int i = 0;
        for (Slot slot : this.slotRefs.get(section)) {
          if (slot.slotNumber == slotNumber)
            return i;
          i++;
        }
      }
    }
    return -1;
  }

  public ContainerSection getSlotSection(int slotNumber) {
    for (ContainerSection section : this.slotRefs.keySet()) {
      if (section != ContainerSection.INVENTORY)
        for (Slot slot : this.slotRefs.get(section)) {
          if (slot.slotNumber == slotNumber)
            return section;
        }
    }
    return null;
  }

  public ItemStack getItemStack(ContainerSection section, int index) throws NullPointerException, IndexOutOfBoundsException {
    int slot = indexToSlot(section, index);
    if (slot >= 0 && slot < getSlots(this.container).size())
      return getSlotStack(this.container, slot);
    return null;
  }

  public Container getContainer() {
    return this.container;
  }

  private int getFirstEmptyUsableSlotNumber() {
    for (ContainerSection section : this.slotRefs.keySet()) {
      for (Slot slot : this.slotRefs.get(section)) {
        if (slot.getClass().equals(Slot.class) && !slot.getHasStack())
          return slot.slotNumber;
      }
    }
    return -1;
  }

  private int indexToSlot(ContainerSection section, int index) {
    if (index == -999)
      return -999;
    if (hasSection(section)) {
      Slot slot = ((List<Slot>)this.slotRefs.get(section)).get(index);
      if (slot != null)
        return slot.slotNumber;
      return -1;
    }
    return -1;
  }
}
