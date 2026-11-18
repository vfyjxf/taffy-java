package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression tests for the definite final-size (two-stage) layout of absolutely positioned
 * boxes (PA-05, upstream #1120's "measure, then lay out with the definite final size" contract).
 *
 * <p>Scenario: a 300x200 root contains an absolutely positioned block whose width is 100 and
 * whose height is {@code auto} (so it is sized by its content: the 100x100 first child). A second
 * child is {@code height: 50%}. The percentage must resolve against the container's
 * <em>final</em> height (100), so the child must be 50 tall at y = 100. Before PA-05 the
 * absolutely positioned child was laid out exactly once, while its own content size was still
 * indefinite, so the percentage child collapsed to 0 and the container's content size was wrong.
 *
 * <p>Both containing-block implementations are covered: a flex root (the
 * {@code FlexboxComputer} absolute path used by the frozen G0 scenario) and a block root (the
 * {@code BlockComputer} absolute path).
 */
public class AbsoluteFinalSizeTest {

    private static final float EPSILON = 0.1f;

    @Test
    @DisplayName("flex root: content-sized abspos container resolves height:50% against its final height")
    void flexRootContentSizedAbsoluteContainer() {
        TaffyTree tree = new TaffyTree();
        Scenario scenario = buildScenario(tree, TaffyDisplay.FLEX);

        tree.computeLayout(scenario.root(), definiteSpace(300f, 200f));

        assertDefiniteFinalSize(tree, scenario);
    }

    @Test
    @DisplayName("block root: content-sized abspos container resolves height:50% against its final height")
    void blockRootContentSizedAbsoluteContainer() {
        TaffyTree tree = new TaffyTree();
        Scenario scenario = buildScenario(tree, TaffyDisplay.BLOCK);

        tree.computeLayout(scenario.root(), definiteSpace(300f, 200f));

        assertDefiniteFinalSize(tree, scenario);
    }

    @Test
    @DisplayName("relayout after a style change keeps the definite final size (cache regression)")
    void relayoutAfterStyleChangeKeepsDefiniteFinalSize() {
        TaffyTree tree = new TaffyTree();
        Scenario scenario = buildScenario(tree, TaffyDisplay.FLEX);

        tree.computeLayout(scenario.root(), definiteSpace(300f, 200f));
        assertDefiniteFinalSize(tree, scenario);

        // Grow the in-flow sibling that determines the absolutely positioned container's
        // content-derived height from 100 to 120. The percentage child must follow the new
        // definite final size (60 at y = 120), not a cached indefinite-size result.
        TaffyStyle tallerFixedStyle = fixedChildStyle(120f);
        tree.setStyle(scenario.fixedChild(), tallerFixedStyle);

        tree.computeLayout(scenario.root(), definiteSpace(300f, 200f));

        assertEquals(100f, width(tree, scenario.container()), EPSILON, "container width after relayout");
        assertEquals(120f, height(tree, scenario.container()), EPSILON, "container height after relayout");
        assertEquals(120f, height(tree, scenario.fixedChild()), EPSILON, "fixed child height after relayout");
        assertEquals(100f, width(tree, scenario.percentageChild()), EPSILON,
            "percentage child width after relayout");
        assertEquals(60f, height(tree, scenario.percentageChild()), EPSILON,
            "percentage child height after relayout");
        assertEquals(120f, y(tree, scenario.percentageChild()), EPSILON,
            "percentage child top after relayout");
        assertEquals(180f, contentHeight(tree, scenario.container()), EPSILON,
            "container content height after relayout");

        // A style change on a node deep inside the out-of-flow subtree must still dirty its
        // ancestors: the cache entries of the definite-size pass must not be left empty, or
        // TaffyTree.markDirty would stop propagating and the root would be reused from cache.
        TaffyStyle smallerPercentageStyle = new TaffyStyle();
        smallerPercentageStyle.display = TaffyDisplay.BLOCK;
        smallerPercentageStyle.size = size(len(100f), TaffyDimension.percent(0.25f));
        tree.setStyle(scenario.percentageChild(), smallerPercentageStyle);

        tree.computeLayout(scenario.root(), definiteSpace(300f, 200f));

        assertEquals(30f, height(tree, scenario.percentageChild()), EPSILON,
            "percentage child height after a deep style change");
        assertEquals(120f, y(tree, scenario.percentageChild()), EPSILON,
            "percentage child top after a deep style change");
        assertEquals(150f, contentHeight(tree, scenario.container()), EPSILON,
            "container content height after a deep style change");
    }

    // === Assertions ===

    private static void assertDefiniteFinalSize(TaffyTree tree, Scenario scenario) {
        // The absolutely positioned container is width 100 / height auto: its height is the
        // content height of the 100x100 child.
        assertEquals(100f, width(tree, scenario.container()), EPSILON, "abspos container width");
        assertEquals(100f, height(tree, scenario.container()), EPSILON, "abspos container height");
        assertEquals(0f, x(tree, scenario.container()), EPSILON, "abspos container x");
        assertEquals(0f, y(tree, scenario.container()), EPSILON, "abspos container y");

        // The fixed 100x100 child must be unaffected by the definite-size pass.
        assertEquals(100f, width(tree, scenario.fixedChild()), EPSILON, "fixed child width");
        assertEquals(100f, height(tree, scenario.fixedChild()), EPSILON, "fixed child height");
        assertEquals(0f, y(tree, scenario.fixedChild()), EPSILON, "fixed child top");

        // 50% of the container's definite final height (100) = 50, placed below the first child.
        assertEquals(100f, width(tree, scenario.percentageChild()), EPSILON, "percentage child width");
        assertEquals(50f, height(tree, scenario.percentageChild()), EPSILON,
            "percentage child height must resolve against the definite final height");
        assertEquals(100f, y(tree, scenario.percentageChild()), EPSILON, "percentage child top");

        // The container's content size comes from the definite-size pass: 100x100 plus 100x50.
        assertEquals(100f, contentWidth(tree, scenario.container()), EPSILON,
            "abspos container content width");
        assertEquals(150f, contentHeight(tree, scenario.container()), EPSILON,
            "abspos container content height");
    }

    // === Scenario ===

    /**
     * Builds the G0 {@code abspos_content_sized_percentage_height} scenario under a root of the
     * given display type.
     */
    private static Scenario buildScenario(TaffyTree tree, TaffyDisplay rootDisplay) {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = rootDisplay;
        if (rootDisplay == TaffyDisplay.FLEX) {
            rootStyle.alignItems = AlignItems.FLEX_START;
        }
        rootStyle.size = size(len(300f), len(200f));

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.BLOCK;
        containerStyle.position = TaffyPosition.ABSOLUTE;
        containerStyle.size = size(len(100f), TaffyDimension.AUTO);
        containerStyle.inset = TaffyRect.ltrb(
            lpa(0f), lpa(0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);

        TaffyStyle fixedStyle = fixedChildStyle(100f);

        TaffyStyle percentageStyle = new TaffyStyle();
        percentageStyle.display = TaffyDisplay.BLOCK;
        percentageStyle.size = size(len(100f), TaffyDimension.percent(0.5f));

        NodeId fixedChild = tree.newLeaf(fixedStyle);
        NodeId percentageChild = tree.newLeaf(percentageStyle);
        NodeId container = tree.newWithChildren(containerStyle, fixedChild, percentageChild);
        NodeId root = tree.newWithChildren(rootStyle, container);
        return new Scenario(root, container, fixedChild, percentageChild);
    }

    private static TaffyStyle fixedChildStyle(float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = size(len(100f), len(height));
        return style;
    }

    private record Scenario(NodeId root, NodeId container, NodeId fixedChild, NodeId percentageChild) {}

    // === Helpers ===

    private static Layout layout(TaffyTree tree, NodeId node) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "layout of " + node + " must exist");
        return layout;
    }

    private static float width(TaffyTree tree, NodeId node) {
        return layout(tree, node).size().width;
    }

    private static float height(TaffyTree tree, NodeId node) {
        return layout(tree, node).size().height;
    }

    private static float x(TaffyTree tree, NodeId node) {
        return layout(tree, node).location().x;
    }

    private static float y(TaffyTree tree, NodeId node) {
        return layout(tree, node).location().y;
    }

    private static float contentWidth(TaffyTree tree, NodeId node) {
        return layout(tree, node).contentSize().width;
    }

    private static float contentHeight(TaffyTree tree, NodeId node) {
        return layout(tree, node).contentSize().height;
    }

    private static TaffySize<TaffyDimension> size(TaffyDimension width, TaffyDimension height) {
        return new TaffySize<>(width, height);
    }

    private static TaffyDimension len(float value) {
        return TaffyDimension.length(value);
    }

    private static LengthPercentageAuto lpa(float value) {
        return LengthPercentageAuto.length(value);
    }

    private static TaffySize<AvailableSpace> definiteSpace(float width, float height) {
        return new TaffySize<>(
            AvailableSpace.definite(width),
            AvailableSpace.definite(height));
    }
}
