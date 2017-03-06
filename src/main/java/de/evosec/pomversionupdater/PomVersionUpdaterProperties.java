package de.evosec.pomversionupdater;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pomversionupdater")
public class PomVersionUpdaterProperties {

	private String groupId;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

}
