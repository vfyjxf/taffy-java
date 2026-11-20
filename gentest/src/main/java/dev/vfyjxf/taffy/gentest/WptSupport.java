package dev.vfyjxf.taffy.gentest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.vfyjxf.taffy.gentest.WptSource.NodeInfo;

/**
 * Classifies a collected WPT page against the layout features taffy-java
 * actually implements.
 *
 * <p>Only immutable, feature-based reasons are reported here (never "the test
 * failed"): table formatting, UA form controls, embedded content, vertical
 * writing modes, size containment, replaced resources that failed to load and
 * inline formatting contexts. A fixture that needs one of these is skipped as
 * a whole with a {@code [skip] <path>: <reason>} log line, so no node is
 * silently dropped from an otherwise generated test.</p>
 */
final class WptSupport {

    private static final Set<String> TABLE_TAGS = Set.of(
            "table", "caption", "colgroup", "col", "thead", "tbody", "tfoot", "tr", "td", "th");

    private static final Set<String> FORM_CONTROL_TAGS = Set.of(
            "fieldset", "legend", "select", "option", "optgroup", "datalist",
            "input", "textarea", "button", "meter", "progress");

    /** Content with an intrinsic size or UA layout that this port does not model. */
    private static final Set<String> EMBEDDED_TAGS = Set.of(
            "svg", "object", "embed", "iframe", "audio", "video");

    private WptSupport() {}

    /** Per-run skip bookkeeping, printed once at the end of generation. */
    static final class SkipStats {
        private final Map<String, Integer> byReason = new LinkedHashMap<>();
        private int total;

        void record(String reason) {
            total++;
            byReason.merge(reason, 1, Integer::sum);
        }

        int total() {
            return total;
        }

        Map<String, Integer> byReason() {
            return byReason;
        }

        void printSummary() {
            System.out.println("WPT skips: " + total + " fixture(s)");
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(byReason.entrySet());
            entries.sort((a, b) -> {
                int byCount = Integer.compare(b.getValue(), a.getValue());
                return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
            });
            for (Map.Entry<String, Integer> entry : entries) {
                System.out.println("  [skip] " + entry.getValue() + "x " + entry.getKey());
            }
        }
    }

    /**
     * Returns the first unsupported feature in the generated subtree, or
     * {@code null} when taffy-java can model it.
     *
     * @param infos     collected DOM nodes; node 0 is {@code body}
     * @param rootIndex index of the test container (a direct child of body)
     */
    static String unsupportedReason(List<NodeInfo> infos, int rootIndex) {
        if (rootIndex < 0 || rootIndex >= infos.size()) {
            return "no test container selected";
        }
        List<Integer> subtree = subtree(infos, rootIndex);

        for (int index : subtree) {
            NodeInfo info = infos.get(index);
            String tag = lower(info.tag);
            String display = lower(info.computed.get("display"));

            if (TABLE_TAGS.contains(tag)) {
                return "table layout is not implemented (element <" + tag + ">)";
            }
            if (FORM_CONTROL_TAGS.contains(tag)) {
                return "UA form-control layout is not implemented (element <" + tag + ">)";
            }
            if (EMBEDDED_TAGS.contains(tag)) {
                return "embedded content is not modelled (element <" + tag + ">)";
            }
            if (display != null && display.startsWith("table")) {
                return "table layout is not implemented (display: " + display + ")";
            }

            String position = lower(info.computed.get("position"));
            if ("fixed".equals(position)) {
                return "position: fixed is not modelled";
            }
            if ("sticky".equals(position)) {
                return "position: sticky is not modelled";
            }

            String writingMode = lower(info.computed.get("writing-mode"));
            if (writingMode != null && !writingMode.isBlank() && !"horizontal-tb".equals(writingMode)) {
                return "vertical writing mode is not modelled (writing-mode: " + writingMode + ")";
            }

            String columnCount = lower(info.computed.get("column-count"));
            String columnWidth = lower(info.computed.get("column-width"));
            if ((columnCount != null && !columnCount.isBlank()
                    && !"auto".equals(columnCount) && !"1".equals(columnCount))
                    || (columnWidth != null && !columnWidth.isBlank() && !"auto".equals(columnWidth))) {
                return "multi-column layout is not implemented (column-count: " + columnCount
                        + ", column-width: " + columnWidth + ")";
            }

            String contain = lower(info.computed.get("contain"));
            if (contain != null && !contain.isBlank() && List.of(contain.split("\\s+")).contains("size")) {
                return "size containment is not modelled (contain: " + contain + ")";
            }
            String containIntrinsic = lower(info.computed.get("contain-intrinsic-size"));
            if (containIntrinsic != null && !containIntrinsic.isBlank()
                    && !"none".equals(containIntrinsic) && !"auto none".equals(containIntrinsic)) {
                return "contain-intrinsic-size is not modelled (contain-intrinsic-size: " + containIntrinsic + ")";
            }

            if (("img".equals(tag) || "canvas".equals(tag))
                    && (info.naturalWidth <= 0 || info.naturalHeight <= 0)) {
                return "replaced element <" + tag + "> failed to load; intrinsic size unavailable";
            }
        }

        NodeInfo root = infos.get(rootIndex);
        String rootDisplay = lower(root.computed.get("display"));
        if (isInlineLevel(rootDisplay)) {
            return "inline-level test container requires a line box (display: " + rootDisplay + ")";
        }
        if (root.hasText) {
            return "text content requires inline layout (element <" + lower(root.tag) + ">)";
        }

        for (int index : subtree) {
            if (index == rootIndex) {
                continue;
            }
            NodeInfo info = infos.get(index);
            if (info.hasText) {
                return "text content requires inline layout (element <" + lower(info.tag) + ">)";
            }
            int parentIndex = info.parentIndex;
            if (parentIndex < 0 || parentIndex >= infos.size()) {
                continue;
            }
            String parentDisplay = lower(infos.get(parentIndex).computed.get("display"));
            if (isFlexOrGrid(parentDisplay)) {
                // Inline-level children of flex/grid containers are blockified.
                continue;
            }
            if (isInlineLevel(lower(info.computed.get("display")))) {
                return "inline-level element <" + lower(info.tag)
                        + "> inside a block container requires a line box";
            }
        }
        return null;
    }

    private static List<Integer> subtree(List<NodeInfo> infos, int rootIndex) {
        Map<Integer, List<Integer>> children = new LinkedHashMap<>();
        for (int i = 0; i < infos.size(); i++) {
            children.put(i, new ArrayList<>());
        }
        for (int i = 0; i < infos.size(); i++) {
            int parent = infos.get(i).parentIndex;
            if (parent >= 0 && parent < infos.size()) {
                children.get(parent).add(i);
            }
        }
        List<Integer> out = new ArrayList<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(rootIndex);
        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            out.add(index);
            queue.addAll(children.getOrDefault(index, List.of()));
        }
        return out;
    }

    private static boolean isFlexOrGrid(String display) {
        if (display == null) {
            return false;
        }
        return switch (display) {
            case "flex", "inline-flex", "grid", "inline-grid" -> true;
            default -> false;
        };
    }

    private static boolean isInlineLevel(String display) {
        if (display == null) {
            return false;
        }
        return display.equals("inline") || display.startsWith("inline-");
    }

    private static String lower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
