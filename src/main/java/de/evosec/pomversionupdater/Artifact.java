package de.evosec.pomversionupdater;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(exclude = "version")
public class Artifact {

    @NonNull
    String groupId;
    @NonNull
    String artifactId;
    String type;
    String classifier;
    String version;

}
