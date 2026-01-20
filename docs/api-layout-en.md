# About Taffy-Java

Taffy-Java is an embeddable layout engine. It is not a UI framework and does not draw anything.

Its responsibility is simple: **compute the size and position of boxes** from input styles (`Style`), then provide per-node results via `Layout`.

## Laying out a tree

Each box is represented by a node (`NodeId`). Nodes form a tree that stores layout inputs (style/measure/structure) and receives layout outputs (`Layout`).

### Building a tree

- Leaf node: `newLeaf(style)`
- Measured leaf (text/images): `newLeafWithMeasure(style, measureFunc)` or `setMeasureFunc(node, func)`
- Container node: `newWithChildren(style, ...)`, or assemble via `addChild / setChildren`

> **WARNING**
>
> `getChildren(parent)` returns the internal list. Callers must treat it as read-only; change structure via `addChild / setChildren / removeChild`.

### Running layout

Layout is computed for the whole tree at once:

- `computeLayout(root, availableSpace)`
- `availableSpace` describes the space available to the root (typically the viewport/window)

Typical usage: treat the root as a viewport and compute under definite constraints.

```java
tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(viewportWidth),
  AvailableSpace.definite(viewportHeight)
));
```

If an axis is allowed to grow "unbounded", pass `AvailableSpace.maxContent()` for that axis.

### Reading layout results

Results are written back to each node:

- `Layout.location()` (offset relative to the parent)
- `Layout.size()`
- resolved margin/border/padding values

#### Incremental consumption: Yoga-style dirty propagation

Taffy-Java uses a Yoga-style dirty propagation system that allows efficient tree traversal:

- `hasNewLayout(node)`: returns `true` if this node was laid out since last acknowledgement
- `hasDirtyDescendant(node)`: returns `true` if any descendant has a new layout
- `needsVisit(node)`: returns `true` if this node or any descendant needs attention
- `acknowledgeLayout(node)`: mark this node's layout as consumed
- `acknowledgeSubtree(node)`: mark this node's layout as consumed AND clear dirty descendant flag

> **NOTE**: `hasUnconsumedLayout(node)` is still available but deprecated - use `hasNewLayout(node)` instead.

**Efficient tree walking pattern:**

```java
tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(viewportWidth),
  AvailableSpace.definite(viewportHeight)
));

// Start from root and walk only dirty subtrees
void processLayout(TaffyTree tree, NodeId node) {
    if (!tree.needsVisit(node)) {
        return; // Skip this entire subtree - no changes
    }
    
    // Process this node if it has a new layout
    if (tree.hasNewLayout(node)) {
        Layout l = tree.getLayout(node);
        // ... push l into the renderer (transform/size/clipping/etc.)
    }
    
    // Recurse to children
    for (NodeId child : tree.getChildren(node)) {
        processLayout(tree, child);
    }
    
    // Acknowledge after processing children (bottom-up)
    tree.acknowledgeSubtree(node);
}

processLayout(tree, root);
```

This pattern allows you to:
1. Skip entire unchanged subtrees (when `needsVisit()` returns false)
2. Process only nodes with new layouts
3. Use bottom-up acknowledgement for proper dirty flag clearing

> **NOTE**
>
> Unlike a version-based approach, `hasNewLayout()` is set for ALL nodes that are laid out during `computeLayout()`, regardless of whether their layout actually changed. This matches Yoga's behavior and allows reliable tree walking without binding.

> **NOTE**
>
> `Layout.location()` is commonly interpreted as "child border-box relative to parent border-box". For absolute render coordinates, accumulate locations up the parent chain.

```java
float absX = 0f, absY = 0f;
NodeId cur = node;
while (cur != null) {
  Layout l = tree.getLayout(cur);
  absX += l.location().x;
  absY += l.location().y;
  cur = tree.getParent(cur);
}
```

## Styling

### Default styles (important)

`Style` defaults directly affect layout results. In particular:

