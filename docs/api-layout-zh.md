# 关于 Taffy-Java

Taffy-Java 是一个可嵌入的布局引擎。它不是 UI 框架，也不做任何绘制。

它只做一件事：**根据样式（`Style`）计算盒子的尺寸与位置（`Layout`）**，把结果交给渲染系统。

## 布局一棵树

每个盒子对应一个节点（`NodeId`），节点组成一棵树：树上保存布局输入（style/measure/结构），布局输出写回到每个节点的 `Layout`。

### 构建节点树

- 叶子节点：`newLeaf(style)`
- 带测量的叶子节点（文本/图片等）：`newLeafWithMeasure(style, measureFunc)` 或 `setMeasureFunc(node, func)`
- 容器节点：`newWithChildren(style, ...)`，或先建节点再通过 `addChild / setChildren` 组装

> **WARNING**
> 
> `getChildren(parent)` 返回树内部维护的列表。调用方必须视为只读；结构变更请使用 `addChild / setChildren / removeChild`。

### 执行布局

布局计算在整棵树上一次性完成：

- 使用 `computeLayout(root, availableSpace)`
- `availableSpace` 表示根节点可用空间（通常来自视口/窗口大小）

常用写法：把 root 当作视口/窗口，在 definite 约束下计算布局。

```java
tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(viewportWidth),
  AvailableSpace.definite(viewportHeight)
));
```

若某个轴允许“无限扩展”，可传入 `AvailableSpace.maxContent()`（语义上类似不设上限）。

### 读取布局结果

布局结果写回每个节点：

- 位置：`Layout.location()`（相对父节点的偏移）
- 尺寸：`Layout.size()`
- 以及解析后的 margin/border/padding 等

#### 增量消费：`hasUnconsumedLayout` / `acknowledgeLayout`

在一些渲染集成中，需要区分“本轮布局是否真的改变了该节点的结果”，以避免重复提交渲染指令。

- `hasUnconsumedLayout(node)`：当节点布局结果自上次确认后发生变化时返回 `true`
- `acknowledgeLayout(node)`：在调用方消费完该节点布局后进行确认

典型用法：仅对发生变化的节点更新渲染数据。

```java
tree.computeLayout(root, new Size<>(
  AvailableSpace.definite(viewportWidth),
  AvailableSpace.definite(viewportHeight)
));

for (NodeId n : tree.getAllNodes()) {
  if (!tree.hasUnconsumedLayout(n)) continue;
  Layout l = tree.getLayout(n);
  // ... 将 l 写入渲染系统（例如更新变换、大小、裁剪等）
  tree.acknowledgeLayout(n);
}
```

> **NOTE**
>
> 布局变化的判定与 rounding 设置有关：rounding 开启时以最终（四舍五入后的）布局结果为准；rounding 关闭时以未四舍五入结果为准。

> **NOTE**
> 
> `Layout.location()` 通常可理解为“子节点 border-box 相对父节点 border-box”。渲染系统需要绝对坐标时，一般沿父链累加。

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

## 样式与约定

### 默认样式（重要）

`Style` 的默认值会直接影响布局结果。特别注意：

- `display` 默认是 `FLEX`（`Display.DEFAULT`）
- `boxSizing` 默认是 `BORDER_BOX`
- `position` 默认是 `RELATIVE`，`inset` 默认是 `AUTO`

当需要块布局或其他行为时，请显式设置：

```java
Style rootStyle = new Style();
rootStyle.display = Display.BLOCK;
```

### 百分比表示

`Dimension.percent(x)` / `LengthPercentage.percent(x)` 使用 0.0 ~ 1.0。

```java
// 50%
Dimension.percent(0.5f);
```

### 变更提交（不要让树猜）

`Style` 字段为 public，允许直接修改，但树不会自动识别字段级变更。

推荐：修改后用 `setStyle(node, style)` 提交变更（它会触发脏标记）。

```java
Style s = tree.getStyle(node);
s.flexGrow = 1.0f;
tree.setStyle(node, s);
```

必要时可调用 `markDirty(node)` 显式标记。

### rounding（像素栅格对齐）

`TaffyTree` 默认开启 rounding。需要保留小数坐标时可关闭。

```java
TaffyTree tree = new TaffyTree();
// tree.disableRounding();
```

### 内容驱动尺寸与 `MeasureFunc`

Taffy-Java 不负责文本排版与测量。对于文本/图片等“尺寸由内容决定”的叶子节点，`MeasureFunc` 只回答一个问题：**在约束下应该是多少宽/高**。

> **WARNING**
> 
> `MeasureFunc` 不应尝试做布局（对齐/换行/分配空间）。它只返回尺寸；位置与分配由引擎负责。

## 示例

### 示例 1：Flex 行布局（以视口约束计算）

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

### 示例 2：绝对定位（`position` + `inset`）

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

### 示例 3：带测量的叶子节点（文本/图片）

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
