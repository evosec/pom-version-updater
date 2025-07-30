package de.evosec.pomversionupdater;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.Assert;

@SpringBootApplication
@EnableConfigurationProperties(PomVersionUpdaterProperties.class)
public class PomVersionUpdaterApplication implements ApplicationRunner {

	private static final Logger LOG =
			LoggerFactory.getLogger(PomVersionUpdaterApplication.class);

	private static final String MVN_VERSIONS_PLUGIN_VERSION = "2.17.1";

	public static void main(String[] args) {
		SpringApplication.run(PomVersionUpdaterApplication.class, args);
	}

	private final Path workingDirectory =
			Paths.get(System.getProperty("user.dir", "."));

	private final PomVersionUpdaterProperties properties;
	private final String mavenCommand;

	public PomVersionUpdaterApplication(
			PomVersionUpdaterProperties properties) {
		this.properties = properties;
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			mavenCommand = "mvn.cmd";
		} else {
			mavenCommand = "mvn";
		}
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Path pom = workingDirectory.resolve("pom.xml").toAbsolutePath();
		try (Git git = tryGit()) {

			assertWorkingTreeIsClean(git);

			Optional<Artifact> beforeParent =
					selectArtifactsFromPom(pom, "project > parent", "pom")
						.stream()
						.findFirst();
			if (beforeParent.isPresent()
					&& beforeParent.get().getVersion() != null) {
				ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand,
					"--batch-mode", "--update-snapshots", "--non-recursive",
					"versions:" + MVN_VERSIONS_PLUGIN_VERSION
							+ ":update-parent",
					"-DgenerateBackupPoms=false",
					"-DallowMajorUpdates=" + properties.isAllowMajorUpdates())
						.inheritIO()
						.directory(workingDirectory.toFile());
				LOG.info("Calling {}", processBuilder.command());
				Assert.isTrue(0 == processBuilder.start().waitFor(),
					"mvn failed");
				Artifact afterParent =
						selectArtifactsFromPom(pom, "project > parent", "jar")
							.get(0);
				commitIfNecessary(git, beforeParent.get(), afterParent);
			}

			processDependencies(pom, git,
				"project > dependencyManagement > dependencies > dependency");
			processDependencies(pom, git,
				"project > dependencies > dependency");
		}
	}

	private Git tryGit() {
		try {
			return Git.open(workingDirectory.toFile());
		} catch (IOException e) {
			LOG.error("Problem opening git repository. Will not use git", e);
			return null;
		}
	}

	private void assertWorkingTreeIsClean(Git git) throws Exception {
		if (git == null) {
			return;
		}
		git.getRepository().getRefDatabase().refresh();
		IndexDiff diffIndex = new IndexDiff(git.getRepository(), Constants.HEAD,
			new FileTreeIterator(git.getRepository()));
		if (diffIndex.diff() && diffIndex.getModified().contains("pom.xml")) {
			throw new Exception("The working tree is not clean");
		}
	}

	private void processDependencies(Path pom, Git git, String selector)
			throws Exception {
		List<Artifact> dependencies =
				selectArtifactsFromPom(pom, selector, "jar");
		for (Artifact dependency : dependencies.stream()
			.filter(a -> a.getVersion() != null)
			.toList()) {
			ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand,
				"--batch-mode", "--update-snapshots", "--non-recursive",
				"versions:" + MVN_VERSIONS_PLUGIN_VERSION
						+ ":use-latest-versions",
				"-DgenerateBackupPoms=false",
				"-DallowMajorUpdates=" + properties.isAllowMajorUpdates(),
				"-Dincludes=" + dependency).inheritIO()
					.directory(workingDirectory.toFile());
			LOG.info("Calling {} in {}", processBuilder.command(),
				processBuilder.directory());
			Assert.isTrue(0 == processBuilder.start().waitFor(), "mvn failed");
			Artifact afterDependency =
					selectArtifactsFromPom(pom, selector, "jar").stream()
						.filter(a -> a.equals(dependency))
						.findAny()
						.orElseThrow();
			commitIfNecessary(git, dependency, afterDependency);
		}
	}

	private void commitIfNecessary(Git git, Artifact before, Artifact after)
			throws Exception {
		if (git == null) {
			return;
		}
		if (!after.getVersion().equals(before.getVersion())) {
			String message =
					String.format("%s -> %s", before, after.getVersion());
			git.commit()
				.setOnly("pom.xml")
				.setAllowEmpty(false)
				.setMessage(message)
				.call();
			assertWorkingTreeIsClean(git);
		}
	}

	private List<Artifact> selectArtifactsFromPom(Path pom, String selector,
			String defaultType) throws IOException {
		List<Artifact> artifacts = new ArrayList<>();
		try (InputStream inputStream = Files.newInputStream(pom)) {
			Document document = Jsoup.parse(inputStream, UTF_8.name(), "",
				Parser.xmlParser());
			for (Element element : document.select(selector)) {
				String groupId = selectValue(element, "groupId", null);
				String artifactId = selectValue(element, "artifactId", null);
				if (shouldSkipArtifact(groupId)) {
					continue;
				}

				String type = selectValue(element, "type", defaultType);
				String classifier = selectValue(element, "classifier", "*");
				String version = selectValue(element, "version", null);

				artifacts.add(new Artifact(groupId, artifactId, type,
					classifier, version));
			}
		}
		return artifacts;
	}

	private static String selectValue(Element element, String cssQuery,
			String defaultValue) {
		Elements versionSelect = element.select(cssQuery);
		if (!versionSelect.isEmpty()) {
			return requireNonNull(versionSelect.first()).text();
		}
		return defaultValue;
	}

	private boolean shouldSkipArtifact(String artifactGroupId) {
		if (artifactGroupId == null) {
			return true;
		}

		String groupId = properties.getGroupId();
		if (groupId.isEmpty()) {
			return false;
		}

		if (groupId.endsWith("*")) {
			// Remove trailing *
			String cleanGroupId = groupId.substring(0, groupId.length() - 1);
			return !artifactGroupId.toLowerCase()
				.startsWith(cleanGroupId.toLowerCase());
		} else {
			return !groupId.equalsIgnoreCase(artifactGroupId);
		}
	}
}
