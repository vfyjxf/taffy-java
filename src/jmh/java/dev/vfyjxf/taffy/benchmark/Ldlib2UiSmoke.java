package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Plain-java smoke harness for {@link Ldlib2UiBenchmark}.
 *
 * <p>This is not a JMH run: it builds every scenario state once, performs exactly one invocation
 * of every benchmark method (all parameter combinations), and checks the structural invariants a
 * real LDLib2 frame depends on — node counts, root geometry, hover/tooltip/scroll offsets, child
 * add/remove round trips and the cold-vs-warm measure-call split. Nothing here is timed: the JMH
 * fork is what produces numbers, this harness only answers "does every combination run, and does
 * the tree respond the way the scenario claims".</p>
 *
 * <p>Run it through {@code ./gradlew jmhSmokeLdlib}, which puts the jmh runtime classpath on the
 * JVM; a hand-written {@code java -cp} invocation needs jmh-core as well because the benchmark
 * methods take a {@link Blackhole}.</p>
 */
public final class Ldlib2UiSmoke {

    private static final float EPSILON = 0.01f;

    private static int checks;
    private static int failures;

    private Ldlib2UiSmoke() {
    }

    public static void main(String[] args) {
        Blackhole bh = new Blackhole(
            "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."
        );
        Ldlib2UiBenchmark bench = new Ldlib2UiBenchmark();

        System.out.println("LDLib2 UI scenario smoke — one invocation per parameter combination, no JMH measurement");
        System.out.println();

        form(bench, bh);
        dashboard(bench, bh);
        scroll(bench, bh);
        inventory(bench, bh);
        measure(bench, bh);

        System.out.println();
        System.out.printf("smoke result: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new IllegalStateException(failures + " LDLib2 UI scenario smoke checks failed");
        }
    }

    // ========================================================================================= form

