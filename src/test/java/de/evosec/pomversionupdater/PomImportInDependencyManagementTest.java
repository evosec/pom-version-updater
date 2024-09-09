package de.evosec.pomversionupdater;

import org.junit.jupiter.api.BeforeEach;

public class PomImportInDependencyManagementTest extends AbstractTest {

	@BeforeEach
	public void setUp() {
		groupId = "com.fasterxml.jackson";
		select = "dependencyManagement version";
		version = "2.9.1";
	}

}
