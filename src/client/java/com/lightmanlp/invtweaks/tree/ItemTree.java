package com.lightmanlp.invtweaks.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

public class ItemTree {
  public static final int MAX_CATEGORY_RANGE = 1000;

  private static final Logger log = Logger.getLogger("InvTweaks");

  private Map<String, ItemTreeCategory> categories = new HashMap<String, ItemTreeCategory>();

  private Map<Integer, Vector<ItemTreeItem>> itemsById = new HashMap<Integer, Vector<ItemTreeItem>>(500);

  private static Vector<ItemTreeItem> defaultItems = null;

  private Map<String, Vector<ItemTreeItem>> itemsByName = new HashMap<String, Vector<ItemTreeItem>>(500);

  private String rootCategory;

  public ItemTree() {
    reset();
  }

  public void reset() {
    if (defaultItems == null) {
      defaultItems = new Vector<ItemTreeItem>();
      defaultItems.add(new ItemTreeItem("unknown", -1, -1, Integer.MAX_VALUE));
    }
    this.categories.clear();
    this.itemsByName.clear();
    this.itemsById.clear();
  }

  public boolean matches(List<ItemTreeItem> items, String keyword) {
    if (items == null)
      return false;
    for (ItemTreeItem item : items) {
      if (item.getName().equals(keyword))
        return true;
    }
    ItemTreeCategory category = getCategory(keyword);
    if (category != null)
      for (ItemTreeItem item : items) {
        if (category.contains(item))
          return true;
      }
    if (keyword.equals(this.rootCategory))
      return true;
    return false;
  }

  public int getKeywordDepth(String keyword) {
    try {
      return getRootCategory().findKeywordDepth(keyword);
    } catch (NullPointerException e) {
      log.severe("The root category is missing: " + e.getMessage());
      return 0;
    }
  }

  public int getKeywordOrder(String keyword) {
    List<ItemTreeItem> items = getItems(keyword);
    if (items != null && items.size() != 0)
      return ((ItemTreeItem)items.get(0)).getOrder();
    try {
      return getRootCategory().findCategoryOrder(keyword);
    } catch (NullPointerException e) {
      log.severe("The root category is missing: " + e.getMessage());
      return -1;
    }
  }

  public boolean isKeywordValid(String keyword) {
    if (containsItem(keyword))
      return true;
    ItemTreeCategory category = getCategory(keyword);
    return (category != null);
  }

  public Collection<ItemTreeCategory> getAllCategories() {
    return this.categories.values();
  }

  public ItemTreeCategory getRootCategory() {
    return this.categories.get(this.rootCategory);
  }

  public ItemTreeCategory getCategory(String keyword) {
    return this.categories.get(keyword);
  }

  public List<ItemTreeItem> getItems(int id, int damage) {
    List<ItemTreeItem> items = this.itemsById.get(Integer.valueOf(id));
    List<ItemTreeItem> filteredItems = null;
    if (items != null) {
      for (ItemTreeItem item : items) {
        if (item.getDamage() != -1 && item.getDamage() != damage) {
          if (filteredItems == null)
            filteredItems = new ArrayList<ItemTreeItem>(items);
          filteredItems.remove(item);
        }
      }
      return (filteredItems != null && !filteredItems.isEmpty()) ? filteredItems : items;
    }
    log.warning("Unknown item id: " + id);
    return defaultItems;
  }

  public List<ItemTreeItem> getItems(String name) {
    return this.itemsByName.get(name);
  }

  public ItemTreeItem getRandomItem(Random r) {
    return (ItemTreeItem)this.itemsByName.values().toArray()[r.nextInt(this.itemsByName.size())];
  }

  public boolean containsItem(String name) {
    return this.itemsByName.containsKey(name);
  }

  public boolean containsCategory(String name) {
    return this.categories.containsKey(name);
  }

  protected void setRootCategory(ItemTreeCategory category) {
    this.rootCategory = category.getName();
    this.categories.put(this.rootCategory, category);
  }

  protected void addCategory(String parentCategory, ItemTreeCategory newCategory) throws NullPointerException {
    ((ItemTreeCategory)this.categories.get(parentCategory.toLowerCase())).addCategory(newCategory);
    this.categories.put(newCategory.getName(), newCategory);
  }

  protected void addItem(String parentCategory, ItemTreeItem newItem) throws NullPointerException {
    ((ItemTreeCategory)this.categories.get(parentCategory.toLowerCase())).addItem(newItem);
    if (this.itemsByName.containsKey(newItem.getName())) {
      ((Vector<ItemTreeItem>)this.itemsByName.get(newItem.getName())).add(newItem);
    } else {
      Vector<ItemTreeItem> list = new Vector<ItemTreeItem>();
      list.add(newItem);
      this.itemsByName.put(newItem.getName(), list);
    }
    if (this.itemsById.containsKey(Integer.valueOf(newItem.getId()))) {
      ((Vector<ItemTreeItem>)this.itemsById.get(Integer.valueOf(newItem.getId()))).add(newItem);
    } else {
      Vector<ItemTreeItem> list = new Vector<ItemTreeItem>();
      list.add(newItem);
      this.itemsById.put(Integer.valueOf(newItem.getId()), list);
    }
  }

  private void log(ItemTreeCategory category, int indentLevel) {
    String logIdent = "";
    for (int i = 0; i < indentLevel; i++)
      logIdent = logIdent + "  ";
    log.info(logIdent + category.getName());
    for (ItemTreeCategory subCategory : category.getSubCategories())
      log(subCategory, indentLevel + 1);
    for (List<ItemTreeItem> itemList : category.getItems()) {
      for (ItemTreeItem item : itemList)
        log.info(logIdent + "  " + item + " " + item.getId() + " " + item.getDamage());
    }
  }
}
