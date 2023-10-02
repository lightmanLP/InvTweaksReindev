package com.lightmanlp.invtweaks.config;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Vector;
import com.lightmanlp.invtweaks.tree.ItemTree;

public class InventoryConfigRuleset {
  private String name;

  private int[] lockPriorities;

  private boolean[] frozenSlots;

  private Vector<Integer> lockedSlots;

  private Vector<SortingRule> rules;

  private Vector<String> autoReplaceRules;

  private boolean debugEnabled;

  private ItemTree tree;

  public InventoryConfigRuleset(ItemTree tree, String name) {
    this.tree = tree;
    this.name = name;
    this.lockPriorities = new int[36];
    int i;
    for (i = 0; i < this.lockPriorities.length; i++)
      this.lockPriorities[i] = 0;
    this.frozenSlots = new boolean[36];
    for (i = 0; i < this.frozenSlots.length; i++)
      this.frozenSlots[i] = false;
    this.lockedSlots = new Vector<Integer>();
    this.rules = new Vector<SortingRule>();
    this.autoReplaceRules = new Vector<String>();
    this.debugEnabled = false;
  }

  public String registerLine(String rawLine) throws InvalidParameterException {
    String[] words = rawLine.split(" ");
    String lineText = rawLine.toLowerCase();
    SortingRule newRule = null;
    if (words.length == 2) {
      if (lineText.matches("^([a-d]|[1-9]|[r]){1,2} [\\w]*$") || lineText.matches("^[a-d][1-9]-[a-d][1-9][rv]?[rv]? [\\w]*$")) {
        words[0] = words[0].toLowerCase();
        if (words[1].equals("LOCKED")) {
          int[] newLockedSlots = SortingRule.getRulePreferredPositions(words[0], 36, 9);
          int lockPriority = SortingRule.getRuleType(words[0]).getHighestPriority();
          for (int i : newLockedSlots)
            this.lockPriorities[i] = lockPriority;
          return null;
        }
        if (words[1].equals("FROZEN")) {
          int[] newLockedSlots = SortingRule.getRulePreferredPositions(words[0], 36, 9);
          for (int i : newLockedSlots)
            this.frozenSlots[i] = true;
          return null;
        }
        String keyword = words[1].toLowerCase();
        boolean isValidKeyword = this.tree.isKeywordValid(keyword);
        if (!isValidKeyword) {
          Vector<String> wordVariants = getKeywordVariants(keyword);
          for (String wordVariant : wordVariants) {
            if (this.tree.isKeywordValid(wordVariant.toLowerCase())) {
              isValidKeyword = true;
              keyword = wordVariant;
              break;
            }
          }
        }
        if (isValidKeyword) {
          newRule = new SortingRule(this.tree, words[0], keyword.toLowerCase(), 36, 9);
          this.rules.add(newRule);
          return null;
        }
        return keyword.toLowerCase();
      }
      if (words[0].equals("AUTOREPLACE")) {
        words[1] = words[1].toLowerCase();
        if (this.tree.isKeywordValid(words[1]) || words[1].equals("nothing"))
          this.autoReplaceRules.add(words[1]);
        return null;
      }
    } else if (words.length == 1) {
      if (words[0].equals("DEBUG")) {
        this.debugEnabled = true;
        return null;
      }
    }
    throw new InvalidParameterException();
  }

  public void finalize() {
    if (this.autoReplaceRules.isEmpty())
      try {
        this.autoReplaceRules.add(this.tree.getRootCategory().getName());
      } catch (NullPointerException e) {
        throw new NullPointerException("No root category is defined.");
      }
    Collections.sort(this.rules, Collections.reverseOrder());
    for (int i = 0; i < this.lockPriorities.length; i++) {
      if (this.lockPriorities[i] > 0)
        this.lockedSlots.add(Integer.valueOf(i));
    }
  }

  public String getName() {
    return this.name;
  }

  public int[] getLockPriorities() {
    return this.lockPriorities;
  }

  public boolean[] getFrozenSlots() {
    return this.frozenSlots;
  }

  public Vector<Integer> getLockedSlots() {
    return this.lockedSlots;
  }

  public Vector<SortingRule> getRules() {
    return this.rules;
  }

  public Vector<String> getAutoReplaceRules() {
    return this.autoReplaceRules;
  }

  public boolean isDebugEnabled() {
    return this.debugEnabled;
  }

  private Vector<String> getKeywordVariants(String keyword) {
    Vector<String> variants = new Vector<String>();
    if (keyword.endsWith("es"))
      variants.add(keyword.substring(0, keyword.length() - 2));
    if (keyword.endsWith("s"))
      variants.add(keyword.substring(0, keyword.length() - 1));
    if (keyword.contains("en")) {
      variants.add(keyword.replaceAll("en", ""));
    } else {
      if (keyword.contains("wood"))
        variants.add(keyword.replaceAll("wood", "wooden"));
      if (keyword.contains("gold"))
        variants.add(keyword.replaceAll("gold", "golden"));
    }
    if (keyword.matches("\\w*[A-Z]\\w*")) {
      byte[] keywordBytes = keyword.getBytes();
      for (int i = 0; i < keywordBytes.length; i++) {
        if (keywordBytes[i] >= 65 && keywordBytes[i] <= 90) {
          String swapped = (keyword.substring(i) + keyword.substring(0, i)).toLowerCase();
          variants.add(swapped);
          variants.addAll(getKeywordVariants(swapped));
        }
      }
    }
    return variants;
  }
}
