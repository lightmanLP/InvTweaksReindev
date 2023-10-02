package com.lightmanlp.invtweaks.config;

import java.awt.Point;
import com.lightmanlp.invtweaks.tree.ItemTree;

public class SortingRule implements Comparable<SortingRule> {
  public enum RuleType {
    RECTANGLE(1),
    ROW(2),
    COLUMN(3),
    TILE(4);

    private int highestPriority;

    private int lowestPriority;

    RuleType(int priorityLevel) {
      this.lowestPriority = priorityLevel * 1000000;
      this.highestPriority = (priorityLevel + 1) * 1000000 - 1;
    }

    public int getLowestPriority() {
      return this.lowestPriority;
    }

    public int getHighestPriority() {
      return this.highestPriority;
    }
  }

  private String constraint;

  private int[] preferredPositions;

  private String keyword;

  private RuleType type;

  private int priority;

  private int containerSize;

  private int containerRowSize;

  public SortingRule(ItemTree tree, String constraint, String keyword, int containerSize, int containerRowSize) {
    this.keyword = keyword;
    this.constraint = constraint;
    this.type = getRuleType(constraint);
    this.containerSize = containerSize;
    this.containerRowSize = containerRowSize;
    this.preferredPositions = getRulePreferredPositions(constraint);
    this.priority = this.type.getLowestPriority() + 100000 + tree.getKeywordDepth(keyword) * 1000 - tree.getKeywordOrder(keyword);
  }

  public RuleType getType() {
    return this.type;
  }

  public int[] getPreferredSlots() {
    return this.preferredPositions;
  }

  public String getKeyword() {
    return this.keyword;
  }

  public String getRawConstraint() {
    return this.constraint;
  }

  public int getPriority() {
    return this.priority;
  }

  public int compareTo(SortingRule o) {
    return getPriority() - o.getPriority();
  }

  public int[] getRulePreferredPositions(String constraint) {
    return getRulePreferredPositions(constraint, this.containerSize, this.containerRowSize);
  }

  public static int[] getRulePreferredPositions(String constraint, int containerSize, int containerRowSize) {
    int[] result = null;
    int containerColumnSize = containerSize / containerRowSize;
    if (constraint.length() >= 5) {
      boolean vertical = false;
      if (constraint.contains("v")) {
        vertical = true;
        constraint = constraint.replaceAll("v", "");
      }
      String[] elements = constraint.split("-");
      if (elements.length == 2) {
        int[] slots1 = getRulePreferredPositions(elements[0], containerSize, containerRowSize);
        int[] slots2 = getRulePreferredPositions(elements[1], containerSize, containerRowSize);
        if (slots1.length == 1 && slots2.length == 1) {
          int slot1 = slots1[0], slot2 = slots2[0];
          Point point1 = new Point(slot1 % containerRowSize, slot1 / containerRowSize);
          Point point2 = new Point(slot2 % containerRowSize, slot2 / containerRowSize);
          result = new int[(Math.abs(point2.y - point1.y) + 1) * (Math.abs(point2.x - point1.x) + 1)];
          int resultIndex = 0;
          if (vertical)
            for (Point p : new Point[] { point1, point2 }) {
              int buffer = p.x;
              p.x = p.y;
              p.y = buffer;
            }
          int y = point1.y;
          while ((point1.y < point2.y) ? (y <= point2.y) : (y >= point2.y)) {
            int x = point1.x;
            while ((point1.x < point2.x) ? (x <= point2.x) : (x >= point2.x)) {
              result[resultIndex++] = vertical ? index(containerRowSize, x, y) : index(containerRowSize, y, x);
              x += (point1.x < point2.x) ? 1 : -1;
            }
            y += (point1.y < point2.y) ? 1 : -1;
          }
          if (constraint.contains("r"))
            reverseArray(result);
        }
      }
    } else {
      int column = -1, row = -1;
      boolean reverse = false;
      int i;
      for (i = 0; i < constraint.length(); i++) {
        char c = constraint.charAt(i);
        if (c <= '9') {
          column = c - 49;
        } else if (c == 'r') {
          reverse = true;
        } else {
          row = c - 97;
        }
      }
      if (column != -1 && row != -1) {
        result = new int[] { index(containerRowSize, row, column) };
      } else if (row != -1) {
        result = new int[containerRowSize];
        for (i = 0; i < containerRowSize; i++)
          result[i] = index(containerRowSize, row, reverse ? (containerRowSize - 1 - i) : i);
      } else {
        result = new int[containerColumnSize];
        for (i = 0; i < containerColumnSize; i++)
          result[i] = index(containerRowSize, reverse ? i : (containerColumnSize - 1 - i), column);
      }
    }
    return result;
  }

  public static RuleType getRuleType(String constraint) {
    RuleType result = RuleType.TILE;
    if (constraint.length() == 1 || (constraint.length() == 2 && constraint.contains("r"))) {
      constraint = constraint.replace("r", "");
      if (constraint.getBytes()[0] <= 57) {
        result = RuleType.COLUMN;
      } else {
        result = RuleType.ROW;
      }
    } else if (constraint.length() > 4) {
      result = RuleType.RECTANGLE;
    }
    return result;
  }

  public String toString() {
    return this.constraint + " " + this.keyword;
  }

  private static int index(int rowSize, int row, int column) {
    return row * rowSize + column;
  }

  private static void reverseArray(int[] data) {
    int left = 0;
    int right = data.length - 1;
    while (left < right) {
      int temp = data[left];
      data[left] = data[right];
      data[right] = temp;
      left++;
      right--;
    }
  }
}
