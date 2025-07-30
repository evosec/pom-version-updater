package de.evosec.pomversionupdater;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class Artifact {

	private final String groupId;
	private final String artifactId;
	private final String type;
	private final String classifier;
	private final String version;

	Artifact(@NonNull String groupId, @NonNull String artifactId,
			@NonNull String type, @NonNull String classifier,
			@Nullable String version) {
		this.groupId = requireNonNull(groupId);
		this.artifactId = requireNonNull(artifactId);
		this.type = requireNonNull(type);
		this.classifier = requireNonNull(classifier);
		this.version = requireNonNull(version);
	}

	public String getType() {
		return type;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getVersion() {
		return version;
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

	/**
	 * Returns the string representation of the artifact in pattern
	 * {@code groupId:artifactId:type:classifier[:version]}. The {@code version}
	 * part is omitted if {@code null}.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(groupId).append(":")
			.append(artifactId)
			.append(":")
			.append(type)
			.append(":")
			.append(classifier);
		if (version != null) {
			builder.append(":").append(version);
		}
		return builder.toString();
	}

}
