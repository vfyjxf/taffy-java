package dev.vfyjxf.taffy.gradle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Reads the JarCompatibilityChecker JSON report and fails when any incompatibility is
 * flagged as an error. Mirrors JEI's ValidateApiCompatibilityReport logic: every class,
 * method and field incompatibility entry with {@code isError == true} fails the build.
 */
public abstract class ValidateApiCompatibilityReport extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getReportFile();

    @Input
    public abstract Property<String> getBaselineVersion();

    @Input
    public abstract Property<String> getBaselineCoordinates();

    @Input
    public abstract Property<String> getBaselineRepository();

    @TaskAction
    public void validateReport() {
        File reportFile = getReportFile().get().getAsFile();
        List<String> errors = new ArrayList<>();
        int classCount = 0;

        try (Reader reader = Files.newBufferedReader(reportFile.toPath())) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new GradleException("Unexpected API compatibility report format in " + reportFile);
            }
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                classCount++;
                collectClassErrors(entry.getKey(), entry.getValue().getAsJsonObject(), errors);
            }
        } catch (IOException e) {
            throw new GradleException("Could not read API compatibility report " + reportFile, e);
        }

        if (!errors.isEmpty()) {
            throw new GradleException(
                "API compatibility check failed against " + getBaselineDescription() + ":\n"
                    + String.join("\n", errors)
            );
        }

        getLogger().lifecycle(
            "API compatibility check passed against {} (report contains {} class entries; no errors)",
            getBaselineDescription(),
            classCount
        );
    }

    private String getBaselineDescription() {
        return getBaselineCoordinates().get() + ":"
            + getBaselineVersion().get() + " from " + getBaselineRepository().get();
    }

    private static void collectClassErrors(String className, JsonObject classReport, List<String> errors) {
        collectErrors(className, null, classReport.get("classIncompatibilities"), errors);

        collectMemberErrors(className, classReport.get("methodIncompatibilities"), errors);
        collectMemberErrors(className, classReport.get("fieldIncompatibilities"), errors);
    }

    private static void collectMemberErrors(String className, JsonElement members, List<String> errors) {
        if (members == null || !members.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : members.getAsJsonObject().entrySet()) {
            collectErrors(className, entry.getKey(), entry.getValue(), errors);
        }
    }

    private static void collectErrors(String className, String memberName, JsonElement incompatibilities,
        List<String> errors) {
        if (incompatibilities == null || !incompatibilities.isJsonArray()) {
            return;
        }
        for (JsonElement element : incompatibilities.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject incompatibility = element.getAsJsonObject();
            JsonElement isError = incompatibility.get("isError");
            if (isError == null || !isError.isJsonPrimitive() || !isError.getAsBoolean()) {
                continue;
            }
            String message = incompatibility.has("message") && !incompatibility.get("message").isJsonNull()
                ? incompatibility.get("message").getAsString()
                : incompatibility.toString();
            errors.add(memberName == null
                ? className + ": " + message
                : className + ": " + memberName + " - " + message);
        }
    }
}
