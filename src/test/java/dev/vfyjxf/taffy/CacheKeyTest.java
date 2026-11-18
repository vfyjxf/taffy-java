package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.CollapsibleMarginSet;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutCache;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutInput;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.RequestedAxis;
import dev.vfyjxf.taffy.tree.RunMode;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the layout cache key and eviction policy (P0-b1: CR-01/CR-02/CR-03, BL-04).
 *
 * <p>The cache is a pure unit here: entries are stored and looked up with explicit
 * {@link LayoutInput}s, so every test states exactly which input component changes.
 */
public class CacheKeyTest {

    private static final float EPSILON = 0.1f;
    private static final TaffySize<AvailableSpace> MAX_CONTENT =
        new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT);

    private static LayoutInput performLayout(FloatSize knownDimensions, FloatSize parentSize,
                                             TaffySize<AvailableSpace> availableSpace) {
        return input(RunMode.PERFORM_LAYOUT, knownDimensions, parentSize, availableSpace,
            SizingMode.INHERENT_SIZE, TaffyLine.FALSE);
    }

    private static LayoutInput computeSize(FloatSize knownDimensions, FloatSize parentSize,
                                           TaffySize<AvailableSpace> availableSpace) {
        return input(RunMode.COMPUTE_SIZE, knownDimensions, parentSize, availableSpace,
            SizingMode.INHERENT_SIZE, TaffyLine.FALSE);
    }

    private static LayoutInput input(RunMode runMode, FloatSize knownDimensions, FloatSize parentSize,
                                     TaffySize<AvailableSpace> availableSpace, SizingMode sizingMode,
                                     TaffyLine<Boolean> verticalMarginsAreCollapsible) {
        return new LayoutInput(
            runMode,
            sizingMode,
            RequestedAxis.BOTH,
            knownDimensions,
            parentSize,
            availableSpace,
            verticalMarginsAreCollapsible
        );
    }

    private static LayoutOutput output(float width, float height) {
        return LayoutOutput.fromSizes(new FloatSize(width, height), FloatSize.zero());
    }

    // === CR-01: parent size is part of the key ===

    @Test
    @DisplayName("final_layout_misses_when_parent_size_changes")
    void finalLayoutMissesWhenParentSizeChanges() {
        LayoutCache cache = new LayoutCache();
        LayoutInput base = performLayout(FloatSize.none(), new FloatSize(100f, 50f), MAX_CONTENT);
        cache.store(base, output(100f, 50f));

        assertNotNull(cache.get(base), "identical inputs must hit");

        assertNull(cache.get(performLayout(FloatSize.none(), new FloatSize(200f, 50f), MAX_CONTENT)),
            "a different parent width must miss");
        assertNull(cache.get(performLayout(FloatSize.none(), new FloatSize(100f, 60f), MAX_CONTENT)),
            "a different parent height must miss");
    }

    @Test
    @DisplayName("measure_misses_when_parent_size_changes")
    void measureMissesWhenParentSizeChanges() {
        LayoutCache cache = new LayoutCache();
        LayoutInput base = computeSize(new FloatSize(100f, Float.NaN), new FloatSize(100f, 50f), MAX_CONTENT);
        cache.store(base, output(100f, 50f));

        assertNotNull(cache.get(base));

        assertNull(cache.get(computeSize(new FloatSize(100f, Float.NaN), new FloatSize(200f, 50f), MAX_CONTENT)),
            "percentage sizes can resolve differently against a different parent width");
        assertNull(cache.get(computeSize(new FloatSize(100f, Float.NaN), new FloatSize(100f, 60f), MAX_CONTENT)),
            "percentage heights resolve against the parent height");
    }

    @Test
    @DisplayName("known_axis_ignores_available_space_changes")
    void knownAxisIgnoresAvailableSpaceChanges() {
        LayoutCache cache = new LayoutCache();

        // The width is known, so the available width is not part of the key. The height is
        // unknown, so the available height is.
        LayoutInput measure = computeSize(
            new FloatSize(100f, Float.NaN),
            new FloatSize(300f, 300f),
            new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MAX_CONTENT)
        );
        cache.store(measure, output(100f, 50f));

        LayoutInput widerAvailableWidth = computeSize(
            new FloatSize(100f, Float.NaN),
            new FloatSize(300f, 300f),
            new TaffySize<>(AvailableSpace.definite(400f), AvailableSpace.MAX_CONTENT)
        );
        assertNotNull(cache.get(widerAvailableWidth),
            "a known width must not miss because the available width changed");

        LayoutInput differentAvailableHeight = computeSize(
            new FloatSize(100f, Float.NaN),
            new FloatSize(300f, 300f),
            new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MIN_CONTENT)
        );
        assertNull(cache.get(differentAvailableHeight),
            "an unknown height must still be keyed on its available space");

        // The final layout key follows the same rule.
        LayoutCache finalCache = new LayoutCache();
        LayoutInput finalLayout = performLayout(
            new FloatSize(80f, Float.NaN),
            new FloatSize(300f, 300f),
            new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MAX_CONTENT)
        );
        finalCache.store(finalLayout, output(80f, 40f));
        assertNotNull(finalCache.get(performLayout(
            new FloatSize(80f, Float.NaN),
            new FloatSize(300f, 300f),
            new TaffySize<>(AvailableSpace.definite(400f), AvailableSpace.MAX_CONTENT)
        )), "a known width in a final layout key ignores the available width");
    }

    // === CR-03: sizingMode and margin collapsibility are part of a cache-wide mode ===

    @Test
    @DisplayName("sizing_mode_change_misses_and_evicts")
    void sizingModeChangeMissesAndEvicts() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inherent = performLayout(FloatSize.none(), FloatSize.none(), MAX_CONTENT);
        cache.store(inherent, output(10f, 10f));
        assertNotNull(cache.get(inherent));

        LayoutInput content = input(RunMode.PERFORM_LAYOUT, FloatSize.none(), FloatSize.none(), MAX_CONTENT,
            SizingMode.CONTENT_SIZE, TaffyLine.FALSE);
        assertNull(cache.get(content), "a different sizing mode must miss");

        cache.store(content, output(20f, 20f));
        assertNotNull(cache.get(content));
        assertNull(cache.get(inherent),
            "storing with a different mode must evict the old entries");
    }

    @Test
    @DisplayName("margin_collapsibility_change_misses_and_evicts")
    void marginCollapsibilityChangeMissesAndEvicts() {
        LayoutCache cache = new LayoutCache();
        LayoutInput collapsible = input(RunMode.PERFORM_LAYOUT, FloatSize.none(), FloatSize.none(), MAX_CONTENT,
            SizingMode.INHERENT_SIZE, TaffyLine.TRUE);
        LayoutInput nonCollapsible = input(RunMode.PERFORM_LAYOUT, FloatSize.none(), FloatSize.none(), MAX_CONTENT,
            SizingMode.INHERENT_SIZE, TaffyLine.FALSE);

        cache.store(collapsible, output(10f, 10f));
        assertNull(cache.get(nonCollapsible), "a different margin collapsibility must miss");

        cache.store(nonCollapsible, output(20f, 20f));
        assertNull(cache.get(collapsible), "the old mode's entries must have been evicted");
        assertNotNull(cache.get(nonCollapsible));
    }

    // === CR-02: margin-collapse metadata must not be lost through the measure cache ===

    @Test
    @DisplayName("measure_with_margin_collapse_metadata_is_not_cached")
    void measureWithMarginCollapseMetadataIsNotCached() {
        LayoutInput measure = computeSize(FloatSize.none(), FloatSize.none(), MAX_CONTENT);

        LayoutCache collapseThroughCache = new LayoutCache();
        LayoutOutput collapseThrough = new LayoutOutput(
            new FloatSize(10f, 10f),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, Float.NaN),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            true
        );
        collapseThroughCache.store(measure, collapseThrough);
        assertNull(collapseThroughCache.get(measure),
            "a result that can collapse through must not be reconstructed from its size");
        assertTrue(collapseThroughCache.isEmpty());

        LayoutCache carriedMarginCache = new LayoutCache();
        LayoutOutput carriedMargin = new LayoutOutput(
            new FloatSize(10f, 10f),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, Float.NaN),
            CollapsibleMarginSet.fromMargin(10f),
            CollapsibleMarginSet.ZERO,
            false
        );
        carriedMarginCache.store(measure, carriedMargin);
        assertNull(carriedMarginCache.get(measure),
            "a result carrying a collapsible margin must not be reconstructed from its size");
        assertTrue(carriedMarginCache.isEmpty());
    }

    @Test
    @DisplayName("measure_with_collapsible_vertical_margins_is_not_cached")
    void measureWithCollapsibleVerticalMarginsIsNotCached() {
        LayoutCache cache = new LayoutCache();
        LayoutInput measure = input(RunMode.COMPUTE_SIZE, FloatSize.none(), FloatSize.none(), MAX_CONTENT,
            SizingMode.INHERENT_SIZE, TaffyLine.TRUE);

        cache.store(measure, output(10f, 10f));

        assertNull(cache.get(measure),
            "a measure computed with collapsible vertical margins carries metadata the cache cannot rebuild");
        assertTrue(cache.isEmpty());
    }

    @Test
    @DisplayName("final_layout_keeps_margin_collapse_metadata")
    void finalLayoutKeepsMarginCollapseMetadata() {
        LayoutCache cache = new LayoutCache();
        LayoutInput layout = performLayout(FloatSize.none(), FloatSize.none(), MAX_CONTENT);
        LayoutOutput output = new LayoutOutput(
            new FloatSize(10f, 10f),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, Float.NaN),
            CollapsibleMarginSet.fromMargin(10f),
            CollapsibleMarginSet.ZERO,
            true
        );

        cache.store(layout, output);

        LayoutOutput cached = cache.get(layout);
        assertNotNull(cached, "final layout entries keep the whole output");
        assertEquals(output.topMargin().resolve(), cached.topMargin().resolve(), EPSILON);
        assertTrue(cached.marginsCanCollapseThrough());
    }

    // === Eviction policy ===

    @Test
    @DisplayName("recently_used_measure_entries_get_a_second_chance")
    void recentlyUsedMeasureEntriesGetASecondChance() {
        LayoutCache cache = new LayoutCache();
        for (int i = 0; i < 9; i++) {
            cache.store(measureOfWidth(i), output(i, i));
        }
        // A tenth entry forces one eviction from the full cache.
        cache.store(measureOfWidth(9), output(9f, 9f));

        // Touch entry 1 so that the clock gives it a second chance.
        assertNotNull(cache.get(measureOfWidth(1)));
        cache.store(measureOfWidth(10), output(10f, 10f));

        assertNotNull(cache.get(measureOfWidth(1)), "a recently used entry must not be evicted");
        assertNull(cache.get(measureOfWidth(2)), "an unused entry is evicted instead");
    }

    private static LayoutInput measureOfWidth(int width) {
        return new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(width, Float.NaN),
            FloatSize.none(),
            MAX_CONTENT,
            TaffyLine.FALSE
        );
    }

    // === BL-04: deprecated overloads keep working and delegate ===

    @Test
    @DisplayName("deprecated_float_size_overloads_delegate")
    @SuppressWarnings("deprecation")
    void deprecatedFloatSizeOverloadsDelegate() {
        LayoutCache cache = new LayoutCache();

        cache.store(FloatSize.none(), MAX_CONTENT, RunMode.PERFORM_LAYOUT, output(50f, 50f));
        assertNotNull(cache.get(FloatSize.none(), MAX_CONTENT, RunMode.PERFORM_LAYOUT));
        assertNull(cache.get(FloatSize.none(), MAX_CONTENT, RunMode.COMPUTE_SIZE),
            "a ComputeSize lookup no longer reuses the final layout entry");
    }

    // === The removed ComputeSize -> finalLayoutEntry fallback only affected hit rate ===

    /**
     * The Java port used to let a {@code COMPUTE_SIZE} lookup reuse the cached final layout entry
     * and reconstruct its size with {@code fromOuterSize} (upstream taffy never did that). For the
     * block layout path, a measure pass returns exactly the final outer size, so removing the
     * fallback only turns a hit into a recomputation: the value is the same. This test pins that
     * equivalence down for the reachable block path.
     */
    @Test
    @DisplayName("block_measure_output_matches_final_layout_size")
    void blockMeasureOutputMatchesFinalLayoutSize() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(40f), TaffyDimension.length(20f));
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle blockStyle = new TaffyStyle();
        blockStyle.display = TaffyDisplay.BLOCK;
        NodeId block = tree.newWithChildren(blockStyle, child);

        TaffySize<AvailableSpace> maxContent = TaffySize.maxContent();
        tree.computeLayout(block, maxContent);
        Layout finalLayout = tree.getUnroundedLayout(block);
        assertNotNull(finalLayout);

        LayoutComputer computer = new LayoutComputer(tree, null);
        FloatSize measured = computer.measureChildSize(
            block,
            FloatSize.none(),
            FloatSize.none(),
            maxContent,
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );

        assertEquals(finalLayout.size().width, measured.width, EPSILON);
        assertEquals(finalLayout.size().height, measured.height, EPSILON);
    }
}
