package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.RoundLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContentSizeAndRoundingCompatTest {

    private static final float EPSILON = 0.01f;

    @Test
    @DisplayName("rounding_rounds_content_size_with_cumulative_offset")
    void roundingRoundsContentSizeWithCumulativeOffset() {
        // 这个测试专门验证“rounding pass”（RoundLayout）本身的累积坐标取整逻辑，
        // 直接构造 unrounded layout，避免被 flex/block/grid 计算路径的其它细节影响。

        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.display = TaffyDisplay.BLOCK;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        NodeId root = tree.newWithChildren(rootStyle, child);

        // Root 的 unrounded layout（位置 0,0），用于提供累积坐标基准
        tree.setUnroundedLayout(
            root,
            new Layout(
                0,
                new FloatPoint(0f, 0f),
                new FloatSize(0f, 0f),
                new FloatSize(0f, 0f),
                new FloatSize(0f, 0f),
                FloatRect.zero(),
                FloatRect.zero(),
                FloatRect.zero()
            )
        );

        // Child 的 unrounded layout：location 有小数偏移，size/contentSize 有小数
        tree.setUnroundedLayout(
            child,
            new Layout(
                0,
                new FloatPoint(0.2f, 0.2f),
                new FloatSize(10.4f, 5.6f),
                new FloatSize(10.4f, 5.6f),
                new FloatSize(0f, 0f),
                FloatRect.zero(),
                FloatRect.zero(),
                FloatRect.zero()
            )
        );

        RoundLayout.roundLayout(tree, root);

        Layout childLayout = tree.getLayout(child);

        // With cumulative offset 0.2, rounding uses: round(0.2 + 10.4) - round(0.2) = 11 - 0 = 11
        assertEquals(11.0f, childLayout.size().width, EPSILON);
        assertEquals(6.0f, childLayout.size().height, EPSILON);

        // Rust also rounds content_size using the same cumulative coordinate logic
        assertEquals(11.0f, childLayout.contentSize().width, EPSILON);
        assertEquals(6.0f, childLayout.contentSize().height, EPSILON);
    }

    @Test
    @DisplayName("rounding_rounds_scrollbar_size")
    void roundingRoundsScrollbarSize() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
        rootStyle.scrollbarWidth = 3.6f;

        NodeId root = tree.newLeaf(rootStyle);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertEquals(4.0f, rootLayout.scrollbarSize().width, EPSILON);
        assertEquals(4.0f, rootLayout.scrollbarSize().height, EPSILON);
    }

    @Test
    @DisplayName("content_size_aggregates_child_overflow_visible_content")
    void contentSizeAggregatesChildOverflowVisibleContent() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        MeasureFunc intrinsic50x5 = (known, available) -> new FloatSize(50.0f, 5.0f);

        TaffyStyle grandChildStyle = new TaffyStyle();
        grandChildStyle.display = TaffyDisplay.FLEX;
        NodeId grandChild = tree.newLeafWithMeasure(grandChildStyle, intrinsic50x5);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.display = TaffyDisplay.FLEX;
        childStyle.flexDirection = FlexDirection.COLUMN;
        childStyle.alignItems = AlignItems.FLEX_START; // prevent cross-axis stretch
        childStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(10.0f));
        NodeId child = tree.newWithChildren(childStyle, grandChild);

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.alignItems = AlignItems.FLEX_START;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(20.0f), TaffyDimension.length(20.0f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        Layout rootLayout = tree.getLayout(root);

        // Child's own contentSize should reflect its descendant overflow (grandChild is wider than the child)
        assertEquals(50.0f, childLayout.contentSize().width, EPSILON);
        assertEquals(5.0f, childLayout.contentSize().height, EPSILON);

        // Parent should use max(child.size, child.contentSize) when child's overflow is VISIBLE
        assertEquals(50.0f, rootLayout.contentSize().width, EPSILON);
        assertEquals(10.0f, rootLayout.contentSize().height, EPSILON);
    }
}
