package com.lightmanlp.invtweaks.library;

import java.io.File;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.src.client.KeyBinding;
import net.minecraft.src.client.gui.Container;
import net.minecraft.src.client.gui.ContainerPlayer;
import net.minecraft.src.client.gui.GuiContainer;
import net.minecraft.src.client.gui.GuiContainerChest;
import net.minecraft.src.client.gui.GuiContainerDispenser;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.Slot;
import net.minecraft.src.client.player.PlayerController;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.entity.player.InventoryPlayer;
import net.minecraft.src.game.item.ItemStack;

public class Obfuscation {
  protected Minecraft mc;

  public Obfuscation(Minecraft mc) {
    this.mc = mc;
  }

  protected void addChatMessage(String message) {
    if (this.mc.ingameGUI != null)
      this.mc.ingameGUI.addChatMessage(message);
  }

  protected boolean isMultiplayerWorld() {
    return this.mc.isMultiplayerWorld();
  }

  protected EntityPlayer getThePlayer() {
    return (EntityPlayer)this.mc.thePlayer;
  }

  protected PlayerController getPlayerController() {
    return this.mc.playerController;
  }

  protected GuiScreen getCurrentScreen() {
    return this.mc.currentScreen;
  }

  protected InventoryPlayer getInventoryPlayer() {
    return (getThePlayer()).inventory;
  }

  protected ItemStack getCurrentEquippedItem() {
    return getThePlayer().getCurrentEquippedItem();
  }

  protected Container getCraftingInventory() {
    return getThePlayer().currentContainer;
  }

  protected ContainerPlayer getPlayerContainer() {
    return (ContainerPlayer)getThePlayer().playerContainer;
  }

  protected ItemStack[] getMainInventory() {
    return (getInventoryPlayer()).mainInventory;
  }

  protected void setMainInventory(ItemStack[] value) {
    (getInventoryPlayer()).mainInventory = (ItemStack[])value;
  }

  protected void setHasInventoryChanged(boolean value) {
    (getInventoryPlayer()).inventoryChanged = value;
  }

  protected void setHoldStack(ItemStack stack) {
    getInventoryPlayer().setCursorStack(stack);
  }

  protected boolean hasInventoryChanged() {
    return (getInventoryPlayer()).inventoryChanged;
  }

  protected ItemStack getHoldStack() {
    return getInventoryPlayer().getCursorStack();
  }

  protected ItemStack getFocusedStack() {
    return getInventoryPlayer().getCurrentItem();
  }

  protected int getFocusedSlot() {
    return (getInventoryPlayer()).currentItem;
  }

  protected ItemStack createItemStack(int id, int size, int damage) {
    return new ItemStack(id, size, damage);
  }

  protected ItemStack copy(ItemStack itemStack) {
    return itemStack.copy();
  }

  protected int getItemDamage(ItemStack itemStack) {
    return itemStack.getItemDamage();
  }

  protected int getMaxStackSize(ItemStack itemStack) {
    return itemStack.getMaxStackSize();
  }

  protected int getStackSize(ItemStack itemStack) {
    return itemStack.stackSize;
  }

  protected void setStackSize(ItemStack itemStack, int value) {
    itemStack.stackSize = value;
  }

  protected int getItemID(ItemStack itemStack) {
    return itemStack.itemID;
  }

  protected boolean areItemStacksEqual(ItemStack itemStack1, ItemStack itemStack2) {
    return ItemStack.areItemStacksEqual(itemStack1, itemStack2);
  }

  protected boolean areSameItemType(ItemStack itemStack1, ItemStack itemStack2) {
    return (itemStack1.isItemEqual(itemStack2) || (itemStack1.isItemStackDamageable() && getItemID(itemStack1) == getItemID(itemStack2)));
  }

  protected ItemStack clickInventory(PlayerController playerController, int windowId, int slot, int clickButton, boolean shiftHold, EntityPlayer entityPlayer) {
    // TODO: check shiftHold because of transferType
    return playerController.clickSlot(windowId, slot, clickButton, (shiftHold ? 1 : 0), entityPlayer);
  }

  protected int getWindowId(Container container) {
    return container.windowId;
  }

  protected List<?> getSlots(Container container) {
    return container.slots;
  }

  protected Slot getSlot(Container container, int i) {
    return (Slot)getSlots(container).get(i);
  }

  protected ItemStack getSlotStack(Container container, int i) {
    Slot slot = (Slot)getSlots(container).get(i);
    return (slot == null) ? null : slot.getStack();
  }

  protected void setSlotStack(Container container, int i, ItemStack stack) {
    container.putStackInSlot(i, stack);
  }

  protected Container getContainer(GuiContainer guiContainer) {
    return guiContainer.inventorySlots;
  }

  protected boolean isChestOrDispenser(GuiScreen guiScreen) {
    // TODO: check MLGuiChestBuilding
    return ((guiScreen instanceof GuiContainerChest && !guiScreen.getClass().getSimpleName().equals("MLGuiChestBuilding")) || guiScreen instanceof GuiContainerDispenser);
  }

  protected int getKeycode(KeyBinding keyBinding) {
    return keyBinding.keyCode;
  }

  public static String getMinecraftDir() {
    String absolutePath = Minecraft.getMinecraftDir().getAbsolutePath();
    if (absolutePath.endsWith("."))
      return absolutePath.substring(0, absolutePath.length() - 1);
    if (absolutePath.endsWith(File.separator))
      return absolutePath;
    return absolutePath + File.separatorChar;
  }

  public static ItemStack getHoldStackStatic(Minecraft mc) {
    return (new Obfuscation(mc)).getHoldStack();
  }

  public static GuiScreen getCurrentScreenStatic(Minecraft mc) {
    return (new Obfuscation(mc)).getCurrentScreen();
  }
}