    private static void form(Ldlib2UiBenchmark bench, Blackhole bh) {
        for (int target : new int[]{1000, 5000, 20000}) {
            String scenario = "form/" + target;

            Ldlib2UiBenchmark.FormColdState cold = new Ldlib2UiBenchmark.FormColdState();
            cold.targetNodes = target;
            cold.setupTrial();

            int expectedNodes = 1 + cold.shape.panels * Ldlib2UiBenchmark.FORM_NODES_PER_PANEL;
            check(scenario + "/cold", "node count " + cold.shape.nodeCount(),
                cold.shape.nodeCount() == expectedNodes && expectedNodes > 0);

            cold.invalidate();
            check(scenario + "/cold", "root dirty after clearCache",
                cold.shape.tree.isDirty(cold.shape.root));
            bench.formColdLayout(cold, bh);
            Layout root = cold.shape.tree.getLayout(cold.shape.root);
            check(scenario + "/cold", "root " + root.size().width + "x" + root.size().height,
                near(root.size().width, Ldlib2UiBenchmark.FORM_WIDTH)
                && near(root.size().height, Ldlib2UiBenchmark.FORM_HEIGHT));

            Ldlib2UiBenchmark.FormIncrementalState inc = new Ldlib2UiBenchmark.FormIncrementalState();
            inc.targetNodes = target;
            inc.setupTrial();
            float plainHeight = inc.shape.tree.getLayout(inc.shape.hoveredLabel).size().height;

            bench.formPerFrameStyleChange(inc, bh);
            float hoverHeight = inc.shape.tree.getLayout(inc.shape.hoveredLabel).size().height;
            check(scenario + "/styleChange", "hover height " + plainHeight + " -> " + hoverHeight,
                hoverHeight > plainHeight);
            bench.formPerFrameStyleChange(inc, bh);
            check(scenario + "/styleChange", "height restored to " + plainHeight,
                near(inc.shape.tree.getLayout(inc.shape.hoveredLabel).size().height, plainHeight));

            bench.formResizeRoot(inc, bh);
            check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
                near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.FORM_WIDTH));
            bench.formResizeRoot(inc, bh);
            check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
                near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.FORM_RESIZED_WIDTH));

            int rowsBefore = inc.shape.tree.childCount(inc.shape.hostPanel);
            bench.formAddRemoveChild(inc, bh);
            check(scenario + "/addRemove", "panel rows restored to " + rowsBefore,
                inc.shape.tree.childCount(inc.shape.hostPanel) == rowsBefore);
        }

        // Determinism: two independently built trees of the same size must produce the same layout.
        Ldlib2UiBenchmark.FormColdState first = new Ldlib2UiBenchmark.FormColdState();
        first.targetNodes = 1000;
        first.setupTrial();
        first.invalidate();
        bench.formColdLayout(first, bh);

        Ldlib2UiBenchmark.FormColdState second = new Ldlib2UiBenchmark.FormColdState();
        second.targetNodes = 1000;
        second.setupTrial();
        second.invalidate();
        bench.formColdLayout(second, bh);
        check("form/determinism", "two identical trees, equal layout checksum",
            near(first.shape.checksum(), second.shape.checksum()));
        System.out.println();
    }

    // ==================================================================================== dashboard

    private static void dashboard(Ldlib2UiBenchmark bench, Blackhole bh) {
        for (int side : new int[]{8, 16}) {
            String scenario = "dashboard/" + side;

            Ldlib2UiBenchmark.DashboardColdState cold = new Ldlib2UiBenchmark.DashboardColdState();
            cold.side = side;
            cold.setupTrial();
            int expectedNodes = 1 + side * side * 4;
            check(scenario + "/cold", "node count " + cold.shape.nodeCount(),
                cold.shape.nodeCount() == expectedNodes);

            cold.invalidate();
            bench.dashboardColdLayout(cold, bh);
            Layout root = cold.shape.tree.getLayout(cold.shape.root);
            check(scenario + "/cold", "root " + root.size().width + "x" + root.size().height,
                near(root.size().width, Ldlib2UiBenchmark.DASHBOARD_WIDTH)
                && near(root.size().height, Ldlib2UiBenchmark.DASHBOARD_HEIGHT));
            check(scenario + "/cold", "out-of-flow badge does not create a grid item",
                cold.shape.tree.childCount(cold.shape.root) == side * side);

            Ldlib2UiBenchmark.DashboardIncrementalState inc = new Ldlib2UiBenchmark.DashboardIncrementalState();
            inc.side = side;
            inc.setupTrial();
            float badgeWidth = inc.shape.tree.getLayout(inc.shape.badge).size().width;
            bench.dashboardPerFrameStyleChange(inc, bh);
            float animatedWidth = inc.shape.tree.getLayout(inc.shape.badge).size().width;
            check(scenario + "/styleChange", "badge width " + badgeWidth + " -> " + animatedWidth,
                near(animatedWidth, 30f) && near(badgeWidth, 24f));

            bench.dashboardResizeRoot(inc, bh);
            check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
                near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.DASHBOARD_WIDTH));
            bench.dashboardResizeRoot(inc, bh);
            check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
                near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.DASHBOARD_RESIZED_WIDTH));

            int cardsBefore = inc.shape.tree.childCount(inc.shape.hostGrid);
            bench.dashboardAddRemoveChild(inc, bh);
            check(scenario + "/addRemove", "cards restored to " + cardsBefore,
                inc.shape.tree.childCount(inc.shape.hostGrid) == cardsBefore);
        }
        System.out.println();
    }

    // ======================================================================================= scroll

    private static void scroll(Ldlib2UiBenchmark bench, Blackhole bh) {
        int rows = 5000;
        String scenario = "scroll/" + rows;

        Ldlib2UiBenchmark.ScrollColdState cold = new Ldlib2UiBenchmark.ScrollColdState();
        cold.contentRows = rows;
        cold.setupTrial();
        int expectedNodes = 6 + rows;
        check(scenario + "/cold", "node count " + cold.shape.nodeCount(),
            cold.shape.nodeCount() == expectedNodes);

        cold.invalidate();
        bench.scrollColdLayout(cold, bh);
        Layout root = cold.shape.tree.getLayout(cold.shape.root);
        check(scenario + "/cold", "root " + root.size().width + "x" + root.size().height,
            near(root.size().width, Ldlib2UiBenchmark.SCROLL_WIDTH)
            && near(root.size().height, Ldlib2UiBenchmark.SCROLL_HEIGHT));

        Ldlib2UiBenchmark.ScrollIncrementalState inc = new Ldlib2UiBenchmark.ScrollIncrementalState();
        inc.contentRows = rows;
        inc.setupTrial();
        float plainY = inc.shape.tree.getLayout(inc.shape.viewContainer).location().y;
        bench.scrollPerFrameStyleChange(inc, bh);
        float scrolledY = inc.shape.tree.getLayout(inc.shape.viewContainer).location().y;
        check(scenario + "/styleChange", "view container y " + plainY + " -> " + scrolledY,
            near(scrolledY, plainY - 120f));

        bench.scrollResizeRoot(inc, bh);
        check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
            near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.SCROLL_WIDTH));
        bench.scrollResizeRoot(inc, bh);
        check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
            near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.SCROLL_RESIZED_WIDTH));

        int rowsBefore = inc.shape.tree.childCount(inc.shape.hostView);
        bench.scrollAddRemoveChild(inc, bh);
        check(scenario + "/addRemove", "content rows restored to " + rowsBefore,
            inc.shape.tree.childCount(inc.shape.hostView) == rowsBefore);
        System.out.println();
    }

    // ==================================================================================== inventory

    private static void inventory(Ldlib2UiBenchmark bench, Blackhole bh) {
        String scenario = "inventory/9x6";

        Ldlib2UiBenchmark.InventoryColdState cold = new Ldlib2UiBenchmark.InventoryColdState();
        cold.setupTrial();
        int expectedNodes = 1 + Ldlib2UiBenchmark.INVENTORY_ROWS
                            + Ldlib2UiBenchmark.INVENTORY_ROWS * Ldlib2UiBenchmark.INVENTORY_COLUMNS
                            + 3;
        check(scenario + "/cold", "node count " + cold.shape.nodeCount(),
            cold.shape.nodeCount() == expectedNodes);

        cold.invalidate();
        bench.inventoryColdLayout(cold, bh);
        Layout root = cold.shape.tree.getLayout(cold.shape.root);
        check(scenario + "/cold", "root " + root.size().width + "x" + root.size().height,
            near(root.size().width, Ldlib2UiBenchmark.INVENTORY_WIDTH)
            && near(root.size().height, Ldlib2UiBenchmark.INVENTORY_HEIGHT));

        Ldlib2UiBenchmark.InventoryIncrementalState inc = new Ldlib2UiBenchmark.InventoryIncrementalState();
        inc.setupTrial();
        float plainContent = inc.shape.tree.getLayout(inc.shape.hoveredSlot).contentBoxWidth();
        bench.inventoryPerFrameStyleChange(inc, bh);
        float hoveredContent = inc.shape.tree.getLayout(inc.shape.hoveredSlot).contentBoxWidth();
        check(scenario + "/styleChange", "hovered slot content box " + plainContent + " -> " + hoveredContent,
            hoveredContent < plainContent);

        float tooltipX = inc.shape.tree.getLayout(inc.shape.tooltip).location().x;
        float tooltipY = inc.shape.tree.getLayout(inc.shape.tooltip).location().y;
        bench.inventoryTooltipFollow(inc, bh);
        float movedX = inc.shape.tree.getLayout(inc.shape.tooltip).location().x;
        float movedY = inc.shape.tree.getLayout(inc.shape.tooltip).location().y;
        check(scenario + "/tooltip", "tooltip " + tooltipX + "," + tooltipY
                                    + " -> " + movedX + "," + movedY,
            near(movedX, tooltipX + 4f) && near(movedY, tooltipY + 4f));

        bench.inventoryResizeRoot(inc, bh);
        check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
            near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.INVENTORY_WIDTH));
        bench.inventoryResizeRoot(inc, bh);
        check(scenario + "/resize", "root width " + inc.shape.tree.getLayout(inc.shape.root).size().width,
            near(inc.shape.tree.getLayout(inc.shape.root).size().width, Ldlib2UiBenchmark.INVENTORY_RESIZED_WIDTH));

        int slotsBefore = inc.shape.tree.childCount(inc.shape.hostRow);
        bench.inventoryAddRemoveChild(inc, bh);
        check(scenario + "/addRemove", "row slots restored to " + slotsBefore,
            inc.shape.tree.childCount(inc.shape.hostRow) == slotsBefore);
        System.out.println();
    }

    // ================================================================================== measure text

    private static void measure(Ldlib2UiBenchmark bench, Blackhole bh) {
        int labels = 2000;
        String scenario = "measure/" + labels;

        Ldlib2UiBenchmark.MeasureColdState cold = new Ldlib2UiBenchmark.MeasureColdState();
        cold.labelCount = labels;
        cold.setupTrial();
        check(scenario + "/cold", "node count " + cold.shape.nodeCount(),
            cold.shape.nodeCount() == 1 + labels);
        cold.shape.resetMeasureCalls();
        cold.invalidate();
        bench.measureCold(cold, bh);
        long coldCalls = cold.shape.measureCalls();
        check(scenario + "/cold", "measure calls " + coldCalls,
            coldCalls >= labels);

        Ldlib2UiBenchmark.MeasureWarmState warm = new Ldlib2UiBenchmark.MeasureWarmState();
        warm.labelCount = labels;
        warm.setupTrial();
        warm.shape.resetMeasureCalls();
        warm.invalidate();
        bench.measureWarm(warm, bh);
        long warmCalls = warm.shape.measureCalls();
        check(scenario + "/warm", "measure calls " + warmCalls,
            warmCalls == 0L);
        System.out.println();
    }

    // ====================================================================================== helpers

    private static void check(String scenario, String what, boolean ok) {
        check(scenario, what, ok, "");
    }

    private static void check(String scenario, String what, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.printf("[PASS] %-24s %-56s %s%n", scenario, what, detail);
        } else {
            failures++;
            System.out.printf("[FAIL] %-24s %-56s %s%n", scenario, what, detail);
        }
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }
}
