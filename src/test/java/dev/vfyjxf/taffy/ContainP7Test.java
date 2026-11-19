package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.Contain;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7/ST-07 tests for the layout-affecting parts of the CSS {@code contain} property.
 *
 * <p>Scope: the {@link Contain} value type, the {@code TaffyStyle.contain} field, and the block/leaf
 * integration of the LAYOUT and PAINT containment types, strictly following upstream taffy #1128
 * ({@code 4ae4219}). Both types make the box establish an independent formatting context (margin
 * collapse isolation, float containment/avoidance); LAYOUT additionally suppresses the box's
 * baseline. Size and inline-size containment are not implemented (upstream ships without them
 * either), so only their constant/flag contract and the absence of any layout effect are pinned.</p>
 *
 * <p>Expected numbers for the layout/paint scenarios mirror the upstream generated fixtures
 * {@code contain_layout_block_margin_collapse}, {@code contain_layout_block_contains_floats},
 * {@code contain_layout_block_avoids_float}, {@code contain_layout_flex_baseline} and their
 * {@code contain_paint_*} counterparts.</p>
 */
public class ContainP7Test {

    private static final float EPS = 0.001f;

    // ------------------------------------------------------------------
    // Style helpers
    // ------------------------------------------------------------------

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    /** A content-box block, matching the upstream fixtures' {@code box-sizing: content-box}. */
    private static TaffyStyle contentBox(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.boxSizing = BoxSizing.CONTENT_BOX;
        return style;
    }

    private static TaffyStyle contained(TaffyStyle style, Contain contain) {
        style.contain = contain;
        return style;
    }

    private static TaffyStyle floated(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.float_ = TaffyFloat.LEFT;
        return style;
    }

