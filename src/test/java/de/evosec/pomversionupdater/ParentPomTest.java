package de.evosec.pomversionupdater;

import org.junit.Before;

public class ParentPomTest extends AbstractTest {

	@Before
	public void setUp() throws Exception {
		groupId = "org.springframework.boot";
		select = "parent > version";
		version = "1.5.5.RELEASE";
	}

}
