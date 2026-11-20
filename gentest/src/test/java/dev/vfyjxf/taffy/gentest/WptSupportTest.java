package dev.vfyjxf.taffy.gentest;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit coverage for the WPT feature classifier and the CSS-to-style
 * mapping that decides which fixtures taffy-java can model.
 *
 * <p>No Chrome is involved: {@link WptSource.NodeInfo} is built from the same
 * JSON shape the collector emits.</p>
 */
class WptSupportTest {

    @Test
    void simpleFlexPageIsSupported() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "flex", Map.of()),
                node(2, 1, "div", "block", Map.of()));

        assertNull(WptSupport.unsupportedReason(nodes, 1));
    }

    @Test
    void inlineChildrenOfFlexContainersAreBlockified() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "flex", Map.of()),
                node(2, 1, "span", "inline-block", Map.of()));

        assertNull(WptSupport.unsupportedReason(nodes, 1));
    }

    @Test
    void inlineLevelRootIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "inline-flex", Map.of()));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("inline-level test container"), reason);
    }

    @Test
    void inlineLevelChildOfBlockContainerIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "block", Map.of()),
                node(2, 1, "span", "inline-block", Map.of()));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("line box"), reason);
        assertTrue(reason.contains("<span>"), reason);
    }

    @Test
    void textContentIsSkipped() {
        JsonObject textNode = jsonNode(2, 1, "div", "block", Map.of(), true, 0, 0);
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "block", Map.of()),
                WptSource.NodeInfo.from(textNode));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("text content"), reason);
    }

    @Test
    void tableElementIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "table", "table", Map.of()));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("table layout"), reason);
    }

    @Test
    void formControlIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "fieldset", "block", Map.of()));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("form-control"), reason);
    }

    @Test
    void embeddedContentIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "svg", "block", Map.of()));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("embedded content"), reason);
    }

    @Test
    void verticalWritingModeIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "block", Map.of("writing-mode", "vertical-lr")));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("writing mode"), reason);
    }

    @Test
    void sizeContainmentIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "block", Map.of("contain", "size")));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("size containment"), reason);
    }

    @Test
    void multiColumnLayoutIsSkipped() {
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "block", Map.of("column-count", "4")));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("multi-column"), reason);
    }

    @Test
    void failedReplacedResourceIsSkipped() {
        JsonObject img = jsonNode(2, 1, "img", "block", Map.of(), false, 0, 0);
        List<WptSource.NodeInfo> nodes = List.of(
                node(0, -1, "body", "block", Map.of()),
                node(1, 0, "div", "block", Map.of()),
                WptSource.NodeInfo.from(img));

        String reason = WptSupport.unsupportedReason(nodes, 1);

        assertNotNull(reason);
        assertTrue(reason.contains("failed to load"), reason);
    }

    @Test
    void aspectRatioParsingAcceptsAutoPrefix() {
        assertEquals(1.0f, WptSource.WptStyle.parseAspectRatio("auto 1 / 1"));
        assertEquals(0.5f, WptSource.WptStyle.parseAspectRatio("1 / 2"));
        assertEquals(0.0f, WptSource.WptStyle.parseAspectRatio("0 / 1"));
        assertNull(WptSource.WptStyle.parseAspectRatio("auto"));
        assertNull(WptSource.WptStyle.parseAspectRatio(null));
    }

    @Test
    void replacedElementUsesNaturalRatioAndIntrinsicSize() {
        WptSource.NodeInfo img = WptSource.NodeInfo.from(jsonNode(
                1, 0, "img", "block",
                Map.of("width", "auto", "height", "auto", "aspect-ratio", "auto 10 / 1"),
                false, 20, 50));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(img).node;

        assertTrue(style.itemIsReplaced);
        assertEquals(20f / 50f, style.aspectRatio);
        assertEquals(20.0f, style.intrinsicWidth);
        assertEquals(50.0f, style.intrinsicHeight);
    }

    @Test
    void replacedElementKeepsExplicitAspectRatio() {
        WptSource.NodeInfo img = WptSource.NodeInfo.from(jsonNode(
                1, 0, "img", "block",
                Map.of("width", "100px", "height", "auto", "aspect-ratio", "1 / 5"),
                false, 20, 50));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(img).node;

        assertTrue(style.itemIsReplaced);
        assertEquals(0.2f, style.aspectRatio);
        assertNull(style.intrinsicWidth);
    }

    @Test
    void floatClearAndOverflowAreMapped() {
        WptSource.NodeInfo div = WptSource.NodeInfo.from(jsonNode(
                1, 0, "div", "block",
                Map.of("float", "left", "clear", "both", "overflow-x", "auto", "overflow-y", "clip",
                        "direction", "ltr"),
                false, 0, 0));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(div).node;

        assertEquals("LEFT", style.floatSide);
        assertEquals("BOTH", style.clear);
        assertEquals("Overflow.HIDDEN", style.overflow.x());
        assertEquals("Overflow.CLIP", style.overflow.y());
    }

    @Test
    void flowRootDisplayIsPreserved() {
        WptSource.NodeInfo div = WptSource.NodeInfo.from(jsonNode(
                1, 0, "div", "flow-root", Map.of("direction", "ltr"), false, 0, 0));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(div).node;

        assertEquals("FLOW_ROOT", style.display);
    }

    @Test
    void flexBasisContentIsNotFoldedIntoAuto() {
        WptSource.NodeInfo div = WptSource.NodeInfo.from(jsonNode(
                2, 1, "div", "block",
                Map.of("flex-basis", "content", "width", "20px", "height", "100px"),
                false, 0, 0));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(div).node;

        assertEquals("TaffyDimension.content()", style.flexBasis);
    }

    @Test
    void flexBasisContentDropsRowMainSize() {
        JsonObject raw = jsonNode(2, 1, "div", "block",
                Map.of("flex-basis", "content", "width", "20px", "height", "100px"),
                false, 0, 0);
        raw.addProperty("style", "flex-basis: content; width: 20px; height: 100px;");
        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;
        // The inline width wins the style-map merge, which is why the main axis
        // has to be dropped after the mapping.
        assertEquals("TaffyDimension.length(20.0f)", style.size.width());
        String height = style.size.height();

        StyleCodegen.NodeStyle parent = new StyleCodegen.NodeStyle();
        parent.display = "FLEX";
        parent.flexDirection = "ROW";
        WptSource.WptStyle.resolveFlexBasisContent(style, parent);

        assertEquals("TaffyDimension.AUTO", style.size.width());
        assertEquals(height, style.size.height());
    }

    @Test
    void flexBasisContentDropsColumnMainSize() {
        JsonObject raw = jsonNode(2, 1, "div", "block",
                Map.of("flex-basis", "content", "width", "100px", "height", "20px"),
                false, 0, 0);
        raw.addProperty("style", "flex-basis: content; width: 100px; height: 20px;");
        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;
        String width = style.size.width();

        StyleCodegen.NodeStyle parent = new StyleCodegen.NodeStyle();
        parent.display = "FLEX";
        parent.flexDirection = "COLUMN";
        WptSource.WptStyle.resolveFlexBasisContent(style, parent);

        assertEquals(width, style.size.width());
        assertEquals("TaffyDimension.AUTO", style.size.height());
    }

    @Test
    void flexBasisContentKeepsSizeForNonFlexParent() {
        JsonObject raw = jsonNode(2, 1, "div", "block",
                Map.of("flex-basis", "content", "width", "20px", "height", "100px"),
                false, 0, 0);
        raw.addProperty("style", "flex-basis: content; width: 20px; height: 100px;");
        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;
        String width = style.size.width();

        StyleCodegen.NodeStyle parent = new StyleCodegen.NodeStyle();
        parent.display = "BLOCK";
        WptSource.WptStyle.resolveFlexBasisContent(style, parent);

        assertEquals(width, style.size.width());
    }

    @Test
    void rootSizeKeepsBodyHeightIndefiniteWithoutExplicitHeight() {
        StyleCodegen.NodeStyle body = new StyleCodegen.NodeStyle();
        body.size = new StyleCodegen.Size("TaffyDimension.length(800.0f)", "TaffyDimension.AUTO");

        StyleCodegen.Size root = WptSource.WptStyle.rootSize(800, 457, body);

        assertEquals("TaffyDimension.length(800f)", root.width());
        assertEquals("TaffyDimension.AUTO", root.height());
    }

    @Test
    void rootSizeUsesViewportHeightForExplicitBodyHeight() {
        StyleCodegen.NodeStyle body = new StyleCodegen.NodeStyle();
        body.size = new StyleCodegen.Size("TaffyDimension.length(800.0f)", "TaffyDimension.length(100.0f)");

        StyleCodegen.Size root = WptSource.WptStyle.rootSize(800, 457, body);

        assertEquals("TaffyDimension.length(800f)", root.width());
        assertEquals("TaffyDimension.length(457f)", root.height());
    }

    @Test
    void specifiedMinSizeZeroClosesTheAutomaticMinimum() {
        // abspos-017/018: Chrome computes min-width: 0 and min-width: auto to the
        // same 0px, so the specified declaration decides whether the automatic
        // minimum size may floor a ratio-transferred axis.
        JsonObject raw = jsonNode(1, 0, "div", "block",
                Map.of("position", "absolute", "aspect-ratio", "1 / 1",
                        "width", "auto", "height", "100px",
                        "min-width", "0px", "min-height", "0px"),
                false, 0, 0);
        addSpecified(raw, Map.of("min-width", "0"));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;

        assertEquals("TaffyDimension.length(0.0f)", style.minSize.width());
        assertEquals("TaffyDimension.AUTO", style.minSize.height());
    }

    @Test
    void unspecifiedOutOfFlowMinSizeStaysAuto() {
        // abspos-012/013: an out-of-flow box without a declared min-size keeps the
        // initial value auto, so the ratio-transferred axis is floored by content.
        JsonObject raw = jsonNode(1, 0, "div", "block",
                Map.of("position", "absolute", "aspect-ratio", "2 / 1",
                        "width", "100px", "height", "auto",
                        "min-width", "0px", "min-height", "0px"),
                false, 0, 0);

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;

        assertEquals("TaffyDimension.AUTO", style.minSize.width());
        assertEquals("TaffyDimension.AUTO", style.minSize.height());
    }

    @Test
    void specifiedAutoWidthSurvivesTheComputedUsedSize() {
        JsonObject raw = jsonNode(1, 0, "div", "flow-root",
                Map.of("width", "40px", "height", "40px", "aspect-ratio", "1 / 1"),
                false, 0, 0);
        addSpecified(raw, Map.of("width", "auto", "height", "auto"));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;

        assertEquals("TaffyDimension.AUTO", style.size.width());
        assertEquals("TaffyDimension.AUTO", style.size.height());
    }

    @Test
    void specifiedPercentageHeightBeatsTheComputedUsedSize() {
        // block-aspect-ratio-025 declares height: 100% against an indefinite body;
        // the computed value is the used pixel height, which would freeze the box.
        JsonObject raw = jsonNode(1, 0, "div", "block",
                Map.of("float", "left", "aspect-ratio", "1 / 1",
                        "width", "auto", "height", "100px",
                        "min-height", "100px"),
                false, 0, 0);
        addSpecified(raw, Map.of("height", "100%"));

        StyleCodegen.NodeStyle style = WptSource.WptStyle.fromDom(WptSource.NodeInfo.from(raw)).node;

        assertEquals("TaffyDimension.AUTO", style.size.width());
        assertEquals("TaffyDimension.percent(1.0f)", style.size.height());
        assertEquals("TaffyDimension.length(100.0f)", style.minSize.height());
    }

    private static void addSpecified(JsonObject node, Map<String, String> specified) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, String> entry : specified.entrySet()) {
            object.addProperty(entry.getKey(), entry.getValue());
        }
        node.add("specified", object);
    }

    private static WptSource.NodeInfo node(int index, int parent, String tag, String display,
                                           Map<String, String> extra) {
        return WptSource.NodeInfo.from(jsonNode(index, parent, tag, display, extra, false, 0, 0));
    }

    private static JsonObject jsonNode(int index, int parent, String tag, String display,
                                       Map<String, String> extra, boolean hasText,
                                       int naturalWidth, int naturalHeight) {
        JsonObject object = new JsonObject();
        object.addProperty("index", index);
        object.addProperty("parent", parent);
        object.addProperty("tag", tag);
        object.addProperty("id", "");
        object.addProperty("style", "");
        JsonObject computed = new JsonObject();
        computed.addProperty("display", display);
        computed.addProperty("position", "static");
        computed.addProperty("direction", "ltr");
        computed.addProperty("writing-mode", "horizontal-tb");
        computed.addProperty("contain", "none");
        computed.addProperty("contain-intrinsic-size", "none");
        computed.addProperty("column-count", "auto");
        computed.addProperty("column-width", "auto");
        computed.addProperty("width", "100px");
        computed.addProperty("height", "100px");
        for (Map.Entry<String, String> entry : new HashMap<>(extra).entrySet()) {
            computed.addProperty(entry.getKey(), entry.getValue());
        }
        object.add("computed", computed);
        object.addProperty("hasText", hasText);
        object.addProperty("naturalWidth", naturalWidth);
        object.addProperty("naturalHeight", naturalHeight);
        JsonObject rect = new JsonObject();
        rect.addProperty("x", 0f);
        rect.addProperty("y", 0f);
        rect.addProperty("w", 100f);
        rect.addProperty("h", 100f);
        object.add("rect", rect);
        return object;
    }
}