- `display` defaults to `FLEX` (`Display.DEFAULT`)
- `boxSizing` defaults to `BORDER_BOX`
- `position` defaults to `RELATIVE`, and `inset` defaults to `AUTO`

When block layout (or other behavior) is required, set it explicitly.

```java
Style rootStyle = new Style();
rootStyle.display = Display.BLOCK;
```

### Percent representation

`Dimension.percent(x)` / `LengthPercentage.percent(x)` use 0.0 ~ 1.0 fractional percentages.

```java
// 50%
Dimension.percent(0.5f);
```

### Mutations: do not make the tree guess

`Style` fields are public, but the tree does not automatically detect field-level mutations.

Recommended: after mutating a style, commit it via `setStyle(node, style)` (which also marks dirtiness).

```java
Style s = tree.getStyle(node);
s.flexGrow = 1.0f;
tree.setStyle(node, s);
```

Call `markDirty(node)` explicitly if a change cannot be expressed via `setStyle(...)`.

### Rounding (pixel grid alignment)

Rounding is enabled by default. Disable it only if the renderer intentionally consumes sub-pixel values.

```java
TaffyTree tree = new TaffyTree();
// tree.disableRounding();
```

### Content-sized leaves and `MeasureFunc`

Taffy-Java does not shape or measure text. For content-sized leaves, `MeasureFunc` answers one question: **what intrinsic width/height should this node have under constraints**.

> **WARNING**
>
> `MeasureFunc` should not perform layout (alignment/line breaking/space distribution). It returns a size; placement is handled by the engine.

## Examples

### Example 1: flex row under viewport constraints

```java
TaffyTree tree = new TaffyTree();

Style childStyle = new Style();
childStyle.size = new Size<>(Dimension.length(100f), Dimension.length(50f));

NodeId a = tree.newLeaf(childStyle);
NodeId b = tree.newLeaf(childStyle);

Style rootStyle = new Style();
rootStyle.display = Display.FLEX;
rootStyle.flexDirection = FlexDirection.ROW;
rootStyle.gap = new Size<>(LengthPercentage.length(10f), LengthPercentage.length(0f));

NodeId root = tree.newWithChildren(rootStyle, a, b);
tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(800f),
  AvailableSpace.definite(600f)
));

Layout la = tree.getLayout(a);
Layout lb = tree.getLayout(b);
```

### Example 2: absolute positioning (`position` + `inset`)

```java
TaffyTree tree = new TaffyTree();

Style rootStyle = new Style();
rootStyle.display = Display.BLOCK;
rootStyle.size = new Size<>(Dimension.length(300f), Dimension.length(200f));
NodeId root = tree.newLeaf(rootStyle);

Style abs = new Style();
abs.position = Position.ABSOLUTE;
abs.inset = new Rect<>(
  LengthPercentageAuto.length(10f),
  LengthPercentageAuto.AUTO,
  LengthPercentageAuto.length(20f),
  LengthPercentageAuto.AUTO
);
abs.size = new Size<>(Dimension.length(50f), Dimension.length(40f));
NodeId child = tree.newLeaf(abs);

tree.setChildren(root, child);
tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(300f),
  AvailableSpace.definite(200f)
));

Layout lc = tree.getLayout(child);
```

### Example 3: measured leaf (`MeasureFunc`) for text/images

```java
MeasureFunc measureText = (known, space) -> {
  float w = (!Float.isNaN(known.width)) ? known.width : 100f;
  float h = (!Float.isNaN(known.height)) ? known.height : 20f;
  return new FloatSize(w, h);
};

TaffyTree tree = new TaffyTree();
Style textStyle = new Style();
NodeId text = tree.newLeafWithMeasure(textStyle, measureText);

Style rootStyle = new Style();
rootStyle.display = Display.BLOCK;
NodeId root = tree.newWithChildren(rootStyle, text);

tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(800f),
  AvailableSpace.definite(600f)
));
Layout lt = tree.getLayout(text);
```
