package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
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
 * P5 regression tests for CSS Box Alignment in block layout.
 *
 * <p>This class starts with the block {@code align-content} wiring (BK-14): the offset now goes
 * through the shared {@link dev.vfyjxf.taffy.util.AlignmentHelper} instead of the previous local
 * switch, so</p>
 *
 * <ul>
 *   <li>an explicit {@code normal} resolves to {@code START} for a block container, while the
 *       legacy {@link AlignContent#AUTO} sentinel stays a no-op;</li>
 *   <li>a {@code SAFE_*} value that overflows falls back to {@code START} (a negative offset
 *       becomes zero), whereas the unsafe positional keywords keep overflowing;</li>
 *   <li>the legacy keyword offsets (start/end/center/stretch/space-*) are unchanged.</li>
 * </ul>
 */
public class BlockP5Test {

    private static final float EPS = 0.01f;

    // === Helpers ===

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static TaffyStyle containerStyle(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.position = TaffyPosition.RELATIVE;
        return style;
    }

    private static void assertLocation(TaffyTree tree, NodeId node, float x, float y, String message) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "node has no layout");
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    private static void assertStack(AlignContent alignContent, float y0, float y1, float y2) {
        TaffyTree tree = new TaffyTree();
        NodeId first = tree.newLeaf(sized(50f, 20f));
        NodeId second = tree.newLeaf(sized(50f, 20f));
        NodeId third = tree.newLeaf(sized(50f, 20f));
        TaffyStyle style = containerStyle(100f, 120f);
        style.alignContent = alignContent;
        NodeId root = tree.newWithChildren(style, first, second, third);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, first, 0f, y0, alignContent + " child 0");
        assertLocation(tree, second, 0f, y1, alignContent + " child 1");
        assertLocation(tree, third, 0f, y2, alignContent + " child 2");
    }

    // === BK-14: align-content through AlignmentHelper ===

    @Test
    @DisplayName("BK-14: explicit NORMAL align-content resolves to START in block layout")
    void normalAlignContentResolvesToStart() {
        assertStack(AlignContent.NORMAL, 0f, 20f, 40f);
    }

    @Test
    @DisplayName("BK-14: legacy align-content keywords keep their pre-helper offsets")
    void legacyAlignContentOffsetsUnchanged() {
        // 100x120 container, three 20px children: free space = 60.
        assertStack(AlignContent.START, 0f, 20f, 40f);
        assertStack(AlignContent.FLEX_START, 0f, 20f, 40f);
        assertStack(AlignContent.END, 60f, 80f, 100f);
        assertStack(AlignContent.FLEX_END, 60f, 80f, 100f);
        assertStack(AlignContent.CENTER, 30f, 50f, 70f);
        assertStack(AlignContent.STRETCH, 0f, 20f, 40f);
        assertStack(AlignContent.SPACE_BETWEEN, 0f, 20f, 40f);
        assertStack(AlignContent.SPACE_AROUND, 30f, 50f, 70f);
        assertStack(AlignContent.SPACE_EVENLY, 30f, 50f, 70f);
    }

    @Test
    @DisplayName("BK-14: a SAFE_* align-content value falls back to start on overflow")
    void safeAlignContentFallsBackToStartOnOverflow() {
        // Two 80px children in a 100px-high container: free space = -60.
        TaffyTree safeEndTree = new TaffyTree();
        NodeId safeEndFirst = safeEndTree.newLeaf(sized(50f, 80f));
        NodeId safeEndSecond = safeEndTree.newLeaf(sized(50f, 80f));
        TaffyStyle safeEndStyle = containerStyle(100f, 100f);
        safeEndStyle.alignContent = AlignContent.SAFE_END;
        NodeId safeEndRoot = safeEndTree.newWithChildren(safeEndStyle, safeEndFirst, safeEndSecond);
        safeEndTree.computeLayout(safeEndRoot, TaffySize.maxContent());
        assertLocation(safeEndTree, safeEndFirst, 0f, 0f, "safe end overflow -> start");
        assertLocation(safeEndTree, safeEndSecond, 0f, 80f, "safe end overflow -> start, second child");

        TaffyTree safeCenterTree = new TaffyTree();
        NodeId safeCenterFirst = safeCenterTree.newLeaf(sized(50f, 80f));
        NodeId safeCenterSecond = safeCenterTree.newLeaf(sized(50f, 80f));
        TaffyStyle safeCenterStyle = containerStyle(100f, 100f);
        safeCenterStyle.alignContent = AlignContent.SAFE_CENTER;
        NodeId safeCenterRoot = safeCenterTree.newWithChildren(safeCenterStyle, safeCenterFirst, safeCenterSecond);
        safeCenterTree.computeLayout(safeCenterRoot, TaffySize.maxContent());
        assertLocation(safeCenterTree, safeCenterFirst, 0f, 0f, "safe center overflow -> start");
        assertLocation(safeCenterTree, safeCenterSecond, 0f, 80f, "safe center overflow -> start, second child");

        // The unsafe keywords keep overflowing with a negative offset.
        TaffyTree endTree = new TaffyTree();
        NodeId endFirst = endTree.newLeaf(sized(50f, 80f));
        NodeId endSecond = endTree.newLeaf(sized(50f, 80f));
        TaffyStyle endStyle = containerStyle(100f, 100f);
        endStyle.alignContent = AlignContent.END;
        NodeId endRoot = endTree.newWithChildren(endStyle, endFirst, endSecond);
        endTree.computeLayout(endRoot, TaffySize.maxContent());
        assertLocation(endTree, endFirst, 0f, -60f, "unsafe end keeps the negative offset");
        assertLocation(endTree, endSecond, 0f, 20f, "unsafe end second child");

        TaffyTree centerTree = new TaffyTree();
        NodeId centerFirst = centerTree.newLeaf(sized(50f, 80f));
        NodeId centerSecond = centerTree.newLeaf(sized(50f, 80f));
        TaffyStyle centerStyle = containerStyle(100f, 100f);
        centerStyle.alignContent = AlignContent.CENTER;
        NodeId centerRoot = centerTree.newWithChildren(centerStyle, centerFirst, centerSecond);
        centerTree.computeLayout(centerRoot, TaffySize.maxContent());
        assertLocation(centerTree, centerFirst, 0f, -30f, "unsafe center keeps the negative offset");
        assertLocation(centerTree, centerSecond, 0f, 50f, "unsafe center second child");
    }

    @Test
    @DisplayName("BK-14: a SAFE_* align-content value keeps its keyword when there is free space")
    void safeAlignContentKeepsKeywordWithoutOverflow() {
        TaffyTree safeEndTree = new TaffyTree();
        NodeId safeEndChild = safeEndTree.newLeaf(sized(50f, 20f));
        TaffyStyle safeEndStyle = containerStyle(100f, 120f);
        safeEndStyle.alignContent = AlignContent.SAFE_END;
        NodeId safeEndRoot = safeEndTree.newWithChildren(safeEndStyle, safeEndChild);
        safeEndTree.computeLayout(safeEndRoot, TaffySize.maxContent());
        assertLocation(safeEndTree, safeEndChild, 0f, 100f, "safe end with free space is end");

        TaffyTree safeCenterTree = new TaffyTree();
        NodeId safeCenterChild = safeCenterTree.newLeaf(sized(50f, 20f));
        TaffyStyle safeCenterStyle = containerStyle(100f, 120f);
        safeCenterStyle.alignContent = AlignContent.SAFE_CENTER;
        NodeId safeCenterRoot = safeCenterTree.newWithChildren(safeCenterStyle, safeCenterChild);
        safeCenterTree.computeLayout(safeCenterRoot, TaffySize.maxContent());
        assertLocation(safeCenterTree, safeCenterChild, 0f, 50f, "safe center with free space is center");

        TaffyTree safeStartTree = new TaffyTree();
        NodeId safeStartChild = safeStartTree.newLeaf(sized(50f, 20f));
        TaffyStyle safeStartStyle = containerStyle(100f, 120f);
        safeStartStyle.alignContent = AlignContent.SAFE_START;
        NodeId safeStartRoot = safeStartTree.newWithChildren(safeStartStyle, safeStartChild);
        safeStartTree.computeLayout(safeStartRoot, TaffySize.maxContent());
        assertLocation(safeStartTree, safeStartChild, 0f, 0f, "safe start is start");
    }

    @Test
    @DisplayName("BK-14: align-content AUTO remains a no-op")
    void alignContentAutoIsANoOp() {
        assertStack(AlignContent.AUTO, 0f, 20f, 40f);
    }
}
