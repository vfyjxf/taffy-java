package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.BlockContext;
import dev.vfyjxf.taffy.util.FloatContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7/FT-01 regression tests for the CSS {@code float}/{@code clear} style foundation.
 *
 * <p><b>Scope.</b> These tests cover only the additive types of the first FT-01 batch:
 * {@link TaffyFloat}, {@link Clear} and the two {@link TaffyStyle} fields. No layout algorithm
 * reads them yet, so the assertions here are deliberately style-level (defaults and
 * {@link TaffyStyle#copy()} propagation) rather than layout-level.</p>
 *
 * <p><b>Naming.</b> The upstream Rust type is {@code Float}; the Java port uses
 * {@link TaffyFloat} because a top-level {@code dev.vfyjxf.taffy.style.Float} would shadow
 * {@link java.lang.Float} inside the style package (existing {@code Float.NaN}/{@code isNaN}
 * uses) and make {@code import dev.vfyjxf.taffy.style.*} ambiguous in generated tests. The
 * {@code Taffy}-prefix convention matches {@code TaffyDisplay}/{@code TaffyPosition}.</p>
 */
public class FloatStyleP7Test {

    private static final float EPSILON = 0.001f;

    // === TaffyFloat ===

    @Test
    @DisplayName("TaffyFloat exposes NONE/LEFT/RIGHT and the CSS default NONE")
    void floatEnumValuesAndDefault() {
        assertEquals(3, TaffyFloat.values().length);
        assertSame(TaffyFloat.NONE, TaffyFloat.valueOf("NONE"));
        assertSame(TaffyFloat.LEFT, TaffyFloat.valueOf("LEFT"));
        assertSame(TaffyFloat.RIGHT, TaffyFloat.valueOf("RIGHT"));

        TaffyStyle style = new TaffyStyle();
        assertSame(TaffyFloat.NONE, style.float_);
        assertSame(TaffyFloat.NONE, style.getFloat());
    }

    @Test
    @DisplayName("TaffyFloat.isFloated/isLeft/isRight follow the CSS keywords")
    void floatEnumQueries() {
        assertFalse(TaffyFloat.NONE.isFloated());
        assertFalse(TaffyFloat.NONE.isLeft());
        assertFalse(TaffyFloat.NONE.isRight());

        assertTrue(TaffyFloat.LEFT.isFloated());
        assertTrue(TaffyFloat.LEFT.isLeft());
        assertFalse(TaffyFloat.LEFT.isRight());

        assertTrue(TaffyFloat.RIGHT.isFloated());
        assertFalse(TaffyFloat.RIGHT.isLeft());
        assertTrue(TaffyFloat.RIGHT.isRight());
    }

    // === Clear ===

    @Test
    @DisplayName("Clear exposes NONE/LEFT/RIGHT/BOTH and the CSS default NONE")
    void clearEnumValuesAndDefault() {
        assertEquals(4, Clear.values().length);
        assertSame(Clear.NONE, Clear.valueOf("NONE"));
        assertSame(Clear.LEFT, Clear.valueOf("LEFT"));
        assertSame(Clear.RIGHT, Clear.valueOf("RIGHT"));
        assertSame(Clear.BOTH, Clear.valueOf("BOTH"));

        TaffyStyle style = new TaffyStyle();
        assertSame(Clear.NONE, style.clear);
        assertSame(Clear.NONE, style.getClear());
    }

    @Test
    @DisplayName("Clear.clearsLeft/clearsRight cover the BOTH shorthand")
    void clearEnumQueries() {
        assertFalse(Clear.NONE.clearsLeft());
        assertFalse(Clear.NONE.clearsRight());

        assertTrue(Clear.LEFT.clearsLeft());
        assertFalse(Clear.LEFT.clearsRight());

        assertFalse(Clear.RIGHT.clearsLeft());
        assertTrue(Clear.RIGHT.clearsRight());

        assertTrue(Clear.BOTH.clearsLeft());
        assertTrue(Clear.BOTH.clearsRight());
    }

    // === TaffyStyle fields and copy() ===

    @Test
    @DisplayName("TaffyStyle.copy() preserves float_ and clear")
    void copyPreservesFloatAndClear() {
        TaffyStyle style = new TaffyStyle();
        style.float_ = TaffyFloat.RIGHT;
        style.clear = Clear.BOTH;

        TaffyStyle copy = style.copy();

        assertSame(TaffyFloat.RIGHT, copy.float_);
        assertSame(TaffyFloat.RIGHT, copy.getFloat());
        assertSame(Clear.BOTH, copy.clear);
        assertSame(Clear.BOTH, copy.getClear());
    }

    @Test
    @DisplayName("TaffyStyle.copy() keeps the CSS defaults when the fields are untouched")
    void copyPreservesDefaults() {
        TaffyStyle copy = new TaffyStyle().copy();

        assertSame(TaffyFloat.NONE, copy.float_);
        assertSame(Clear.NONE, copy.clear);
    }

    // === FloatContext: recording and side sums ===

    @Test
    @DisplayName("A new FloatContext has no floats and zero intrinsic accounting")
    void floatContextStartsEmpty() {
        FloatContext context = new FloatContext();

        assertFalse(context.hasFloats());
        assertFalse(context.hasActiveFloats(0.0f));
        assertTrue(context.leftFloats().isEmpty());
        assertTrue(context.rightFloats().isEmpty());
        assertArrayEquals(new float[] {0.0f, 0.0f}, context.sideSums(), EPSILON);
        assertEquals(0.0f, context.sideSum(FloatContext.Direction.LEFT), EPSILON);
        assertEquals(0.0f, context.sideSum(FloatContext.Direction.RIGHT), EPSILON);
        assertEquals(0.0f, context.widest(), EPSILON);
        assertNull(context.clearedThreshold(Clear.NONE));
        assertNull(context.clearedThreshold(Clear.BOTH));
    }

    @Test
    @DisplayName("recordFloat stores boxes on each side and updates side sums and widest")
    void recordFloatUpdatesAccounting() {
        FloatContext context = new FloatContext();
        context.recordFloat(50.0f, 20.0f, 0.0f, 0.0f,
                            FloatContext.Direction.LEFT, Clear.NONE);
        context.recordFloat(new FloatContext.PlacedFloatedBox(80.0f, 30.0f, 5.0f, 10.0f),
                            FloatContext.Direction.RIGHT, Clear.NONE);

        assertTrue(context.hasFloats());
        assertEquals(1, context.leftFloats().size());
        assertEquals(1, context.rightFloats().size());
        assertEquals(50.0f, context.leftFloats().get(0).width(), EPSILON);
        assertEquals(5.0f, context.rightFloats().get(0).xInset(), EPSILON);
        assertEquals(50.0f, context.sideSum(FloatContext.Direction.LEFT), EPSILON);
        assertEquals(80.0f, context.sideSum(FloatContext.Direction.RIGHT), EPSILON);
        assertArrayEquals(new float[] {50.0f, 80.0f}, context.sideSums(), EPSILON);
        assertEquals(80.0f, context.widest(), EPSILON);
    }

    @Test
    @DisplayName("clear resets the cleared side sums before the new float is added")
    void clearResetsSideSums() {
        FloatContext context = new FloatContext();
        context.recordFloat(50.0f, 10.0f, 0.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        context.recordFloat(80.0f, 10.0f, 0.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        context.recordFloat(30.0f, 10.0f, 0.0f, 20.0f, FloatContext.Direction.LEFT, Clear.LEFT);
        assertEquals(30.0f, context.sideSum(FloatContext.Direction.LEFT), EPSILON);
        assertEquals(80.0f, context.sideSum(FloatContext.Direction.RIGHT), EPSILON);
        assertEquals(80.0f, context.widest(), EPSILON);

        context.recordFloat(40.0f, 10.0f, 0.0f, 30.0f, FloatContext.Direction.RIGHT, Clear.BOTH);
        assertEquals(0.0f, context.sideSum(FloatContext.Direction.LEFT), EPSILON);
        assertEquals(40.0f, context.sideSum(FloatContext.Direction.RIGHT), EPSILON);
        assertArrayEquals(new float[] {0.0f, 40.0f}, context.sideSums(), EPSILON);
        assertEquals(80.0f, context.widest(), EPSILON);
    }

    @Test
    @DisplayName("hasActiveFloats uses the lowest recorded float bottom")
    void hasActiveFloatsUsesFloatBottoms() {
        FloatContext context = new FloatContext();
        context.recordFloat(50.0f, 20.0f, 0.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        assertTrue(context.hasActiveFloats(0.0f));
        assertTrue(context.hasActiveFloats(19.0f));
        assertFalse(context.hasActiveFloats(20.0f));
        assertFalse(context.hasActiveFloats(100.0f));

        context.recordFloat(40.0f, 10.0f, 0.0f, 30.0f, FloatContext.Direction.RIGHT, Clear.NONE);
        assertTrue(context.hasActiveFloats(25.0f));
        assertFalse(context.hasActiveFloats(40.0f));
    }

    @Test
    @DisplayName("clearedThreshold reports the lowest cleared float bottom")
    void clearedThresholdUsesRecordedBottoms() {
        FloatContext context = new FloatContext();
        context.recordFloat(50.0f, 20.0f, 0.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        context.recordFloat(40.0f, 10.0f, 0.0f, 30.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        assertEquals(20.0f, context.clearedThreshold(Clear.LEFT), EPSILON);
        assertEquals(40.0f, context.clearedThreshold(Clear.RIGHT), EPSILON);
        assertEquals(40.0f, context.clearedThreshold(Clear.BOTH), EPSILON);
        assertNull(context.clearedThreshold(Clear.NONE));
    }

    // === FloatContext: BFC regions ===

    @Test
    @DisplayName("regions are appended in order and can be queried by y")
    void regionsInsertAndQuery() {
        FloatContext context = new FloatContext();
        FloatContext.BfcRegion first = new FloatContext.BfcRegion(0.0f, 20.0f, 50.0f, 0.0f);
        FloatContext.BfcRegion second = new FloatContext.BfcRegion(20.0f, 40.0f, 0.0f, 40.0f);
        context.addRegion(first);
        context.addRegion(second);

        assertEquals(2, context.regions().size());
        assertEquals(0, context.regionIndexAt(0.0f));
        assertEquals(0, context.regionIndexAt(19.999f));
        assertEquals(1, context.regionIndexAt(20.0f));
        assertEquals(1, context.regionIndexAt(39.999f));
        assertEquals(FloatContext.NO_REGION, context.regionIndexAt(40.0f));
        assertEquals(first, context.regionAt(10.0f));
        assertEquals(second, context.regionAt(30.0f));
        assertNull(context.regionAt(40.0f));
        assertNull(context.regionAt(-1.0f));

        assertEquals(20.0f, first.height(), EPSILON);
        assertEquals(150.0f, first.width(200.0f), EPSILON);
        assertTrue(first.containsY(0.0f));
        assertFalse(first.containsY(20.0f));
    }

    @Test
    @DisplayName("region insertion rejects overlap and invalid geometry")
    void regionValidation() {
        FloatContext context = new FloatContext();
        context.addRegion(new FloatContext.BfcRegion(0.0f, 20.0f, 50.0f, 0.0f));

        assertThrows(IllegalArgumentException.class,
                     () -> context.addRegion(new FloatContext.BfcRegion(10.0f, 30.0f, 0.0f, 0.0f)));
        assertThrows(IllegalArgumentException.class,
                     () -> new FloatContext.BfcRegion(30.0f, 20.0f, 0.0f, 0.0f));
        assertThrows(IllegalArgumentException.class,
                     () -> new FloatContext.BfcRegion(0.0f, 20.0f, -1.0f, 0.0f));
        assertThrows(IllegalArgumentException.class,
                     () -> new FloatContext.BfcRegion(0.0f, 20.0f, 0.0f, -1.0f));
    }

    @Test
    @DisplayName("the float lists and region list are unmodifiable views")
    void contextListsAreUnmodifiable() {
        FloatContext context = new FloatContext();
        context.recordFloat(10.0f, 10.0f, 0.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        context.addRegion(new FloatContext.BfcRegion(0.0f, 10.0f, 10.0f, 0.0f));

        assertThrows(UnsupportedOperationException.class,
                     () -> context.leftFloats().add(new FloatContext.PlacedFloatedBox(1.0f, 1.0f, 0.0f, 0.0f)));
        assertThrows(UnsupportedOperationException.class,
                     () -> context.rightFloats().clear());
        assertThrows(UnsupportedOperationException.class, () -> context.regions().clear());
    }

    // === FloatContext: content slots ===

    @Test
    @DisplayName("contentSlot returns the full BFC width when no float is active")
    void contentSlotWithoutActiveFloats() {
        FloatContext context = new FloatContext();
        context.setWidth(200.0f);

        FloatContext.ContentSlot slot = context.contentSlot(10.0f, 15.0f, 25.0f, Clear.NONE);

        assertEquals(FloatContext.NO_REGION, slot.segmentId());
        assertFalse(slot.hasRegion());
        assertEquals(15.0f, slot.x(), EPSILON);
        assertEquals(10.0f, slot.y(), EPSILON);
        assertEquals(160.0f, slot.width(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, slot.height());
    }

    @Test
    @DisplayName("contentSlot applies region insets and the clear threshold")
    void contentSlotUsesRegionsAndClearThreshold() {
        FloatContext context = new FloatContext();
        context.setWidth(200.0f);
        // A 50x20 left float and a 40x60 right float, plus matching BFC regions.
        context.recordFloat(50.0f, 20.0f, 0.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        context.recordFloat(40.0f, 60.0f, 0.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);
        context.addRegion(new FloatContext.BfcRegion(0.0f, 20.0f, 50.0f, 40.0f));
        context.addRegion(new FloatContext.BfcRegion(20.0f, 60.0f, 0.0f, 40.0f));
        context.addRegion(new FloatContext.BfcRegion(60.0f, 80.0f, 0.0f, 0.0f));

        FloatContext.ContentSlot first = context.contentSlot(0.0f, 0.0f, 0.0f, Clear.NONE);
        assertEquals(0, first.segmentId());
        assertTrue(first.hasRegion());
        assertEquals(50.0f, first.x(), EPSILON);
        assertEquals(0.0f, first.y(), EPSILON);
        assertEquals(110.0f, first.width(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, first.height());

        // minY inside the second region: only the right float still constrains the slot.
        FloatContext.ContentSlot second = context.contentSlot(25.0f, 0.0f, 0.0f, Clear.NONE);
        assertEquals(1, second.segmentId());
        assertEquals(0.0f, second.x(), EPSILON);
        assertEquals(25.0f, second.y(), EPSILON);
        assertEquals(160.0f, second.width(), EPSILON);

        // clear:left moves the slot below the 20px left float.
        FloatContext.ContentSlot clearedLeft = context.contentSlot(0.0f, 0.0f, 0.0f, Clear.LEFT);
        assertEquals(1, clearedLeft.segmentId());
        assertEquals(0.0f, clearedLeft.x(), EPSILON);
        assertEquals(20.0f, clearedLeft.y(), EPSILON);
        assertEquals(160.0f, clearedLeft.width(), EPSILON);

        // clear:both moves the slot below the 60px right float into the unconstrained region.
        FloatContext.ContentSlot clearedBoth = context.contentSlot(0.0f, 0.0f, 0.0f, Clear.BOTH);
        assertEquals(2, clearedBoth.segmentId());
        assertEquals(0.0f, clearedBoth.x(), EPSILON);
        assertEquals(60.0f, clearedBoth.y(), EPSILON);
        assertEquals(200.0f, clearedBoth.width(), EPSILON);

        // Caller insets wider than the region insets win.
        FloatContext.ContentSlot callerInsets = context.contentSlot(0.0f, 60.0f, 50.0f, Clear.NONE);
        assertEquals(60.0f, callerInsets.x(), EPSILON);
        assertEquals(90.0f, callerInsets.width(), EPSILON);
    }

    // === BlockContext: offsets, insets and coordinate translation ===

    @Test
    @DisplayName("BlockContext.root() is the zero-offset BFC root")
    void rootContextDefaults() {
        FloatContext floatContext = new FloatContext();
        BlockContext root = BlockContext.root(floatContext);

        assertTrue(root.isBfcRoot());
        assertEquals(0.0f, root.yOffset(), EPSILON);
        assertEquals(0.0f, root.leftInset(), EPSILON);
        assertEquals(0.0f, root.rightInset(), EPSILON);
        assertEquals(0.0f, root.contentBoxLeftInset(), EPSILON);
        assertEquals(0.0f, root.contentBoxRightInset(), EPSILON);
        assertFalse(root.hasFloats());

        root.setWidth(200.0f);
        assertEquals(200.0f, floatContext.availableWidth(), EPSILON);

        BlockContext freshRoot = BlockContext.root();
        freshRoot.setWidth(120.0f);
        assertEquals(120.0f, freshRoot.findContentSlot(0.0f, Clear.NONE).width(), EPSILON);
    }

    @Test
    @DisplayName("subContext accumulates the y-offset and border/content insets")
    void subContextAccumulatesGeometry() {
        BlockContext root = BlockContext.root();
        BlockContext child = root.subContext(30.0f, 10.0f, 20.0f);

        assertFalse(child.isBfcRoot());
        assertEquals(30.0f, child.yOffset(), EPSILON);
        assertEquals(10.0f, child.leftInset(), EPSILON);
        assertEquals(20.0f, child.rightInset(), EPSILON);
        assertEquals(10.0f, child.contentBoxLeftInset(), EPSILON);
        assertEquals(20.0f, child.contentBoxRightInset(), EPSILON);

        child.applyContentBoxInset(5.0f, 7.0f);
        assertEquals(15.0f, child.contentBoxLeftInset(), EPSILON);
        assertEquals(27.0f, child.contentBoxRightInset(), EPSILON);

        BlockContext grandchild = child.subContext(10.0f, 5.0f, 5.0f);
        assertEquals(40.0f, grandchild.yOffset(), EPSILON);
        assertEquals(15.0f, grandchild.leftInset(), EPSILON);
        assertEquals(25.0f, grandchild.rightInset(), EPSILON);
        assertEquals(15.0f, grandchild.contentBoxLeftInset(), EPSILON);
        assertEquals(25.0f, grandchild.contentBoxRightInset(), EPSILON);
    }

    @Test
    @DisplayName("BlockContext.addRegion/regionAt translate between local and BFC coordinates")
    void blockContextRegionRoundTrip() {
        FloatContext floatContext = new FloatContext();
        BlockContext root = BlockContext.root(floatContext);
        BlockContext child = root.subContext(30.0f, 10.0f, 20.0f);

        child.addRegion(0.0f, 20.0f, 15.0f, 5.0f);

        FloatContext.BfcRegion local = child.regionAt(5.0f);
        assertNotNull(local);
        assertEquals(0.0f, local.startY(), EPSILON);
        assertEquals(20.0f, local.endY(), EPSILON);
        assertEquals(15.0f, local.leftInset(), EPSILON);
        assertEquals(5.0f, local.rightInset(), EPSILON);
        assertNull(child.regionAt(20.0f));

        FloatContext.BfcRegion absolute = floatContext.regionAt(35.0f);
        assertNotNull(absolute);
        assertEquals(30.0f, absolute.startY(), EPSILON);
        assertEquals(50.0f, absolute.endY(), EPSILON);
        assertEquals(25.0f, absolute.leftInset(), EPSILON);
        assertEquals(25.0f, absolute.rightInset(), EPSILON);
        assertEquals(absolute, root.regionAt(35.0f));
    }

    @Test
    @DisplayName("regionAt clamps local insets when the BFC region is less inset than the block")
    void blockContextRegionInsetsClampAtZero() {
        FloatContext floatContext = new FloatContext();
        BlockContext root = BlockContext.root(floatContext);
        BlockContext child = root.subContext(10.0f, 30.0f, 40.0f);

        root.addRegion(0.0f, 100.0f, 0.0f, 0.0f);

        FloatContext.BfcRegion local = child.regionAt(15.0f); // BFC y = 25
        assertNotNull(local);
        assertEquals(0.0f, local.leftInset(), EPSILON);
        assertEquals(0.0f, local.rightInset(), EPSILON);
    }

    @Test
    @DisplayName("BlockContext queries use local coordinates for floats, clear and content slots")
    void blockContextQueriesUseLocalCoordinates() {
        FloatContext floatContext = new FloatContext();
        BlockContext root = BlockContext.root(floatContext);
        BlockContext child = root.subContext(30.0f, 10.0f, 20.0f);
        child.applyContentBoxInset(5.0f, 5.0f);
        child.setWidth(200.0f);
        child.addRegion(0.0f, 20.0f, 15.0f, 5.0f);

        assertFalse(child.hasFloats());
        assertFalse(child.hasActiveFloats(0.0f));

        floatContext.recordFloat(new FloatContext.PlacedFloatedBox(50.0f, 40.0f, 0.0f, 30.0f),
                                 FloatContext.Direction.LEFT, Clear.NONE);

        assertTrue(child.hasFloats());
        assertTrue(child.hasActiveFloats(0.0f));
        assertFalse(child.hasActiveFloats(40.0f));
        assertEquals(40.0f, child.clearedThreshold(Clear.LEFT), EPSILON);

        FloatContext.ContentSlot slot = child.findContentSlot(0.0f, Clear.NONE);
        assertEquals(0, slot.segmentId());
        assertEquals(15.0f, slot.x(), EPSILON);
        assertEquals(0.0f, slot.y(), EPSILON);
        assertEquals(150.0f, slot.width(), EPSILON);

        FloatContext.ContentSlot cleared = child.findContentSlot(0.0f, Clear.LEFT);
        assertEquals(FloatContext.NO_REGION, cleared.segmentId());
        assertEquals(5.0f, cleared.x(), EPSILON);
        assertEquals(40.0f, cleared.y(), EPSILON);
        assertEquals(160.0f, cleared.width(), EPSILON);
    }
}
