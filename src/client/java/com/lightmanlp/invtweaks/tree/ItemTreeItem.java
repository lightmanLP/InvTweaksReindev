package com.lightmanlp.invtweaks.tree;

import com.lightmanlp.invtweaks.library.Obfuscation;

import net.minecraft.src.game.item.ItemStack;

public class ItemTreeItem extends Obfuscation implements Comparable<ItemTreeItem> {
  private String name;

  private int id;

  private int damage;

  private int order;

  public ItemTreeItem(String name, int id, int damage, int order) {
    super(null);
    this.name = name;
    this.id = id;
    this.damage = damage;
    this.order = order;
  }

  public String getName() {
    return this.name;
  }

  public int getId() {
    return this.id;
  }

  public int getDamage() {
    return this.damage;
  }

  public int getOrder() {
    return this.order;
  }

  public boolean matchesStack(ItemStack stack) {
    return (getItemID(stack) == this.id && (getMaxStackSize(stack) == 1 || getItemDamage(stack) == this.damage));
  }

  public boolean equals(Object o) {
    if (o == null || !(o instanceof ItemTreeItem))
      return false;
    ItemTreeItem item = (ItemTreeItem)o;
    return (this.id == item.getId() && (this.damage == -1 || this.damage == item.getDamage()));
  }

  public String toString() {
    return this.name;
  }

  public int compareTo(ItemTreeItem item) {
    return item.order - this.order;
  }
}
