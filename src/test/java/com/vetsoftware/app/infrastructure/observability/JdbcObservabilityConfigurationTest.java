package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import net.ttddyy.dsproxy.StatementType;
import net.ttddyy.observation.boot.autoconfigure.JdbcProperties;
import net.ttddyy.observation.boot.autoconfigure.opentelemetry.DataSourceObservationOpenTelemetryAutoConfiguration;
import net.ttddyy.observation.boot.autoconfigure.opentelemetry.JdbcOpenTelemetryProperties;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryMeterObservationHandler;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryObservationConvention;
import net.ttddyy.observation.tracing.opentelemetry.QueryAnalysisResult;
import net.ttddyy.observation.tracing.opentelemetry.jsqlparser.JSqlParserQueryAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

class JdbcObservabilityConfigurationTest {

  private static final String SENSITIVE_SENTINEL = "patient-secret@example.test";

  @Test
  void observesOnlyQueriesWithSemanticAnalysisAndSafeDefaults() throws IOException {
    MutablePropertySources propertySources = loadApplicationProperties();
    Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
    PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);

    JdbcProperties jdbc =
        binder
            .bind("jdbc", Bindable.of(JdbcProperties.class))
            .orElseThrow(() -> new IllegalStateException("Missing jdbc configuration"));
    JdbcOpenTelemetryProperties openTelemetry =
        binder
            .bind("jdbc.opentelemetry", Bindable.of(JdbcOpenTelemetryProperties.class))
            .orElseThrow(
                () -> new IllegalStateException("Missing jdbc.opentelemetry configuration"));

    assertThat(jdbc.getIncludes()).containsExactly(JdbcProperties.TraceType.QUERY);
    assertThat(jdbc.getDatasourceProxy().isIncludeParameterValues()).isFalse();
    assertThat(jdbc.getDatasourceProxy().getQuery().isEnableLogging()).isFalse();

    assertThat(resolver.getProperty("jdbc.opentelemetry.enabled", Boolean.class)).isTrue();
    assertThat(resolver.getProperty("jdbc.opentelemetry.spans.enabled", Boolean.class)).isTrue();
    assertThat(resolver.getProperty("jdbc.opentelemetry.metrics.enabled", Boolean.class)).isFalse();

    JdbcOpenTelemetryProperties.Analysis analysis = openTelemetry.getAnalysis();
    assertThat(analysis.isEnabled()).isTrue();
    assertThat(analysis.getSummary().isEnabled()).isTrue();
    assertThat(analysis.getSanitize().isEnabled()).isTrue();
    assertThat(analysis.getCache().isEnabled()).isTrue();
    assertThat(analysis.getCache().getMaxSize()).isEqualTo(1000);
  }

  @Test
  void semanticAnalyzerRemovesSqlLiteralsAndExtractsStableAttributes() {
    JSqlParserQueryAnalyzer analyzer = new JSqlParserQueryAnalyzer();

    QueryAnalysisResult result =
        analyzer.analyze(
            "SELECT id, name FROM patients WHERE email = '"
                + SENSITIVE_SENTINEL
                + "' AND id = 912345",
            false,
            StatementType.STATEMENT);

    assertThat(result.getOperationName()).isEqualTo("SELECT");
    assertThat(result.getCollectionName()).isEqualTo("patients");
    assertThat(result.getQuerySummary()).containsIgnoringCase("SELECT").contains("patients");
    assertThat(result.getQueryText())
        .isNotBlank()
        .doesNotContain(SENSITIVE_SENTINEL)
        .doesNotContain("912345");
  }

  @Test
  void autoConfigurationRegistersSemanticSpansWithoutHighCardinalityMetricHandler() {
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(DataSourceObservationOpenTelemetryAutoConfiguration.class))
        .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
        .withPropertyValues(
            "jdbc.opentelemetry.enabled=true",
            "jdbc.opentelemetry.spans.enabled=true",
            "jdbc.opentelemetry.metrics.enabled=false",
            "jdbc.opentelemetry.analysis.enabled=true",
            "jdbc.opentelemetry.analysis.summary.enabled=true",
            "jdbc.opentelemetry.analysis.sanitize.enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(OpenTelemetryQueryObservationConvention.class);
              assertThat(context).hasSingleBean(OpenTelemetryQueryAnalyzer.class);
              assertThat(context).doesNotHaveBean(OpenTelemetryMeterObservationHandler.class);
            });
  }

  private static MutablePropertySources loadApplicationProperties() throws IOException {
    MutablePropertySources propertySources = new MutablePropertySources();
    new YamlPropertySourceLoader()
        .load("application", new ClassPathResource("application.yml"))
        .forEach(propertySources::addLast);
    return propertySources;
  }
}
