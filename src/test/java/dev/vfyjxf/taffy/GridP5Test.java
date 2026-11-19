package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P5 grid batch: wiring the alignment keyword/safety model of
 * {@code AlignmentHelper} into the grid algorithm.
 *
 * <p>Covers the upstream changes listed in {@code alignment-changes.md} §5.6:
 * ST-02 ({@code safe} overflow-position keywords, #952), ST-03 ({@code self-start}/{@code self-end}
 * resolved against the item's own direction, #1077) and ST-04 (first-class {@code normal},
 * #1254/#1255). The tests assert grid-specific semantics:</p>
 *
 * <ul>
 *   <li>content alignment ({@code align-content}/{@code justify-content}) resolves {@code normal} to
 *       {@code stretch}, honours the single-subject fallbacks and the {@code safe} start fallback, and
 *       keeps the empty-axis (single grid line) offset;</li>
 *   <li>item alignment ({@code align-items}/{@code justify-items}/{@code align-self}/
 *       {@code justify-self}) resolves {@code normal} contextually (definite size → start, otherwise
 *       stretch), {@code self-start}/{@code self-end} against the item's own direction (the inline axis
 *       flips when it differs from the container's, the block axis never flips), and {@code safe} to
 *       logical start on overflow;</li>
 *   <li>legacy keywords keep exactly their pre-P5 geometry.</li>
 * </ul>
 */
class GridP5Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // 1. Content alignment (align-content / justify-content)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("content alignment: safe overflow-position keywords")
    class ContentSafeAlignment {

        @Test
        @DisplayName("align-content: safe end packs tracks at the end when they fit")
        void alignContentSafeEndNoOverflow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(20f), fixed(20f));
            containerStyle.gridTemplateColumns = List.of(fixed(100f));
            containerStyle.alignContent = AlignContent.SAFE_END;

            NodeId first = tree.newLeaf(sized(10f, 20f));
            NodeId second = tree.newLeaf(sized(10f, 20f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // 40px of tracks in a 100px container: 60px of free space packs the block start at the end.
            assertLocation(tree.getLayout(first), 0f, 60f, "first row");
            assertLocation(tree.getLayout(second), 0f, 80f, "second row");
        }

        @Test
        @DisplayName("align-content: safe end falls back to start when the tracks overflow")
        void alignContentSafeEndOverflow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(80f), fixed(80f));
            containerStyle.gridTemplateColumns = List.of(fixed(100f));
            containerStyle.alignContent = AlignContent.SAFE_END;

            NodeId first = tree.newLeaf(sized(10f, 80f));
            NodeId second = tree.newLeaf(sized(10f, 80f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 0f, 0f, "first row anchored at start");
            assertLocation(tree.getLayout(second), 0f, 80f, "second row");
        }

        @Test
        @DisplayName("align-content: end (unsafe) keeps the overflowing position [legacy regression]")
        void alignContentEndOverflowUnchanged() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(80f), fixed(80f));
            containerStyle.gridTemplateColumns = List.of(fixed(100f));
            containerStyle.alignContent = AlignContent.END;

            NodeId first = tree.newLeaf(sized(10f, 80f));
            NodeId second = tree.newLeaf(sized(10f, 80f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // 160px of tracks in 100px: end alignment anchors the start at -60 (data loss preserved).
            assertLocation(tree.getLayout(first), 0f, -60f, "first row");
            assertLocation(tree.getLayout(second), 0f, 20f, "second row");
        }

        @Test
        @DisplayName("justify-content: safe end falls back to the inline start on overflow")
        void justifyContentSafeEndOverflow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(100f));
            containerStyle.gridTemplateColumns = List.of(fixed(80f), fixed(80f));
            containerStyle.justifyContent = AlignContent.SAFE_END;

            NodeId first = tree.newLeaf(sized(80f, 10f));
            NodeId second = tree.newLeaf(sized(80f, 10f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 0f, 0f, "first column anchored at start");
            assertLocation(tree.getLayout(second), 80f, 0f, "second column");
        }

        @Test
        @DisplayName("justify-content: end (unsafe) keeps the overflowing position [legacy regression]")
        void justifyContentEndOverflowUnchanged() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(100f));
            containerStyle.gridTemplateColumns = List.of(fixed(80f), fixed(80f));
            containerStyle.justifyContent = AlignContent.END;

            NodeId first = tree.newLeaf(sized(80f, 10f));
            NodeId second = tree.newLeaf(sized(80f, 10f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), -60f, 0f, "first column");
            assertLocation(tree.getLayout(second), 20f, 0f, "second column");
        }

        @Test
        @DisplayName("justify-content: normal behaves as stretch for auto tracks")
        void justifyContentNormalStretchesAutoTracks() {
            TaffyTree normalTree = new TaffyTree();
            TaffyStyle normalStyle = container(100f, 100f);
            normalStyle.gridTemplateRows = List.of(fixed(100f));
            normalStyle.gridTemplateColumns = List.of(auto(), auto());
            normalStyle.justifyContent = AlignContent.NORMAL;

            NodeId normalFirst = normalTree.newLeaf(sized(20f, 10f));
            NodeId normalSecond = normalTree.newLeaf(sized(20f, 10f));
            NodeId normalRoot = normalTree.newWithChildren(normalStyle, normalFirst, normalSecond);
            normalTree.computeLayout(normalRoot, TaffySize.maxContent());

            // The two 20px auto columns split the 100px content box: 50px each.
            assertLocation(normalTree.getLayout(normalFirst), 0f, 0f, "normal first column");
            assertLocation(normalTree.getLayout(normalSecond), 50f, 0f, "normal second column");

            TaffyTree startTree = new TaffyTree();
            TaffyStyle startStyle = container(100f, 100f);
            startStyle.gridTemplateRows = List.of(fixed(100f));
            startStyle.gridTemplateColumns = List.of(auto(), auto());
            startStyle.justifyContent = AlignContent.START;

            NodeId startFirst = startTree.newLeaf(sized(20f, 10f));
            NodeId startSecond = startTree.newLeaf(sized(20f, 10f));
            NodeId startRoot = startTree.newWithChildren(startStyle, startFirst, startSecond);
            startTree.computeLayout(startRoot, TaffySize.maxContent());

            // Explicit start does not stretch: tracks stay at their 20px content size.
            assertLocation(startTree.getLayout(startFirst), 0f, 0f, "start first column");
            assertLocation(startTree.getLayout(startSecond), 20f, 0f, "start second column");
        }

        @Test
        @DisplayName("align-content: normal behaves as stretch (zero offset for fixed tracks)")
        void alignContentNormalBehavesAsStretch() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(20f), fixed(20f));
            containerStyle.gridTemplateColumns = List.of(fixed(100f));
            containerStyle.alignContent = AlignContent.NORMAL;

            NodeId first = tree.newLeaf(sized(10f, 20f));
            NodeId second = tree.newLeaf(sized(10f, 20f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 0f, 0f, "normal does not pack at the end");
            assertLocation(tree.getLayout(second), 0f, 20f, "second row");
        }

        @Test
        @DisplayName("empty axis: normal/safe/distribution offsets for the single grid line")
        void emptyAxisContentAlignment() {
            // No in-flow items: every auto-fit column collapses, leaving an axis with a single grid
            // line. Absolutely positioned children expose the content-alignment offset of that line.
            assertEquals(0f, emptyAxisAbsoluteX(AlignContent.START), "start offset");
            assertEquals(100f, emptyAxisAbsoluteX(AlignContent.END), "end offset");
            assertEquals(100f, emptyAxisAbsoluteX(AlignContent.SAFE_END), "safe end offset (fits)");
            assertEquals(50f, emptyAxisAbsoluteX(AlignContent.CENTER), "center offset");
            assertEquals(50f, emptyAxisAbsoluteX(AlignContent.SPACE_AROUND), "space-around falls back to center");
            assertEquals(0f, emptyAxisAbsoluteX(AlignContent.NORMAL), "normal behaves as stretch");
        }

        private float emptyAxisAbsoluteX(AlignContent justifyContent) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(100f));
            containerStyle.gridTemplateColumnsWithRepeat =
                List.of(GridTemplateComponent.autoFit(TrackSizingFunction.fixed(100f)));
            containerStyle.justifyContent = justifyContent;

            TaffyStyle childStyle = new TaffyStyle();
            childStyle.position = TaffyPosition.ABSOLUTE;
            childStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
            childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
            childStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
            NodeId child = tree.newLeaf(childStyle);

            NodeId root = tree.newWithChildren(containerStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());
            return tree.getLayout(child).location().x;
        }
    }

    // ------------------------------------------------------------------
    // 2. Item safe alignment
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("item alignment: safe overflow-position keywords")
    class ItemSafeAlignment {

        @Test
        @DisplayName("safe align-self: end packs at the end when the item fits")
        void safeAlignSelfEndNoOverflow() {
            TaffyTree tree = new TaffyTree();
            NodeId root = tree.newWithChildren(singleCellContainer(), sizedChild(tree, 10f, 40f, AlignItems.SAFE_END, null));
            tree.computeLayout(root, TaffySize.maxContent());

            // 40px item in a 100px row: safe end behaves as end.
            assertLocation(tree.getLayout(tree.getChildren(root).get(0)), 0f, 60f, "safe end item");
        }

        @Test
        @DisplayName("safe align-self: end falls back to start on overflow")
        void safeAlignSelfEndOverflow() {
            TaffyTree tree = new TaffyTree();
            NodeId root = tree.newWithChildren(singleCellContainer(), sizedChild(tree, 10f, 150f, AlignItems.SAFE_END, null));
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(tree.getChildren(root).get(0)), 0f, 0f, "safe end fallback");
        }

        @Test
        @DisplayName("align-self: end (unsafe) keeps the overflowing position [legacy regression]")
        void unsafeAlignSelfEndOverflow() {
            TaffyTree tree = new TaffyTree();
            NodeId root = tree.newWithChildren(singleCellContainer(), sizedChild(tree, 10f, 150f, AlignItems.END, null));
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(tree.getChildren(root).get(0)), 0f, -50f, "unsafe end item");
        }

        @Test
        @DisplayName("safe justify-self: end packs at the end when the item fits")
        void safeJustifySelfEndNoOverflow() {
            TaffyTree tree = new TaffyTree();
            NodeId root = tree.newWithChildren(singleCellContainer(), sizedChild(tree, 40f, 10f, null, AlignItems.SAFE_END));
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(tree.getChildren(root).get(0)), 60f, 0f, "safe end item");
        }

        @Test
        @DisplayName("safe justify-self: safe center falls back to start only on overflow")
        void safeCenterFallback() {
            TaffyTree fitsTree = new TaffyTree();
            NodeId fitsRoot = fitsTree.newWithChildren(singleCellContainer(), sizedChild(fitsTree, 40f, 10f, null, AlignItems.SAFE_CENTER));
            fitsTree.computeLayout(fitsRoot, TaffySize.maxContent());
            assertLocation(fitsTree.getLayout(fitsTree.getChildren(fitsRoot).get(0)), 30f, 0f, "safe center fits");

            TaffyTree overflowTree = new TaffyTree();
            NodeId overflowRoot = overflowTree.newWithChildren(
                singleCellContainer(), sizedChild(overflowTree, 150f, 10f, null, AlignItems.SAFE_CENTER));
            overflowTree.computeLayout(overflowRoot, TaffySize.maxContent());
            assertLocation(overflowTree.getLayout(overflowTree.getChildren(overflowRoot).get(0)), 0f, 0f,
                "safe center overflow");
        }

        @Test
        @DisplayName("safe justify-self in RTL falls back to the RTL start edge")
        void safeJustifySelfRtlOverflow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = singleCellContainer();
            containerStyle.direction = TaffyDirection.RTL;
            NodeId root = tree.newWithChildren(containerStyle, sizedChild(tree, 150f, 10f, null, AlignItems.SAFE_END));
            tree.computeLayout(root, TaffySize.maxContent());

            // RTL inline-start is the right edge: the overflowing 150px item is anchored at x == -50.
            assertLocation(tree.getLayout(tree.getChildren(root).get(0)), -50f, 0f, "rtl safe end fallback");
        }
    }

    // ------------------------------------------------------------------
    // 3. self-start / self-end direction resolution (ST-03 / #1077)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("self-start/self-end: item direction and axis")
    class SelfDirection {

        @Test
        @DisplayName("inline axis flips self keywords when the item direction differs from the container")
        void inlineAxisFlips() {
            assertEquals(60f, selfAbsoluteX(TaffyDirection.LTR, TaffyDirection.RTL, AlignItems.SELF_START, 40f),
                "self-start of an RTL item in an LTR container resolves to end");
            assertEquals(0f, selfAbsoluteX(TaffyDirection.LTR, TaffyDirection.RTL, AlignItems.SELF_END, 40f),
                "self-end of an RTL item in an LTR container resolves to start");
            assertEquals(0f, selfAbsoluteX(TaffyDirection.LTR, TaffyDirection.LTR, AlignItems.SELF_START, 40f),
                "same direction keeps self-start as start");
            assertEquals(60f, selfAbsoluteX(TaffyDirection.LTR, TaffyDirection.LTR, AlignItems.SELF_END, 40f),
                "same direction keeps self-end as end");
        }

        @Test
        @DisplayName("RTL container: self keywords resolve against the item's own direction")
        void rtlContainer() {
            assertEquals(60f, selfAbsoluteX(TaffyDirection.RTL, TaffyDirection.RTL, AlignItems.SELF_START, 40f),
                "RTL item in RTL container: self-start is the inline start (right edge)");
            assertEquals(0f, selfAbsoluteX(TaffyDirection.RTL, TaffyDirection.LTR, AlignItems.SELF_START, 40f),
                "LTR item in RTL container: self-start flips to logical end (left edge)");
        }

        @Test
        @DisplayName("block axis never flips self keywords")
        void blockAxisDoesNotFlip() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle childStyle = sized(10f, 20f);
            childStyle.direction = TaffyDirection.RTL;
            childStyle.alignSelf = AlignItems.SELF_END;
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(singleCellContainer(), child);
            tree.computeLayout(root, TaffySize.maxContent());

            // 20px item in a 100px row: RTL direction does not flip the block axis, so self-end is end.
            assertLocation(tree.getLayout(child), 0f, 80f, "block axis self-end");
        }

        @Test
        @DisplayName("container justify-items: self-start resolves per item direction")
        void containerSelfItemsResolvePerItemDirection() {
            TaffyTree rtlItemTree = new TaffyTree();
            TaffyStyle rtlContainer = singleCellContainer();
            rtlContainer.justifyItems = AlignItems.SELF_START;
            TaffyStyle rtlChildStyle = sized(40f, 10f);
            rtlChildStyle.direction = TaffyDirection.RTL;
            NodeId rtlChild = rtlItemTree.newLeaf(rtlChildStyle);
            NodeId rtlRoot = rtlItemTree.newWithChildren(rtlContainer, rtlChild);
            rtlItemTree.computeLayout(rtlRoot, TaffySize.maxContent());
            assertLocation(rtlItemTree.getLayout(rtlChild), 60f, 0f, "RTL item self-start flips to end");

            TaffyTree ltrItemTree = new TaffyTree();
            TaffyStyle ltrContainer = singleCellContainer();
            ltrContainer.justifyItems = AlignItems.SELF_START;
            NodeId ltrChild = ltrItemTree.newLeaf(sized(40f, 10f));
            NodeId ltrRoot = ltrItemTree.newWithChildren(ltrContainer, ltrChild);
            ltrItemTree.computeLayout(ltrRoot, TaffySize.maxContent());
            assertLocation(ltrItemTree.getLayout(ltrChild), 0f, 0f, "LTR item self-start stays start");
        }

        @Test
        @DisplayName("safe self keywords keep the safety modifier through direction resolution")
        void safeSelfKeywords() {
            // safe self-start of an RTL item in an LTR container resolves to safe end, so it packs at
            // the end when it fits and falls back to start when it overflows.
            assertEquals(60f, selfAbsoluteX(TaffyDirection.LTR, TaffyDirection.RTL, AlignItems.SAFE_SELF_START, 40f),
                "safe self-start flips to safe end (fits)");
            assertEquals(0f, selfAbsoluteX(TaffyDirection.LTR, TaffyDirection.RTL, AlignItems.SAFE_SELF_START, 150f),
                "safe self-start flips to safe end then falls back on overflow");
        }

        private float selfAbsoluteX(TaffyDirection containerDirection,
                                    TaffyDirection itemDirection,
                                    AlignItems justifySelf,
                                    float itemWidth) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = singleCellContainer();
            containerStyle.direction = containerDirection;
            TaffyStyle childStyle = sized(itemWidth, 10f);
            childStyle.direction = itemDirection;
            childStyle.justifySelf = justifySelf;
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(containerStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());
            return tree.getLayout(child).location().x;
        }
    }

    // ------------------------------------------------------------------
    // 4. Explicit normal item alignment (ST-04 / #1254)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("explicit normal item alignment")
    class ExplicitNormal {

        @Test
        @DisplayName("align-self/justify-self: normal overrides the container and stretches auto-sized items")
        void normalOverridesContainerForAutoSizedItem() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = singleCellContainer();
            containerStyle.alignItems = AlignItems.CENTER;
            containerStyle.justifyItems = AlignItems.CENTER;

            TaffyStyle childStyle = new TaffyStyle();
            childStyle.alignSelf = AlignItems.NORMAL;
            childStyle.justifySelf = AlignItems.NORMAL;
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(containerStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().width, EPSILON, "normal justify-self stretches an auto width");
            assertEquals(100f, layout.size().height, EPSILON, "normal align-self stretches an auto height");
            assertLocation(layout, 0f, 0f, "stretched item starts at the cell origin");
        }

        @Test
        @DisplayName("align-items/justify-items: normal stretches an auto-sized item")
        void containerNormalStretches() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = singleCellContainer();
            containerStyle.alignItems = AlignItems.NORMAL;
            containerStyle.justifyItems = AlignItems.NORMAL;

            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(containerStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().width, EPSILON, "container normal justify-items");
            assertEquals(100f, layout.size().height, EPSILON, "container normal align-items");
        }

        @Test
        @DisplayName("normal uses start for a definite size instead of stretching")
        void normalWithDefiniteSizeUsesStart() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = singleCellContainer();
            containerStyle.alignItems = AlignItems.CENTER;
            containerStyle.justifyItems = AlignItems.CENTER;

            TaffyStyle childStyle = sized(40f, 20f);
            childStyle.alignSelf = AlignItems.NORMAL;
            childStyle.justifySelf = AlignItems.NORMAL;
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(containerStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(40f, layout.size().width, EPSILON, "definite width is not stretched");
            assertEquals(20f, layout.size().height, EPSILON, "definite height is not stretched");
            assertLocation(layout, 0f, 0f, "definite-size normal is start-aligned");
        }
    }

    // ------------------------------------------------------------------
    // 5. Legacy regression matrix
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("legacy alignment regression")
    class LegacyRegression {

        @Test
        @DisplayName("AUTO sentinels keep the pre-P5 grid defaults")
        void autoDefaultsUnchanged() {
            TaffyTree tree = new TaffyTree();
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(singleCellContainer(), child);
            tree.computeLayout(root, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().width, EPSILON, "default justify-items stretches");
            assertEquals(100f, layout.size().height, EPSILON, "default align-items stretches");
            assertLocation(layout, 0f, 0f, "default location");
        }

        @Test
        @DisplayName("align-content legacy keywords keep their offsets")
        void alignContentLegacyOffsets() {
            assertEquals(0f, alignContentFirstY(AlignContent.START), "start");
            assertEquals(60f, alignContentFirstY(AlignContent.END), "end");
            assertEquals(30f, alignContentFirstY(AlignContent.CENTER), "center");
            assertEquals(0f, alignContentFirstY(AlignContent.SPACE_BETWEEN), "space-between first");
            assertEquals(15f, alignContentFirstY(AlignContent.SPACE_AROUND), "space-around first (60/(2*2))");
            assertEquals(20f, alignContentFirstY(AlignContent.SPACE_EVENLY), "space-evenly first (60/(2+1))");
            assertEquals(80f, alignContentSecondY(AlignContent.SPACE_BETWEEN), "space-between second");
        }

        @Test
        @DisplayName("item START/END/CENTER geometry is unchanged")
        void itemAlignmentLegacyGeometry() {
            TaffyTree endTree = new TaffyTree();
            TaffyStyle endStyle = sized(10f, 10f);
            endStyle.alignSelf = AlignItems.END;
            NodeId endChild = endTree.newLeaf(endStyle);
            NodeId endRoot = endTree.newWithChildren(singleCellContainer(), endChild);
            endTree.computeLayout(endRoot, TaffySize.maxContent());
            assertLocation(endTree.getLayout(endChild), 0f, 90f, "align-self end");

            TaffyTree centerTree = new TaffyTree();
            TaffyStyle centerStyle = sized(10f, 10f);
            centerStyle.justifySelf = AlignItems.CENTER;
            NodeId centerChild = centerTree.newLeaf(centerStyle);
            NodeId centerRoot = centerTree.newWithChildren(singleCellContainer(), centerChild);
            centerTree.computeLayout(centerRoot, TaffySize.maxContent());
            assertLocation(centerTree.getLayout(centerChild), 45f, 0f, "justify-self center");

            TaffyTree startTree = new TaffyTree();
            TaffyStyle startStyle = sized(10f, 10f);
            startStyle.alignSelf = AlignItems.FLEX_END;
            startStyle.justifySelf = AlignItems.FLEX_END;
            NodeId startChild = startTree.newLeaf(startStyle);
            NodeId startRoot = startTree.newWithChildren(singleCellContainer(), startChild);
            startTree.computeLayout(startRoot, TaffySize.maxContent());
            assertLocation(startTree.getLayout(startChild), 90f, 90f, "flex-end pair");
        }

        private float alignContentFirstY(AlignContent alignContent) {
            return alignContentY(alignContent, 0);
        }

        private float alignContentSecondY(AlignContent alignContent) {
            return alignContentY(alignContent, 1);
        }

        private float alignContentY(AlignContent alignContent, int childIndex) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(20f), fixed(20f));
            containerStyle.gridTemplateColumns = List.of(fixed(100f));
            containerStyle.alignContent = alignContent;
            NodeId first = tree.newLeaf(sized(10f, 20f));
            NodeId second = tree.newLeaf(sized(10f, 20f));
            NodeId root = tree.newWithChildren(containerStyle, first, second);
            tree.computeLayout(root, TaffySize.maxContent());
            return tree.getLayout(tree.getChildren(root).get(childIndex)).location().y;
        }
    }

    // ------------------------------------------------------------------
    // 6. Upstream fixture cross-checks
    //
    // These reproduce the expected values of the upstream gentest fixtures that
    // exercise the P5 keywords (the Java generated suite does not include the new
    // fixtures yet, TS-06).
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("upstream fixture cross-checks")
    class UpstreamFixtures {

        @Test
        @DisplayName("grid_justify_items_self_end_child_rtl")
        void justifyItemsSelfEndChildRtl() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = fixtureContainer();
            containerStyle.justifyItems = AlignItems.SELF_END;

            TaffyStyle firstStyle = sized(20f, 20f);
            firstStyle.direction = TaffyDirection.RTL;
            firstStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
            firstStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
            NodeId first = tree.newLeaf(firstStyle);

            TaffyStyle secondStyle = sized(20f, 20f);
            secondStyle.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
            secondStyle.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
            NodeId second = tree.newLeaf(secondStyle);

            NodeId root = tree.newWithChildren(containerStyle, first, second);
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 0f, 0f, "RTL item flips self-end to start");
            assertLocation(tree.getLayout(second), 60f, 40f, "LTR item self-end stays end");
        }

        @Test
        @DisplayName("grid_justify_self_self_start_child_rtl")
        void justifySelfSelfStartChildRtl() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = fixtureContainer();

            NodeId first = tree.newLeaf(fixtureChild(1, 1, TaffyDirection.RTL, AlignItems.SELF_START));
            NodeId second = tree.newLeaf(fixtureChild(2, 2, TaffyDirection.RTL, AlignItems.SELF_END));
            NodeId third = tree.newLeaf(fixtureChild(3, 3, TaffyDirection.LTR, AlignItems.SELF_START));

            NodeId root = tree.newWithChildren(containerStyle, first, second, third);
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 20f, 0f, "RTL self-start flips to end");
            assertLocation(tree.getLayout(second), 40f, 40f, "RTL self-end flips to start");
            assertLocation(tree.getLayout(third), 80f, 80f, "LTR self-start stays start");
        }

        @Test
        @DisplayName("grid_align_self_self_start_self_end")
        void alignSelfSelfStartSelfEnd() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = fixtureContainer();

            TaffyStyle firstStyle = fixtureChild(1, 1, TaffyDirection.LTR, null);
            firstStyle.alignSelf = AlignItems.SELF_START;
            NodeId first = tree.newLeaf(firstStyle);

            TaffyStyle secondStyle = fixtureChild(2, 2, TaffyDirection.RTL, null);
            secondStyle.alignSelf = AlignItems.SELF_END;
            NodeId second = tree.newLeaf(secondStyle);

            NodeId root = tree.newWithChildren(containerStyle, first, second);
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 0f, 0f, "block-axis self-start");
            assertLocation(tree.getLayout(second), 40f, 60f, "block-axis self-end does not flip");
        }

        @Test
        @DisplayName("grid_absolute_align_self_self_end")
        void absoluteAlignSelfSelfEnd() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = fixtureContainer();

            TaffyStyle firstStyle = sized(20f, 20f);
            firstStyle.position = TaffyPosition.ABSOLUTE;
            firstStyle.alignSelf = AlignItems.SELF_END;
            NodeId first = tree.newLeaf(firstStyle);

            TaffyStyle secondStyle = sized(20f, 20f);
            secondStyle.position = TaffyPosition.ABSOLUTE;
            secondStyle.direction = TaffyDirection.RTL;
            secondStyle.alignSelf = AlignItems.SELF_END;
            NodeId second = tree.newLeaf(secondStyle);

            NodeId root = tree.newWithChildren(containerStyle, first, second);
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 0f, 100f, "absolute self-end");
            assertLocation(tree.getLayout(second), 0f, 100f, "absolute RTL self-end (block axis does not flip)");
        }

        @Test
        @DisplayName("grid_absolute_justify_self_self_start_child_rtl")
        void absoluteJustifySelfSelfStartChildRtl() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = fixtureContainer();

            TaffyStyle firstStyle = sized(20f, 20f);
            firstStyle.position = TaffyPosition.ABSOLUTE;
            firstStyle.direction = TaffyDirection.RTL;
            firstStyle.justifySelf = AlignItems.SELF_START;
            NodeId first = tree.newLeaf(firstStyle);

            TaffyStyle secondStyle = sized(20f, 20f);
            secondStyle.position = TaffyPosition.ABSOLUTE;
            secondStyle.justifySelf = AlignItems.SELF_START;
            NodeId second = tree.newLeaf(secondStyle);

            NodeId root = tree.newWithChildren(containerStyle, first, second);
            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(first), 100f, 0f, "absolute RTL self-start flips to end");
            assertLocation(tree.getLayout(second), 0f, 0f, "absolute LTR self-start stays start");
        }

        @Test
        @DisplayName("grid_absolute safe justify-self falls back to start on overflow")
        void absoluteSafeJustifySelfOverflow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle childStyle = sized(150f, 10f);
            childStyle.position = TaffyPosition.ABSOLUTE;
            childStyle.justifySelf = AlignItems.SAFE_END;
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(singleCellContainer(), child);
            tree.computeLayout(root, TaffySize.maxContent());

            // The absolutely positioned 150px box overflows the 100px area: safe end anchors at start.
            assertLocation(tree.getLayout(child), 0f, 0f, "absolute safe end overflow");
        }

        @Test
        @DisplayName("grid_safe_justify_content_center_overflow and grid_unsafe_justify_content_end_overflow")
        void safeAndUnsafeJustifyContentOverflow() {
            // safe center: 120px of tracks in 100px overflow the container, so it falls back to start.
            float[] safeCenter = justifyContentFirstColumnX(AlignContent.SAFE_CENTER);
            assertEquals(0f, safeCenter[0], EPSILON, "safe center first column");
            assertEquals(40f, safeCenter[1], EPSILON, "safe center second column");
            assertEquals(80f, safeCenter[2], EPSILON, "safe center third column");

            // unsafe end: the overflow position is preserved (start at -20).
            float[] unsafeEnd = justifyContentFirstColumnX(AlignContent.END);
            assertEquals(-20f, unsafeEnd[0], EPSILON, "unsafe end first column");
            assertEquals(20f, unsafeEnd[1], EPSILON, "unsafe end second column");
            assertEquals(60f, unsafeEnd[2], EPSILON, "unsafe end third column");
        }

        @Test
        @DisplayName("grid_safe_align_content_center_overflow")
        void safeAlignContentCenterOverflow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = fixtureContainer();
            containerStyle.alignContent = AlignContent.SAFE_CENTER;

            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                children.add(tree.newLeaf(sized(40f, 40f)));
            }
            NodeId root = tree.newWithChildren(containerStyle, children);
            tree.computeLayout(root, TaffySize.maxContent());

            // 120px of rows in 100px overflow: safe center falls back to start.
            assertLocation(tree.getLayout(children.get(0)), 0f, 0f, "first row");
            assertLocation(tree.getLayout(children.get(3)), 0f, 40f, "second row");
            assertLocation(tree.getLayout(children.get(6)), 0f, 80f, "third row");
        }

        private TaffyStyle fixtureContainer() {
            TaffyStyle style = container(120f, 120f);
            style.gridTemplateRows = List.of(fixed(40f), fixed(40f), fixed(40f));
            style.gridTemplateColumns = List.of(fixed(40f), fixed(40f), fixed(40f));
            return style;
        }

        private TaffyStyle fixtureChild(int row, int column,
                                        TaffyDirection direction, AlignItems justifySelf) {
            TaffyStyle style = sized(20f, 20f);
            style.direction = direction;
            style.gridRow = new TaffyLine<>(GridPlacement.line(row), GridPlacement.auto());
            style.gridColumn = new TaffyLine<>(GridPlacement.line(column), GridPlacement.auto());
            if (justifySelf != null) {
                style.justifySelf = justifySelf;
            }
            return style;
        }

        private float[] justifyContentFirstColumnX(AlignContent justifyContent) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = container(100f, 100f);
            containerStyle.gridTemplateRows = List.of(fixed(40f), fixed(40f), fixed(40f));
            containerStyle.gridTemplateColumns = List.of(fixed(40f), fixed(40f), fixed(40f));
            containerStyle.justifyContent = justifyContent;

            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                children.add(tree.newLeaf(sized(40f, 40f)));
            }
            NodeId root = tree.newWithChildren(containerStyle, children);
            tree.computeLayout(root, TaffySize.maxContent());

            return new float[]{
                tree.getLayout(children.get(0)).location().x,
                tree.getLayout(children.get(1)).location().x,
                tree.getLayout(children.get(2)).location().x
            };
        }

    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static TaffyStyle container(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle singleCellContainer() {
        TaffyStyle style = container(100f, 100f);
        style.gridTemplateRows = List.of(fixed(100f));
        style.gridTemplateColumns = List.of(fixed(100f));
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static NodeId sizedChild(TaffyTree tree, float width, float height,
                                     AlignItems alignSelf, AlignItems justifySelf) {
        TaffyStyle style = sized(width, height);
        if (alignSelf != null) {
            style.alignSelf = alignSelf;
        }
        if (justifySelf != null) {
            style.justifySelf = justifySelf;
        }
        return tree.newLeaf(style);
    }

    private static TrackSizingFunction fixed(float value) {
        return TrackSizingFunction.fixed(value);
    }

    private static TrackSizingFunction auto() {
        return TrackSizingFunction.auto();
    }

    private static void assertLocation(Layout layout, float expectedX, float expectedY, String message) {
        assertEquals(expectedX, layout.location().x, EPSILON, message + " x");
        assertEquals(expectedY, layout.location().y, EPSILON, message + " y");
    }
}
