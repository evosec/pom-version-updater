package de.evosec.pomversionupdater;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Path;

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

@SpringBootApplication
public class PomVersionUpdaterApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(PomVersionUpdaterApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Path pom = Paths.get("pom.xml").toAbsolutePath();
		Git git = Git.open(pom.getParent().toFile());

		git.getRepository().getRefDatabase().refresh();
		IndexDiff diffIndex = new IndexDiff(git.getRepository(), Constants.HEAD, new FileTreeIterator(git.getRepository()));
		if (diffIndex.diff()) {
			throw new Exception("The working tree is not clean");
		}

		Artifact beforeParent = selectArtifactsFromPom(pom, "project > parent").get(0);
		if (beforeParent != null && beforeParent.getVersion() != null) {
			// TODO: log output and check return code
			new ProcessBuilder("mvn", "versions:update-parent", "-DgenerateBackupPoms=false").start().waitFor();
			Artifact afterParent = selectArtifactsFromPom(pom, "project > parent").get(0);
			commitIfNecessary(git, beforeParent, afterParent);
		}

		processDependencies(pom, git, "project > dependencyManagement > dependencies > dependency");
		processDependencies(pom, git, "project > dependencies > dependency");
	}

	private void processDependencies(Path pom, Git git, String selector) throws Exception {
		List<Artifact> dependencies = selectArtifactsFromPom(pom, selector);
		for (Artifact dependency : dependencies.stream().filter(a -> a.getVersion() != null).collect(Collectors.toList())) {
			String include = String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId());
			new ProcessBuilder("mvn", "versions:use-latest-versions", "-DgenerateBackupPoms=false", "-Dinclude=" + include).start().waitFor();
			Artifact afterDependency = selectArtifactsFromPom(pom, selector).stream().filter(a -> a.equals(dependency)).findAny().get();
			commitIfNecessary(git, dependency, afterDependency);
		}
	}

	private void commitIfNecessary(Git git, Artifact before, Artifact after) throws Exception {
		if (!after.getVersion().equals(before.getVersion())) {
			String message = String.format("%s:%s: %s -> %s", after.getGroupId(), after.getArtifactId(), before.getVersion(), after.getVersion());
			git.add().addFilepattern("pom.xml").call();
			git.commit().setAllowEmpty(false).setMessage(message).call();
		}
	}

	private List<Artifact> selectArtifactsFromPom(Path pom, String selector) throws IOException {
		List<Artifact> artifacts = new ArrayList<>();
		try(InputStream inputStream = Files.newInputStream(pom)) {
			Document document = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "", Parser.xmlParser());
			for (Element element : document.select(selector)) {
				Artifact.ArtifactBuilder builder = new Artifact.ArtifactBuilder();
				builder.groupId(element.select("groupId").first().text());
				builder.artifactId(element.select("artifactId").first().text());
				Elements versionSelect = element.select("version");
				if (!versionSelect.isEmpty()) {
					builder.version(versionSelect.first().text());
				}
				Elements classifierSelect = element.select("classifier");
				if (!classifierSelect.isEmpty()) {
					builder.classifier(classifierSelect.first().text());
				}
				Elements typeSelect = element.select("type");
				if (!typeSelect.isEmpty()) {
					builder.type(typeSelect.first().text());
				}
				artifacts.add(builder.build());
			}
		}
		return artifacts;
	}

}
