package de.evosec.pomversionupdater;

import static java.util.Objects.requireNonNull;

public class Artifact {

	private final String groupId;
	private final String artifactId;
	private String type;
	private String classifier;
	private String version;

	public Artifact(String groupId, String artifactId) {
		this.groupId = requireNonNull(groupId);
		this.artifactId = requireNonNull(artifactId);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		        + (artifactId == null ? 0 : artifactId.hashCode());
		result = prime * result
		        + (classifier == null ? 0 : classifier.hashCode());
		result = prime * result + (groupId == null ? 0 : groupId.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Artifact other = (Artifact) obj;
		if (artifactId == null) {
			if (other.artifactId != null) {
				return false;
			}
		} else if (!artifactId.equals(other.artifactId)) {
			return false;
		}
		if (classifier == null) {
			if (other.classifier != null) {
				return false;
			}
		} else if (!classifier.equals(other.classifier)) {
			return false;
		}
		if (groupId == null) {
			if (other.groupId != null) {
				return false;
			}
		} else if (!groupId.equals(other.groupId)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder =
		        new StringBuilder(groupId).append(":").append(artifactId);
		if (type != null) {
			builder.append(":").append(type);
		}
		if (classifier != null) {
			builder.append(":").append(classifier);
		}
		if (version != null) {
			builder.append(":").append(version);
		}
		return builder.toString();
	}

}
