package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.LengthPercentage;
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

/**
 * P1 regression tests for block layout correctness fixes (BK-01..BK-07, BK-10, BK-11, BK-15, BK-21).
 *
 * <p>Each test mirrors the upstream taffy fixture that accompanied the corresponding fix, using
 * the browser-computed expectations from {@code tests/xml/block/**} as the oracle. Fixtures whose
 * root is a {@code display:block} {@code position:relative} box are reproduced with a block root;
 * the outer {@code #test-root} wrapper is only needed by the browser harness.</p>
 */
public class BlockP1Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle blockStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffySize<TaffyDimension> size(TaffyDimension width, TaffyDimension height) {
        return new TaffySize<>(width, height);
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    // === BK-06: percentage padding/border resolve against the containing block width ===

    /**
     * Upstream fixture {@code block_padding_percentage_nonstretch_width} (#1083).
     *
     * <p>A 100px wide block with {@code padding: 10%} inside a 40px wide block: percentage
     * padding resolves against the containing block width (40 -> 4px per side), not the box's own
     * width (which would give 10px per side).</p>
     */
    @Test
    @DisplayName("BK-06: percentage padding resolves against the containing block width")
    void percentagePaddingUsesContainingBlockWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle innerStyle = blockStyle();
        innerStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId inner = tree.newLeaf(innerStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newWithChildren(childStyle, inner);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(40f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertLocation(rootLayout, 0f, 0f, "root");
        assertSize(rootLayout, 40f, 18f, "root");

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        // height = 4 (top padding) + 10 (inner) + 4 (bottom padding)
        assertSize(childLayout, 100f, 18f, "child");

        Layout innerLayout = tree.getLayout(inner);
        assertLocation(innerLayout, 4f, 4f, "inner");
        // width = 100 - 4 - 4
        assertSize(innerLayout, 92f, 10f, "inner");
    }
}
