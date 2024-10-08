package de.evosec.pomversionupdater;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

public abstract class AbstractTest {

	@TempDir
	public Path directory;

	protected String groupId = "";
	protected String select = "";
	protected String version = "";

	@Test
	public void testName() throws Exception {
		System.setProperty("user.dir", directory.toString());

		Path pom = directory.resolve("pom.xml");
		Files.copy(
			new ClassPathResource(this.getClass().getSimpleName() + ".xml")
				.getInputStream(),
			pom);

		PomVersionUpdaterApplication
			.main(new String[] {"--groupId=" + groupId});

		try (InputStream inputStream = Files.newInputStream(pom)) {
			Document document = Jsoup.parse(inputStream, UTF_8.name(), "",
				Parser.xmlParser());
			Assertions.assertThat(document.select(select).first().text())
				.isNotEqualTo(version);
		}
	}

}
