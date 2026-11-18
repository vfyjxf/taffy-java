package dev.vfyjxf.taffy.gentest;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.manager.SeleniumManagerOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared headless-Chrome oracle.
 *
 * <p>Chrome binary resolution order: explicit {@code --chrome-binary}, the
 * {@code TAFFY_CHROME_BINARY}/{@code CHROME_BIN}/{@code CHROME_PATH}
 * environment variables, well-known macOS/Windows/Linux installations, then
 * Selenium Manager. chromedriver is resolved by Selenium Manager (or an
 * explicit {@code --chrome-driver}); WebDriverManager is kept as a fallback.</p>
 */
final class ChromeOracle implements AutoCloseable {

    /** Chrome flag that makes scrollbars occupy space in modern headless Chrome. */
    private static final String DISABLE_OVERLAY_SCROLLBAR = "--disable-features=OverlayScrollbar";

    private final ChromeDriver driver;
    private final Path chromeBinary;
    private float currentScrollbarWidth = -1f;

    private ChromeOracle(ChromeDriver driver, Path chromeBinary) {
        this.driver = driver;
        this.chromeBinary = chromeBinary;
    }

    static ChromeOracle start(Path explicitBinary, Path explicitDriver) {
        return start(explicitBinary, explicitDriver, 0, 0);
    }

    static ChromeOracle start(Path explicitBinary, Path explicitDriver, int viewportWidth, int viewportHeight) {
        Path binary = resolveChromeBinary(explicitBinary);
        if (binary != null) {
            System.out.println("Chrome binary: " + binary);
        } else {
            System.out.println("Chrome binary: (auto-detected by Selenium Manager)");
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox",
                "--disable-dev-shm-usage", DISABLE_OVERLAY_SCROLLBAR);
        if (viewportWidth > 0 && viewportHeight > 0) {
            options.addArguments("--window-size=" + viewportWidth + "," + viewportHeight);
        }
        if (binary != null) {
            options.setBinary(binary.toAbsolutePath().toString());
        }
        if (explicitDriver != null) {
            System.setProperty("webdriver.chrome.driver", explicitDriver.toAbsolutePath().toString());
            System.out.println("chromedriver: " + explicitDriver);
        } else {
            System.out.println("chromedriver: (Selenium Manager)");
        }

        ChromeDriver driver;
        try {
            driver = new ChromeDriver(options);
        } catch (RuntimeException first) {
            if (explicitDriver != null) {
                throw first;
            }
            System.err.println("[warn] Selenium Manager failed to start Chrome (" + first.getMessage() + ")");
            System.err.println("[warn] retrying with WebDriverManager");
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        driver.manage().timeouts().scriptTimeout(PageReadiness.TIMEOUT);
        return new ChromeOracle(driver, binary);
    }

    Path chromeBinary() {
        return chromeBinary;
    }

    void loadFile(Path file) {
        load(file.toUri().toString());
    }

    void loadUrl(String url) {
        load(url);
    }

    private void load(String url) {
        currentScrollbarWidth = -1f;
        driver.get(url);
        awaitDocumentReady();
    }

    /**
     * Wait for the page's load event (upstream #1054). Stylesheets are applied
     * by then; navigation can occasionally return earlier.
     */
    void awaitDocumentReady() {
        try {
            executeAsyncScript(PageReadiness.JS_AWAIT_LOAD);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    PageReadiness.loadTimeoutMessage(currentUrl(), readyState()), ex);
        }
    }