    private static TaffyStyle flexRowBaseline(float width) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.alignItems = AlignItems.BASELINE;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.AUTO);
        return style;
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " width");
        assertEquals(height, layout.size().height, EPS, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    /** Runs a full layout on {@code node} and returns its output (the container baseline lives here). */
    private static LayoutOutput layoutOutput(TaffyTree tree, NodeId node) {
        LayoutComputer computer = new LayoutComputer(tree, (MeasureFunc) null);
        return computer.performChildLayout(
            node,
            FloatSize.none(),
            FloatSize.none(),
            TaffySize.maxContent(),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );
    }

    /** A leaf whose measure function reports a size and a first baseline. */
    private static NodeId measuredLeaf(TaffyTree tree, float width, float height, float baseline) {
        TaffyStyle style = block();
        return tree.newLeafWithMeasureFunction(style, context -> LayoutOutput.fromSizesAndBaselines(
            new FloatSize(width, height),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, baseline)
        ));
    }

    /** Asserts that two structurally identical trees produced identical node geometry. */
    private static void assertTreesMatch(TaffyTree expected, NodeId expectedRoot,
                                         TaffyTree actual, NodeId actualRoot, String path) {
        Layout expectedLayout = expected.getLayout(expectedRoot);
        Layout actualLayout = actual.getLayout(actualRoot);
        assertEquals(expectedLayout.size().width, actualLayout.size().width, EPS, path + " width");
        assertEquals(expectedLayout.size().height, actualLayout.size().height, EPS, path + " height");
        assertEquals(expectedLayout.location().x, actualLayout.location().x, EPS, path + " x");
        assertEquals(expectedLayout.location().y, actualLayout.location().y, EPS, path + " y");

        List<NodeId> expectedChildren = expected.getChildren(expectedRoot);
        List<NodeId> actualChildren = actual.getChildren(actualRoot);
        assertEquals(expectedChildren.size(), actualChildren.size(), path + " child count");
        for (int index = 0; index < expectedChildren.size(); index++) {
            assertTreesMatch(expected, expectedChildren.get(index), actual, actualChildren.get(index),
                path + "/" + index);
        }
    }

    /**
     * The upstream contain_layout_block_margin_collapse fixture tree: a bordered root with a plain
     * block, a box carrying {@code contain} and another plain block. The contained box's child
     * carries 20px vertical margins, so margin-collapse isolation is observable in the box height
     * and in the siblings' positions.
     */
    private static NodeId newMarginCollapseFixture(TaffyTree tree, Contain contain) {
        NodeId grandChild = tree.newLeaf(withVerticalMargins(sized(Float.NaN, 10f), 20f, 20f));
        NodeId middle = tree.newWithChildren(contained(block(), contain), grandChild);
        NodeId first = tree.newLeaf(withBottomMargin(sized(Float.NaN, 10f), 10f));
        NodeId last = tree.newLeaf(withTopMargin(sized(Float.NaN, 10f), 10f));
        TaffyStyle rootStyle = contentBox(100f, Float.NaN);
        rootStyle.border = TaffyRect.all(LengthPercentage.length(2f));
        return tree.newWithChildren(rootStyle, first, middle, last);
    }

    private static TaffyStyle withVerticalMargins(TaffyStyle style, float top, float bottom) {
        style.margin = TaffyRect.ltrb(
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(top),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(bottom)
        );
        return style;
    }

    private static TaffyStyle withBottomMargin(TaffyStyle style, float bottom) {
        style.margin = TaffyRect.ltrb(
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(bottom)
        ); // left, top, right, bottom
        return style;
    }

    private static TaffyStyle withTopMargin(TaffyStyle style, float top) {
        style.margin = TaffyRect.ltrb(
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(top),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO
        );
        return style;
    }

    // ==================================================================
    // Contain value type
    // ==================================================================

    @Test
    @DisplayName("ST-07: Contain constants expose the upstream flag layout")
    void containConstantsExposeUpstreamFlagLayout() {
        assertEquals(0, Contain.NONE.bits(), "NONE bits");
        assertEquals(1, Contain.SIZE.bits(), "SIZE bits");
        assertEquals(2, Contain.INLINE_SIZE.bits(), "INLINE_SIZE bits");
        assertEquals(4, Contain.LAYOUT.bits(), "LAYOUT bits");
        assertEquals(8, Contain.PAINT.bits(), "PAINT bits");
        assertEquals(13, Contain.STRICT.bits(), "STRICT bits (size | layout | paint)");
        assertEquals(12, Contain.CONTENT.bits(), "CONTENT bits (layout | paint)");

        assertSame(Contain.NONE, Contain.DEFAULT, "DEFAULT is NONE");
        assertTrue(Contain.NONE.isEmpty(), "NONE is empty");
        assertFalse(Contain.LAYOUT.isEmpty(), "LAYOUT is not empty");

        assertTrue(Contain.STRICT.contains(Contain.SIZE), "STRICT contains SIZE");
        assertTrue(Contain.STRICT.contains(Contain.LAYOUT), "STRICT contains LAYOUT");
        assertTrue(Contain.STRICT.contains(Contain.PAINT), "STRICT contains PAINT");
        assertTrue(Contain.CONTENT.contains(Contain.LAYOUT), "CONTENT contains LAYOUT");
        assertTrue(Contain.CONTENT.contains(Contain.PAINT), "CONTENT contains PAINT");
        assertFalse(Contain.CONTENT.contains(Contain.SIZE), "CONTENT does not contain SIZE");
        assertFalse(Contain.SIZE.intersects(Contain.INLINE_SIZE), "SIZE and INLINE_SIZE are distinct");
        assertTrue(Contain.STRICT.intersects(Contain.PAINT), "STRICT intersects PAINT");
        assertFalse(Contain.NONE.intersects(Contain.PAINT), "NONE intersects nothing");

        assertEquals("Contain{NONE}", Contain.NONE.toString(), "NONE toString");
        assertEquals("Contain{LAYOUT}", Contain.LAYOUT.toString(), "LAYOUT toString");
        assertEquals("Contain{LAYOUT PAINT}", Contain.CONTENT.toString(), "CONTENT toString");
        assertEquals("Contain{SIZE LAYOUT PAINT}", Contain.STRICT.toString(), "STRICT toString");
    }

    @Test
    @DisplayName("ST-07: only layout/paint containment affect layout")
    void onlyLayoutAndPaintContainmentAffectLayout() {
        // Both layout and paint containment establish an independent formatting context.
        assertFalse(Contain.NONE.establishesIndependentFormattingContext(), "NONE");
        assertFalse(Contain.SIZE.establishesIndependentFormattingContext(), "SIZE unsupported");
        assertFalse(Contain.INLINE_SIZE.establishesIndependentFormattingContext(), "INLINE_SIZE unsupported");
        assertTrue(Contain.LAYOUT.establishesIndependentFormattingContext(), "LAYOUT");
        assertTrue(Contain.PAINT.establishesIndependentFormattingContext(), "PAINT");
        assertTrue(Contain.CONTENT.establishesIndependentFormattingContext(), "CONTENT");
        assertTrue(Contain.STRICT.establishesIndependentFormattingContext(), "STRICT");

        // Only layout containment suppresses the baseline.
        assertFalse(Contain.NONE.suppressesBaseline(), "NONE");
        assertFalse(Contain.SIZE.suppressesBaseline(), "SIZE has no layout effect");
        assertFalse(Contain.INLINE_SIZE.suppressesBaseline(), "INLINE_SIZE has no layout effect");
        assertTrue(Contain.LAYOUT.suppressesBaseline(), "LAYOUT");
        assertFalse(Contain.PAINT.suppressesBaseline(), "PAINT keeps its baseline");
        assertTrue(Contain.CONTENT.suppressesBaseline(), "CONTENT includes LAYOUT");
        assertTrue(Contain.STRICT.suppressesBaseline(), "STRICT includes LAYOUT");

        // Layout/paint containment also stop overflow propagation upstream; the predicate exists
        // for API parity even though this port does not wire the scrollable-overflow gate yet.
        assertFalse(Contain.NONE.containsScrollableOverflow(), "NONE");
        assertFalse(Contain.SIZE.containsScrollableOverflow(), "SIZE unsupported");
        assertTrue(Contain.LAYOUT.containsScrollableOverflow(), "LAYOUT");
        assertTrue(Contain.PAINT.containsScrollableOverflow(), "PAINT");
    }

    @Test
    @DisplayName("ST-07: Contain union is immutable and structural equality is by flags")
    void containUnionIsImmutable() {
        assertSame(Contain.LAYOUT, Contain.LAYOUT.union(Contain.NONE), "union with NONE is a no-op");
        assertSame(Contain.LAYOUT, Contain.NONE.union(Contain.LAYOUT), "NONE union X is X");
        assertEquals(Contain.CONTENT, Contain.LAYOUT.union(Contain.PAINT), "LAYOUT | PAINT is CONTENT");
        assertEquals(Contain.STRICT, Contain.SIZE.union(Contain.CONTENT), "SIZE | CONTENT is STRICT");
        assertEquals(Contain.CONTENT, Contain.LAYOUT.union(Contain.PAINT), "union is stable");
        assertEquals(Contain.CONTENT.hashCode(), Contain.LAYOUT.union(Contain.PAINT).hashCode(), "hashCode");
        assertNotEquals(Contain.LAYOUT, Contain.PAINT, "different flags are not equal");
        assertNotEquals(Contain.SIZE, Contain.NONE, "SIZE differs from NONE");
        // LAYOUT is untouched by union (immutability).
        Contain combined = Contain.LAYOUT.union(Contain.PAINT);
        assertFalse(Contain.LAYOUT.contains(Contain.PAINT), "operand is not mutated");
        assertTrue(combined.contains(Contain.PAINT), "result carries the added flag");
    }

    @Test
    @DisplayName("ST-07: TaffyStyle defaults to no containment and copy() copies the flags")
    void styleDefaultsToNoneAndCopyCopiesContain() {
        TaffyStyle style = new TaffyStyle();
        assertSame(Contain.NONE, style.getContain(), "default field");
        assertTrue(style.getContain().isEmpty(), "default is NONE");

        style.contain = Contain.CONTENT;
        TaffyStyle copy = style.copy();
        assertEquals(Contain.CONTENT, copy.getContain(), "copy carries the flags");

        // The value type is immutable, but changing the source style must not change the copy.
        style.contain = Contain.NONE;
        assertEquals(Contain.CONTENT, copy.getContain(), "copy is independent of later style changes");

        // A null field (only possible when assigned explicitly) reads back as NONE.
        style.contain = null;
        assertSame(Contain.NONE, style.getContain(), "null reads as NONE");
    }

    // ==================================================================
    // SIZE / INLINE_SIZE: constants exist, no layout effect
    // ==================================================================

    @Test
    @DisplayName("ST-07: contain: size and contain: inline-size have no layout effect")
    void sizeAndInlineSizeContainmentHaveNoLayoutEffect() {
        TaffyTree reference = new TaffyTree();
        NodeId referenceRoot = newMarginCollapseFixture(reference, Contain.NONE);
        reference.computeLayout(referenceRoot, TaffySize.maxContent());

        for (Contain unsupported : new Contain[]{
            Contain.SIZE, Contain.INLINE_SIZE, Contain.SIZE.union(Contain.INLINE_SIZE)
        }) {
            TaffyTree tree = new TaffyTree();
            NodeId root = newMarginCollapseFixture(tree, unsupported);
            tree.computeLayout(root, TaffySize.maxContent());
            assertTreesMatch(reference, referenceRoot, tree, root, unsupported.toString());
        }
    }

    // ==================================================================
    // Block integration: layout/paint containment
    // ==================================================================

    @Test
    @DisplayName("ST-07: contain layout/paint isolates a box's margins from its parent and siblings")
    void layoutAndPaintContainmentIsolatesMargins() {
        for (Contain contain : new Contain[]{Contain.LAYOUT, Contain.PAINT}) {
            TaffyTree tree = new TaffyTree();
            NodeId root = newMarginCollapseFixture(tree, contain);
            tree.computeLayout(root, TaffySize.maxContent());

            // Mirrors contain_layout_block_margin_collapse / contain_paint_block_margin_collapse:
            // the contained box's child margins stay inside it (height 50 instead of 10).
            List<NodeId> children = tree.getChildren(root);
            NodeId first = children.get(0);
            NodeId middle = children.get(1);
            NodeId last = children.get(2);
            NodeId grandChild = tree.getChildren(middle).get(0);

            assertSize(tree.getLayout(root), 104f, 94f, contain + " root");
            assertLocation(tree.getLayout(root), 0f, 0f, contain + " root");

            assertSize(tree.getLayout(first), 100f, 10f, contain + " first");
            assertLocation(tree.getLayout(first), 2f, 2f, contain + " first");

            assertSize(tree.getLayout(middle), 100f, 50f, contain + " contained");
            assertLocation(tree.getLayout(middle), 2f, 22f, contain + " contained");

            assertSize(tree.getLayout(grandChild), 100f, 10f, contain + " grandchild");
            assertLocation(tree.getLayout(grandChild), 0f, 20f, contain + " grandchild");

            assertSize(tree.getLayout(last), 100f, 10f, contain + " last");
            assertLocation(tree.getLayout(last), 2f, 82f, contain + " last");
        }
    }

    @Test
    @DisplayName("ST-07: contain layout/paint contains the box's own floats")
    void layoutAndPaintContainmentContainsOwnFloats() {
        for (Contain contain : new Contain[]{Contain.LAYOUT, Contain.PAINT}) {
            TaffyTree tree = new TaffyTree();

            NodeId floatNode = tree.newLeaf(floated(30f, 30f));
            NodeId containedBox = tree.newWithChildren(contained(block(), contain), floatNode);
            NodeId sibling = tree.newLeaf(sized(Float.NaN, 10f));
            NodeId root = tree.newWithChildren(sized(100f, Float.NaN), containedBox, sibling);

            tree.computeLayout(root, TaffySize.maxContent());

            // Mirrors contain_layout_block_contains_floats / contain_paint_block_contains_floats.
            assertSize(tree.getLayout(containedBox), 100f, 30f, contain + " contained box");
            assertLocation(tree.getLayout(floatNode), 0f, 0f, contain + " float");
            assertSize(tree.getLayout(floatNode), 30f, 30f, contain + " float");
            assertLocation(tree.getLayout(sibling), 0f, 30f, contain + " sibling");
            assertSize(tree.getLayout(sibling), 100f, 10f, contain + " sibling");
            assertSize(tree.getLayout(root), 100f, 40f, contain + " root");
        }
    }

    @Test
    @DisplayName("ST-07: a contained box avoids an external float instead of flowing into it")
    void containedBoxAvoidsExternalFloat() {
        for (Contain contain : new Contain[]{Contain.LAYOUT, Contain.PAINT}) {
            TaffyTree tree = new TaffyTree();

            NodeId floatNode = tree.newLeaf(floated(30f, 30f));
            NodeId grandChild = tree.newLeaf(sized(Float.NaN, 10f));
            NodeId containedBox = tree.newWithChildren(
                contained(sized(Float.NaN, 50f), contain), grandChild);
            NodeId root = tree.newWithChildren(sized(100f, Float.NaN), floatNode, containedBox);

            tree.computeLayout(root, TaffySize.maxContent());

            // Mirrors contain_layout_block_avoids_float: the contained box starts beside the float
            // (x=30, width 70) and its own subtree works in that narrower content box.
            assertLocation(tree.getLayout(floatNode), 0f, 0f, contain + " float");
            assertSize(tree.getLayout(floatNode), 30f, 30f, contain + " float");

            assertLocation(tree.getLayout(containedBox), 30f, 0f, contain + " contained box");
            assertSize(tree.getLayout(containedBox), 70f, 50f, contain + " contained box");
            assertLocation(tree.getLayout(grandChild), 0f, 0f, contain + " grandchild");
            assertSize(tree.getLayout(grandChild), 70f, 10f, contain + " grandchild");
            assertSize(tree.getLayout(root), 100f, 50f, contain + " root");
        }
    }

    @Test
    @DisplayName("ST-07: layout containment suppresses the block's first baseline, paint does not")
    void layoutContainmentSuppressesBaseline() {
        // Control: the container propagates its first in-flow child's baseline (12).
        TaffyTree control = new TaffyTree();
        NodeId controlChild = measuredLeaf(control, 50f, 20f, 12f);
        NodeId controlBox = control.newWithChildren(block(), controlChild);
        LayoutOutput controlOutput = layoutOutput(control, controlBox);
        assertEquals(12f, controlOutput.baselines().first().y, EPS, "control baseline");
        assertEquals(12f, controlOutput.firstBaselines().y, EPS, "control legacy baseline");

        // PAINT establishes an independent formatting context but keeps the baseline.
        TaffyTree paint = new TaffyTree();
        NodeId paintChild = measuredLeaf(paint, 50f, 20f, 12f);
        NodeId paintBox = paint.newWithChildren(contained(block(), Contain.PAINT), paintChild);
        LayoutOutput paintOutput = layoutOutput(paint, paintBox);
        assertEquals(12f, paintOutput.baselines().first().y, EPS, "paint baseline");
        assertEquals(12f, paintOutput.firstBaselines().y, EPS, "paint legacy baseline");

        // LAYOUT (and CONTENT/STRICT, which include it) suppresses the baseline entirely.
        for (Contain contain : new Contain[]{Contain.LAYOUT, Contain.CONTENT, Contain.STRICT}) {
            TaffyTree tree = new TaffyTree();
            NodeId child = measuredLeaf(tree, 50f, 20f, 12f);
            NodeId box = tree.newWithChildren(contained(block(), contain), child);
            LayoutOutput output = layoutOutput(tree, box);
            assertTrue(Float.isNaN(output.baselines().first().y), contain + " baselines.first.y");
            assertTrue(Float.isNaN(output.firstBaselines().y), contain + " legacy firstBaselines.y");
            assertEquals(20f, output.size().height, EPS, contain + " height is unaffected");
        }

        // The suppression propagates: a block ancestor of a contained box has no baseline either.
        TaffyTree ancestor = new TaffyTree();
        NodeId innerChild = measuredLeaf(ancestor, 50f, 20f, 12f);
        NodeId innerBox = ancestor.newWithChildren(contained(block(), Contain.LAYOUT), innerChild);
        NodeId outerBox = ancestor.newWithChildren(block(), innerBox);
        LayoutOutput outerOutput = layoutOutput(ancestor, outerBox);
        assertTrue(Float.isNaN(outerOutput.baselines().first().y), "outer baselines.first.y");
    }

    @Test
    @DisplayName("ST-07: layout containment matches the upstream flex baseline fixture")
    void layoutContainmentMatchesUpstreamFlexBaselineFixture() {
        // Mirrors contain_layout_flex_baseline: the suppressed baseline of the second item falls
        // back to its border-box height (20), so it is shifted down by 10 relative to the first
        // item's 30px baseline.
        TaffyTree tree = new TaffyTree();

        NodeId firstChild = measuredLeaf(tree, 50f, 30f, 30f);
        NodeId firstItem = tree.newWithChildren(sized(50f, 60f), firstChild);
        NodeId secondChild = tree.newLeaf(sized(50f, 10f));
        NodeId secondItem = tree.newWithChildren(contained(sized(50f, 20f), Contain.LAYOUT), secondChild);
        NodeId root = tree.newWithChildren(flexRowBaseline(200f), firstItem, secondItem);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(firstItem), 0f, 0f, "first item");
        assertSize(tree.getLayout(firstItem), 50f, 60f, "first item");
        assertLocation(tree.getLayout(secondItem), 50f, 10f, "contained item");
        assertSize(tree.getLayout(secondItem), 50f, 20f, "contained item");
        assertSize(tree.getLayout(root), 200f, 60f, "root");
    }

    // ==================================================================
    // Default regression
    // ==================================================================

    @Test
    @DisplayName("ST-07: the default (no containment) still collapses margins and lets floats escape")
    void defaultContainmentKeepsLegacyLayout() {
        TaffyTree tree = new TaffyTree();
        NodeId root = newMarginCollapseFixture(tree, Contain.NONE);
        tree.computeLayout(root, TaffySize.maxContent());

        List<NodeId> children = tree.getChildren(root);
        NodeId middle = children.get(1);
        NodeId grandChild = tree.getChildren(middle).get(0);

        // Without containment the middle box's first child top margin collapses through its top
        // edge: the box itself is only 10 tall and its child starts at y = 0.
        assertSize(tree.getLayout(middle), 100f, 10f, "default contained box height");
        assertLocation(tree.getLayout(grandChild), 0f, 0f, "default grandchild");
    }
}
