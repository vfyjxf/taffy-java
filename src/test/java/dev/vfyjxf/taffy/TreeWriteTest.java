package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TaffyTree#writeTree(java.io.Writer, NodeId)} and the delegated
 * {@link TaffyTree#printTree(NodeId)} (CR-05, upstream taffy #925).
 */
public class TreeWriteTest {

    // ==================== Full field coverage ====================

    @Test
    @DisplayName("write_tree_writes_every_layout_field")
    void writeTreeWritesEveryLayoutField() throws Exception {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());

        Layout layout = new Layout(
            3,
            new FloatPoint(1f, 2f),
            new FloatSize(30f, 40f),
            new FloatSize(50f, 60f),
            new ScrollableOverflow(new FloatRect(-2f, 10f, -3f, 12f), true, false),
            new FloatSize(4f, 5f),
            new FloatRect(6f, 7f, 8f, 9f),
            new FloatRect(10f, 11f, 12f, 13f),
            new FloatRect(14f, 15f, 16f, 17f)
        );
        tree.setLayout(root, layout);

        String output = write(tree, root);

        String expected = "[LEAF] order=3"
            + " location=FloatPoint{x=1.0, y=2.0}"
            + " size=FloatSize{width=30.0, height=40.0}"
            + " contentSize=FloatSize{width=50.0, height=60.0}"
            + " scrollableOverflow=ScrollableOverflow{rect=FloatRect{left=-2.0, right=10.0, top=-3.0, bottom=12.0}, reversedX=true, reversedY=false}"
            + " scrollbarSize=FloatSize{width=4.0, height=5.0}"
            + " border=FloatRect{left=6.0, right=7.0, top=8.0, bottom=9.0}"
            + " padding=FloatRect{left=10.0, right=11.0, top=12.0, bottom=13.0}"
            + " margin=FloatRect{left=14.0, right=15.0, top=16.0, bottom=17.0}"
            + System.lineSeparator();
        assertEquals(expected, output);
    }

    @Test
    @DisplayName("write_tree_prints_no_layout_for_a_node_without_layout")
    void writeTreePrintsNoLayout() throws Exception {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());
        tree.setLayout(root, null);

        assertEquals("[LEAF] no layout" + System.lineSeparator(), write(tree, root));
    }

    // ==================== Two trees / nesting ====================

    @Test
    @DisplayName("two_identical_trees_write_identical_output")
    void twoIdenticalTreesWriteIdenticalOutput() throws Exception {
        TaffyTree first = new TaffyTree();
        TaffyTree second = new TaffyTree();

        String firstOutput = write(first, buildNestedTree(first));
        String secondOutput = write(second, buildNestedTree(second));

        assertEquals(firstOutput, secondOutput);
        assertTrue(firstOutput.contains("FLEX ROW"), firstOutput);
        assertTrue(firstOutput.contains(System.lineSeparator() + "  [LEAF]"), firstOutput);
    }

    @Test
    @DisplayName("write_tree_indents_nested_nodes_depth_first")
    void writeTreeIndentsNestedNodesDepthFirst() throws Exception {
        TaffyTree tree = new TaffyTree();
        NodeId root = buildNestedTree(tree);

        String[] lines = write(tree, root).split("\\R", -1);
        // One root line + two child lines + trailing empty element from the final newline.
        assertEquals(4, lines.length);
        assertTrue(lines[0].startsWith("[FLEX ROW] order="), lines[0]);
        assertTrue(lines[1].startsWith("  [LEAF] order="), lines[1]);
        assertTrue(lines[2].startsWith("  [LEAF] order="), lines[2]);
        assertEquals("", lines[3]);
    }

    // ==================== printTree delegation ====================

    @Test
    @DisplayName("print_tree_matches_write_tree_output")
    void printTreeMatchesWriteTreeOutput() throws Exception {
        TaffyTree tree = new TaffyTree();
        NodeId root = buildNestedTree(tree);

        String written = write(tree, root);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            tree.printTree(root);
        } finally {
            System.setOut(original);
        }
        String printed = buffer.toString(StandardCharsets.UTF_8);

        assertEquals(written, printed);
        assertFalse(printed.isEmpty());
    }

    @Test
    @DisplayName("print_tree_keeps_system_out_open")
    void printTreeKeepsSystemOutOpen() {
        TaffyTree tree = new TaffyTree();
        NodeId root = buildNestedTree(tree);

        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        try {
            System.setOut(capture);
            tree.printTree(root);
            // printTree flushes but must not close System.out: a write afterwards still succeeds.
            System.out.print("still-open");
            assertFalse(capture.checkError());
        } finally {
            System.setOut(original);
        }
        assertTrue(buffer.toString(StandardCharsets.UTF_8).endsWith("still-open"));
    }

    // ==================== Boundaries ====================

    @Test
    @DisplayName("empty_tree_writes_nothing")
    void emptyTreeWritesNothing() throws Exception {
        TaffyTree tree = new TaffyTree();
        NodeId missing = NodeId.of(9999);

        assertEquals("", write(tree, missing));
        assertEquals(0, tree.totalNodeCount());
    }

    @Test
    @DisplayName("single_node_tree_writes_one_line")
    void singleNodeTreeWritesOneLine() throws Exception {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = new TaffyStyle();
        style.size = TaffySize.all(TaffyDimension.length(100f));
        NodeId root = tree.newLeaf(style);
        tree.computeLayout(root, TaffySize.maxContent());

        String output = write(tree, root);
        String[] lines = output.split("\\R", -1);

        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("[LEAF] order="), output);
        assertEquals("", lines[1]);
    }

    @Test
    @DisplayName("write_tree_rejects_null_writer")
    void writeTreeRejectsNullWriter() {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());

        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> tree.writeTree(null, root)
        );
    }

    // ==================== Helpers ====================

    private static String write(TaffyTree tree, NodeId root) throws Exception {
        StringWriter writer = new StringWriter();
        tree.writeTree(writer, root);
        return writer.toString();
    }

    private static NodeId buildNestedTree(TaffyTree tree) {
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = TaffySize.all(TaffyDimension.length(20f));

        NodeId firstChild = tree.newLeaf(leafStyle);
        NodeId secondChild = tree.newLeaf(leafStyle);

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.size = TaffySize.all(TaffyDimension.length(100f));

        NodeId root = tree.newWithChildren(rootStyle, firstChild, secondChild);
        tree.computeLayout(root, TaffySize.maxContent());
        return root;
    }
}
