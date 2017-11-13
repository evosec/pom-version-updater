package de.evosec.pomversionupdater;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.beans.factory.annotation.Autowired;
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

	public static void main(String[] args) {
		SpringApplication.run(PomVersionUpdaterApplication.class, args);
	}

	@Autowired
	private PomVersionUpdaterProperties properties;
	private String mavenCommand;
	private final Path workingDirectory =
	        Paths.get(System.getProperty("user.dir", "."));

	public PomVersionUpdaterApplication() {
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
			        selectArtifactsFromPom(pom, "project > parent").stream()
			            .findFirst();
			if (beforeParent.isPresent()
			        && beforeParent.get().getVersion() != null) {
				beforeParent.get().setType("pom");
				ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand,
				    "--batch-mode", "--update-snapshots", "--non-recursive",
				    "versions:update-parent", "-DgenerateBackupPoms=false")
				        .inheritIO().directory(workingDirectory.toFile());
				LOG.info("Calling {}", processBuilder.command());
				Assert.isTrue(0 == processBuilder.start().waitFor(),
				    "mvn failed");
				Artifact afterParent =
				        selectArtifactsFromPom(pom, "project > parent").get(0);
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

	private void assertWorkingTreeIsClean(Git git)
	        throws IOException, Exception {
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
		List<Artifact> dependencies = selectArtifactsFromPom(pom, selector);
		for (Artifact dependency : dependencies.stream()
		    .filter(a -> a.getVersion() != null).collect(Collectors.toList())) {
			ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand,
			    "--batch-mode", "--update-snapshots", "--non-recursive",
			    "versions:use-latest-versions", "-DgenerateBackupPoms=false",
			    "-Dincludes=" + dependency).inheritIO()
			        .directory(workingDirectory.toFile());
			LOG.info("Calling {} in {}", processBuilder.command(),
			    processBuilder.directory());
			Assert.isTrue(0 == processBuilder.start().waitFor(), "mvn failed");
			Artifact afterDependency = selectArtifactsFromPom(pom, selector)
			    .stream().filter(a -> a.equals(dependency)).findAny().get();
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
			git.add().addFilepattern("pom.xml").call();
			git.commit().setAllowEmpty(false).setMessage(message).call();
			assertWorkingTreeIsClean(git);
		}
	}

	private List<Artifact> selectArtifactsFromPom(Path pom, String selector)
	        throws IOException {
		List<Artifact> artifacts = new ArrayList<>();
		try (InputStream inputStream = Files.newInputStream(pom)) {
			Document document = Jsoup.parse(inputStream, UTF_8.name(), "",
			    Parser.xmlParser());
			for (Element element : document.select(selector)) {
				Artifact artifact =
				        new Artifact(element.select("groupId").first().text(),
				            element.select("artifactId").first().text());
				if (!properties.getGroupId().isEmpty() && !properties
				    .getGroupId().equalsIgnoreCase(artifact.getGroupId())) {
					continue;
				}
				Elements versionSelect = element.select("version");
				if (!versionSelect.isEmpty()) {
					artifact.setVersion(versionSelect.first().text());
				}
				Elements classifierSelect = element.select("classifier");
				if (!classifierSelect.isEmpty()) {
					artifact.setClassifier(classifierSelect.first().text());
				}
				Elements typeSelect = element.select("type");
				if (!typeSelect.isEmpty()) {
					artifact.setType(typeSelect.first().text());
				}
				artifacts.add(artifact);
			}
		}
		return artifacts;
	}

}
