package de.evosec.pomversionupdater;

import org.junit.jupiter.api.BeforeEach;

public class ParentPomTest extends AbstractTest {

	@BeforeEach
	public void setUp() {
		groupId = "org.springframework.boot";
		select = "parent > version";
		version = "1.5.5.RELEASE";
	}

}
