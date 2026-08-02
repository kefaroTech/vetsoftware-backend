package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Prueba de gobierno sobre {@code logback-spring.xml}: la redacción no puede depender de que
 * alguien recuerde envolver el appender que añade.
 *
 * <p>Afirma que <b>todo</b> appender referenciado desde {@code <root>} —en cualquier perfil— es un
 * {@link RedactingAppender}. Un destino nuevo enganchado directamente a la raíz rompe esta prueba,
 * que es el único momento razonable para enterarse.
 */
class LogbackRedactionConfigTest {

  private static final String REDACTING_APPENDER = RedactingAppender.class.getName();

  private static Document configuration;

  @BeforeAll
  static void parseLogbackConfiguration() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    try (var input = new ClassPathResource("logback-spring.xml").getInputStream()) {
      configuration = factory.newDocumentBuilder().parse(input);
    }
  }

  /** Nombre de appender → clase declarada. */
  private static Map<String, String> declaredAppenders() {
    Map<String, String> appenders = new LinkedHashMap<>();
    NodeList nodes = configuration.getElementsByTagName("appender");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element appender = (Element) nodes.item(i);
      appenders.put(appender.getAttribute("name"), appender.getAttribute("class"));
    }
    return appenders;
  }

  /** Appenders referenciados desde un elemento dado ({@code root} o {@code logger}). */
  private static List<String> referencesFrom(String parentTag) {
    List<String> references = new ArrayList<>();
    NodeList parents = configuration.getElementsByTagName(parentTag);
    for (int i = 0; i < parents.getLength(); i++) {
      NodeList children = parents.item(i).getChildNodes();
      for (int j = 0; j < children.getLength(); j++) {
        Node child = children.item(j);
        if (child.getNodeType() == Node.ELEMENT_NODE
            && "appender-ref".equals(child.getNodeName())) {
          references.add(((Element) child).getAttribute("ref"));
        }
      }
    }
    return references;
  }

  @Test
  @DisplayName("todo appender de la raíz pasa por la redacción, en todos los perfiles")
  void everyRootAppenderIsRedacting() {
    Map<String, String> appenders = declaredAppenders();
    List<String> rootReferences = referencesFrom("root");

    assertThat(rootReferences).isNotEmpty();
    assertThat(rootReferences)
        .allSatisfy(
            reference ->
                assertThat(appenders.get(reference))
                    .as(
                        "el appender '%s' referenciado desde <root> debe ser un %s",
                        reference, REDACTING_APPENDER)
                    .isEqualTo(REDACTING_APPENDER));
  }

  @Test
  @DisplayName("cada appender redactor envuelve al menos un destino")
  void everyRedactingAppenderWrapsADestination() {
    Map<String, String> appenders = declaredAppenders();
    NodeList nodes = configuration.getElementsByTagName("appender");

    for (int i = 0; i < nodes.getLength(); i++) {
      Element appender = (Element) nodes.item(i);
      if (!REDACTING_APPENDER.equals(appender.getAttribute("class"))) {
        continue;
      }
      NodeList nested = appender.getElementsByTagName("appender-ref");
      assertThat(nested.getLength())
          .as("el appender redactor '%s' no envuelve ningún destino", appender.getAttribute("name"))
          .isPositive();
      for (int j = 0; j < nested.getLength(); j++) {
        String reference = ((Element) nested.item(j)).getAttribute("ref");
        assertThat(appenders).containsKey(reference);
      }
    }
  }

  @Test
  @DisplayName("el único logger que puede saltarse la redacción es la previsualización local")
  void theOnlyUnredactedChannelIsTheLocalEmailPreview() {
    Map<String, String> appenders = declaredAppenders();

    NodeList loggers = configuration.getElementsByTagName("logger");
    for (int i = 0; i < loggers.getLength(); i++) {
      Element logger = (Element) loggers.item(i);
      NodeList references = logger.getElementsByTagName("appender-ref");
      for (int j = 0; j < references.getLength(); j++) {
        String reference = ((Element) references.item(j)).getAttribute("ref");
        if (REDACTING_APPENDER.equals(appenders.get(reference))) {
          continue;
        }
        // Un logger con appender crudo solo se admite si es el canal de previsualización,
        // aislado con additivity="false" y sin ruta hacia el appender de OpenTelemetry.
        assertThat(logger.getAttribute("name")).isEqualTo("DEV_EMAIL_PREVIEW");
        assertThat(logger.getAttribute("additivity")).isEqualTo("false");
        assertThat(appenders.get(reference)).contains("ConsoleAppender");
      }
    }
  }

  @Test
  @DisplayName("el canal de previsualización está declarado y solo fuera de prod y dev")
  void theEmailPreviewChannelIsScopedToLocalProfiles() {
    NodeList profiles = configuration.getElementsByTagName("springProfile");
    boolean declared = false;

    for (int i = 0; i < profiles.getLength(); i++) {
      Element profile = (Element) profiles.item(i);
      NodeList loggers = profile.getElementsByTagName("logger");
      for (int j = 0; j < loggers.getLength(); j++) {
        if (!"DEV_EMAIL_PREVIEW".equals(((Element) loggers.item(j)).getAttribute("name"))) {
          continue;
        }
        declared = true;
        assertThat(profile.getAttribute("name")).isEqualTo("!prod & !dev");
      }
    }
    assertThat(declared)
        .as("DEV_EMAIL_PREVIEW debe declararse dentro de un <springProfile>, no globalmente")
        .isTrue();
  }
}
