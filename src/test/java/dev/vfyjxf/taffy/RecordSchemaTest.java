package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.tree.Baselines;
import dev.vfyjxf.taffy.tree.CollapsibleMarginSet;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G9 record-schema contract for the additive T1/T2 components.
 *
 * <p>Asserts that the legacy 1.1.x constructors/accessors still exist bytecode-wise, that the new
 * components default to zero/NaN, that {@link Layout#copy()} carries the new component, and that
 * the component name/type/order shape of the affected records is explicitly registered.</p>
 */
class RecordSchemaTest {

    private static final FloatSize SIZE = new FloatSize(10f, 20f);
    private static final FloatSize CONTENT_SIZE = new FloatSize(30f, 40f);
    private static final FloatSize SCROLLBAR_SIZE = new FloatSize(1f, 2f);
    private static final FloatRect BORDER = FloatRect.of(1f, 2f, 3f, 4f);
    private static final FloatRect PADDING = FloatRect.of(5f, 6f, 7f, 8f);
    private static final FloatRect MARGIN = FloatRect.of(9f, 10f, 11f, 12f);

    // === Layout: legacy constructor and accessors ===

    @Test
    @DisplayName("legacy 8-arg Layout constructor and accessors still work")
    void legacyLayoutConstructorAndAccessorsRemain() throws Exception {
        Layout layout = new Layout(
            3,
            new FloatPoint(1f, 2f),
            SIZE,
            CONTENT_SIZE,
            SCROLLBAR_SIZE,
            BORDER,
            PADDING,
            MARGIN
        );

        assertEquals(3, layout.order());
        assertEquals(new FloatPoint(1f, 2f), layout.location());
        assertEquals(SIZE, layout.size());
        assertEquals(CONTENT_SIZE, layout.contentSize());
        assertEquals(SCROLLBAR_SIZE, layout.scrollbarSize());
        assertEquals(BORDER, layout.border());
        assertEquals(PADDING, layout.padding());
        assertEquals(MARGIN, layout.margin());
        assertEquals(ScrollableOverflow.ZERO, layout.scrollableOverflow(),
            "the legacy constructor must default the additive component to zero overflow");

        Constructor<Layout> legacyConstructor = Layout.class.getConstructor(
            int.class, FloatPoint.class, FloatSize.class, FloatSize.class,
            FloatSize.class, FloatRect.class, FloatRect.class, FloatRect.class
        );
        assertNotNull(legacyConstructor, "JCC sees the old 8-arg constructor as a plain method");
        assertTrue(Modifier.isPublic(legacyConstructor.getModifiers()));
        assertNotNull(Layout.class.getMethod("contentSize"),
            "the legacy contentSize accessor must remain public");
    }

    @Test
    @DisplayName("zero/withOrder Layout constructors leave the new component at zero")
    void layoutZeroConstructorsAndWithOrderUseZeroOverflow() {
        Layout zero = new Layout();
        assertEquals(0, zero.order());
        assertEquals(FloatPoint.zero(), zero.location());
        assertEquals(FloatSize.zero(), zero.size());
        assertEquals(FloatSize.zero(), zero.contentSize());
        assertEquals(ScrollableOverflow.ZERO, zero.scrollableOverflow());
        assertEquals(FloatSize.zero(), zero.scrollbarSize());
        assertEquals(FloatRect.zero(), zero.border());
        assertEquals(FloatRect.zero(), zero.padding());
        assertEquals(FloatRect.zero(), zero.margin());

        Layout ordered = new Layout(7);
        assertEquals(7, ordered.order());
        assertEquals(ScrollableOverflow.ZERO, ordered.scrollableOverflow());

        Layout withOrder = Layout.withOrder(11);
        assertEquals(11, withOrder.order());
        assertEquals(ScrollableOverflow.ZERO, withOrder.scrollableOverflow());
    }

    @Test
    @DisplayName("canonical Layout constructor carries a non-zero scrollable overflow")
    void canonicalLayoutConstructorCarriesScrollableOverflow() {
        ScrollableOverflow overflow = new ScrollableOverflow(FloatRect.of(-1f, 2f, -3f, 4f), true, false);
        Layout layout = new Layout(
            1, new FloatPoint(0f, 0f), SIZE, CONTENT_SIZE, overflow,
            SCROLLBAR_SIZE, BORDER, PADDING, MARGIN
        );

        assertSame(overflow, layout.scrollableOverflow());
        assertEquals(CONTENT_SIZE, layout.contentSize());
    }

    @Test
    @DisplayName("Layout.copy copies the new component and deep-copies geometry")
    void layoutCopyCarriesScrollableOverflow() {
        ScrollableOverflow overflow = new ScrollableOverflow(FloatRect.of(-1f, 2f, -3f, 4f), true, false);
        Layout layout = new Layout(
            2, new FloatPoint(3f, 4f), SIZE, CONTENT_SIZE, overflow,
            SCROLLBAR_SIZE, BORDER, PADDING, MARGIN
        );

        Layout copy = layout.copy();
        assertEquals(layout, copy);
        assertEquals(overflow, copy.scrollableOverflow());
        assertNotSame(overflow, copy.scrollableOverflow());
        assertNotSame(overflow.rect(), copy.scrollableOverflow().rect());
        assertNotSame(layout.location(), copy.location());

        overflow.rect().left = -99f;
        assertEquals(-1f, copy.scrollableOverflow().left(),
            "copy() must not share the mutable overflow rectangle with the source");
    }

    // === LayoutOutput: legacy constructor, factories, hidden defaults ===

    @Test
    @DisplayName("legacy 6-arg LayoutOutput constructor and accessors still work")
    void legacyLayoutOutputConstructorAndAccessorsRemain() throws Exception {
        FloatPoint firstBaseline = new FloatPoint(Float.NaN, 7f);
        CollapsibleMarginSet top = CollapsibleMarginSet.fromMargin(5f);
        CollapsibleMarginSet bottom = CollapsibleMarginSet.fromMargin(-3f);

        LayoutOutput output = new LayoutOutput(SIZE, CONTENT_SIZE, firstBaseline, top, bottom, true);

        assertEquals(SIZE, output.size());
        assertEquals(CONTENT_SIZE, output.contentSize());
        assertSame(firstBaseline, output.firstBaselines());
        assertSame(top, output.topMargin());
        assertSame(bottom, output.bottomMargin());
        assertTrue(output.marginsCanCollapseThrough());
        assertEquals(ScrollableOverflow.ZERO, output.scrollableOverflow(),
            "the legacy constructor must default the additive overflow component to zero");
        assertSame(firstBaseline, output.baselines().first(),
            "the legacy constructor must mirror firstBaselines into baselines.first");
        assertTrue(output.baselines().hasFirst());
        assertFalse(output.baselines().hasLast(), "last has no producer in 1.2");

        Constructor<LayoutOutput> legacyConstructor = LayoutOutput.class.getConstructor(
            FloatSize.class, FloatSize.class, FloatPoint.class,
            CollapsibleMarginSet.class, CollapsibleMarginSet.class, boolean.class
        );
        assertNotNull(legacyConstructor, "JCC sees the old 6-arg constructor as a plain method");
        assertTrue(Modifier.isPublic(legacyConstructor.getModifiers()));
        assertNotNull(LayoutOutput.class.getMethod("firstBaselines"),
            "the legacy firstBaselines accessor must remain public");
    }

    @Test
    @DisplayName("HIDDEN/DEFAULT use an empty overflow and NONE baselines")
    void hiddenAndDefaultUseNoneBaselines() {
        LayoutOutput hidden = LayoutOutput.HIDDEN;
        assertSame(hidden, LayoutOutput.hidden());
        assertSame(hidden, LayoutOutput.DEFAULT);

        assertEquals(FloatSize.zero(), hidden.size());
        assertEquals(FloatSize.zero(), hidden.contentSize());
        assertEquals(ScrollableOverflow.ZERO, hidden.scrollableOverflow());
        assertTrue(Float.isNaN(hidden.firstBaselines().x));
        assertTrue(Float.isNaN(hidden.firstBaselines().y));
        assertEquals(Baselines.NONE, hidden.baselines());
        assertFalse(hidden.baselines().hasFirst());
        assertFalse(hidden.baselines().hasLast());
        assertEquals(CollapsibleMarginSet.ZERO, hidden.topMargin());
        assertEquals(CollapsibleMarginSet.ZERO, hidden.bottomMargin());
        assertFalse(hidden.marginsCanCollapseThrough());

        assertEquals(hidden, LayoutOutput.fromOuterSize(FloatSize.zero()),
            "fromOuterSize must keep the legacy all-zero/NaN shape");
    }

    @Test
    @DisplayName("fromSizesAndBaselines keeps its legacy signature and mirrors first")
    void layoutOutputFactoriesKeepLegacySignatures() {
        FloatPoint firstBaseline = new FloatPoint(Float.NaN, 12f);

        LayoutOutput fromBoth = LayoutOutput.fromSizesAndBaselines(SIZE, CONTENT_SIZE, firstBaseline);
        assertSame(firstBaseline, fromBoth.firstBaselines());
        assertSame(firstBaseline, fromBoth.baselines().first());
        assertFalse(fromBoth.baselines().hasLast());
        assertEquals(ScrollableOverflow.ZERO, fromBoth.scrollableOverflow());

        LayoutOutput fromSizes = LayoutOutput.fromSizes(SIZE, CONTENT_SIZE);
        assertEquals(CONTENT_SIZE, fromSizes.contentSize());
        assertTrue(Float.isNaN(fromSizes.firstBaselines().x));
        assertTrue(Float.isNaN(fromSizes.firstBaselines().y));
        assertFalse(fromSizes.baselines().hasFirst());
        assertFalse(fromSizes.baselines().hasLast());
        assertEquals(ScrollableOverflow.ZERO, fromSizes.scrollableOverflow());

        LayoutOutput fromOuter = LayoutOutput.fromOuterSize(SIZE);
        assertEquals(SIZE, fromOuter.size());
        assertEquals(FloatSize.zero(), fromOuter.contentSize());
        assertEquals(ScrollableOverflow.ZERO, fromOuter.scrollableOverflow());
    }

    @Test
    @DisplayName("canonical LayoutOutput constructor carries both new components")
    void canonicalLayoutOutputConstructorCarriesNewComponents() {
        ScrollableOverflow overflow = new ScrollableOverflow(FloatRect.of(0f, 5f, 0f, 6f), false, true);
        Baselines baselines = new Baselines(new FloatPoint(Float.NaN, 8f), new FloatPoint(Float.NaN, 9f));
        FloatPoint firstBaseline = new FloatPoint(Float.NaN, 8f);

        LayoutOutput output = new LayoutOutput(
            SIZE, CONTENT_SIZE, overflow, firstBaseline, baselines,
            CollapsibleMarginSet.ZERO, CollapsibleMarginSet.ZERO, false
        );

        assertSame(overflow, output.scrollableOverflow());
        assertSame(baselines, output.baselines());
        assertSame(firstBaseline, output.firstBaselines());
    }

    // === Baselines / ScrollableOverflow semantics ===

    @Test
    @DisplayName("Baselines models four values with NaN meaning absent")
    void baselinesFourValueSemantics() {
        assertFalse(Baselines.NONE.hasFirst());
        assertFalse(Baselines.NONE.hasLast());

        Baselines firstOnly = Baselines.fromFirst(new FloatPoint(Float.NaN, 4f));
        assertTrue(firstOnly.hasFirst());
        assertFalse(firstOnly.hasLast());
        assertTrue(Float.isNaN(firstOnly.last().x));
        assertTrue(Float.isNaN(firstOnly.last().y));

        Baselines both = new Baselines(new FloatPoint(Float.NaN, 1f), new FloatPoint(Float.NaN, 2f));
        assertTrue(both.hasFirst());
        assertTrue(both.hasLast());
        assertEquals(1f, both.first().y);
        assertEquals(2f, both.last().y);
    }

    @Test
    @DisplayName("ScrollableOverflow exposes physical sides and transposes flags")
    void scrollableOverflowPerSideAccessorsAndTranspose() {
        ScrollableOverflow overflow = new ScrollableOverflow(FloatRect.of(-1f, 2f, -3f, 4f), true, false);

        assertEquals(-1f, overflow.left());
        assertEquals(2f, overflow.right());
        assertEquals(-3f, overflow.top());
        assertEquals(4f, overflow.bottom());
        assertTrue(overflow.reversedX());
        assertFalse(overflow.reversedY());

        ScrollableOverflow transposed = overflow.transpose();
        assertEquals(new FloatRect(-3f, 4f, -1f, 2f), transposed.rect());
        assertFalse(transposed.reversedX());
        assertTrue(transposed.reversedY());
        assertEquals(overflow, transposed.transpose(), "transpose must be its own inverse");
    }

    @Test
    @DisplayName("ScrollableOverflow.round rounds away from zero and preserves -0.0")
    void scrollableOverflowRoundPreservesNegativeZero() {
        ScrollableOverflow overflow = new ScrollableOverflow(
            new FloatRect(-0.0f, 1.5f, -2.5f, 2.4f), false, true
        ).round();

        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(overflow.left()),
            "rounding -0.0 must not turn it into +0.0");
        assertEquals(2f, overflow.right(), 0f);
        assertEquals(-3f, overflow.top(), 0f);
        assertEquals(2f, overflow.bottom(), 0f);
        assertFalse(overflow.reversedX());
        assertTrue(overflow.reversedY());
    }

    // === G9 record schema ===

    @Test
    @DisplayName("G9: Layout/LayoutOutput schema is registered component by component")
    void layoutRecordSchemaIsRegistered() {
        assertRecordSchema(Layout.class,
            List.of("order", "location", "size", "contentSize", "scrollableOverflow",
                "scrollbarSize", "border", "padding", "margin"),
            List.of(int.class, FloatPoint.class, FloatSize.class, FloatSize.class,
                ScrollableOverflow.class, FloatSize.class, FloatRect.class, FloatRect.class,
                FloatRect.class));

        assertRecordSchema(LayoutOutput.class,
            List.of("size", "contentSize", "scrollableOverflow", "firstBaselines", "baselines",
                "topMargin", "bottomMargin", "marginsCanCollapseThrough"),
            List.of(FloatSize.class, FloatSize.class, ScrollableOverflow.class, FloatPoint.class,
                Baselines.class, CollapsibleMarginSet.class, CollapsibleMarginSet.class, boolean.class));

        assertRecordSchema(Baselines.class,
            List.of("first", "last"),
            List.of(FloatPoint.class, FloatPoint.class));

        assertRecordSchema(ScrollableOverflow.class,
            List.of("rect", "reversedX", "reversedY"),
            List.of(FloatRect.class, boolean.class, boolean.class));
    }

    private static void assertRecordSchema(Class<?> type, List<String> names, List<Class<?>> types) {
        assertTrue(type.isRecord(), type.getSimpleName() + " must stay a record");
        RecordComponent[] components = type.getRecordComponents();
        assertNotNull(components);
        assertEquals(names.size(), components.length, type.getSimpleName() + " component count");
        for (int i = 0; i < components.length; i++) {
            assertEquals(names.get(i), components[i].getName(),
                type.getSimpleName() + " component #" + i + " name");
            assertEquals(types.get(i), components[i].getType(),
                type.getSimpleName() + " component #" + i + " type");
            assertTrue(Modifier.isPublic(components[i].getAccessor().getModifiers()),
                type.getSimpleName() + "." + names.get(i) + " accessor must be public");
        }
    }
}
