package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignSelf;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression tests for flexbox {@code align-items: start/end} vs {@code flex-start/flex-end}
 * inside a {@code wrap-reverse} row container (PA-02).
 * <p>
 * {@code start}/{@code end} are physical cross-axis edges and must not flip when flex-wrap is
 * reversed. {@code flex-start}/{@code flex-end} are flex-relative and must flip when
 * flex-wrap is reversed. Before the fix, {@code AlignSelf.fromAlignItems} folded
 * START into FLEX_START (and END into FLEX_END), so {@code align-items: start} inside a
 * wrap-reverse container was wrongly placed on the bottom edge.
 * <p>
 * Rust reference: taffy src/compute/flexbox.rs {@code align_flex_items_along_cross_axis}
 * ({@code AlignSelf::Start => 0.0}, {@code AlignSelf::End => free_space},
 * {@code AlignSelf::FlexStart => if is_wrap_reverse { free_space } else { 0.0 }} ...).
 */
public class AlignSelfWrapReverseTest {

    private static final float EPSILON = 0.01f;
    private static final float CONTAINER_SIZE = 100f;
    private static final float ITEM_SIZE = 20f;
    /** Cross-axis free space of the single stretched flex line: 100 - 20. */
    private static final float FREE_SPACE = CONTAINER_SIZE - ITEM_SIZE;

    // ------------------------------------------------------------------
    // align-items: start/end/flex-start/flex-end
    // ------------------------------------------------------------------

