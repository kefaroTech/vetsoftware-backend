package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ObservationNamingConventionTest {

  private static final Path SOURCE_ROOT = Path.of("src", "main", "java");
  private static final Pattern OBSERVED_ANNOTATION =
      Pattern.compile("@Observed\\s*\\(([^)]*)\\)", Pattern.DOTALL);
  private static final Pattern NAME_ARGUMENT = Pattern.compile("\\bname\\s*=\\s*\"([^\"]+)\"");
  private static final Pattern CONTEXTUAL_NAME_ARGUMENT =
      Pattern.compile("\\bcontextualName\\s*=\\s*\"([^\"]+)\"");
  private static final Pattern OBSERVATION_NAME =
      Pattern.compile("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+$");
  private static final Pattern CONTEXTUAL_NAME =
      Pattern.compile("^[a-z][a-z0-9]*(?: [a-z][a-z0-9]*)+$");

  @Test
  void observedAnnotationsFollowTheDocumentedConvention() throws IOException {
    List<DeclaredObservation> observations = scanObservations();

    assertThat(observations).isNotEmpty();
    assertThat(observations)
        .allSatisfy(
            observation -> {
              assertThat(observation.name())
                  .as("nombre de observación en %s", observation.source())
                  .matches(OBSERVATION_NAME);
              if (observation.contextualName() != null) {
                assertThat(observation.contextualName())
                    .as("nombre contextual en %s", observation.source())
                    .matches(CONTEXTUAL_NAME);
              }
            });

    Set<String> uniqueNames = new HashSet<>();
    assertThat(observations)
        .allSatisfy(
            observation ->
                assertThat(uniqueNames.add(observation.name()))
                    .as("nombre de observación duplicado: %s", observation.name())
                    .isTrue());
  }

  private static List<DeclaredObservation> scanObservations() throws IOException {
    List<DeclaredObservation> observations = new ArrayList<>();
    try (var files = Files.walk(SOURCE_ROOT)) {
      for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String javaSource = Files.readString(source);
        var annotationMatcher = OBSERVED_ANNOTATION.matcher(javaSource);
        while (annotationMatcher.find()) {
          String arguments = annotationMatcher.group(1);
          var nameMatcher = NAME_ARGUMENT.matcher(arguments);
          assertThat(nameMatcher.find()).as("@Observed sin name explícito en %s", source).isTrue();
          var contextualMatcher = CONTEXTUAL_NAME_ARGUMENT.matcher(arguments);
          observations.add(
              new DeclaredObservation(
                  nameMatcher.group(1),
                  contextualMatcher.find() ? contextualMatcher.group(1) : null,
                  source));
        }
      }
    }
    return observations;
  }

  private record DeclaredObservation(String name, String contextualName, Path source) {}
}
