package dev.vfyjxf.taffy.gradle;

import java.util.Arrays;
import java.util.List;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Command-line arguments for {@code net.neoforged.jarcompatibilitychecker.ConsoleTool}.
 *
 * <p>{@code --api} selects API-only mode: only public and protected members of the base
 * jar are compared. Without it the tool performs binary compatibility checks over all
 * members, including private ones. The checker itself exits with 0 unless {@code --fail}
 * is passed; the report is validated by {@link ValidateApiCompatibilityReport} instead.</p>
 */
public abstract class CheckerArguments implements CommandLineArgumentProvider {

    @Classpath
    public abstract ConfigurableFileCollection getBaselineJar();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Override
    public Iterable<String> asArguments() {
        List<String> arguments = Arrays.asList(
            "--api",
            "--base-jar", getBaselineJar().getSingleFile().getAbsolutePath(),
            "--input-jar", getInputJar().get().getAsFile().getAbsolutePath(),
            "--output", getOutputFile().get().getAsFile().getAbsolutePath()
        );
        return arguments;
    }
}
