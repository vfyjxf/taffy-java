package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutCache;
import dev.vfyjxf.taffy.tree.LayoutInput;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.RequestedAxis;
import dev.vfyjxf.taffy.tree.RunMode;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * P17: the final layout cache entry compares one exact packed key instead of the key fields one
 * by one.
 *
 * <p>The packed key must remain a <em>complete</em> key: it keeps the known-dimension flag and
 * the exact float bits of every value, so two inputs hit only when the old field-by-field
 * comparison would have said yes. These tests pin the whole key (known axes, available-space type
 * and value, parent size), the {@code Float.compare} edge cases ({@code NaN}, {@code -0.0f}), the
 * axis order, and the fact that the 32-bit fingerprint is only a fast path and never the key.
 * Rounding is not part of a {@link LayoutCache} key, so a rounding-mode change must not evict a
 * warm cache.
 */
public class LayoutCachePackedKeyP17Test {

    private static final float EPSILON = 0.001f;
    private static final LayoutOutput OUTPUT = LayoutOutput.fromOuterSize(new FloatSize(10f, 10f));
    private static final TaffySize<AvailableSpace> MAX_CONTENT = TaffySize.maxContent();

    // === The whole packed key ===

    @Test
    @DisplayName("final_layout_hits_only_when_the_whole_packed_key_is_equal")
    void finalLayoutHitsOnlyWhenTheWholePackedKeyIsEqual() {
        LayoutInput stored = performLayout(
            new FloatSize(30f, Float.NaN),
            new FloatSize(300f, 200f),
            new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MIN_CONTENT));
        LayoutCache cache = cacheWith(stored);

