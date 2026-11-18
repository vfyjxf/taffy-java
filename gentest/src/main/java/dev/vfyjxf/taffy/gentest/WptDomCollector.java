package dev.vfyjxf.taffy.gentest;

/** JavaScript collector shared by the WPT source adapter. */
final class WptDomCollector {

    private WptDomCollector() {}

    static final String JS_COLLECT =
    "return (function(){\n" +
        "  const SKIP = new Set(['SCRIPT','STYLE','LINK','META']);\n" +
        "  const SKIP_IDS = new Set(['log','metadata','description','testharness','testharness-log']);\n" +
        "  const css = document.createElement('style');\n" +
        "  css.textContent = 'html,body{margin:0;padding:0;border:0;}';\n" +
        "  document.head.appendChild(css);\n" +
        "\n" +
        "  const body = document.body;\n" +
        "  const bodyRect = body.getBoundingClientRect();\n" +
        "\n" +
        "  const props = [\n" +
        "    'display','position','box-sizing','direction',\n" +
        "    'width','height','min-width','min-height','max-width','max-height',\n" +
        "    'aspect-ratio',\n" +
        "    'margin','margin-left','margin-right','margin-top','margin-bottom',\n" +
        "    'padding','padding-left','padding-right','padding-top','padding-bottom',\n" +
        "    'border','border-left-width','border-right-width','border-top-width','border-bottom-width',\n" +
        "    'left','right','top','bottom',\n" +
        "    'flex-direction','flex-wrap','flex-grow','flex-shrink','flex-basis',\n" +
        "    'align-items','align-content','justify-content','align-self','justify-self',\n" +
        "    'gap','row-gap','column-gap',\n" +
        "    'grid-template-columns','grid-template-rows',\n" +
        "    'grid-auto-columns','grid-auto-rows','grid-auto-flow',\n" +
        "    'grid-column','grid-column-start','grid-column-end',\n" +
        "    'grid-row','grid-row-start','grid-row-end',\n" +
        "    'order'\n" +
        "  ];\n" +
        "\n" +
        "  // Get original (non-computed) CSS values from stylesheets and inline styles\n" +
        "  function getOriginalStyles(el) {\n" +
        "    const orig = {};\n" +
        "    // First, collect from matching stylesheet rules\n" +
        "    try {\n" +
        "      for (const sheet of document.styleSheets) {\n" +
        "        try {\n" +
        "          const rules = sheet.cssRules || sheet.rules;\n" +
        "          if (!rules) continue;\n" +
        "          for (const rule of rules) {\n" +
        "            if (rule.type !== 1) continue; // CSSStyleRule\n" +
        "            try {\n" +
        "              if (el.matches(rule.selectorText)) {\n" +
        "                const style = rule.style;\n" +
        "                for (const p of props) {\n" +
        "                  const v = style.getPropertyValue(p);\n" +
        "                  if (v) orig[p] = v;\n" +
        "                }\n" +
        "              }\n" +
        "            } catch (e) {}\n" +
        "          }\n" +
        "        } catch (e) {}\n" +
        "      }\n" +
        "    } catch (e) {}\n" +
        "    // Then, overlay inline styles (higher priority)\n" +
        "    if (el.style) {\n" +
        "      for (const p of props) {\n" +
        "        const v = el.style.getPropertyValue(p);\n" +
        "        if (v) orig[p] = v;\n" +
        "      }\n" +
        "    }\n" +
        "    return orig;\n" +
        "  }\n" +
        "\n" +
        "  const nodes = [];\n" +
        "\n" +
        "  function shouldSkip(el) {\n" +
        "    if (!el) return true;\n" +
        "    if (SKIP.has(el.tagName)) return true;\n" +
        "    const id = (el.id || '').toLowerCase();\n" +
        "    if (id && SKIP_IDS.has(id)) return true;\n" +
        "    const cls = (el.className || '').toString().toLowerCase();\n" +
        "    if (cls.includes('testharness') || cls.includes('wptreport')) return true;\n" +
        "    return false;\n" +
        "  }\n" +
        "\n" +
        "  function addNode(el, parentIndex) {\n" +
        "    if (shouldSkip(el)) return -1;\n" +
        "    const idx = nodes.length;\n" +
        "    const r = el.getBoundingClientRect();\n" +
        "    const cs = getComputedStyle(el);\n" +
        "    const computed = {};\n" +
        "    for (const p of props) { computed[p] = cs.getPropertyValue(p); }\n" +
        "    // Get original styles (preserves 'auto' values)\n" +
        "    const original = getOriginalStyles(el);\n" +
        "    // Properties that should default to 'auto' when not explicitly set\n" +
        "    const insetProps = new Set(['left','right','top','bottom']);\n" +
        "    const sizeProps = new Set(['width','height']);\n" +
        "    // Check if element has aspect-ratio set (from CSS or computed)\n" +
        "    const hasAR = original['aspect-ratio'] || (computed['aspect-ratio'] && computed['aspect-ratio'] !== 'auto');\n" +
        "    // Merge: use original value if it contains 'auto'/'content', otherwise use computed\n" +
        "    for (const p of props) {\n" +
        "      if (original[p] && (original[p].includes('auto') || original[p].includes('content'))) {\n" +
        "        computed[p] = original[p];\n" +
        "      } else if (insetProps.has(p) && !original[p]) {\n" +
        "        computed[p] = 'auto';\n" +
        "      } else if (sizeProps.has(p) && !original[p] && hasAR) {\n" +
        "        // For elements with aspect-ratio, unspecified width/height must be 'auto'\n" +
        "        // so the layout engine resolves them via the aspect-ratio, not a fixed value.\n" +
        "        computed[p] = 'auto';\n" +
        "      }\n" +
        "    }\n" +
        "    // flex-basis: content → convert to auto + clear main-axis dimension\n" +
        "    const fb = (original['flex-basis'] || '').trim().toLowerCase();\n" +
        "    if (fb === 'content') {\n" +
        "      computed['flex-basis'] = 'auto';\n" +
        "      // Determine parent's flex-direction to know main axis\n" +
        "      if (parentIndex >= 0) {\n" +
        "        const pfd = (nodes[parentIndex].computed['flex-direction'] || 'row').toLowerCase();\n" +
        "        if (pfd === 'column' || pfd === 'column-reverse') {\n" +
        "          computed['height'] = 'auto';\n" +
        "        } else {\n" +
        "          computed['width'] = 'auto';\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "    // For <img> elements with width/height HTML attributes, derive aspect-ratio\n" +
        "    if (el.tagName === 'IMG' && (!computed['aspect-ratio'] || computed['aspect-ratio'] === 'auto')) {\n" +
        "      const aw = el.getAttribute('width');\n" +
        "      const ah = el.getAttribute('height');\n" +
        "      if (aw && ah) {\n" +
        "        const nw = parseFloat(aw);\n" +
        "        const nh = parseFloat(ah);\n" +
        "        if (nw > 0 && nh > 0) {\n" +
        "          computed['aspect-ratio'] = nw + ' / ' + nh;\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "    nodes.push({\n" +
        "      index: idx,\n" +
        "      parent: parentIndex,\n" +
        "      tag: el.tagName.toLowerCase(),\n" +
        "      id: el.id || '',\n" +
        "      className: el.className || '',\n" +
        "      style: el.getAttribute('style') || '',\n" +
        "      computed,\n" +
        "      rect: { x: (r.left - bodyRect.left), y: (r.top - bodyRect.top), w: r.width, h: r.height }\n" +
        "    });\n" +
        "    return idx;\n" +
        "  }\n" +
        "\n" +
        "  function walk(el, parentIndex) {\n" +
        "    const idx = addNode(el, parentIndex);\n" +
        "    if (idx < 0) return;\n" +
        "    for (const child of el.children) { walk(child, idx); }\n" +
        "  }\n" +
        "\n" +
        "  const bodyIdx = addNode(body, -1);\n" +
        "  let root = document.getElementById('container') || document.getElementById('test');\n" +
        "  if (!root) {\n" +
        "    // Priority 1: Find element with flex/grid display (actual test container)\n" +
        "    let best = null;\n" +
        "    let bestArea = 0;\n" +
        "    for (const child of body.children) {\n" +
        "      if (shouldSkip(child)) continue;\n" +
        "      const cs = getComputedStyle(child);\n" +
        "      const disp = cs.display;\n" +
        "      // Prioritize flex/grid containers as they are the actual test subjects\n" +
        "      if (disp === 'flex' || disp === 'inline-flex' || disp === 'grid' || disp === 'inline-grid') {\n" +
        "        const r = child.getBoundingClientRect();\n" +
        "        const area = r.width * r.height;\n" +
        "        if (area > bestArea) { bestArea = area; best = child; }\n" +
        "      }\n" +
        "    }\n" +
        "    // Priority 2: If no flex/grid found, use largest non-paragraph element\n" +
        "    if (!best) {\n" +
        "      bestArea = 0;\n" +
        "      for (const child of body.children) {\n" +
        "        if (shouldSkip(child)) continue;\n" +
        "        // Skip paragraphs (usually test descriptions like 'Test passes if...')\n" +
        "        if (child.tagName === 'P') continue;\n" +
        "        const r = child.getBoundingClientRect();\n" +
        "        const area = r.width * r.height;\n" +
        "        if (area > bestArea) { bestArea = area; best = child; }\n" +
        "      }\n" +
        "    }\n" +
        "    // Priority 3: Fallback to any largest element\n" +
        "    if (!best) {\n" +
        "      bestArea = 0;\n" +
        "      for (const child of body.children) {\n" +
        "        if (shouldSkip(child)) continue;\n" +
        "        const r = child.getBoundingClientRect();\n" +
        "        const area = r.width * r.height;\n" +
        "        if (area > bestArea) { bestArea = area; best = child; }\n" +
        "      }\n" +
        "    }\n" +
        "    root = best;\n" +
        "  }\n" +
        "  if (root) {\n" +
        "    walk(root, bodyIdx);\n" +
        "  }\n" +
        "\n" +
        "  const viewport = {\n" +
        "    width: document.documentElement.clientWidth,\n" +
        "    height: document.documentElement.clientHeight,\n" +
        "    dpr: window.devicePixelRatio\n" +
        "  };\n" +
        "  return JSON.stringify({ viewport, nodes });\n" +
        "})();";
}
