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
 * Prueba de gobierno sobre {@code logback-spring.xml}: la redacción no puede
 * depender de que alguien recuerde envolver el appender que añade.
 *
 * <p>
 * Afirma que <b>todo</b> appender referenciado desde {@code <root>} —en
 * cualquier perfil— es un {@link RedactingAppender}. Un destino nuevo
 * enganchado directamente a la raíz rompe esta prueba, que es el único momento
 * razonable para enterarse.
 */
class LogbackRedactionConfigTest {

    private static final String REDACTING_APPENDER = RedactingAppender.class.getName();

    /**
     * ⛔ <b>La lista cerrada de loggers que pueden llevar un appender sin
     * redactar.</b> Son dos y solo dos:
     *
     * <ul>
     * <li>{@code DEV_EMAIL_PREVIEW} — el enlace o los códigos del correo que no se
     * envió, ver {@link DevEmailPreview};</li>
     * <li>{@code AI_PAYLOAD} — el prompt entero y la respuesta entera del modelo,
     * ver {@code BedrockModelInvoker}.</li>
     * </ul>
     *
     * <p>
     * <b>Es una lista y no un nombre porque un nombre ya se quedó corto.</b> Al
     * entrar {@code AI_PAYLOAD} esta prueba se puso roja diciendo
     * {@code expected "DEV_EMAIL_PREVIEW" but was "AI_PAYLOAD"}, y la salida fácil
     * —relajar la aserción a "cualquier logger vale"— habría dejado la política de
     * redacción sin gate. Añadir un tercer canal exige tocar esta constante, que es
     * el único momento razonable para que alguien lo mire.
     */
    private static final List<String> CANALES_SIN_REDACTAR = List.of("DEV_EMAIL_PREVIEW",
            "AI_PAYLOAD");

    /**
     * El perfil en el que —y solo en el que— pueden existir esos dos canales. Fuera
     * de local no se declaran, así que sus eventos caen en la raíz redactada.
     */
    private static final String PERFIL_LOCAL = "!prod & !dev";

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

    /**
     * Appenders referenciados desde un elemento dado ({@code root} o
     * {@code logger}).
     */
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
                        reference -> assertThat(appenders.get(reference))
                                .as("el appender '%s' referenciado desde <root> debe ser un %s",
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
                    .as("el appender redactor '%s' no envuelve ningún destino",
                            appender.getAttribute("name"))
                    .isPositive();
            for (int j = 0; j < nested.getLength(); j++) {
                String reference = ((Element) nested.item(j)).getAttribute("ref");
                assertThat(appenders).containsKey(reference);
            }
        }
    }

    @Test
    @DisplayName("los únicos loggers que pueden saltarse la redacción son los dos canales locales")
    void theOnlyUnredactedChannelsAreTheTwoLocalOnes() {
        Map<String, String> appenders = declaredAppenders();

        NodeList loggers = configuration.getElementsByTagName("logger");
        for (int i = 0; i < loggers.getLength(); i++) {
            Element logger = (Element) loggers.item(i);
            String nombre = logger.getAttribute("name");
            NodeList references = logger.getElementsByTagName("appender-ref");
            for (int j = 0; j < references.getLength(); j++) {
                String reference = ((Element) references.item(j)).getAttribute("ref");
                if (REDACTING_APPENDER.equals(appenders.get(reference))) {
                    continue;
                }
                // Un logger con appender crudo solo se admite si está en la lista cerrada,
                // aislado con additivity="false" y sin ruta hacia el appender de OpenTelemetry.
                assertThat(nombre)
                        .as("el logger '%s' lleva el appender crudo '%s' y no es un canal"
                                + " declarado sin redacción", nombre, reference)
                        .isIn(CANALES_SIN_REDACTAR);
                assertThat(logger.getAttribute("additivity"))
                        .as("el canal sin redactar '%s' debe ser additivity=false: si propaga,"
                                + " su contenido alcanza la raíz y con ella el pipeline"
                                + " exportado", nombre)
                        .isEqualTo("false");
                assertThat(appenders.get(reference))
                        .as("el canal sin redactar '%s' solo puede escribir a consola", nombre)
                        .contains("ConsoleAppender");
            }
        }
    }

    /**
     * ⛔ <b>Comprueba los DOS canales, no uno.</b> Antes filtraba por el nombre
     * {@code DEV_EMAIL_PREVIEW}, de modo que {@code AI_PAYLOAD} tenía la propiedad
     * correcta —estar dentro del perfil local— <b>sin nadie que la vigilara</b>:
     * sacarlo del {@code <springProfile>} habría publicado el prompt entero del
     * prospecto en CloudWatch y en Grafana Cloud sin poner una sola prueba en rojo.
     *
     * <p>
     * Y exige la lista <b>completa</b>, no una inclusión: si alguien borra un canal
     * de {@code logback-spring.xml} y se olvida de {@link #CANALES_SIN_REDACTAR},
     * esto también se entera.
     */
    @Test
    @DisplayName("los dos canales sin redacción están declarados y solo fuera de prod y dev")
    void theUnredactedChannelsAreScopedToLocalProfiles() {
        NodeList profiles = configuration.getElementsByTagName("springProfile");
        List<String> declared = new ArrayList<>();

        for (int i = 0; i < profiles.getLength(); i++) {
            Element profile = (Element) profiles.item(i);
            NodeList loggers = profile.getElementsByTagName("logger");
            for (int j = 0; j < loggers.getLength(); j++) {
                String nombre = ((Element) loggers.item(j)).getAttribute("name");
                if (!CANALES_SIN_REDACTAR.contains(nombre)) {
                    continue;
                }
                declared.add(nombre);
                assertThat(profile.getAttribute("name")).as(
                        "el canal sin redactar '%s' está declarado en el perfil '%s': fuera"
                                + " de local su contenido llega a Loki sin redactar",
                        nombre, profile.getAttribute("name")).isEqualTo(PERFIL_LOCAL);
            }
        }
        assertThat(declared)
                .as("cada canal sin redacción debe declararse dentro de un <springProfile>, no"
                        + " globalmente ni en ninguna otra parte")
                .containsExactlyInAnyOrderElementsOf(CANALES_SIN_REDACTAR);
    }
}