    /**
     * Verify that the taffy test base stylesheet participates in the cascade
     * before getTestData() starts switching body classes.
     */
    void verifyBaseStylesheetApplied() {
        String probeResult;
        try {
            Object result = executeScript(PageReadiness.JS_BASE_STYLESHEET_APPLIED);
            probeResult = result == null ? "no result" : result.toString();
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "could not probe the taffy test base stylesheet on " + currentUrl() + ": " + ex.getMessage(), ex);
        }
        if (!PageReadiness.isBaseStylesheetApplied(probeResult)) {
            throw new IllegalStateException(
                    PageReadiness.stylesheetFailureMessage(probeResult, currentUrl(), readyState()));
        }
    }

    private String readyState() {
        try {
            return String.valueOf(executeScript(PageReadiness.JS_READY_STATE));
        } catch (RuntimeException ex) {
            return "unavailable (" + ex.getMessage() + ")";
        }
    }

    Object executeScript(String script) {
        return ((JavascriptExecutor) driver).executeScript(script);
    }

    private Object executeAsyncScript(String script) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script);
    }

    /**
     * Measure the space-taking scrollbar width of the current page.
     *
     * <p>The historical helper used a zero-sized probe element, which reports 0
     * on modern Chrome (and used to report the CSS width on older versions).
     * A sized, overflowing probe with the page's own CSS is stable across
     * Chrome versions and matches the 15px forced by test_base_style.css.</p>
     */
    float measureScrollbarWidth() {
        if (currentScrollbarWidth >= 0) {
            return currentScrollbarWidth;
        }
        float measuredPx;
        try {
            Object result = executeScript(
                    "const el = document.createElement('div');"
                            + "el.style.cssText = 'overflow:scroll; visibility:hidden; position:absolute; width:100px; height:100px;';"
                            + "el.innerHTML = '<div style=\"width:200px;height:200px\"></div>';"
                            + "document.body.appendChild(el);"
                            + "const w = el.offsetWidth - el.clientWidth;"
                            + "el.remove();"
                            + "return w;");
            measuredPx = result instanceof Number n ? n.floatValue() : 0f;
        } catch (RuntimeException ex) {
            throw new ScrollbarWidth.ContractViolation(
                    "could not measure the scrollbar width on " + currentUrl() + ": " + ex.getMessage(), ex);
        }
        ScrollbarWidth.verify(measuredPx, currentUrl());
        currentScrollbarWidth = measuredPx;
        return currentScrollbarWidth;
    }

    private String currentUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException ex) {
            return "<unknown page: " + ex.getMessage() + ">";
        }
    }

    static Path resolveChromeBinary(Path explicit) {
        if (explicit != null) {
            if (!Files.isRegularFile(explicit)) {
                throw new GeneratorOptions.UsageException("--chrome-binary is not a file: " + explicit);
            }
            return explicit;
        }
        for (String envName : List.of("TAFFY_CHROME_BINARY", "CHROME_BIN", "CHROME_PATH")) {
            String value = System.getenv(envName);
            if (value != null && !value.isBlank()) {
                Path path = Paths.get(value);
                if (Files.isRegularFile(path)) {
                    return path.toAbsolutePath().normalize();
                }
                System.err.println("[warn] " + envName + " does not point at a file: " + value);
            }
        }
        for (Path candidate : commonChromePaths()) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        Path fromSeleniumManager = probeSeleniumManagerBrowser();
        if (fromSeleniumManager != null) {
            return fromSeleniumManager;
        }
        return null;
    }

    private static Path probeSeleniumManagerBrowser() {
        try {
            SeleniumManagerOutput.Result result =
                    SeleniumManager.getInstance().getDriverPath(new ChromeOptions(), false);
            if (result != null && result.getBrowserPath() != null && !result.getBrowserPath().isBlank()) {
                Path browser = Paths.get(result.getBrowserPath());
                if (Files.isRegularFile(browser)) {
                    System.out.println("Chrome binary: discovered by Selenium Manager: " + browser);
                    return browser;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("[warn] Selenium Manager browser probe failed: " + ex.getMessage());
        }
        return null;
    }

    private static List<Path> commonChromePaths() {
        List<Path> candidates = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", "");
        if (os.contains("mac")) {
            candidates.add(Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));
            candidates.add(Paths.get("/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary"));
            candidates.add(Paths.get("/Applications/Chromium.app/Contents/MacOS/Chromium"));
            candidates.add(Paths.get(home, "Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));
            candidates.add(Paths.get(home, "Applications/Chromium.app/Contents/MacOS/Chromium"));
        } else if (os.contains("win")) {
            String programFiles = System.getenv("ProgramFiles");
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (programFiles != null) {
                candidates.add(Paths.get(programFiles, "Google", "Chrome", "Application", "chrome.exe"));
            }
            if (programFilesX86 != null) {
                candidates.add(Paths.get(programFilesX86, "Google", "Chrome", "Application", "chrome.exe"));
            }
            if (localAppData != null) {
                candidates.add(Paths.get(localAppData, "Google", "Chrome", "Application", "chrome.exe"));
            }
        } else {
            candidates.add(Paths.get("/usr/bin/google-chrome"));
            candidates.add(Paths.get("/usr/bin/google-chrome-stable"));
            candidates.add(Paths.get("/usr/bin/chromium"));
            candidates.add(Paths.get("/usr/bin/chromium-browser"));
            candidates.add(Paths.get("/snap/bin/chromium"));
            candidates.add(Paths.get("/opt/google/chrome/chrome"));
        }
        return candidates;
    }

    ChromeDriver driver() {
        return driver;
    }

    private boolean closed;

    /**
     * End the WebDriver session before the process exits (upstream #1029).
     * Selenium's quit() closes the session and then stops chromedriver, so no
     * manual "close first, kill later" sequence is needed here. Idempotent
     * because Generator uses try-with-resources and callers may close twice.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            driver.quit();
        } catch (RuntimeException ignored) {
            // Best effort: the JVM is about to exit.
        }
    }

    static void ensureOutputParent(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
