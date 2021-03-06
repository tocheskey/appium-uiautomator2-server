/*
 * Copyright (C) 2013 DroidDriver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appium.uiautomator2.model;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.appium.uiautomator2.utils.Attribute;
import io.appium.uiautomator2.utils.Preconditions;


/**
 * A UiElement that gets attributes via the Accessibility API.
 */
@TargetApi(18)
public class UiAutomationElement extends UiElement<AccessibilityNodeInfo, UiAutomationElement> {

  private final Map<Attribute, Object> attributes;
  private final boolean visible;
  private final Rect visibleBounds;
  private final UiAutomationElement parent;
  private final List<UiAutomationElement> children;
  public final static Map<AccessibilityNodeInfo, UiAutomationElement>  map = new WeakHashMap<AccessibilityNodeInfo, UiAutomationElement>();
  /**
   * A snapshot of all attributes is taken at construction. The attributes of a
   * {@code UiAutomationElement} instance are immutable. If the underlying
   * {@link AccessibilityNodeInfo} is updated, a new {@code UiAutomationElement}
   * instance will be created in
   */
  protected UiAutomationElement( AccessibilityNodeInfo node,
                                UiAutomationElement parent, int index) {
    this.node = Preconditions.checkNotNull(node);
    this.parent = parent;

    Map<Attribute, Object> attribs = new EnumMap<Attribute, Object>(Attribute.class);

    put(attribs, Attribute.INDEX, index);
    put(attribs, Attribute.PACKAGE, charSequenceToString(node.getPackageName()));
    put(attribs, Attribute.CLASS, charSequenceToString(node.getClassName()));
    put(attribs, Attribute.TEXT, charSequenceToString(node.getText()));
    put(attribs, Attribute.CONTENT_DESC, charSequenceToString(node.getContentDescription()));
    put(attribs, Attribute.RESOURCE_ID, charSequenceToString(node.getViewIdResourceName()));
    put(attribs, Attribute.CHECKABLE, node.isCheckable());
    put(attribs, Attribute.CHECKED, node.isChecked());
    put(attribs, Attribute.CLICKABLE, node.isClickable());
    put(attribs, Attribute.ENABLED, node.isEnabled());
    put(attribs, Attribute.FOCUSABLE, node.isFocusable());
    put(attribs, Attribute.FOCUSED, node.isFocused());
    put(attribs, Attribute.LONG_CLICKABLE, node.isLongClickable());
    put(attribs, Attribute.PASSWORD, node.isPassword());
    put(attribs, Attribute.SCROLLABLE, node.isScrollable());
    if (node.getTextSelectionStart() >= 0
        && node.getTextSelectionStart() != node.getTextSelectionEnd()) {
      attribs.put(Attribute.SELECTION_START, node.getTextSelectionStart());
      attribs.put(Attribute.SELECTION_END, node.getTextSelectionEnd());
    }
    put(attribs, Attribute.SELECTED, node.isSelected());
    put(attribs, Attribute.BOUNDS, getBounds(node));
    attributes = Collections.unmodifiableMap(attribs);

    // Order matters as getVisibleBounds depends on visible
    visible = node.isVisibleToUser();
    visibleBounds = getVisibleBounds(node);
    List<UiAutomationElement> mutableChildren = buildChildren(node);
    this.children = mutableChildren == null ? null : Collections.unmodifiableList(mutableChildren);
  }

  private void put(Map<Attribute, Object> attribs, Attribute key, Object value) {
    if (value != null) {
      attribs.put(key, value);
    }
  }

  private List<UiAutomationElement> buildChildren(AccessibilityNodeInfo node) {
    List<UiAutomationElement> children;
    int childCount = node.getChildCount();
    if (childCount == 0) {
      children = null;
    } else {
      children = new ArrayList<UiAutomationElement>(childCount);
      for (int i = 0; i < childCount; i++) {
        AccessibilityNodeInfo child = node.getChild(i);
        if (child != null) {
          children.add(this.getElement(child, this, i));
        }
      }
    }
    return children;
  }

  public static UiAutomationElement newRootElement(AccessibilityNodeInfo rawRoot) {
    clearData();
    return getElement(rawRoot, null /* parent */, 0 /* index */);
  }

  private static void clearData() {
    map.clear();
    XPathFinder.clearData();
  }

  public static UiAutomationElement getElement(AccessibilityNodeInfo rawElement, UiAutomationElement parent, int index) {
    UiAutomationElement element = map.get(rawElement);
    if (element == null) {
      element = new UiAutomationElement(rawElement, parent, index);
      map.put(rawElement, element);
    }
    return element;
  }

  private Rect getBounds(AccessibilityNodeInfo node) {
    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    return rect;
  }

  private Rect getVisibleBounds(AccessibilityNodeInfo node) {
    if (!visible) {
      return new Rect();
    }
    Rect visibleBounds = getBounds(this.node);
    UiAutomationElement parent = getParent();
    Rect parentBounds;
    while (parent != null) {
      parentBounds = parent.getBounds(this.parent.node);
      visibleBounds.intersect(parentBounds);
      parent = parent.getParent();
    }
    return visibleBounds;
  }

  public UiAutomationElement getParent() {
    return parent;
  }

  @Override
  protected List<UiAutomationElement> getChildren() {
    if (children == null) {
      return Collections.emptyList();
    }
    return children;
  }

  @Override
  protected Map<Attribute, Object> getAttributes() {
    return attributes;
  }

  public static String charSequenceToString(CharSequence input) {
    return input == null ? null : input.toString();
  }
}
