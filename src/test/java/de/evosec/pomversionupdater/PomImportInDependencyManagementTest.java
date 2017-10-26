package de.evosec.pomversionupdater;

import org.junit.Before;

public class PomImportInDependencyManagementTest extends AbstractTest {

	@Before
	public void setUp() throws Exception {
		groupId = "com.fasterxml.jackson";
		select = "dependencyManagement version";
		version = "2.9.1";
	}

}
