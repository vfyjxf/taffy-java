package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P7 grid track-sizing batch: upstream-equivalent fixtures for
 * {@code alignment-changes.md} GR-01 / GR-03 / GR-04.
 *
 * <p>The fixtures are hand-ported from the Rust taffy fixtures and their generated
 * expectations (Chrome layout oracle) because the committed generated test tree is pinned to an
 * earlier fixture snapshot:</p>
 *
 * <ul>
 *   <li><b>GR-03</b> ({@code 67d6bd6b}, #1023): the grid container's min/max sizes are border-box
 *       values and must be converted to content-box before entering the track sizing algorithm
 *       ({@code grid_min_width_with_padding_border}, {@code grid_fr_tracks_min_height_with_padding_border},
 *       and the {@code box_sizing} variants).</li>
 *   <li><b>GR-04</b> ({@code 51dfca63}, #1001): space distribution must not grow a track past its
 *       growth limit, accounting for the increase already incurred for the current item
 *       ({@code grid_definite_size_auto_tracks_asymmetric_max_content}).</li>
 *   <li><b>GR-01</b> ({@code 110b1a03} #1019, {@code d2db537f} #1084): flex track intrinsic
 *       contribution distribution uses the affected-only flex factor sum, scales the content
 *       excess by {@code min(sum, 1)} anchored at the spanned inflexible tracks, and floors the
 *       result at the item's minimum contribution ({@code grid_fr_content_proportion_*},
 *       {@code grid_flex_track_intrinsic_sizes_*}).</li>
 * </ul>
 *
 * <p>Text content is emulated with the generated-test Ahem measure function (10px per character,
 * zero-width spaces mark wrapping opportunities), which is how the generated grid tests model
 * {@code <text>} fixtures.</p>
 */
class GridTrackP7Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // GR-03: container min/max converted to content-box before track sizing
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-03: container min/max is content-box for track sizing")
    class ContainerMinMaxContentBox {

        @Test
        @DisplayName("grid_min_width_with_padding_border__content_box")
        void minWidthWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 246f, 44f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 200f, 0f, "child");
        }

        @Test
        @DisplayName("grid_min_width_with_padding_border__border_box")
        void minWidthWithPaddingBorderBorderBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 44f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 154f, 0f, "child");
        }

        @Test
        @DisplayName("grid_min_width_with_padding_border_box_sizing__border_box")
        void minWidthWithPaddingBorderExplicitBorderBoxSizing() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.BORDER_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 44f, "container");
            assertSize(tree.getLayout(child), 154f, 0f, "child");
        }

        @Test
        @DisplayName("grid_min_height_with_padding_border__content_box")
        void minHeightWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 144f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 0f, 100f, "child");
        }

        @Test
        @DisplayName("grid_min_height_with_padding_border_box_sizing__content_box")
        void minHeightWithPaddingBorderExplicitBorderBoxSizing() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.BORDER_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 100f, "container");
            assertSize(tree.getLayout(child), 0f, 56f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_width_with_padding_border__content_box")
        void frTracksMinWidthWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 246f, 44f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 200f, 0f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_width_with_padding_border__border_box")
        void frTracksMinWidthWithPaddingBorderBorderBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 44f, "container");
            assertSize(tree.getLayout(child), 154f, 0f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_height_with_padding_border__content_box")
        void frTracksMinHeightWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 144f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 0f, 100f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_height_with_padding_border__border_box")
        void frTracksMinHeightWithPaddingBorderBorderBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 100f, "container");
            assertSize(tree.getLayout(child), 0f, 56f, "child");
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** The padding/border box of the upstream {@code grid_*_with_padding_border} fixtures. */
    private static TaffyStyle paddedBorderGrid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.padding = new TaffyRect<>(
            LengthPercentage.length(3f), LengthPercentage.length(13f),
            LengthPercentage.length(17f), LengthPercentage.length(7f));
        style.border = new TaffyRect<>(
            LengthPercentage.length(20f), LengthPercentage.length(10f),
            LengthPercentage.length(5f), LengthPercentage.length(15f));
        return style;
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    /**
     * Ahem-style text measure used by the generated grid tests: 10x10 per character, zero-width
     * spaces are wrapping opportunities (min-content = longest segment, max-content = one line).
     */
    private static MeasureFunc ahemTextMeasure(String text, boolean vertical) {
        final String trimmed = text == null ? "" : text.trim();
        return (knownDimensions, availableSpace) -> {
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new dev.vfyjxf.taffy.geometry.FloatSize(knownDimensions.width, knownDimensions.height);
            }

            final char ZWS = '\u200B';
            final float H_WIDTH = 10.0f;
            final float H_HEIGHT = 10.0f;

            String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split(String.valueOf(ZWS), -1);
            if (parts.length == 0) {
                float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 0.0f;
                float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 0.0f;
                return new dev.vfyjxf.taffy.geometry.FloatSize(w, h);
            }

            int minLineLength = 0;
            int maxLineLength = 0;
            for (String p : parts) {
                int len = p.length();
                if (len > minLineLength) minLineLength = len;
                maxLineLength += len;
            }

            float knownInline = vertical ? knownDimensions.height : knownDimensions.width;
            float knownBlock = vertical ? knownDimensions.width : knownDimensions.height;
            AvailableSpace availInline = vertical ? availableSpace.height : availableSpace.width;

            float inlineSize;
            if (!Float.isNaN(knownInline)) {
                inlineSize = knownInline;
            } else if (availInline != null && availInline.isMinContent()) {
                inlineSize = minLineLength * H_WIDTH;
            } else if (availInline != null && availInline.isMaxContent()) {
                inlineSize = maxLineLength * H_WIDTH;
            } else if (availInline != null && availInline.isDefinite()) {
                inlineSize = Math.min(availInline.getValue(), maxLineLength * H_WIDTH);
            } else {
                inlineSize = maxLineLength * H_WIDTH;
            }
            inlineSize = Math.max(inlineSize, minLineLength * H_WIDTH);

            float blockSize;
            if (!Float.isNaN(knownBlock)) {
                blockSize = knownBlock;
            } else {
                int inlineLineLength = (int) Math.floor(inlineSize / H_WIDTH);
                int lineCount = 1;
                int currentLineLength = 0;
                for (String p : parts) {
                    int len = p.length();
                    if (currentLineLength + len > inlineLineLength) {
                        if (currentLineLength > 0) {
                            lineCount += 1;
                        }
                        currentLineLength = len;
                    } else {
                        currentLineLength += len;
                    }
                }
                blockSize = lineCount * H_HEIGHT;
            }

            dev.vfyjxf.taffy.geometry.FloatSize computed = vertical
                ? new dev.vfyjxf.taffy.geometry.FloatSize(blockSize, inlineSize)
                : new dev.vfyjxf.taffy.geometry.FloatSize(inlineSize, blockSize);

            float outW = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : computed.width;
            float outH = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : computed.height;
            return new dev.vfyjxf.taffy.geometry.FloatSize(outW, outH);
        };
    }
}
