package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P30: the grid intrinsic-size (measure) pass must not commit out-of-flow child layouts.
 *
 * <p>A grid whose only children are absolutely positioned produces no in-flow items, so the
 * container-size branch of {@link GridComputer#compute} used to run the whole out-of-flow pass -
 * {@code layoutAbsoluteChildren} and the {@code display: none} pass - even when the pass was a
 * {@link RunMode#COMPUTE_SIZE} measurement. That pass calls
 * {@link LayoutComputer#performChildLayout} for every absolutely positioned child and writes the
 * resulting layout with {@link TaffyTree#setUnroundedLayout}, using the probe's container size
 * (for a percentage-height grid measured with an indefinite containing block: the padding-only
 * height, here 5px) instead of the final one.</p>
 *
 * <p>The minimal reproduction is the P29 fuzz finding, which is deterministic:</p>
 * <pre>
 * root  flex  50% x 10
 *  └ inner block 50% x auto
 *     └ grid auto x 50% (padding 4/1/2/3, rows [auto, 60px], only abspos children)
 *        └ abs flex 10 x 50% position:absolute
 * </pre>
 * <p>The first {@code computeLayout(root, 800x800)} happens to run the grid's final layout after
 * its probes, so the abspos child ends at half of the grid's 5px padding-box height (2.5). A
 * following {@code clearCache(root)} only clears the <em>root</em> cache: the root is laid out
 * again and its probes re-run the grid measure pass, but the inner block's final layout is a
 * cache hit, so the grid's final layout never runs again and the probe's 50% of a 65px
 * content-sized container (32.5) stays in the tree. The same happens for
 * {@code markDirty(root)} / {@code markDirty(inner)} / a no-op {@code setStyle(root)}. Fixing the
 * measure pass to return the container size before the out-of-flow and hidden-child passes (the
 * order upstream's grid/mod.rs uses) keeps every incremental pass equal to a fresh tree.</p>
 */
@DisplayName("P30: grid measure pass keeps out-of-flow child layouts untouched")
class GridAbsoluteProbeP30Test {

    private static final float EPSILON = 0.001f;

    private static final float EXPECTED_ABSOLUTE_HEIGHT = 2.5f;

    private static TaffySize<AvailableSpace> viewport() {
        return TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.definite(800f));
    }

    private static TaffyStyle sized(TaffyDimension width, TaffyDimension height) {
        TaffyStyle style = new TaffyStyle();
        style.size = TaffySize.of(width, height);
        return style;
    }

    /** The node ids of the P29 minimal grid/abspos reproduction. */
    private record Repro(TaffyTree tree, NodeId root, NodeId inner, NodeId grid, NodeId absolute) {}

    private static Repro buildRepro() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
        rootStyle.display = TaffyDisplay.FLEX;

        TaffyStyle innerStyle = sized(TaffyDimension.percent(0.5f), TaffyDimension.AUTO);
        innerStyle.display = TaffyDisplay.BLOCK;

        TaffyStyle gridStyle = sized(TaffyDimension.AUTO, TaffyDimension.percent(0.5f));
        gridStyle.display = TaffyDisplay.GRID;
        // "padding 4/1/2/3" in TaffyRect's (left, right, top, bottom) order: 5px vertically.
        gridStyle.padding = TaffyRect.of(
            LengthPercentage.length(4f), LengthPercentage.length(1f),
            LengthPercentage.length(2f), LengthPercentage.length(3f));
        gridStyle.gridTemplateRows = List.of(
            TrackSizingFunction.auto(), TrackSizingFunction.fixed(60f));

        TaffyStyle absoluteStyle = sized(TaffyDimension.length(10f), TaffyDimension.percent(0.5f));
        absoluteStyle.display = TaffyDisplay.FLEX;
        absoluteStyle.position = TaffyPosition.ABSOLUTE;

        NodeId absolute = tree.newLeaf(absoluteStyle);
        NodeId grid = tree.newWithChildren(gridStyle, absolute);
        NodeId inner = tree.newWithChildren(innerStyle, grid);
        NodeId root = tree.newWithChildren(rootStyle, inner);
        return new Repro(tree, root, inner, grid, absolute);
    }

    @Test
    @DisplayName("an incremental pass after parent invalidation equals a fresh tree")
    void incrementalPassAfterParentInvalidationKeepsProbeOutOfTheTree() {
        Repro fresh = buildRepro();
        fresh.tree().computeLayout(fresh.root(), viewport());
        assertEquals(EXPECTED_ABSOLUTE_HEIGHT,
            fresh.tree().getUnroundedLayout(fresh.absolute()).size().height, EPSILON,
            "fresh tree reference height");

        // The first layout already ends with the grid's final pass, so it is the reference.
        Repro initial = buildRepro();
        initial.tree().computeLayout(initial.root(), viewport());
        assertEquals(EXPECTED_ABSOLUTE_HEIGHT,
            initial.tree().getUnroundedLayout(initial.absolute()).size().height, EPSILON,
            "first computeLayout");

        // Each of these only invalidates the root or the inner block: the grid itself is not
        // dirty, so its final layout is served from the cache and only a probe could write the
        // abspos layout during the second pass.
        assertInvalidationMatchesFresh("clearCache(root)", initial, initial.root(), false);
        assertInvalidationMatchesFresh("markDirty(root)", initial, initial.root(), true);
        assertInvalidationMatchesFresh("markDirty(inner)", initial, initial.inner(), true);
        assertInvalidationMatchesFresh("no-op setStyle(root)", initial, initial.root(), false);
    }

    private static void assertInvalidationMatchesFresh(String variant, Repro repro, NodeId invalidated,
                                                       boolean dirty) {
        if (dirty) {
            repro.tree().markDirty(invalidated);
        } else if (variant.startsWith("clearCache")) {
            repro.tree().clearCache(invalidated);
        } else {
            // no-op setStyle(root): the same style object describes the same input.
            repro.tree().setStyle(invalidated, repro.tree().getStyle(invalidated));
        }
        repro.tree().computeLayout(repro.root(), viewport());

        Layout absolute = repro.tree().getUnroundedLayout(repro.absolute());
        assertEquals(EXPECTED_ABSOLUTE_HEIGHT, absolute.size().height, EPSILON,
            variant + ": abspos child height after the incremental pass");
        assertEquals(10f, absolute.size().width, EPSILON,
            variant + ": abspos child width after the incremental pass");

        Layout grid = repro.tree().getUnroundedLayout(repro.grid());
        assertEquals(200f, grid.size().width, EPSILON, variant + ": grid width");
        assertEquals(5f, grid.size().height, EPSILON, variant + ": grid height");
    }

    @Test
    @DisplayName("a measure probe with a different containing block writes no abspos layout")
    void measureProbeDoesNotWriteOutOfFlowChildLayout() {
        Repro repro = buildRepro();
        repro.tree().computeLayout(repro.root(), viewport());
        Layout before = repro.tree().getUnroundedLayout(repro.absolute());

        // A parent's intrinsic-size pass measures the same content-sized grid against a taller
        // indefinite containing block: its 50% height would be 500, and the abspos child's 50%
        // of the resulting probe container (250) is exactly the value that leaked before the fix.
        LayoutComputer computer = new LayoutComputer(repro.tree(), null);
        FloatSize measured = computer.measureChildSize(
            repro.grid(),
            new FloatSize(Float.NaN, Float.NaN),
            new FloatSize(200f, 1000f),
            TaffySize.of(AvailableSpace.definite(200f), AvailableSpace.definite(1000f)),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE);

        // The probe returns a size, but it must not have committed any layout: the final grid
        // height stays at 5 (the first pass's result) and the abspos child keeps its 2.5.
        assertEquals(EXPECTED_ABSOLUTE_HEIGHT,
            repro.tree().getUnroundedLayout(repro.absolute()).size().height, EPSILON,
            "abspos child layout after a measure probe");
        assertEquals(before.size().height,
            repro.tree().getUnroundedLayout(repro.absolute()).size().height, EPSILON,
            "abspos child layout is the pre-probe layout");
        assertEquals(5f, repro.tree().getUnroundedLayout(repro.grid()).size().height, EPSILON,
            "grid final layout after a measure probe");
        assertEquals(5f, measured.width, EPSILON, "probe width (content-sized: padding only)");
        assertEquals(500f, measured.height, EPSILON, "probe height (50% of the 1000px containing block)");
    }
}
