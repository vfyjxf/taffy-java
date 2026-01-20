package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JustifyContent.STRETCH semantics in CSS Grid.
 * <p>
 * In CSS Grid, justify-content: stretch (the default) expands auto tracks
 * to fill the remaining free space. This is different from flexbox where
 * stretch has no effect on justify-content.
 * <p>
 * Rust reference: taffy/src/compute/grid/track_sizing.rs - stretch_auto_tracks()
 */
public class JustifyContentStretchTest {

    /**
     * Test that JustifyContent enum includes STRETCH value.
     */
    @Test
    public void justifyContentEnumHasStretch() {
        JustifyContent stretch = JustifyContent.STRETCH;
        assertNotNull(stretch);
        assertEquals("STRETCH", stretch.name());
    }

    /**
     * Test that Style.getJustifyContent() correctly maps AlignContent.STRETCH to JustifyContent.STRETCH.
     */
    @Test
    public void styleGetJustifyContentPreservesStretch() {
        TaffyStyle style = new TaffyStyle();
        style.justifyContent = AlignContent.STRETCH;

        JustifyContent jc = style.getJustifyContent();
        assertEquals(JustifyContent.STRETCH, jc, "STRETCH should not be mapped to FLEX_START");
    }

    /**
     * Test that default justify-content in Grid behaves like STRETCH.
     * <p>
     * When a grid container has definite size and auto columns, the free space
     * should be distributed equally among auto tracks (stretch behavior).
     */
    @Test
    public void gridDefaultJustifyContentStretchesAutoTracks() {
        TaffyTree taffy = new TaffyTree();

        // Container: 300px wide, with 3 auto columns
        // Each auto column should stretch to 100px (300/3)
        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));
        containerStyle.gridTemplateColumns = List.of(
                TrackSizingFunction.auto(),
                TrackSizingFunction.auto(),
                TrackSizingFunction.auto()
        );
        // justifyContent is null (default) = STRETCH for Grid

        NodeId container = taffy.newLeaf(containerStyle);

        // Three children
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50)); // min-content = 50px each

        NodeId child1 = taffy.newLeaf(childStyle);
        NodeId child2 = taffy.newLeaf(childStyle);
        NodeId child3 = taffy.newLeaf(childStyle);

        taffy.addChild(container, child1);
        taffy.addChild(container, child2);
        taffy.addChild(container, child3);

        taffy.computeLayout(container, new TaffySize<>(AvailableSpace.definite(300), AvailableSpace.definite(100)));

        Layout layout1 = taffy.getLayout(child1);
        Layout layout2 = taffy.getLayout(child2);
        Layout layout3 = taffy.getLayout(child3);

        // With justify-content: stretch (default), auto tracks should expand:
        // Container width = 300, 3 auto tracks → each track = 100px
        // Children are 50px wide, positioned at start of each 100px track
        // So children should be at x = 0, 100, 200
        assertEquals(0f, layout1.location().x, 0.1f, "Child 1 should be at x=0");
        assertEquals(100f, layout2.location().x, 0.1f, "Child 2 should be at x=100 (first track stretched)");
        assertEquals(200f, layout3.location().x, 0.1f, "Child 3 should be at x=200 (second track stretched)");
    }

    /**
     * Test that explicit justify-content: START does NOT stretch auto tracks.
     * <p>
     * When justify-content is START, auto tracks keep their min-content size
     * and are positioned at the start.
     */
    @Test
    public void gridJustifyContentStartDoesNotStretchAutoTracks() {
        TaffyTree taffy = new TaffyTree();

        // Container: 300px wide, with 3 auto columns, justify-content: START
        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));
        containerStyle.gridTemplateColumns = List.of(
                TrackSizingFunction.auto(),
                TrackSizingFunction.auto(),
                TrackSizingFunction.auto()
        );
        containerStyle.justifyContent = AlignContent.START; // NOT STRETCH

        NodeId container = taffy.newLeaf(containerStyle);

        // Three children with 50px width
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = taffy.newLeaf(childStyle);
        NodeId child2 = taffy.newLeaf(childStyle);
        NodeId child3 = taffy.newLeaf(childStyle);

        taffy.addChild(container, child1);
        taffy.addChild(container, child2);
        taffy.addChild(container, child3);

        taffy.computeLayout(container, new TaffySize<>(AvailableSpace.definite(300), AvailableSpace.definite(100)));

        Layout layout1 = taffy.getLayout(child1);
        Layout layout2 = taffy.getLayout(child2);
        Layout layout3 = taffy.getLayout(child3);

        // With justify-content: START, auto tracks should NOT stretch:
        // Each track keeps its min-content size (50px)
        // Children should be at x = 0, 50, 100 (packed at start)
        assertEquals(0f, layout1.location().x, 0.1f, "Child 1 should be at x=0");
        assertEquals(50f, layout2.location().x, 0.1f, "Child 2 should be at x=50 (no stretching)");
        assertEquals(100f, layout3.location().x, 0.1f, "Child 3 should be at x=100 (no stretching)");
    }

    /**
     * Test that explicit justify-content: STRETCH works the same as default.
     */
    @Test
    public void gridExplicitJustifyContentStretch() {
        TaffyTree taffy = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));
        containerStyle.gridTemplateColumns = List.of(
                TrackSizingFunction.auto(),
                TrackSizingFunction.auto(),
                TrackSizingFunction.auto()
        );
        containerStyle.justifyContent = AlignContent.STRETCH; // Explicit STRETCH

        NodeId container = taffy.newLeaf(containerStyle);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = taffy.newLeaf(childStyle);
        NodeId child2 = taffy.newLeaf(childStyle);
        NodeId child3 = taffy.newLeaf(childStyle);

        taffy.addChild(container, child1);
        taffy.addChild(container, child2);
        taffy.addChild(container, child3);

        taffy.computeLayout(container, new TaffySize<>(AvailableSpace.definite(300), AvailableSpace.definite(100)));

        Layout layout2 = taffy.getLayout(child2);
        Layout layout3 = taffy.getLayout(child3);

        // Same as default: tracks stretch to 100px each
        assertEquals(100f, layout2.location().x, 0.1f, "Child 2 should be at x=100 with explicit STRETCH");
        assertEquals(200f, layout3.location().x, 0.1f, "Child 3 should be at x=200 with explicit STRETCH");
    }

    /**
     * Test that justify-content: STRETCH in Flexbox is equivalent to FLEX_START.
     * <p>
     * In flexbox, STRETCH has no effect on the main axis (justify-content).
     * It should behave the same as FLEX_START.
     */
    @Test
    public void flexboxJustifyContentStretchEqualsFlexStart() {
        TaffyTree taffyStretch = new TaffyTree();
        TaffyTree taffyFlexStart = new TaffyTree();

        // Container with justify-content: STRETCH
        TaffyStyle containerStretch = new TaffyStyle();
        containerStretch.display = TaffyDisplay.FLEX;
        containerStretch.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));
        containerStretch.justifyContent = AlignContent.STRETCH;

        // Container with justify-content: FLEX_START
        TaffyStyle containerFlexStart = new TaffyStyle();
        containerFlexStart.display = TaffyDisplay.FLEX;
        containerFlexStart.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));
        containerFlexStart.justifyContent = AlignContent.FLEX_START;

        NodeId contStretch = taffyStretch.newLeaf(containerStretch);
        NodeId contFlexStart = taffyFlexStart.newLeaf(containerFlexStart);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1s = taffyStretch.newLeaf(childStyle);
        NodeId child2s = taffyStretch.newLeaf(childStyle);
        NodeId child1f = taffyFlexStart.newLeaf(childStyle);
        NodeId child2f = taffyFlexStart.newLeaf(childStyle);

        taffyStretch.addChild(contStretch, child1s);
        taffyStretch.addChild(contStretch, child2s);
        taffyFlexStart.addChild(contFlexStart, child1f);
        taffyFlexStart.addChild(contFlexStart, child2f);

        taffyStretch.computeLayout(contStretch, new TaffySize<>(AvailableSpace.definite(300), AvailableSpace.definite(100)));
        taffyFlexStart.computeLayout(contFlexStart, new TaffySize<>(AvailableSpace.definite(300), AvailableSpace.definite(100)));

        Layout layout1s = taffyStretch.getLayout(child1s);
        Layout layout2s = taffyStretch.getLayout(child2s);
        Layout layout1f = taffyFlexStart.getLayout(child1f);
        Layout layout2f = taffyFlexStart.getLayout(child2f);

        // Both should produce identical results
        assertEquals(layout1f.location().x, layout1s.location().x, 0.01f,
                "STRETCH should equal FLEX_START in flexbox for child1");
        assertEquals(layout2f.location().x, layout2s.location().x, 0.01f,
                "STRETCH should equal FLEX_START in flexbox for child2");
    }
}
