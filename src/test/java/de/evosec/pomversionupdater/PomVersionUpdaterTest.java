package de.evosec.pomversionupdater;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

public class PomVersionUpdaterTest {

	@TempDir
	public Path directory;

	static Stream<Arguments> testCases() {
		TestCase parentPom = new TestCase("ParentPomTest.xml",
			"org.springframework.boot", "parent > version", "1.5.5.RELEASE");

		TestCase dependencyInDependencyManagement =
				new TestCase("DependencyInDependencyManagementTest.xml",
					"org.jsoup", "dependencyManagement version", "1.10.2");

		TestCase pomImportInDependencyManagement = new TestCase(
			"PomImportInDependencyManagementTest.xml", "com.fasterxml.jackson",
			"dependencyManagement version", "2.9.1");

		TestCase dependencyInDependencyManagementWithWildcard =
				new TestCase("DependencyInDependencyManagementTest.xml",
					"org.jsoup*", "dependencyManagement version", "1.10.2");

		TestCase pomImportInDependencyManagementWithWildcard =
				new TestCase("PomImportInDependencyManagementTest.xml",
					"com.fasterxml*", "dependencyManagement version", "2.9.1");

		return Stream.of(parentPom, dependencyInDependencyManagement,
			pomImportInDependencyManagement,
			dependencyInDependencyManagementWithWildcard,
			pomImportInDependencyManagementWithWildcard);
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void createPrototypeBean(String pomXml, String groupId, String select,
			String version) throws Exception {
		System.setProperty("user.dir", directory.toString());

		Path pom = directory.resolve("pom.xml");
		Files.copy(new ClassPathResource(pomXml).getInputStream(), pom);

		PomVersionUpdaterApplication
			.main(new String[] {"--groupId=" + groupId});

		try (InputStream inputStream = Files.newInputStream(pom)) {
			Document document = Jsoup.parse(inputStream, UTF_8.name(), "",
				Parser.xmlParser());
			Assertions.assertThat(document.select(select).first().text())
				.isNotEqualTo(version);
		}
	}

	private record TestCase(String pomXml, String groupId, String select,
			String version) implements Arguments {

		@Override
		public Object[] get() {
			return new Object[] {pomXml, groupId, select, version};
		}
	}
}
