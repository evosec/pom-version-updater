package de.evosec.pomversionupdater;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

public class ParentPomTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void test() throws Exception {
		Path directory = folder.getRoot().toPath();
		System.setProperty("user.dir", directory.toString());

		Path pom = directory.resolve("pom.xml");
		Files.copy(new ClassPathResource(
		    this.getClass().getSimpleName() + "-beforePom.xml")
		        .getInputStream(),
		    pom);

		PomVersionUpdaterApplication
		    .main(new String[] {"--groupId=org.springframework.boot"});

		try (InputStream inputStream = Files.newInputStream(pom)) {
			Document document = Jsoup.parse(inputStream, UTF_8.name(), "",
			    Parser.xmlParser());
			Assertions.assertThat(
			    document.select("project > parent > version").first().text())
			    .isNotEqualTo("1.5.5.RELEASE");
		}
	}

}
