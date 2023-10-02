package com.lightmanlp.invtweaks.tree;

import java.io.File;
import java.util.LinkedList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lightmanlp.invtweaks.InvTweaks;

public class ItemTreeLoader extends DefaultHandler {
  private static final String ATTR_RANGE_MIN = "min";

  private static final String ATTR_RANGE_MAX = "max";

  private static final String ATTR_ID = "id";

  private static final String ATTR_DAMAGE = "damage";

  private ItemTree tree;

  private int itemOrder = 0;

  private LinkedList<String> categoryStack = new LinkedList<String>();

  public ItemTreeLoader() {
    this.tree = new ItemTree();
  }

  public ItemTree load(String filePath) throws Exception {
    this.tree.reset();
    this.categoryStack.clear();
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    SAXParser parser = parserFactory.newSAXParser();
    parser.parse(new File(filePath), this);
    if (!this.categoryStack.isEmpty())
      InvTweaks.logInGameStatic("Warning: The tree file seems to be broken (is '" + (String)this.categoryStack.getLast() + "' closed correctly?)");
    return this.tree;
  }

  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    String rangeMinAttr = attributes.getValue("min");
    if (attributes.getLength() == 0 || rangeMinAttr != null) {
      if (this.categoryStack.isEmpty()) {
        this.tree.setRootCategory(new ItemTreeCategory(name));
      } else {
        this.tree.addCategory(this.categoryStack.getLast(), new ItemTreeCategory(name));
      }
      if (rangeMinAttr != null) {
        int rangeMin = Integer.parseInt(rangeMinAttr);
        int rangeMax = Integer.parseInt(attributes.getValue("max"));
        for (int i = rangeMin; i <= rangeMax; i++)
          this.tree.addItem(name, new ItemTreeItem((name + i).toLowerCase(), i, -1, this.itemOrder++));
      }
      this.categoryStack.add(name);
    } else {
      int id = Integer.parseInt(attributes.getValue("id"));
      int damage = -1;
      if (attributes.getValue("damage") != null)
        damage = Integer.parseInt(attributes.getValue("damage"));
      this.tree.addItem(this.categoryStack.getLast(), new ItemTreeItem(name.toLowerCase(), id, damage, this.itemOrder++));
    }
  }

  public void endElement(String uri, String localName, String name) throws SAXException {
    if (name.equals(this.categoryStack.getLast()))
      this.categoryStack.removeLast();
  }
}
