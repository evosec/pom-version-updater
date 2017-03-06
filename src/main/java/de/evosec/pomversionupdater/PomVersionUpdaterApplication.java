package de.evosec.pomversionupdater;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;

@SpringBootApplication
public class PomVersionUpdaterApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(PomVersionUpdaterApplication.class, args);
	}

	private String mavenCommand;

	public PomVersionUpdaterApplication() {
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			mavenCommand = "mvn.cmd";
		} else {
			mavenCommand = "mvn";
		}
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Path pom = Paths.get("pom.xml").toAbsolutePath();
		try (Git git = Git.open(pom.getParent().toFile())) {

			assertWorkingTreeIsClean(git);

			Artifact beforeParent =
			        selectArtifactsFromPom(pom, "project > parent").get(0);
			if (beforeParent != null && beforeParent.getVersion() != null) {
				Assert.isTrue(
				    0 == new ProcessBuilder(mavenCommand, "--batch-mode",
				        "versions:update-parent", "-DgenerateBackupPoms=false")
				            .inheritIO().start().waitFor(),
				    "mvn failed");
				Artifact afterParent =
				        selectArtifactsFromPom(pom, "project > parent").get(0);
				commitIfNecessary(git, beforeParent, afterParent);
			}

			processDependencies(pom, git,
			    "project > dependencyManagement > dependencies > dependency");
			processDependencies(pom, git,
			    "project > dependencies > dependency");
		}
	}

	private void assertWorkingTreeIsClean(Git git)
	        throws IOException, Exception {
		git.getRepository().getRefDatabase().refresh();
		IndexDiff diffIndex = new IndexDiff(git.getRepository(), Constants.HEAD,
		    new FileTreeIterator(git.getRepository()));
		if (diffIndex.diff()) {
			throw new Exception("The working tree is not clean");
		}
	}

	private void processDependencies(Path pom, Git git, String selector)
	        throws Exception {
		List<Artifact> dependencies = selectArtifactsFromPom(pom, selector);
		for (Artifact dependency : dependencies.stream()
		    .filter(a -> a.getVersion() != null).collect(Collectors.toList())) {
			String include = String.format("%s:%s", dependency.getGroupId(),
			    dependency.getArtifactId());
			Assert.isTrue(0 == new ProcessBuilder(mavenCommand, "--batch-mode",
			    "versions:use-latest-versions", "-DgenerateBackupPoms=false",
			    "-Dinclude=" + include).inheritIO().start().waitFor(),
			    "mvn failed");
			Artifact afterDependency = selectArtifactsFromPom(pom, selector)
			    .stream().filter(a -> a.equals(dependency)).findAny().get();
			commitIfNecessary(git, dependency, afterDependency);
		}
	}

	private void commitIfNecessary(Git git, Artifact before, Artifact after)
	        throws Exception {
		if (!after.getVersion().equals(before.getVersion())) {
			String message = String.format("%s:%s: %s -> %s",
			    after.getGroupId(), after.getArtifactId(), before.getVersion(),
			    after.getVersion());
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
