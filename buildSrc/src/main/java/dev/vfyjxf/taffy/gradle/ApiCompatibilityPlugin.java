package dev.vfyjxf.taffy.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Wires JarCompatibilityChecker into the build so the current API jar is compared
 * against the most recently published release of this library.
 *
 * <p>The check is intentionally based on the plain {@code jar} task, not on
 * {@code shadowJar}: the artifact published to Maven Central is the plain jar.</p>
 */
public final class ApiCompatibilityPlugin implements Plugin<Project> {

    public static final String CHECKER_CONFIGURATION = "apiCompatibilityChecker";
    public static final String BASELINE_CONFIGURATION = "apiCompatibilityBaseline";
    public static final String CHECKER_TASK = "checkApiCompatibilityWithJarCompatibilityChecker";
    public static final String CHECK_TASK = "checkApiCompatibility";

    public static final String BASELINE_GROUP = "dev.vfyjxf";
    public static final String BASELINE_ARTIFACT = "taffy";
    public static final String DEFAULT_BASELINE_VERSION = "1.1.5";
    public static final String BASELINE_VERSION_PROPERTY = "apiCompatibilityBaselineVersion";
    public static final String CHECKER_VERSION = "0.1.23";
    public static final String ASM_VERSION = "9.10.1";

    private static final String NEOFORGED_RELEASES = "https://maven.neoforged.net/releases";
    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2";
    private static final String CHECKER_MAIN_CLASS = "net.neoforged.jarcompatibilitychecker.ConsoleTool";

    @Override
    public void apply(Project project) {
        addRepositories(project);

        Provider<String> baselineVersion = project.getProviders()
            .gradleProperty(BASELINE_VERSION_PROPERTY)
            .orElse(DEFAULT_BASELINE_VERSION);

        Configuration checker = createCheckerConfiguration(project);
        Configuration baseline = createBaselineConfiguration(project, baselineVersion);

        TaskProvider<Jar> jarTask = project.getTasks().named("jar", Jar.class);
        Provider<RegularFile> inputJar = jarTask.flatMap(Jar::getArchiveFile);
        Provider<RegularFile> reportFile = project.getLayout().getBuildDirectory()
            .file("reports/apiCompatibility/report.json");

        TaskProvider<JavaExec> checkerTask = project.getTasks()
            .register(CHECKER_TASK, JavaExec.class, task -> {
                task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
                task.setDescription(
                    "Compares the plain jar against the published " + BASELINE_GROUP + ":" + BASELINE_ARTIFACT
                        + " API baseline using JarCompatibilityChecker."
                );
                task.setClasspath(checker);
                task.getMainClass().set(CHECKER_MAIN_CLASS);
                task.getInputs().property("apiCompatibilityBaselineVersion", baselineVersion);
                task.dependsOn(jarTask);

                CheckerArguments arguments = project.getObjects().newInstance(CheckerArguments.class);
                arguments.getBaselineJar().from(baseline);
                arguments.getInputJar().set(inputJar);
                arguments.getOutputFile().set(reportFile);
                task.getArgumentProviders().add(arguments);
            });

        TaskProvider<ValidateApiCompatibilityReport> validateTask = project.getTasks()
            .register(CHECK_TASK, ValidateApiCompatibilityReport.class, task -> {
                task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
                task.setDescription(
                    "Fails the build if JarCompatibilityChecker reports API incompatibilities against the "
                        + BASELINE_GROUP + ":" + BASELINE_ARTIFACT + " baseline."
                );
                task.dependsOn(checkerTask);
                task.getReportFile().set(reportFile);
                task.getBaselineVersion().set(baselineVersion);
                task.getBaselineCoordinates().set(BASELINE_GROUP + ":" + BASELINE_ARTIFACT);
                task.getBaselineRepository().set(MAVEN_CENTRAL);
            });

        // Only wire into 'check' once the Java plugins have created that lifecycle task.
        project.getPlugins().withType(org.gradle.api.plugins.JavaBasePlugin.class, plugin ->
            project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME)
                .configure(task -> task.dependsOn(validateTask))
        );
    }

    private static void addRepositories(Project project) {
        project.getRepositories().maven(repository -> {
            repository.setName("neoforgedReleases");
            repository.setUrl(NEOFORGED_RELEASES);
            repository.content(content -> content.includeGroup("net.neoforged"));
        });
        project.getRepositories().maven(repository -> {
            repository.setName("taffyBaselineMavenCentral");
            repository.setUrl(MAVEN_CENTRAL);
            repository.content(content -> content.includeGroup(BASELINE_GROUP));
        });
    }

    private static Configuration createCheckerConfiguration(Project project) {
        Configuration checker = project.getConfigurations().create(CHECKER_CONFIGURATION, configuration -> {
            configuration.setDescription("Runtime classpath for JarCompatibilityChecker.");
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
            configuration.attributes(attributes -> {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
            });
            configuration.getResolutionStrategy().eachDependency(details -> {
                if ("org.ow2.asm".equals(details.getRequested().getGroup())) {
                    details.useVersion(ASM_VERSION);
                    details.because("ASM 9.10.1 is required to read newer class files; aligned with JEI.");
                }
            });
        });

        project.getDependencies().add(CHECKER_CONFIGURATION,
            "net.neoforged:jarcompatibilitychecker:" + CHECKER_VERSION);
        for (String module : new String[]{"asm", "asm-analysis", "asm-commons", "asm-tree"}) {
            project.getDependencies().add(CHECKER_CONFIGURATION, "org.ow2.asm:" + module + ":" + ASM_VERSION);
        }
        return checker;
    }

    private static Configuration createBaselineConfiguration(Project project, Provider<String> baselineVersion) {
        Configuration baseline = project.getConfigurations().create(BASELINE_CONFIGURATION, configuration -> {
            configuration.setDescription("Published API baseline jar used by JarCompatibilityChecker.");
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
            configuration.attributes(attributes -> {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
            });
        });

        Dependency dependency = project.getDependencies().create(
            BASELINE_GROUP + ":" + BASELINE_ARTIFACT + ":" + baselineVersion.get()
        );
        if (dependency instanceof ModuleDependency) {
            ((ModuleDependency) dependency).setTransitive(false);
        }
        project.getDependencies().add(BASELINE_CONFIGURATION, dependency);
        return baseline;
    }
}