    static Stream<Arguments> alignItemsCases() {
        return Stream.of(
            // flex-wrap: wrap -- start/flex-start coincide, end/flex-end coincide
            Arguments.of(FlexWrap.WRAP, AlignItems.START, 0f),
            Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_START, 0f),
            Arguments.of(FlexWrap.WRAP, AlignItems.END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_END, FREE_SPACE),
            // flex-wrap: wrap-reverse -- start/end must NOT flip, flex-* must flip
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.START, 0f),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_START, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_END, 0f)
        );
    }

    @ParameterizedTest(name = "align-items: {1} under flex-wrap: {0} -> y={2}")
    @MethodSource("alignItemsCases")
    @DisplayName("align-items start/end are physical while flex-start/flex-end flip with wrap-reverse")
    void alignItemsDistinguishesPhysicalFromFlexRelative(FlexWrap wrap, AlignItems alignItems, float expectedY) {
        float y = layoutSingleInFlowItemY(wrap, alignItems, null);
        assertEquals(expectedY, y, EPSILON,
            "align-items: " + alignItems + " under flex-wrap: " + wrap + " should put the item at y=" + expectedY);
    }

    // ------------------------------------------------------------------
    // align-self: start/end/flex-start/flex-end (overrides container align-items: center)
    // ------------------------------------------------------------------

    static Stream<Arguments> alignSelfCases() {
        return Stream.of(
            Arguments.of(FlexWrap.WRAP, AlignItems.START, 0f),
            Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_START, 0f),
            Arguments.of(FlexWrap.WRAP, AlignItems.END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.START, 0f),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_START, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_END, 0f)
        );
    }

    @ParameterizedTest(name = "align-self: {1} under flex-wrap: {0} -> y={2}")
    @MethodSource("alignSelfCases")
    @DisplayName("align-self start/end are physical while flex-start/flex-end flip with wrap-reverse")
    void alignSelfDistinguishesPhysicalFromFlexRelative(FlexWrap wrap, AlignItems alignSelf, float expectedY) {
        float y = layoutSingleInFlowItemY(wrap, AlignItems.CENTER, alignSelf);
        assertEquals(expectedY, y, EPSILON,
            "align-self: " + alignSelf + " under flex-wrap: " + wrap + " should put the item at y=" + expectedY);
    }

    // ------------------------------------------------------------------
    // Absolutely positioned child (flex container abspos alignment path)
    // ------------------------------------------------------------------

    static Stream<Arguments> absoluteAlignSelfCases() {
        return Stream.of(
            Arguments.of(FlexWrap.WRAP, AlignItems.START, 0f),
            Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_START, 0f),
            Arguments.of(FlexWrap.WRAP, AlignItems.END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.START, 0f),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_START, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.END, FREE_SPACE),
            Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_END, 0f)
        );
    }

    @ParameterizedTest(name = "abspos align-self: {1} under flex-wrap: {0} -> y={2}")
    @MethodSource("absoluteAlignSelfCases")
    @DisplayName("absolutely positioned children keep physical start/end under wrap-reverse")
    void absolutelyPositionedChildrenDistinguishPhysicalFromFlexRelative(
        FlexWrap wrap, AlignItems alignSelf, float expectedY) {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.FLEX;
        containerStyle.flexDirection = FlexDirection.ROW;
        containerStyle.flexWrap = wrap;
        containerStyle.alignItems = AlignItems.CENTER;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(CONTAINER_SIZE), TaffyDimension.length(CONTAINER_SIZE));
        NodeId container = tree.newLeaf(containerStyle);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.alignSelf = alignSelf;
        childStyle.size = new TaffySize<>(TaffyDimension.length(ITEM_SIZE), TaffyDimension.length(ITEM_SIZE));
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(container, child);

        tree.computeLayout(container, new TaffySize<>(AvailableSpace.definite(CONTAINER_SIZE), AvailableSpace.definite(CONTAINER_SIZE)));

        Layout layout = tree.getLayout(child);
        assertEquals(ITEM_SIZE, layout.size().height, EPSILON);
        assertEquals(expectedY, layout.location().y, EPSILON,
            "abspos align-self: " + alignSelf + " under flex-wrap: " + wrap
            + " should put the item at y=" + expectedY);
    }

    // ------------------------------------------------------------------
    // Enum / conversion contract
    // ------------------------------------------------------------------

    @Test
    @DisplayName("starting/ending values are never folded into flex-relative ones")
    void fromAlignItemsPreservesStartAndEnd() {
        assertSame(AlignSelf.START,
            AlignSelf.fromAlignItems(AlignItems.START),
            "AlignItems.START must not be folded into AlignSelf.FLEX_START");
        assertSame(AlignSelf.END,
            AlignSelf.fromAlignItems(AlignItems.END),
            "AlignItems.END must not be folded into AlignSelf.FLEX_END");
        assertSame(AlignSelf.FLEX_START,
            AlignSelf.fromAlignItems(AlignItems.FLEX_START));
        assertSame(AlignSelf.FLEX_END,
            AlignSelf.fromAlignItems(AlignItems.FLEX_END));
        assertSame(AlignSelf.STRETCH,
            AlignSelf.fromAlignItems(AlignItems.AUTO));
        assertSame(AlignSelf.STRETCH,
            AlignSelf.fromAlignItems(null));
    }

    @Test
    @DisplayName("legacy constant ordinals are stable and START/END round-trip to AlignItems")
    void newConstantsAreAppendedAndRoundTrip() {
        // 1.2 compatibility policy Q2/S2: new constants are appended and existing ordinals stay
        // stable. Assert the legacy prefix instead of the tail so later P5 appends (NORMAL,
        // SELF_*, SAFE_*) do not invalidate this stability check.
        AlignSelf[] values = AlignSelf.values();
        assertEquals(AlignSelf.AUTO, values[0]);
        assertEquals(AlignSelf.FLEX_START, values[1]);
        assertEquals(AlignSelf.FLEX_END, values[2]);
        assertEquals(AlignSelf.CENTER, values[3]);
        assertEquals(AlignSelf.BASELINE, values[4]);
        assertEquals(AlignSelf.STRETCH, values[5]);
        assertEquals(AlignSelf.START, values[6], "START keeps its pre-existing ordinal (Q2)");
        assertEquals(AlignSelf.END, values[7], "END keeps its pre-existing ordinal (Q2)");
        assertEquals(AlignItems.START, AlignSelf.START.toAlignItems());
        assertEquals(AlignItems.END, AlignSelf.END.toAlignItems());
        assertEquals(AlignItems.FLEX_START, AlignSelf.FLEX_START.toAlignItems());
        assertEquals(AlignItems.FLEX_END, AlignSelf.FLEX_END.toAlignItems());
        assertEquals(AlignItems.STRETCH, AlignSelf.AUTO.toAlignItems());
    }

    @ParameterizedTest
    @ValueSource(strings = {"START", "END", "FLEX_START", "FLEX_END"})
    @DisplayName("AlignSelf enum exposes START/END alongside the existing flex values")
    void enumExposesAllFourKeywords(String name) {
        assertNotNull(AlignSelf.valueOf(name));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Lays out a row flex container (100x100, wrap or wrap-reverse) with a single 20x20 in-flow
     * item and returns the item's unrounded cross-axis (y) position.
     *
     * @param wrap       flex-wrap mode
     * @param alignItems container align-items value
     * @param alignSelf  item align-self value, or {@code null} to leave it at AUTO
     */
    private static float layoutSingleInFlowItemY(FlexWrap wrap, AlignItems alignItems, AlignItems alignSelf) {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.FLEX;
        containerStyle.flexDirection = FlexDirection.ROW;
        containerStyle.flexWrap = wrap;
        containerStyle.alignItems = alignItems;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(CONTAINER_SIZE), TaffyDimension.length(CONTAINER_SIZE));
        NodeId container = tree.newLeaf(containerStyle);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(ITEM_SIZE), TaffyDimension.length(ITEM_SIZE));
        if (alignSelf != null) {
            childStyle.alignSelf = alignSelf;
        }
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(container, child);

        tree.computeLayout(container, new TaffySize<>(AvailableSpace.definite(CONTAINER_SIZE), AvailableSpace.definite(CONTAINER_SIZE)));

        Layout layout = tree.getLayout(child);
        assertEquals(ITEM_SIZE, layout.size().height, EPSILON,
            "item must keep its definite cross size (not stretched)");
        return layout.location().y;
    }
}