        assertSame(OUTPUT, cache.get(performLayout(
                new FloatSize(30f, Float.NaN),
                new FloatSize(300f, 200f),
                new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MIN_CONTENT))),
            "fresh input instances with equal values must hit the same entry");

        assertNull(cache.get(performLayout(
                new FloatSize(31f, Float.NaN),
                new FloatSize(300f, 200f),
                new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MIN_CONTENT))),
            "the known width is part of the key");
        assertNull(cache.get(performLayout(
                new FloatSize(30f, Float.NaN),
                new FloatSize(301f, 200f),
                new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MIN_CONTENT))),
            "the parent width is part of the key");
        assertNull(cache.get(performLayout(
                new FloatSize(30f, Float.NaN),
                new FloatSize(300f, 201f),
                new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MIN_CONTENT))),
            "the parent height is part of the key");
        assertNull(cache.get(performLayout(
                new FloatSize(30f, Float.NaN),
                new FloatSize(300f, 200f),
                new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MAX_CONTENT))),
            "the unknown height is keyed on its available space");

        assertSame(OUTPUT, cache.get(performLayout(
                new FloatSize(30f, Float.NaN),
                new FloatSize(300f, 200f),
                new TaffySize<>(AvailableSpace.definite(400f), AvailableSpace.MIN_CONTENT))),
            "a known width ignores the available width (CR-01)");
    }

    @Test
    @DisplayName("unknown_axes_compare_available_space_type_and_value")
    void unknownAxesCompareAvailableSpaceTypeAndValue() {
        LayoutInput stored = performLayout(
            FloatSize.none(),
            FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.MIN_CONTENT));
        LayoutCache cache = cacheWith(stored);

        assertSame(OUTPUT, cache.get(performLayout(
                FloatSize.none(),
                FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.MIN_CONTENT))),
            "equal definite values and the same type must hit");

        assertNull(cache.get(performLayout(
                FloatSize.none(),
                FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(100.5f), AvailableSpace.MIN_CONTENT))),
            "the definite value is part of the key");
        assertNull(cache.get(performLayout(
                FloatSize.none(),
                FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.MAX_CONTENT))),
            "MIN_CONTENT and MAX_CONTENT are different keys");
        assertNull(cache.get(performLayout(
                FloatSize.none(),
                FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.definite(0f)))),
            "a definite space and an indefinite space are different keys");
        assertNull(cache.get(performLayout(
                FloatSize.none(),
                FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(100f), null))),
            "a null available space is different from MIN_CONTENT");
    }

    @Test
    @DisplayName("available_space_value_bits_stay_exact")
    void availableSpaceValueBitsStayExact() {
        LayoutCache nanCache = cacheWith(performLayout(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(Float.NaN), AvailableSpace.MAX_CONTENT)));
        assertSame(OUTPUT, nanCache.get(performLayout(FloatSize.none(), FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(Float.NaN), AvailableSpace.MAX_CONTENT))),
            "floatToIntBits canonicalizes NaNs, matching Float.compare's NaN equality");
        assertNull(nanCache.get(performLayout(FloatSize.none(), FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(0f), AvailableSpace.MAX_CONTENT))),
            "a NaN available value is not zero");

        LayoutCache signedZeroCache = cacheWith(performLayout(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(0f), AvailableSpace.MAX_CONTENT)));
        assertNull(signedZeroCache.get(performLayout(FloatSize.none(), FloatSize.none(),
                new TaffySize<>(AvailableSpace.definite(-0.0f), AvailableSpace.MAX_CONTENT))),
            "AvailableSpace.equals uses Float.compare, so -0.0f is not 0.0f");

        LayoutCache nullCache = cacheWith(performLayout(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(null, null)));
        assertSame(OUTPUT, nullCache.get(performLayout(FloatSize.none(), FloatSize.none(),
                new TaffySize<>(null, null))),
            "two null available spaces still compare equal");
        assertNull(nullCache.get(performLayout(FloatSize.none(), FloatSize.none(),
                new TaffySize<>(AvailableSpace.MAX_CONTENT, null))),
            "null does not equal a MAX_CONTENT instance");
    }

    @Test
    @DisplayName("known_axis_flag_is_part_of_the_key")
    void knownAxisFlagIsPartOfTheKey() {
        LayoutCache cache = cacheWith(performLayout(
            new FloatSize(30f, Float.NaN),
            new FloatSize(300f, 200f),
            new TaffySize<>(AvailableSpace.definite(30f), AvailableSpace.MIN_CONTENT)));

        assertNull(cache.get(performLayout(
                FloatSize.none(),
                new FloatSize(300f, 200f),
                new TaffySize<>(AvailableSpace.definite(30f), AvailableSpace.MIN_CONTENT))),
            "a known dimension and an equal-valued available space are different keys");
        assertNull(cache.get(performLayout(
                new FloatSize(30f, 30f),
                new FloatSize(300f, 200f),
                new TaffySize<>(AvailableSpace.definite(30f), AvailableSpace.MIN_CONTENT))),
            "the known height flag differs from the available-space height");
    }

    @Test
    @DisplayName("known_dimensions_are_axis_ordered_and_signed")
    void knownDimensionsAreAxisOrderedAndSigned() {
        LayoutCache bothKnown = cacheWith(performLayout(
            new FloatSize(10f, 20f), FloatSize.none(), MAX_CONTENT));
        assertSame(OUTPUT, bothKnown.get(performLayout(
                new FloatSize(10f, 20f), FloatSize.none(), TaffySize.minContent())),
            "with both axes known the available space is not part of the key");
        assertNull(bothKnown.get(performLayout(
                new FloatSize(20f, 10f), FloatSize.none(), MAX_CONTENT)),
            "the packed key keeps the axis order of the known dimensions");

        LayoutCache negative = cacheWith(performLayout(
            new FloatSize(-1.5f, Float.NaN), FloatSize.none(), MAX_CONTENT));
        assertSame(OUTPUT, negative.get(performLayout(
                new FloatSize(-1.5f, Float.NaN), FloatSize.none(), MAX_CONTENT)),
            "a negative known dimension hits itself");
        assertNull(negative.get(performLayout(
                new FloatSize(1.5f, Float.NaN), FloatSize.none(), MAX_CONTENT)),
            "the sign bit is part of the key");

        LayoutCache signedZero = cacheWith(performLayout(
            new FloatSize(0f, Float.NaN), FloatSize.none(), MAX_CONTENT));
        assertNull(signedZero.get(performLayout(
                new FloatSize(-0.0f, Float.NaN), FloatSize.none(), MAX_CONTENT)),
            "Float.compare(-0.0f, 0.0f) != 0 for known dimensions");
    }

    @Test
    @DisplayName("parent_size_bits_are_exact_and_axis_ordered")
    void parentSizeBitsAreExactAndAxisOrdered() {
        LayoutCache nanCache = cacheWith(performLayout(
            FloatSize.none(), new FloatSize(Float.NaN, 200f), MAX_CONTENT));
        assertSame(OUTPUT, nanCache.get(performLayout(
                FloatSize.none(), new FloatSize(Float.NaN, 200f), MAX_CONTENT)),
            "every NaN parent dimension compares equal");
        assertNull(nanCache.get(performLayout(
                FloatSize.none(), new FloatSize(0f, 200f), MAX_CONTENT)),
            "a NaN parent dimension is not zero");
        assertNull(nanCache.get(performLayout(
                FloatSize.none(), new FloatSize(Float.NaN, 201f), MAX_CONTENT)),
            "the parent height stays part of the key");

        LayoutCache signedZero = cacheWith(performLayout(
            FloatSize.none(), new FloatSize(0f, 50f), MAX_CONTENT));
        assertNull(signedZero.get(performLayout(
                FloatSize.none(), new FloatSize(-0.0f, 50f), MAX_CONTENT)),
            "Float.compare(-0.0f, 0.0f) != 0 for the parent size");

        LayoutCache ordered = cacheWith(performLayout(
            FloatSize.none(), new FloatSize(30f, 40f), MAX_CONTENT));
        assertNull(ordered.get(performLayout(
                FloatSize.none(), new FloatSize(40f, 30f), MAX_CONTENT)),
            "swapped parent dimensions are a different key");

        LayoutCache negative = cacheWith(performLayout(
            FloatSize.none(), new FloatSize(-1.5f, 40f), MAX_CONTENT));
        assertSame(OUTPUT, negative.get(performLayout(
                FloatSize.none(), new FloatSize(-1.5f, 40f), MAX_CONTENT)),
            "a negative parent width hits itself");
        assertNull(negative.get(performLayout(
                FloatSize.none(), new FloatSize(1.5f, 40f), MAX_CONTENT)),
            "the sign bit is part of the parent key");
    }

    @Test
    @DisplayName("fingerprint_is_not_the_final_layout_key")
    void fingerprintIsNotTheFinalLayoutKey() throws Exception {
        LayoutInput stored = performLayout(
            new FloatSize(30f, Float.NaN), new FloatSize(300f, 200f), MAX_CONTENT);
        LayoutCache cache = cacheWith(stored);
        Object entry = finalLayoutEntry(cache);

        LayoutInput different = performLayout(
            new FloatSize(31f, Float.NaN), new FloatSize(300f, 200f), MAX_CONTENT);
        setField(entry, "keyHash", keyHashOf(different));
        assertNull(cache.get(different),
            "a matching fingerprint must not turn a different packed key into a hit");

        setField(entry, "keyHash", 0x5EEDBEEF);
        assertSame(OUTPUT, cache.get(stored),
            "the final lookup compares the exact packed key and never trusts the fingerprint");
    }

    // === Rounding is not part of the cache key ===

    @Test
    @DisplayName("rounding_mode_is_not_part_of_the_cache_key")
    void roundingModeIsNotPartOfTheCacheKey() {
        TaffyTree tree = new TaffyTree();
        AtomicInteger measureCalls = new AtomicInteger();
        MeasureFunc measure = (knownDimensions, availableSpace) -> {
            measureCalls.incrementAndGet();
            return new FloatSize(10.4f, 7.6f);
        };
        NodeId leaf = tree.newLeafWithMeasure(new TaffyStyle(), measure);
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.alignItems = AlignItems.FLEX_START;
        NodeId root = tree.newWithChildren(rootStyle, leaf);
        TaffySize<AvailableSpace> request =
            TaffySize.of(AvailableSpace.definite(100f), AvailableSpace.definite(100f));

        tree.disableRounding();
        tree.computeLayout(root, request);
        int measured = measureCalls.get();
        Layout unrounded = tree.getLayout(leaf);
        assertNotNull(unrounded);
        assertEquals(10.4f, unrounded.size().width, EPSILON);

        tree.enableRounding();
        tree.computeLayout(root, request);
        assertEquals(measured, measureCalls.get(),
            "a rounding-mode change must not evict the node caches: LayoutInput has no rounding mode");
        Layout rounded = tree.getLayout(leaf);
        assertNotNull(rounded);
        assertEquals(10f, rounded.size().width, EPSILON, "the cached size is rounded on read");
        assertEquals(8f, rounded.size().height, EPSILON, "the cached size is rounded on read");
    }

    // === Helpers ===

    private static LayoutCache cacheWith(LayoutInput stored) {
        LayoutCache cache = new LayoutCache();
        cache.store(stored, OUTPUT);
        return cache;
    }

    private static LayoutInput performLayout(FloatSize knownDimensions, FloatSize parentSize,
                                             TaffySize<AvailableSpace> availableSpace) {
        return new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            knownDimensions,
            parentSize,
            availableSpace,
            TaffyLine.FALSE
        );
    }

    private static Object finalLayoutEntry(LayoutCache cache) throws Exception {
        Field field = LayoutCache.class.getDeclaredField("finalLayoutEntry");
        field.setAccessible(true);
        return field.get(cache);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int keyHashOf(LayoutInput input) throws Exception {
        Method method = LayoutCache.class.getDeclaredMethod(
            "keyHash", FloatSize.class, TaffySize.class, FloatSize.class);
        method.setAccessible(true);
        return (int) method.invoke(null, input.knownDimensions(), input.availableSpace(), input.parentSize());
    }
}
