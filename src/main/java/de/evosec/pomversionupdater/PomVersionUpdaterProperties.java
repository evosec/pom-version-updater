package de.evosec.pomversionupdater;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pomversionupdater")
public class PomVersionUpdaterProperties {

	private String groupId;
	private boolean allowMajorUpdates = false;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public boolean isAllowMajorUpdates() {
		return allowMajorUpdates;
	}

	public void setAllowMajorUpdates(boolean allowMajorUpdates) {
		this.allowMajorUpdates = allowMajorUpdates;
	}
}
