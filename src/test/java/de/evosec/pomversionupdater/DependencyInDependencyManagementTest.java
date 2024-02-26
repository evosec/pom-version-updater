package de.evosec.pomversionupdater;

import org.junit.jupiter.api.BeforeEach;

public class DependencyInDependencyManagementTest extends AbstractTest {

	@BeforeEach
	public void setUp() {
		groupId = "org.jsoup";
		select = "dependencyManagement version";
		version = "1.10.2";
	}

}
