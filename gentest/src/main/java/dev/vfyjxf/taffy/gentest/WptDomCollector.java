package dev.vfyjxf.taffy.gentest;

/**
 * JavaScript collector shared by the WPT source adapter.
 *
 * <p>The collector walks the page's test container (never the description
 * paragraphs) and reports the computed style, geometry, direct text children
 * and replaced-element intrinsic data of every node. Layout features that the
 * Java side cannot model (tables, form controls, embedded content, vertical
 * writing modes, inline formatting contexts) are detected by
 * {@link WptSupport} from this data; the collector itself only skips elements
 * that genuinely have no box.</p>
 */
final class WptDomCollector {

    private WptDomCollector() {}

    static final String JS_COLLECT = """
return (function(){
  const SKIP = new Set(['SCRIPT','STYLE','LINK','META','TITLE','BASE','SOURCE','TRACK','TEMPLATE']);
  const SKIP_IDS = new Set(['log','metadata','description','testharness','testharness-log']);
  const css = document.createElement('style');
  css.textContent = 'html,body{margin:0;padding:0;border:0;}';
  document.head.appendChild(css);

  const body = document.body;
  const bodyRect = body.getBoundingClientRect();

  const props = [
    'display','position','box-sizing','direction',
    'width','height','min-width','min-height','max-width','max-height',
    'aspect-ratio',
    'margin','margin-left','margin-right','margin-top','margin-bottom',
    'padding','padding-left','padding-right','padding-top','padding-bottom',
    'border','border-left-width','border-right-width','border-top-width','border-bottom-width',
    'left','right','top','bottom',
    'float','clear',
    'overflow','overflow-x','overflow-y',
    'writing-mode','vertical-align',
    'contain','contain-intrinsic-size',
    'flex-direction','flex-wrap','flex-grow','flex-shrink','flex-basis',
    'align-items','align-content','justify-content','align-self','justify-self',
    'gap','row-gap','column-gap',
    'grid-template-columns','grid-template-rows',
    'grid-auto-columns','grid-auto-rows','grid-auto-flow',
    'grid-column','grid-column-start','grid-column-end',
    'grid-row','grid-row-start','grid-row-end',
    'order'
  ];

  // Get original (non-computed) CSS values from stylesheets and inline styles
  function getOriginalStyles(el) {
    const orig = {};
    // First, collect from matching stylesheet rules
    try {
      for (const sheet of document.styleSheets) {
        try {
          const rules = sheet.cssRules || sheet.rules;
          if (!rules) continue;
          for (const rule of rules) {
            if (rule.type !== 1) continue; // CSSStyleRule
            try {
              if (el.matches(rule.selectorText)) {
                const style = rule.style;
                for (const p of props) {
                  const v = style.getPropertyValue(p);
                  if (v) orig[p] = v;
                }
              }
            } catch (e) {}
          }
        } catch (e) {}
      }
    } catch (e) {}
    // Then, overlay inline styles (higher priority)
    if (el.style) {
      for (const p of props) {
        const v = el.style.getPropertyValue(p);
        if (v) orig[p] = v;
      }
    }
    return orig;
  }

  const nodes = [];

  function shouldSkip(el) {
    if (!el) return true;
    if (SKIP.has(el.tagName)) return true;
    const id = (el.id || '').toLowerCase();
    if (id && SKIP_IDS.has(id)) return true;
    // Reference overlays (e.g. #reference-overlapped-red) are not the test subject.
    if (id.includes('reference') || id.includes('overlapped')) return true;
    const cls = (el.className || '').toString().toLowerCase();
    if (cls.includes('testharness') || cls.includes('wptreport')) return true;
    if (cls.includes('reference')) return true;
    return false;
  }

  function hasDirectText(el) {
    for (const child of el.childNodes) {
      if (child.nodeType === 3 && child.textContent.trim() !== '') {
        return true;
      }
    }
    return false;
  }

  function intrinsicSize(el) {
    if (el.tagName === 'IMG') return [el.naturalWidth || 0, el.naturalHeight || 0];
    if (el.tagName === 'VIDEO') return [el.videoWidth || 0, el.videoHeight || 0];
    if (el.tagName === 'CANVAS') return [el.width || 0, el.height || 0];
    return [0, 0];
  }

  function addNode(el, parentIndex) {
    if (shouldSkip(el)) return -1;
    const cs = getComputedStyle(el);
    if (cs.display === 'none') return -1;
    const idx = nodes.length;
    const r = el.getBoundingClientRect();
    const computed = {};
    for (const p of props) { computed[p] = cs.getPropertyValue(p); }
    // Get original styles (preserves 'auto' values)
    const original = getOriginalStyles(el);
    // Properties that should default to 'auto' when not explicitly set
    const insetProps = new Set(['left','right','top','bottom']);
    const sizeProps = new Set(['width','height']);
    // Check if element has aspect-ratio set (from CSS or computed)
    const hasAR = original['aspect-ratio'] || (computed['aspect-ratio'] && computed['aspect-ratio'] !== 'auto');
    // Merge: use original value if it contains 'auto'/'content', otherwise use computed
    for (const p of props) {
      if (original[p] && (original[p].includes('auto') || original[p].includes('content'))) {
        computed[p] = original[p];
      } else if (insetProps.has(p) && !original[p]) {
        computed[p] = 'auto';
      } else if (sizeProps.has(p) && !original[p] && hasAR) {
        // For elements with aspect-ratio, unspecified width/height must be 'auto'
        // so the layout engine resolves them via the aspect-ratio, not a fixed value.
        computed[p] = 'auto';
      }
    }
    // flex-basis: content -> convert to auto + clear main-axis dimension
    const fb = (original['flex-basis'] || '').trim().toLowerCase();
    if (fb === 'content') {
      computed['flex-basis'] = 'auto';
      // Determine parent's flex-direction to know main axis
      if (parentIndex >= 0) {
        const pfd = (nodes[parentIndex].computed['flex-direction'] || 'row').toLowerCase();
        if (pfd === 'column' || pfd === 'column-reverse') {
          computed['height'] = 'auto';
        } else {
          computed['width'] = 'auto';
        }
      }
    }
    const size = intrinsicSize(el);
    nodes.push({
      index: idx,
      parent: parentIndex,
      tag: el.tagName.toLowerCase(),
      id: el.id || '',
      className: (el.className || '').toString(),
      style: el.getAttribute('style') || '',
      computed,
      hasText: hasDirectText(el),
      naturalWidth: size[0],
      naturalHeight: size[1],
      rect: { x: (r.left - bodyRect.left), y: (r.top - bodyRect.top), w: r.width, h: r.height }
    });
    return idx;
  }

  function walk(el, parentIndex) {
    const idx = addNode(el, parentIndex);
    if (idx < 0) return;
    for (const child of el.children) { walk(child, idx); }
  }

  const bodyIdx = addNode(body, -1);

  function isReference(el) {
    const id = (el.id || '').toLowerCase();
    const cls = (el.className || '').toString().toLowerCase();
    return id.includes('reference') || id.includes('overlapped') || cls.includes('reference');
  }

  function isFlexOrGrid(el) {
    const d = getComputedStyle(el).display;
    return d === 'flex' || d === 'inline-flex' || d === 'grid' || d === 'inline-grid';
  }

  // Pick the test container: an explicit #container/#test id wins, then the first
  // flex/grid body child, then the first non-description body child. Description
  // paragraphs and reference overlays are never promoted to the root, and a body
  // with nothing but descriptions is reported as skipped instead of silently
  // measuring the wrong element.
  let root = document.getElementById('container') || document.getElementById('test');
  let skipReason = null;
  if (root && (shouldSkip(root) || getComputedStyle(root).display === 'none')) {
    skipReason = 'test container is display:none or hidden by policy';
    root = null;
  }
  if (!root) {
    const candidates = [];
    for (const child of body.children) {
      if (shouldSkip(child)) continue;
      if (child.tagName === 'P') continue;
      if (isReference(child)) continue;
      candidates.push(child);
    }
    const flexLike = candidates.filter(isFlexOrGrid);
    if (flexLike.length > 0) {
      root = flexLike[0];
    } else if (candidates.length > 0) {
      root = candidates[0];
    }
    if (!root) {
      skipReason = 'no test container: body only contains description paragraphs';
    }
  }
  if (root) {
    walk(root, bodyIdx);
  }

  const viewport = {
    width: document.documentElement.clientWidth,
    height: document.documentElement.clientHeight,
    dpr: window.devicePixelRatio
  };
  return JSON.stringify({
    viewport,
    rootTag: root ? root.tagName.toLowerCase() : null,
    skipReason,
    nodes
  });
})();
""";
}
