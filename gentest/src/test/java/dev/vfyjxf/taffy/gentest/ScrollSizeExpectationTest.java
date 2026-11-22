package dev.vfyjxf.taffy.gentest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit coverage for the SO-04 scroll-size expectation plumbing (upstream #1052/#1364).
 *
 * <p>The generated-test runner cannot drive Chrome in unit tests, so these cases feed the same
 * {@code getTestData()} JSON shape the generator sees and verify that:
 * <ul>
 *   <li>only scroll containers get scroll assertions ({@code hidden}/{@code scroll}/{@code auto}
 *       on either axis);</li>
 *   <li>the expectation is {@code max(0, scrollWidth - clientWidth)} (and the height
 *       equivalent), with the scroll extent taken from the rounding-mode layout and the client
 *       extent from the naively rounded layout, exactly like upstream's XML fixture
 *       generator;</li>
 *   <li>a fixture that does not carry the fields is skipped instead of failing generation.</li>
 * </ul>
 */
class ScrollSizeExpectationTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static JsonObject scrollerNode() {
        return json("""
            {
              "style": {"overflowX": "scroll", "overflowY": "visible"},
              "unroundedLayout": {
                "width": 100.0, "height": 100.0, "x": 0.0, "y": 0.0,
                "scrollWidth": 180.0, "scrollHeight": 120.0,
                "clientWidth": 80.0, "clientHeight": 70.0
              },
              "smartRoundedLayout": {
                "width": 100.0, "height": 100.0, "x": 0.0, "y": 0.0,
                "scrollWidth": 150.0, "scrollHeight": 90.0
              },
              "naivelyRoundedLayout": {
                "width": 100.0, "height": 100.0, "x": 0.0, "y": 0.0,
                "clientWidth": 100.0, "clientHeight": 50.0
              }
            }
            """);
    }

    @Test
    void scrollContainersAreDetectedFromEitherOverflowAxis() {
        for (String value : new String[]{"hidden", "scroll", "auto"}) {
            assertTrue(TaffySource.isScrollContainer(
                    json("{\"style\": {\"overflowX\": \"" + value + "\", \"overflowY\": \"visible\"}}")),
                "overflow-x " + value);
            assertTrue(TaffySource.isScrollContainer(
                    json("{\"style\": {\"overflowX\": \"visible\", \"overflowY\": \"" + value + "\"}}")),
                "overflow-y " + value);
        }
        assertFalse(TaffySource.isScrollContainer(
            json("{\"style\": {\"overflowX\": \"visible\", \"overflowY\": \"visible\"}}")));
        assertFalse(TaffySource.isScrollContainer(
            json("{\"style\": {\"overflowX\": \"clip\", \"overflowY\": \"clip\"}}")));
        assertFalse(TaffySource.isScrollContainer(json("{\"unroundedLayout\": {}}")),
            "a node without a style object is not a scroll container");
    }

    @Test
    void expectationIsScrollExtentMinusTheNaivelyRoundedClientExtent() {
        JsonObject node = scrollerNode();

        // Unrounded: 180 - 100 = 80; 120 - 50 = 70.
        assertEquals(80f, TaffySource.scrollSizeExpectation(node, false, "scrollWidth", "clientWidth"),
            0.001f, "unrounded scrollWidth expectation");
        assertEquals(70f, TaffySource.scrollSizeExpectation(node, false, "scrollHeight", "clientHeight"),
            0.001f, "unrounded scrollHeight expectation");

        // Smart rounding reads the smart-rounded layout: 150 - 100 = 50; 90 - 50 = 40.
        assertEquals(50f, TaffySource.scrollSizeExpectation(node, true, "scrollWidth", "clientWidth"),
            0.001f, "smart-rounded scrollWidth expectation");
        assertEquals(40f, TaffySource.scrollSizeExpectation(node, true, "scrollHeight", "clientHeight"),
            0.001f, "smart-rounded scrollHeight expectation");
    }

    @Test
    void expectationIsFlooredAtZeroWhenTheContentFits() {
        JsonObject node = json("""
            {
              "unroundedLayout": {"scrollWidth": 90.0, "scrollHeight": 100.0},
              "naivelyRoundedLayout": {"clientWidth": 100.0, "clientHeight": 100.0}
            }
            """);

        assertEquals(0f, TaffySource.scrollSizeExpectation(node, false, "scrollWidth", "clientWidth"),
            0.001f);
        assertEquals(0f, TaffySource.scrollSizeExpectation(node, false, "scrollHeight", "clientHeight"),
            0.001f);
    }

    @Test
    void missingFixtureFieldsAreSkipped() {
        // No naively rounded layout at all.
        JsonObject noNaive = json("""
            {"unroundedLayout": {"scrollWidth": 180.0, "scrollHeight": 120.0}}
            """);
        assertNull(TaffySource.scrollSizeExpectation(noNaive, false, "scrollWidth", "clientWidth"));
        assertNull(TaffySource.scrollSizeExpectation(noNaive, false, "scrollHeight", "clientHeight"));

        // A layout variant without the scroll extent.
        JsonObject noScroll = json("""
            {
              "smartRoundedLayout": {"width": 100.0},
              "naivelyRoundedLayout": {"clientWidth": 100.0, "clientHeight": 100.0}
            }
            """);
        assertNull(TaffySource.scrollSizeExpectation(noScroll, true, "scrollWidth", "clientWidth"));

        // A client extent that is not a number.
        JsonObject badClient = json("""
            {
              "unroundedLayout": {"scrollWidth": 180.0},
              "naivelyRoundedLayout": {"clientWidth": "auto"}
            }
            """);
        assertNull(TaffySource.scrollSizeExpectation(badClient, false, "scrollWidth", "clientWidth"));
    }

    @Test
    void generatedAssertionsCoverBothAxesForAScrollContainer() {
        StringBuilder sb = new StringBuilder();
        TaffySource.appendScrollSizeAssertions(sb, "node", scrollerNode(), false, "        ");

        String ind = "        ";
        assertEquals(ind + "assertEquals(80.0f, nodeLayout.scrollWidth(), EPSILON, \"scroll width of node\");\n"
                     + ind + "assertEquals(70.0f, nodeLayout.scrollHeight(), EPSILON, \"scroll height of node\");\n",
            sb.toString(), "the generated test must assert the Chrome scroll range");
    }

    @Test
    void generatedAssertionsAreSkippedForNonScrollContainersAndIncompleteFixtures() {
        StringBuilder visible = new StringBuilder();
        TaffySource.appendScrollSizeAssertions(visible, "node",
            json("""
                {
                  "style": {"overflowX": "visible", "overflowY": "visible"},
                  "unroundedLayout": {"scrollWidth": 180.0},
                  "naivelyRoundedLayout": {"clientWidth": 100.0}
                }
                """), false, "        ");
        assertEquals("", visible.toString(), "a non-scroll container has no scroll range");

        StringBuilder incomplete = new StringBuilder();
        TaffySource.appendScrollSizeAssertions(incomplete, "node",
            json("""
                {
                  "style": {"overflowX": "scroll", "overflowY": "scroll"},
                  "unroundedLayout": {"width": 100.0}
                }
                """), false, "        ");
        assertEquals("", incomplete.toString(), "missing fixture data must not fail generation");
    }
}
