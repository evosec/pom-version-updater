package de.evosec.pomversionupdater;

import org.junit.Before;

public class DependencyInDependencyManagementTest extends AbstractTest {

	@Before
	public void setUp() throws Exception {
		groupId = "org.jsoup";
		select = "dependencyManagement version";
		version = "1.10.2";
	}

}
