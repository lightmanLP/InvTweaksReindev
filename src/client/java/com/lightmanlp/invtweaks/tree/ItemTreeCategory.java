package com.lightmanlp.invtweaks.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ItemTreeCategory {
  private final Map<Integer, List<ItemTreeItem>> items = new HashMap<Integer, List<ItemTreeItem>>();

  private final Vector<String> matchingItems = new Vector<String>();

  private final Vector<ItemTreeCategory> subCategories = new Vector<ItemTreeCategory>();

  private String name;

  private int order = -1;

  public ItemTreeCategory(String name) {
    this.name = (name != null) ? name.toLowerCase() : null;
  }

  public boolean contains(ItemTreeItem item) {
    List<ItemTreeItem> storedItems = this.items.get(Integer.valueOf(item.getId()));
    if (storedItems != null)
      for (ItemTreeItem storedItem : storedItems) {
        if (storedItem.equals(item))
          return true;
      }
    for (ItemTreeCategory category : this.subCategories) {
      if (category.contains(item))
        return true;
    }
    return false;
  }

  public void addCategory(ItemTreeCategory category) {
    this.subCategories.add(category);
  }

  public void addItem(ItemTreeItem item) {
    if (this.items.get(Integer.valueOf(item.getId())) == null) {
      List<ItemTreeItem> itemList = new ArrayList<ItemTreeItem>();
      itemList.add(item);
      this.items.put(Integer.valueOf(item.getId()), itemList);
    } else {
      ((List<ItemTreeItem>)this.items.get(Integer.valueOf(item.getId()))).add(item);
    }
    this.matchingItems.add(item.getName());
    if (this.order == -1 || this.order > item.getOrder())
      this.order = item.getOrder();
  }

  public int getCategoryOrder() {
    if (this.order != -1)
      return this.order;
    for (ItemTreeCategory category : this.subCategories) {
      int order = category.getCategoryOrder();
      if (order != -1)
        return order;
    }
    return -1;
  }

  public int findCategoryOrder(String keyword) {
    if (keyword.equals(this.name))
      return getCategoryOrder();
    for (ItemTreeCategory category : this.subCategories) {
      int result = category.findCategoryOrder(keyword);
      if (result != -1)
        return result;
    }
    return -1;
  }

  public int findKeywordDepth(String keyword) {
    if (this.name.equals(keyword))
      return 0;
    if (this.matchingItems.contains(keyword))
      return 1;
    for (ItemTreeCategory category : this.subCategories) {
      int result = category.findKeywordDepth(keyword);
      if (result != -1)
        return result + 1;
    }
    return -1;
  }

  public Collection<ItemTreeCategory> getSubCategories() {
    return this.subCategories;
  }

  public Collection<List<ItemTreeItem>> getItems() {
    return this.items.values();
  }

  public String getName() {
    return this.name;
  }

  public String toString() {
    return this.name + " (" + this.subCategories.size() + " cats, " + this.items.size() + " items)";
  }
}
