package dev.vfyjxf.taffy.compat;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Freezes the layout output of six P0-a scenarios on the 49b7ef0 baseline.
 *
 * <p>These are <strong>behavior snapshots, not correctness assertions</strong>: several
 * of the recorded values are the known-wrong output of PA-01..PA-06 (their golden files
 * will change when those fixes are merged). The point is that every intentional behavior
 * change becomes a reviewable golden diff instead of a silent drift.
 *
 * <p>Regenerate the snapshots only after a fix has been merged:
 * <pre>
 * ./gradlew test --tests 'dev.vfyjxf.taffy.compat.*' -Dtaffy.compat.updateGolden=true
 * </pre>
 *
 * <p>See {@code compat-golden/README.md} for the format and usage rules.
 */
class CompatGoldenTest {

    @Test
    @DisplayName("P0-a baseline: grid + named line placement + absolutely positioned children")
    void gridNamedLinesAbsolutePosition() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.GRID;
        rootStyle.direction = TaffyDirection.LTR;
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.size = size(len(200), len(150));
        rootStyle.padding = edges(lp(10));
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.fixed(20f), TrackSizingFunction.fixed(50f));
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(40f), TrackSizingFunction.fixed(60f));
        // Mirrors upstream grid_absolute_known_named_lines: [a] 20px [b] 50px [c].
        rootStyle.gridTemplateRowNames = namedLines("a", "b", "c");
        rootStyle.gridTemplateColumnNames = namedLines("a", "b", "c");

        TaffyStyle fullSpanStyle = absoluteWithZeroInsets();
        fullSpanStyle.gridRow = new TaffyLine<>(GridPlacement.namedLine("a"), GridPlacement.namedLine("c"));
        fullSpanStyle.gridColumn = new TaffyLine<>(GridPlacement.namedLine("a"), GridPlacement.namedLine("c"));

        TaffyStyle endAtBStyle = absoluteWithZeroInsets();
        endAtBStyle.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.namedLine("b"));
        endAtBStyle.gridColumn = new TaffyLine<>(GridPlacement.namedLine("b"), GridPlacement.auto());

        TaffyStyle reversedStyle = absoluteWithZeroInsets();
        reversedStyle.gridRow = new TaffyLine<>(GridPlacement.namedLine("c"), GridPlacement.namedLine("a"));
        reversedStyle.gridColumn = new TaffyLine<>(GridPlacement.namedLine("c"), GridPlacement.namedLine("a"));

        NodeId fullSpan = tree.newLeaf(fullSpanStyle);
        NodeId endAtB = tree.newLeaf(endAtBStyle);
        NodeId reversed = tree.newLeaf(reversedStyle);
        NodeId root = tree.newWithChildren(rootStyle, fullSpan, endAtB, reversed);

        tree.computeLayout(root, definiteSpace(200f, 150f));

        GoldenSupport.assertGolden(
            "grid_named_lines_absolute_position",
            "Grid container with [a]/[b]/[c] named rows and columns plus absolutely positioned "
            + "children using named-line placement; freezes 49b7ef0 output (PA-01).",
            200f, 150f, tree, root);
    }

    @Test
    @DisplayName("P0-a baseline: flex wrap-reverse with align-self:start/end")
    void flexWrapReverseAlignSelfStart() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        // Container-level START is the value that AlignSelf.fromAlignItems collapses today.
        rootStyle.alignItems = AlignItems.START;
        rootStyle.size = size(len(100), len(100));

        TaffyStyle inheritedStartStyle = itemStyle(40f, 20f);
        inheritedStartStyle.alignSelf = AlignItems.AUTO;

        TaffyStyle startStyle = itemStyle(40f, 20f);
        startStyle.alignSelf = AlignItems.START;

        TaffyStyle endStyle = itemStyle(40f, 20f);
        endStyle.alignSelf = AlignItems.END;

        TaffyStyle flexStartStyle = itemStyle(40f, 20f);
        flexStartStyle.alignSelf = AlignItems.FLEX_START;

        TaffyStyle flexEndStyle = itemStyle(40f, 20f);
        flexEndStyle.alignSelf = AlignItems.FLEX_END;

        NodeId inheritedStart = tree.newLeaf(inheritedStartStyle);
        NodeId start = tree.newLeaf(startStyle);
        NodeId end = tree.newLeaf(endStyle);
        NodeId flexStart = tree.newLeaf(flexStartStyle);
        NodeId flexEnd = tree.newLeaf(flexEndStyle);
        NodeId root = tree.newWithChildren(rootStyle, inheritedStart, start, end, flexStart, flexEnd);

        tree.computeLayout(root, definiteSpace(100f, 100f));

        GoldenSupport.assertGolden(
            "flex_wrap_reverse_align_self",
            "Flex row wrap-reverse container with align-items:start and children using "
            + "align-self start/end/flex-start/flex-end; freezes 49b7ef0 output (PA-02).",
            100f, 100f, tree, root);
    }

    @Test
    @DisplayName("P0-a baseline: RTL root location")
    void rtlRootLocation() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.direction = TaffyDirection.RTL;
        rootStyle.alignItems = AlignItems.FLEX_START;
        rootStyle.size = size(len(200), len(100));
        // PA-03's expected formula subtracts margin.right from the available width.
        rootStyle.margin = TaffyRect.ltrb(lpa(0), lpa(0), lpa(5), lpa(0));

        TaffyStyle firstStyle = itemStyle(30f, 20f);
        TaffyStyle secondStyle = itemStyle(40f, 20f);
        NodeId first = tree.newLeaf(firstStyle);
        NodeId second = tree.newLeaf(secondStyle);
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        tree.computeLayout(root, definiteSpace(300f, 150f));

        GoldenSupport.assertGolden(
            "rtl_root_direction",
            "RTL root (200x100, margin-right 5) in a 300x150 viewport with two inherited-RTL "
            + "flex children; freezes 49b7ef0 root location (PA-03).",
            300f, 150f, tree, root);
    }

    @Test
    @DisplayName("P0-a baseline: TaffyStyle.copy() after setFlex")
    void styleCopyFlexShorthand() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle flexedStyle = new TaffyStyle();
        flexedStyle.setFlex(2f);
        flexedStyle.size = size(TaffyDimension.AUTO, len(20));
        // copy() currently drops the Yoga-style flex shorthand field (PA-04).
        TaffyStyle copiedStyle = flexedStyle.copy();

        TaffyStyle fixedStyle = itemStyle(20f, 20f);

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.alignItems = AlignItems.FLEX_START;
        rootStyle.size = size(len(100), len(100));

        NodeId fixed = tree.newLeaf(fixedStyle);
        NodeId copied = tree.newLeaf(copiedStyle);
        NodeId root = tree.newWithChildren(rootStyle, fixed, copied);

        tree.computeLayout(root, definiteSpace(100f, 100f));

        GoldenSupport.assertGolden(
            "style_copy_flex_shorthand",
            "Flex row with a fixed 20px item and an item whose style is setFlex(2).copy(); "
            + "freezes 49b7ef0 output where the copied item loses the flex shorthand (PA-04).",
            100f, 100f, tree, root);
    }

    @Test
    @DisplayName("P0-a baseline: content-sized abspos container with height:50% child")
    void absposContentSizedPercentageHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle outerStyle = new TaffyStyle();
        outerStyle.display = TaffyDisplay.FLEX;
        outerStyle.direction = TaffyDirection.LTR;
        outerStyle.alignItems = AlignItems.FLEX_START;
        outerStyle.size = size(len(300), len(200));

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.BLOCK;
        containerStyle.position = TaffyPosition.ABSOLUTE;
        containerStyle.size = size(len(100), TaffyDimension.AUTO);
        containerStyle.inset = TaffyRect.ltrb(lpa(0), lpa(0), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);

        TaffyStyle fixedChildStyle = new TaffyStyle();
        fixedChildStyle.display = TaffyDisplay.BLOCK;
        fixedChildStyle.size = size(len(100), len(100));

        TaffyStyle percentageChildStyle = new TaffyStyle();
        percentageChildStyle.display = TaffyDisplay.BLOCK;
        percentageChildStyle.size = size(len(100), pct(0.5f));

        NodeId fixedChild = tree.newLeaf(fixedChildStyle);
        NodeId percentageChild = tree.newLeaf(percentageChildStyle);
        NodeId container = tree.newWithChildren(containerStyle, fixedChild, percentageChild);
        NodeId root = tree.newWithChildren(outerStyle, container);

        tree.computeLayout(root, definiteSpace(300f, 200f));

        GoldenSupport.assertGolden(
            "abspos_content_sized_percentage_height",
            "Content-sized absolutely positioned block (width 100, height auto) containing a "
            + "100x100 block and a height:50% block; freezes 49b7ef0 output (PA-05).",
            300f, 200f, tree, root);
    }

    @Test
    @DisplayName("P0-a baseline: block display:none descendants keep stale layout")
    void blockDisplayNoneDescendants() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.direction = TaffyDirection.LTR;
        rootStyle.size = size(len(200), len(150));

        TaffyStyle visibleStyle = new TaffyStyle();
        visibleStyle.display = TaffyDisplay.BLOCK;
        visibleStyle.size = size(len(100), len(30));

        TaffyStyle hiddenWrapperStyle = new TaffyStyle();
        hiddenWrapperStyle.display = TaffyDisplay.BLOCK;
        hiddenWrapperStyle.size = size(len(120), len(80));

        TaffyStyle hiddenFlowStyle = new TaffyStyle();
        hiddenFlowStyle.display = TaffyDisplay.BLOCK;
        hiddenFlowStyle.size = size(len(60), len(20));

        TaffyStyle hiddenOofStyle = new TaffyStyle();
        hiddenOofStyle.position = TaffyPosition.ABSOLUTE;
        hiddenOofStyle.inset = TaffyRect.ltrb(lpa(10), lpa(10), lpa(10), lpa(10));
        hiddenOofStyle.size = size(len(30), len(15));

        NodeId hiddenFlow = tree.newLeaf(hiddenFlowStyle);
        NodeId hiddenOof = tree.newLeaf(hiddenOofStyle);
        NodeId hiddenWrapper = tree.newWithChildren(hiddenWrapperStyle, hiddenFlow, hiddenOof);
        NodeId visible = tree.newLeaf(visibleStyle);
        NodeId root = tree.newWithChildren(rootStyle, visible, hiddenWrapper);

        // Pass 1: the subtree is visible, so it receives real (non-zero) layouts.
        tree.computeLayout(root, definiteSpace(200f, 150f));

        // Pass 2: hide it. PA-06's dead hidden-children loop means the old layouts are
        // never cleared; the snapshot records those stale values on 49b7ef0.
        TaffyStyle hiddenStyle = hiddenWrapperStyle.copy();
        hiddenStyle.display = TaffyDisplay.NONE;
        tree.setStyle(hiddenWrapper, hiddenStyle);
        tree.computeLayout(root, definiteSpace(200f, 150f));

        GoldenSupport.assertGolden(
            "block_display_none_descendants",
            "Block root; a previously visible block subtree (plain child + absolutely positioned "
            + "descendant) is switched to display:none and relaid out; freezes 49b7ef0 stale output (PA-06).",
            200f, 150f, tree, root);
    }

    // === Scenario helpers ===

    private static TaffyStyle absoluteWithZeroInsets() {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.inset = edges(lpa(0));
        return style;
    }

    private static TaffyStyle itemStyle(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = size(len(width), len(height));
        return style;
    }

    private static List<NamedGridLine> namedLines(String... names) {
        ArrayList<NamedGridLine> lines = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; i++) {
            lines.add(new NamedGridLine(names[i], i + 1));
        }
        return lines;
    }

    private static TaffySize<TaffyDimension> size(TaffyDimension width, TaffyDimension height) {
        return new TaffySize<>(width, height);
    }

    private static TaffyRect<LengthPercentage> edges(LengthPercentage value) {
        return TaffyRect.all(value);
    }

    private static TaffyRect<LengthPercentageAuto> edges(LengthPercentageAuto value) {
        return TaffyRect.all(value);
    }

    private static TaffySize<AvailableSpace> definiteSpace(float width, float height) {
        return new TaffySize<>(
            AvailableSpace.definite(width),
            AvailableSpace.definite(height));
    }

    private static TaffyDimension len(float value) {
        return TaffyDimension.length(value);
    }

    private static TaffyDimension pct(float value) {
        // Percentages are 0.0..1.0 in taffy-java.
        return TaffyDimension.percent(value);
    }

    private static LengthPercentage lp(float value) {
        return LengthPercentage.length(value);
    }

    private static LengthPercentageAuto lpa(float value) {
        return LengthPercentageAuto.length(value);
    }
}
