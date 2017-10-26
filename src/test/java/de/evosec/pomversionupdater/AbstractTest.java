package de.evosec.pomversionupdater;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

public abstract class AbstractTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	protected String groupId = "";

	@Test
	public void testName() throws Exception {
		Path directory = folder.getRoot().toPath();
		System.setProperty("user.dir", directory.toString());

		Files.copy(new ClassPathResource(
		    this.getClass().getSimpleName() + "-beforePom.xml")
		        .getInputStream(),
		    directory.resolve("pom.xml"));
		Files.copy(new ClassPathResource(
		    this.getClass().getSimpleName() + "-afterPom.xml").getInputStream(),
		    directory.resolve("afterPom.xml"));

		PomVersionUpdaterApplication
		    .main(new String[] {"--groupId=" + groupId});

		assertThat(directory.resolve("pom.xml"))
		    .hasSameContentAs(directory.resolve("afterPom.xml"));
	}

}
