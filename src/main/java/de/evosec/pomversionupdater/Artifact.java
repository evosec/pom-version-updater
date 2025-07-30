package de.evosec.pomversionupdater;

import static java.util.Objects.requireNonNull;

public class Artifact {

	private final String groupId;
	private final String artifactId;
	private String type = "jar";
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
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Artifact artifact = (Artifact) o;
		return Objects.equals(groupId, artifact.groupId)
				&& Objects.equals(artifactId, artifact.artifactId)
				&& Objects.equals(type, artifact.type)
				&& Objects.equals(classifier, artifact.classifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, artifactId, type, classifier);
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
