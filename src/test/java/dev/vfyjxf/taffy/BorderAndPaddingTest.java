package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Border and padding tests ported from taffy/tests/border_and_padding.rs
 * All tests are ignored in Rust; kept disabled here as well.
 */
// @Disabled("Ignored in Rust; grid/border edge cases pending") - Temporarily enabled for investigation
public class BorderAndPaddingTest {

    private Rect<LengthPercentage> arrToRect(LengthPercentage[] items) {
        return new Rect<>(items[0], items[1], items[2], items[3]);
    }

    @Test
    @DisplayName("border_on_a_single_axis_doesnt_increase_size")
    void borderOnSingleAxisDoesntIncreaseSize() {
        for (int i = 0; i < 4; i++) {
            TaffyTree tree = new TaffyTree();
            LengthPercentage[] lengths = new LengthPercentage[] {
                LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO
            };
            lengths[i] = LengthPercentage.length(10f);
            Style style = new Style();
            style.border = arrToRect(lengths);
            NodeId node = tree.newLeaf(style);

            tree.computeLayout(node, new Size<>(
                AvailableSpace.definite(100f),
                AvailableSpace.definite(100f)
            ));

            Layout layout = tree.getLayout(node);
            assertEquals(0f, layout.size().width * layout.size().height, 0.0001f);
        }
    }

    @Test
    @DisplayName("padding_on_a_single_axis_doesnt_increase_size")
    void paddingOnSingleAxisDoesntIncreaseSize() {
        for (int i = 0; i < 4; i++) {
            TaffyTree tree = new TaffyTree();
            LengthPercentage[] lengths = new LengthPercentage[] {
                LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO
            };
            lengths[i] = LengthPercentage.length(10f);
            Style style = new Style();
            style.padding = arrToRect(lengths);
            NodeId node = tree.newLeaf(style);

            tree.computeLayout(node, new Size<>(
                AvailableSpace.definite(100f),
                AvailableSpace.definite(100f)
            ));

            Layout layout = tree.getLayout(node);
            assertEquals(0f, layout.size().width * layout.size().height, 0.0001f);
        }
    }

    @Test
    @DisplayName("border_and_padding_on_a_single_axis_doesnt_increase_size")
    void borderAndPaddingOnSingleAxisDoesntIncreaseSize() {
        for (int i = 0; i < 4; i++) {
            TaffyTree tree = new TaffyTree();
            LengthPercentage[] lengths = new LengthPercentage[] {
                LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO
            };
            lengths[i] = LengthPercentage.length(10f);
            Rect<LengthPercentage> rect = arrToRect(lengths);
            Style style = new Style();
            style.border = rect;
            style.padding = rect;
            NodeId node = tree.newLeaf(style);

            tree.computeLayout(node, new Size<>(
                AvailableSpace.definite(100f),
                AvailableSpace.definite(100f)
            ));

            Layout layout = tree.getLayout(node);
            assertEquals(0f, layout.size().width * layout.size().height, 0.0001f);
        }
    }

    @Test
    @DisplayName("vertical_border_and_padding_percentage_values_use_available_space_correctly")
    void verticalBorderAndPaddingPercentageValuesUseAvailableSpaceCorrectly() {
        TaffyTree tree = new TaffyTree();

        Style style = new Style();
        style.padding = new Rect<>(
            LengthPercentage.percent(1.0f),
            LengthPercentage.ZERO,
            LengthPercentage.percent(1.0f),
            LengthPercentage.ZERO
        );

        NodeId node = tree.newLeaf(style);
        tree.computeLayout(node, new Size<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(100f)
        ));

        Layout layout = tree.getLayout(node);
        assertEquals(200f, layout.size().width, 0.0001f);
        assertEquals(200f, layout.size().height, 0.0001f);
    }
}
