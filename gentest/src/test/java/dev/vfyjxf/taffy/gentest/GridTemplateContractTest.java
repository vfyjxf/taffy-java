package dev.vfyjxf.taffy.gentest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.vfyjxf.taffy.gentest.StyleCodegen.GridTracks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the generator contract changed by upstream #1138:
 * grid-template-rows/columns arrive either as a CSS string (modern taffy
 * helper) or as the legacy object array. Both must map to the same Java style
 * representation.
 */
class GridTemplateContractTest {

    @Test
    void readsCssStringTemplateWithoutRepeat() {
        JsonObject style = new JsonObject();
        style.addProperty("gridTemplateColumns", "100px [start] 50% 1fr");

        GridTracks tracks = TaffySource.readTracks(style, "gridTemplateColumns", true);

        assertFalse(tracks.withRepeat);
        assertEquals(List.of(
                "TrackSizingFunction.fixed(100.0f)",
                "TrackSizingFunction.percent(0.5f)",
                "TrackSizingFunction.fr(1.0f)"), tracks.entries);
    }

    @Test
    void readsCssStringTemplateWithAutoFill() {
        JsonObject style = new JsonObject();
        style.addProperty("gridTemplateColumns", "repeat(auto-fill, minmax(100px, 1fr))");

        GridTracks tracks = TaffySource.readTracks(style, "gridTemplateColumns", true);

        assertTrue(tracks.withRepeat);
        assertFalse(tracks.initializeList);
        assertEquals(List.of(
                "GridTemplateComponent.autoFill("
                        + "TrackSizingFunction.minmax("
                        + "TrackSizingFunction.fixed(100.0f), "
                        + "TrackSizingFunction.fr(1.0f)))"), tracks.entries);
    }

    @Test
    void readsCssStringTemplateWithIntegerRepeat() {
        JsonObject style = new JsonObject();
        style.addProperty("gridTemplateColumns", "repeat(2, 50px)");

        GridTracks tracks = TaffySource.readTracks(style, "gridTemplateColumns", true);

        assertTrue(tracks.withRepeat);
        assertEquals(List.of(
                "GridTemplateComponent.repeatCount(2, TrackSizingFunction.fixed(50.0f))"), tracks.entries);
    }

    @Test
    void readsLegacyArrayTemplateWithoutRepeat() {
        JsonObject style = new JsonObject();
        style.add("gridTemplateColumns", legacyTracks(
                legacyScalar("px", 100f),
                legacyScalar("fraction", 1f)));

        GridTracks tracks = TaffySource.readTracks(style, "gridTemplateColumns", true);

        assertFalse(tracks.withRepeat);
        // The legacy adapter emits the explicit overload
        // (fixed(LengthPercentage.length(...))); the CSS parser emits the
        // equivalent fixed(float) shortcut.
        assertEquals(List.of(
                "TrackSizingFunction.fixed(LengthPercentage.length(100.0f))",
                "TrackSizingFunction.fr(1.0f)"), tracks.entries);
    }

    @Test
    void readsLegacyArrayTemplateWithAutoFill() {
        JsonObject style = new JsonObject();
        JsonObject repeat = new JsonObject();
        repeat.addProperty("kind", "function");
        repeat.addProperty("name", "repeat");
        JsonArray args = new JsonArray();
        JsonObject count = new JsonObject();
        count.addProperty("unit", "auto-fill");
        args.add(count);
        args.add(legacyScalar("px", 40f));
        repeat.add("arguments", args);
        style.add("gridTemplateRows", legacyTracks(repeat));

        GridTracks tracks = TaffySource.readTracks(style, "gridTemplateRows", true);

        assertTrue(tracks.withRepeat);
        assertTrue(tracks.initializeList);
        assertEquals(List.of(
                "GridTemplateComponent.autoFill("
                        + "TrackSizingFunction.fixed(LengthPercentage.length(40.0f)))"), tracks.entries);
    }

    @Test
    void readsMixedSchemaOnTheSameNode() {
        JsonObject style = new JsonObject();
        style.addProperty("gridTemplateRows", "10px 20px");
        style.add("gridTemplateColumns", legacyTracks(
                legacyScalar("px", 100f),
                legacyScalar("fraction", 1f)));

        assertEquals(List.of(
                "TrackSizingFunction.fixed(10.0f)",
                "TrackSizingFunction.fixed(20.0f)"),
                TaffySource.readTracks(style, "gridTemplateRows", true).entries);
        assertEquals(List.of(
                "TrackSizingFunction.fixed(LengthPercentage.length(100.0f))",
                "TrackSizingFunction.fr(1.0f)"),
                TaffySource.readTracks(style, "gridTemplateColumns", true).entries);
    }

    @Test
    void readsCssStringAutoTracks() {
        JsonObject style = new JsonObject();
        style.addProperty("gridAutoRows", "40px auto");

        GridTracks tracks = TaffySource.readTracks(style, "gridAutoRows", false);

        assertFalse(tracks.withRepeat);
        assertEquals(List.of(
                "TrackSizingFunction.fixed(40.0f)",
                "TrackSizingFunction.auto()"), tracks.entries);
    }

    @Test
    void noneAndAbsentTemplatesAreNull() {
        JsonObject style = new JsonObject();
        style.addProperty("gridTemplateColumns", "none");
        assertNull(TaffySource.readTracks(style, "gridTemplateColumns", true));
        assertNull(TaffySource.readTracks(style, "gridTemplateRows", true));
    }

    private static JsonArray legacyTracks(JsonObject... tracks) {
        JsonArray array = new JsonArray();
        for (JsonObject track : tracks) {
            array.add(track);
        }
        return array;
    }

    private static JsonObject legacyScalar(String unit, float value) {
        JsonObject track = new JsonObject();
        track.addProperty("kind", "scalar");
        track.addProperty("unit", unit);
        track.addProperty("value", value);
        return track;
    }
}
