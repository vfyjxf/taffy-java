package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Baselines;
import dev.vfyjxf.taffy.tree.CollapsibleMarginSet;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyException;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureContext;
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.MeasureFuncAdapter;
import dev.vfyjxf.taffy.util.MeasureFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the 1.2 measure-callback contract (S1, alignment item BL-09):
 * {@link MeasureFunction}, {@link MeasureContext}, {@link MeasureFuncAdapter} and their wiring in
 * {@link TaffyTree} and {@link LayoutComputer}.
 *
 * <p>The legacy {@link MeasureFunc} API must keep behaving exactly as before, the new callback must
 * receive style/node id/context, and a new callback's size plus baselines/scrollable overflow must
 * reach the layout output.</p>
 */
public class MeasureFunctionTest {

    private static final float EPSILON = 0.01f;

    private static TaffyStyle paddedAutoHeightStyle(float padding) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        style.padding = TaffyRect.all(LengthPercentage.length(padding));
        return style;
    }

    private static LayoutOutput measuredOutput(FloatSize size,
                                               FloatPoint firstBaseline,
                                               FloatPoint lastBaseline,
                                               ScrollableOverflow scrollableOverflow) {
        return new LayoutOutput(
            size,
            FloatSize.zero(),
            scrollableOverflow,
            firstBaseline,
            new Baselines(firstBaseline, lastBaseline),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            false
        );
    }

    @Test
    @DisplayName("measure_function_receives_style_node_id_context_and_content_box_input")
    void measureFunctionReceivesStyleNodeIdContextAndContentBoxInput() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = paddedAutoHeightStyle(10.0f);
        Object nodeContext = new Object();
        AtomicReference<MeasureContext> seen = new AtomicReference<>();

        NodeId node = tree.newLeafWithMeasureFunction(style, context -> {
            seen.set(context);
            return LayoutOutput.fromOuterSize(new FloatSize(80.0f, 30.0f));
        });
        tree.setNodeContext(node, nodeContext);

        tree.computeLayout(node, TaffySize.maxContent());

        MeasureContext context = seen.get();
        assertNotNull(context);
        assertSame(style, context.style(), "the callback must receive the node's style");
        assertEquals(node, context.nodeId());
        assertSame(nodeContext, context.nodeContext(), "the callback must receive the node context");

        // The input is the content-box derived input, not the raw layout input: the style width is
        // 100 and the node has 10px of padding on each side.
        assertEquals(80.0f, context.input().knownDimensions().width, EPSILON);
        assertTrue(Float.isNaN(context.input().knownDimensions().height));

        Layout layout = tree.getLayout(node);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(50.0f, layout.size().height, EPSILON, "measured content height + padding");
    }

    @Test
    @DisplayName("measure_function_size_is_content_size_padding_and_border_are_added")
    void measureFunctionSizeIsContentSize() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = paddedAutoHeightStyle(10.0f);
        style.border = TaffyRect.all(LengthPercentage.length(5.0f));

        NodeId node = tree.newLeafWithMeasureFunction(style, context -> {
            // Content-box input: 100 - 2*10 - 2*5 = 70 wide.
            assertEquals(70.0f, context.input().knownDimensions().width, EPSILON);
            return LayoutOutput.fromOuterSize(new FloatSize(70.0f, 20.0f));
        });

        tree.computeLayout(node, TaffySize.maxContent());

        Layout layout = tree.getLayout(node);
        assertEquals(100.0f, layout.size().width, EPSILON, "content 70 + padding 20 + border 10");
        assertEquals(50.0f, layout.size().height, EPSILON, "content 20 + padding 20 + border 10");
        // The legacy contentSize component keeps its historical formula (measured + padding only).
        assertEquals(90.0f, layout.contentSize().width, EPSILON);
        assertEquals(40.0f, layout.contentSize().height, EPSILON);
    }

    @Test
    @DisplayName("measure_function_baselines_and_scrollable_overflow_reach_layout_output")
    void measureFunctionBaselinesAndScrollableOverflowReachLayoutOutput() {
        TaffyTree tree = new TaffyTree();
        FloatPoint firstBaseline = new FloatPoint(Float.NaN, 12.0f);
        FloatPoint lastBaseline = new FloatPoint(Float.NaN, 18.0f);
        ScrollableOverflow overflow = new ScrollableOverflow(new FloatRect(0.0f, 60.0f, 0.0f, 25.0f), false, false);

        NodeId node = tree.newLeafWithMeasureFunction(new TaffyStyle(), context ->
            measuredOutput(new FloatSize(50.0f, 20.0f), firstBaseline, lastBaseline, overflow)
        );

        LayoutOutput output = new LayoutComputer(tree, null, null).performChildLayout(
            node,
            new FloatSize(Float.NaN, Float.NaN),
            new FloatSize(Float.NaN, Float.NaN),
            TaffySize.maxContent(),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );

        assertEquals(50.0f, output.size().width, EPSILON);
        assertEquals(20.0f, output.size().height, EPSILON);
        assertTrue(output.baselines().hasFirst(), "first baseline must survive the leaf path");
        assertTrue(output.baselines().hasLast(), "last baseline must survive the leaf path");
        assertEquals(12.0f, output.baselines().first().y, EPSILON);
        assertEquals(18.0f, output.baselines().last().y, EPSILON);
        // The legacy firstBaselines component is kept in sync with Baselines.first().
        assertEquals(12.0f, output.firstBaselines().y, EPSILON);
        assertEquals(60.0f, output.scrollableOverflow().right(), EPSILON);
        assertEquals(25.0f, output.scrollableOverflow().bottom(), EPSILON);
    }

    @Test
    @DisplayName("legacy_measure_func_keeps_legacy_behaviour")
    void legacyMeasureFuncKeepsLegacyBehaviour() {
        TaffyTree legacyTree = new TaffyTree();
        MeasureFunc legacy = (knownDimensions, availableSpace) -> new FloatSize(30.0f, 45.0f);
        NodeId legacyNode = legacyTree.newLeafWithMeasure(new TaffyStyle(), legacy);

        assertSame(legacy, legacyTree.getMeasureFunc(legacyNode));
        assertNull(legacyTree.getMeasureFunction(legacyNode));

        legacyTree.computeLayout(legacyNode, TaffySize.maxContent());
        Layout legacyLayout = legacyTree.getLayout(legacyNode);
        assertEquals(30.0f, legacyLayout.size().width, EPSILON);
        assertEquals(45.0f, legacyLayout.size().height, EPSILON);

        // The same content size through the new callback produces the same layout.
        TaffyTree newTree = new TaffyTree();
        NodeId newNode = newTree.newLeafWithMeasureFunction(new TaffyStyle(), context ->
            LayoutOutput.fromOuterSize(new FloatSize(30.0f, 45.0f))
        );
        newTree.computeLayout(newNode, TaffySize.maxContent());
        Layout newLayout = newTree.getLayout(newNode);
        assertEquals(legacyLayout.size().width, newLayout.size().width, EPSILON);
        assertEquals(legacyLayout.size().height, newLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("default_measure_function_applies_to_leaves_without_a_callback")
    void defaultMeasureFunctionAppliesToLeavesWithoutCallback() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(new TaffyStyle());

        tree.computeLayoutWithMeasureFunction(node, TaffySize.maxContent(), context -> {
            assertEquals(node, context.nodeId());
            return LayoutOutput.fromOuterSize(new FloatSize(70.0f, 25.0f));
        });

        assertEquals(70.0f, tree.getLayout(node).size().width, EPSILON);
        assertEquals(25.0f, tree.getLayout(node).size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_selection_order_is_node_callbacks_before_defaults")
    void measureSelectionOrder() {
        MeasureFunction defaultMeasure = context ->
            LayoutOutput.fromOuterSize(new FloatSize(70.0f, 25.0f));

        TaffyTree tree = new TaffyTree();

        // A per-node legacy callback wins over a default new-style callback: tree-level defaults
        // are only a fallback and must never override a callback the node registered itself.
        NodeId legacyNode = tree.newLeafWithMeasure(new TaffyStyle(),
            (knownDimensions, availableSpace) -> new FloatSize(100.0f, 100.0f));
        tree.computeLayoutWithMeasureFunction(legacyNode, TaffySize.maxContent(), defaultMeasure);
        assertEquals(100.0f, tree.getLayout(legacyNode).size().width, EPSILON);
        assertEquals(100.0f, tree.getLayout(legacyNode).size().height, EPSILON);

        // A per-node new-style callback wins over the default new-style callback.
        NodeId newNode = tree.newLeafWithMeasureFunction(new TaffyStyle(), context ->
            LayoutOutput.fromOuterSize(new FloatSize(10.0f, 10.0f))
        );
        tree.computeLayoutWithMeasureFunction(newNode, TaffySize.maxContent(), defaultMeasure);
        assertEquals(10.0f, tree.getLayout(newNode).size().width, EPSILON);
        assertEquals(10.0f, tree.getLayout(newNode).size().height, EPSILON);

        // Among the defaults themselves, the new-style default wins over the legacy default.
        TaffyTree defaultsTree = new TaffyTree();
        NodeId plainNode = defaultsTree.newLeaf(new TaffyStyle());
        LayoutOutput defaultOutput = new LayoutComputer(
            defaultsTree,
            (knownDimensions, availableSpace) -> new FloatSize(40.0f, 40.0f),
            defaultMeasure
        ).performChildLayout(
            plainNode,
            new FloatSize(Float.NaN, Float.NaN),
            new FloatSize(Float.NaN, Float.NaN),
            TaffySize.maxContent(),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );
        assertEquals(70.0f, defaultOutput.size().width, EPSILON);
        assertEquals(25.0f, defaultOutput.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_function_returning_null_is_rejected_with_a_clear_error")
    void measureFunctionReturningNullIsRejected() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeafWithMeasureFunction(new TaffyStyle(), context -> null);

        NullPointerException error = assertThrows(NullPointerException.class,
            () -> tree.computeLayout(node, TaffySize.maxContent()));
        assertEquals("MeasureFunction returned null", error.getMessage());
    }

    @Test
    @DisplayName("legacy_measure_func_and_adapter_produce_identical_arguments_and_results")
    void legacyMeasureFuncAndAdapterAreEquivalent() {
        CapturingMeasure legacyCapture = new CapturingMeasure(new FloatSize(80.0f, 30.0f));
        TaffyTree legacyTree = new TaffyTree();
        NodeId legacyNode = legacyTree.newLeafWithMeasure(paddedAutoHeightStyle(10.0f), legacyCapture);
        legacyTree.computeLayout(legacyNode, TaffySize.maxContent());

        CapturingMeasure adapterCapture = new CapturingMeasure(new FloatSize(80.0f, 30.0f));
        TaffyTree adapterTree = new TaffyTree();
        NodeId adapterNode = adapterTree.newLeafWithMeasureFunction(
            paddedAutoHeightStyle(10.0f), new MeasureFuncAdapter(adapterCapture));
        adapterTree.computeLayout(adapterNode, TaffySize.maxContent());

        assertEquals(1, legacyCapture.knownWidths.size());
        assertEquals(1, adapterCapture.knownWidths.size());
        assertEquals(80.0f, legacyCapture.knownWidths.get(0), EPSILON);
        assertEquals(80.0f, adapterCapture.knownWidths.get(0), EPSILON);
        assertTrue(Float.isNaN(legacyCapture.knownHeights.get(0)));
        assertTrue(Float.isNaN(adapterCapture.knownHeights.get(0)));
        assertEquals(legacyCapture.availableSpaces, adapterCapture.availableSpaces,
            "the adapter must forward the exact available space the legacy path used");

        Layout legacyLayout = legacyTree.getLayout(legacyNode);
        Layout adapterLayout = adapterTree.getLayout(adapterNode);
        assertEquals(legacyLayout.size().width, adapterLayout.size().width, EPSILON);
        assertEquals(legacyLayout.size().height, adapterLayout.size().height, EPSILON);
        assertEquals(100.0f, legacyLayout.size().width, EPSILON);
        assertEquals(50.0f, legacyLayout.size().height, EPSILON);

        // The adapter is a new-style callback; the legacy slot of that node stays empty.
        assertNotNull(adapterTree.getMeasureFunction(adapterNode));
        assertNull(adapterTree.getMeasureFunc(adapterNode));
    }

    @Test
    @DisplayName("node_context_can_be_set_and_cleared")
    void nodeContextCanBeSetAndCleared() {
        TaffyTree tree = new TaffyTree();
        List<Object> seenContexts = new ArrayList<>();
        NodeId node = tree.newLeafWithMeasureFunction(new TaffyStyle(), context -> {
            seenContexts.add(context.nodeContext());
            return LayoutOutput.fromOuterSize(new FloatSize(1.0f, 1.0f));
        });

        assertNull(tree.getNodeContext(node));

        tree.computeLayout(node, TaffySize.maxContent());
        assertEquals(1, seenContexts.size());
        assertNull(seenContexts.get(0));

        Object context = new Object();
        tree.setNodeContext(node, context);
        assertSame(context, tree.getNodeContext(node));
        tree.computeLayout(node, TaffySize.maxContent());
        assertEquals(2, seenContexts.size());
        assertSame(context, seenContexts.get(1));

        tree.setNodeContext(node, null);
        assertNull(tree.getNodeContext(node));
        tree.computeLayout(node, TaffySize.maxContent());
        assertEquals(3, seenContexts.size());
        assertNull(seenContexts.get(2));

        assertThrows(TaffyException.class, () -> tree.setNodeContext(NodeId.of(9999L), context));
    }

    @Test
    @DisplayName("legacy_and_new_measure_slots_are_mutually_exclusive")
    void legacyAndNewMeasureSlotsAreMutuallyExclusive() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(new TaffyStyle());

        MeasureFunc legacy = (knownDimensions, availableSpace) -> new FloatSize(5.0f, 5.0f);
        tree.setMeasureFunc(node, legacy);
        assertSame(legacy, tree.getMeasureFunc(node));
        assertNull(tree.getMeasureFunction(node));

        MeasureFunction measureFunction = context ->
            LayoutOutput.fromOuterSize(new FloatSize(9.0f, 9.0f));
        tree.setMeasureFunction(node, measureFunction);
        assertSame(measureFunction, tree.getMeasureFunction(node));
        assertNull(tree.getMeasureFunc(node), "setting a new-style callback clears the legacy slot");

        tree.computeLayout(node, TaffySize.maxContent());
        assertEquals(9.0f, tree.getLayout(node).size().width, EPSILON);

        tree.setMeasureFunction(node, null);
        assertNull(tree.getMeasureFunction(node));
        assertNull(tree.getMeasureFunc(node));

        MeasureFunc legacyAgain = (knownDimensions, availableSpace) -> new FloatSize(6.0f, 6.0f);
        tree.setMeasureFunction(node, measureFunction);
        tree.setMeasureFunc(node, legacyAgain);
        assertSame(legacyAgain, tree.getMeasureFunc(node));
        assertNull(tree.getMeasureFunction(node), "setting a legacy callback clears the new slot");

        tree.computeLayout(node, TaffySize.maxContent());
        assertEquals(6.0f, tree.getLayout(node).size().width, EPSILON);

        assertFalse(tree.containsNode(NodeId.of(123456L)));
        assertThrows(TaffyException.class, () -> tree.setMeasureFunction(NodeId.of(123456L), measureFunction));
        assertThrows(TaffyException.class, () -> tree.setMeasureFunc(NodeId.of(123456L), legacy));
    }

    /**
     * A legacy measure function that records the arguments it receives.
     */
    private static final class CapturingMeasure implements MeasureFunc {

        private final FloatSize result;
        private final List<Float> knownWidths = new ArrayList<>();
        private final List<Float> knownHeights = new ArrayList<>();
        private final List<String> availableSpaces = new ArrayList<>();

        private CapturingMeasure(FloatSize result) {
            this.result = result;
        }

        @Override
        public FloatSize measure(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
            knownWidths.add(knownDimensions.width);
            knownHeights.add(knownDimensions.height);
            availableSpaces.add(availableSpace.width + "|" + availableSpace.height);
            return result;
        }
    }
}
