package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.KeyValuePair;

/**
 * Verifica el motor de redacción (OBS-019) con <b>valores señuelo</b>: cadenas únicas e improbables
 * que, si aparecen en la salida, prueban una fuga.
 */
class LogRedactorTest {

  // Señuelos. Cada uno es único para que la aserción "no contiene" no dé falsos negativos.
  private static final String DECOY_PASSWORD = "Sup3rS3cr3t-Senuelo";
  private static final String DECOY_JWT =
      "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzZW51ZWxvIn0.Zm9yZ2VkU2lnbmF0dXJlU2VudWVsbw";
  private static final String DECOY_OPAQUE_TOKEN = "tok-senuelo-9f3a7c1b4e8d";
  private static final String DECOY_EMAIL_LOCAL = "senuelo.paciente";
  private static final String DECOY_PHONE = "+57 320 555 7788";
  private static final String DECOY_DOCUMENT = "1032456789";
  private static final String DECOY_CARD = "4111111111111111";

  // -------------------------------------------------------------------------------------------
  // Secretos
  // -------------------------------------------------------------------------------------------

  @Test
  void masksPasswordGivenAsKeyValue() {
    String redacted = LogRedactor.redact("login fallido password=" + DECOY_PASSWORD + " user=42");

    assertThat(redacted).doesNotContain(DECOY_PASSWORD);
    assertThat(redacted).isEqualTo("login fallido password=*** user=42");
  }

  @Test
  @DisplayName("la clave sigue visible: se sabe qué campo se suprimió sin conocer su contenido")
  void keepsTheKeyVisible() {
    assertThat(LogRedactor.redact("{\"clave\":\"" + DECOY_PASSWORD + "\"}"))
        .isEqualTo("{\"clave\":\"***\"}");
  }

  @Test
  void masksSecretKeysWithPrefixesAndSuffixes() {
    String redacted =
        LogRedactor.redact("refresh_token=" + DECOY_OPAQUE_TOKEN + " passwordHash=$2a$10$abcdef");

    assertThat(redacted).doesNotContain(DECOY_OPAQUE_TOKEN).doesNotContain("$2a$10$abcdef");
  }

  @Test
  void masksBareJwt() {
    assertThat(LogRedactor.redact("token rechazado: " + DECOY_JWT))
        .isEqualTo("token rechazado: ***");
  }

  @Test
  @DisplayName("una cabecera Authorization no deja escapar el token tras la palabra Bearer")
  void masksAuthorizationHeaderWholeValue() {
    // Regresión: si la regla clave-valor corriera antes que la de esquema HTTP, cortaría en el
    // espacio y solo enmascararía "Bearer", dejando el token visible.
    String redacted = LogRedactor.redact("Authorization: Bearer " + DECOY_JWT);

    assertThat(redacted).doesNotContain(DECOY_JWT).doesNotContain("eyJ");
  }

  @Test
  void masksOpaqueBearerAndBasicCredentials() {
    assertThat(LogRedactor.redact("Bearer " + DECOY_OPAQUE_TOKEN))
        .doesNotContain(DECOY_OPAQUE_TOKEN);
    assertThat(LogRedactor.redact("Basic c2VudWVsbzpzZWNyZXRv")).isEqualTo("Basic ***");
  }

  @Test
  void masksCredentialsEmbeddedInAConnectionUrl() {
    assertThat(LogRedactor.redact("jdbc:mysql://root:" + DECOY_PASSWORD + "@db:3306/vet"))
        .isEqualTo("jdbc:mysql://***:***@db:3306/vet");
  }

  @Test
  @DisplayName("el token de un enlace de restablecimiento no sobrevive")
  void masksTokenInAResetLink() {
    String redacted =
        LogRedactor.redact(
            "enlace https://app.vetrina.co/reset?token=" + DECOY_OPAQUE_TOKEN + " enviado");

    assertThat(redacted).doesNotContain(DECOY_OPAQUE_TOKEN);
  }

  // -------------------------------------------------------------------------------------------
  // Datos personales, tributarios, de pago y clínicos
  // -------------------------------------------------------------------------------------------

  @Test
  @DisplayName("el correo pierde la parte local y conserva el dominio para diagnóstico de entrega")
  void masksEmailLocalPartAndKeepsDomain() {
    assertThat(LogRedactor.redact("No se pudo enviar a " + DECOY_EMAIL_LOCAL + "@gmail.com"))
        .isEqualTo("No se pudo enviar a ***@gmail.com");
  }

  @Test
  void masksInternationalPhone() {
    assertThat(LogRedactor.redact("contacto " + DECOY_PHONE)).doesNotContain("320 555 7788");
  }

  @Test
  void masksStandaloneDocumentNumber() {
    assertThat(LogRedactor.redact("propietario con documento " + DECOY_DOCUMENT))
        .doesNotContain(DECOY_DOCUMENT);
  }

  @Test
  @DisplayName("la tarjeta conserva solo los últimos 4 dígitos, como permite PCI DSS")
  void masksCardNumberKeepingLastFour() {
    assertThat(LogRedactor.redact("pago con " + DECOY_CARD)).isEqualTo("pago con ***1111");
  }

  @Test
  void masksCardNumberWrittenWithSeparators() {
    assertThat(LogRedactor.redact("pago con 4111 1111 1111 1111")).isEqualTo("pago con ***1111");
  }

  @Test
  void masksNamedTaxAndClinicalFields() {
    String redacted =
        LogRedactor.redact("nit=900123456-7 diagnostico=Insuficiencia renal cronica felina");

    assertThat(redacted).doesNotContain("900123456").doesNotContain("Insuficiencia");
  }

  // -------------------------------------------------------------------------------------------
  // Fidelidad: lo que NO debe tocarse
  // -------------------------------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "mutation POST /api/v1/owners -> 201 (SUCCESS)",
        "Reconciliación DIAN finalizada: intentado(s)=3, fallido(s)=0, resultado=OK",
        "Documento 4821 marcado VALIDADO SIN SELLO",
        "Outbox de auditoría depurada; publicados eliminados=1204",
        "rate limited code=LOGIN_RATE_LIMITED",
        "Empresa 17: proveedor DIAN recuperado",
      })
  void leavesOperationalMessagesUntouched(String message) {
    assertThat(LogRedactor.redact(message)).isEqualTo(message);
  }

  @Test
  @DisplayName("un hash hex no se mutila: los lookarounds excluyen letras, no solo dígitos")
  void leavesHexHashesUntouched() {
    // Regresión: con lookarounds que solo excluyeran dígitos, un hash de 64 hex tiene ~50 % de
    // probabilidad de contener 10 dígitos seguidos por azar y los checkpoints de la cadena de
    // auditoría saldrían mutilados.
    String hash = "9f1204738295610384abcdef0123456789012345678901234567890123456789";
    String message = "Checkpoint de la cadena de auditoría emitido; posición=512 hash=" + hash;

    assertThat(LogRedactor.redact(message)).isEqualTo(message);
  }

  @Test
  void leavesIpAddressesAndTimestampsReadable() {
    assertThat(LogRedactor.redact("origen 192.0.2.10 en 2026-07-28T10:15:30"))
        .isEqualTo("origen 192.0.2.10 en 2026-07-28T10:15:30");
  }

  @Test
  void isIdempotent() {
    String once = LogRedactor.redact("password=" + DECOY_PASSWORD + " a " + DECOY_CARD);

    assertThat(LogRedactor.redact(once)).isEqualTo(once);
  }

  @Test
  void toleratesNullAndEmptyText() {
    assertThat(LogRedactor.redact(null)).isNull();
    assertThat(LogRedactor.redact("")).isEmpty();
  }

  @Test
  @DisplayName("devuelve la misma instancia si no hubo cambios, para no copiar el evento")
  void returnsSameInstanceWhenNothingChanged() {
    String clean = "login success type=EMPLOYEE";

    assertThat(LogRedactor.redact(clean)).isSameAs(clean);
  }

  // -------------------------------------------------------------------------------------------
  // Campos estructurados: allowlist
  // -------------------------------------------------------------------------------------------

  @Test
  @DisplayName("una clave MDC no declarada se enmascara por defecto")
  void masksMdcKeysOutsideTheAllowlist() {
    Map<String, String> mdc = new LinkedHashMap<>();
    mdc.put(MdcKeys.ACTOR_EMPLOYEE_ID, "77");
    mdc.put("owner.password", DECOY_PASSWORD);
    mdc.put("clinical.payload", "Anamnesis completa del paciente");

    Map<String, String> redacted = LogRedactor.redactMdc(mdc);

    assertThat(redacted).containsEntry(MdcKeys.ACTOR_EMPLOYEE_ID, "77");
    assertThat(redacted).containsEntry("owner.password", LogRedactor.MASK);
    assertThat(redacted).containsEntry("clinical.payload", LogRedactor.MASK);
  }

  @Test
  void scansAllowedFreeTextMdcValues() {
    Map<String, String> redacted =
        LogRedactor.redactMdc(
            Map.of(MdcKeys.HTTP_PATH, "/owners/by-email/" + DECOY_EMAIL_LOCAL + "@gmail.com"));

    assertThat(redacted.get(MdcKeys.HTTP_PATH)).isEqualTo("/owners/by-email/***@gmail.com");
  }

  @Test
  void keepsVerbatimMdcValuesExactlyAsProduced() {
    // El NIT del tenant es dato mercantil auditado a propósito: no debe caer en la regla de
    // documentos personales.
    Map<String, String> mdc = Map.of(MdcKeys.CLIENT_IP, "192.0.2.10");

    assertThat(LogRedactor.redactMdc(mdc)).isSameAs(mdc);
  }

  @Test
  void masksKeyValuePairsOutsideTheAllowlist() {
    List<KeyValuePair> redacted =
        LogRedactor.redactKeyValuePairs(
            List.of(
                new KeyValuePair("event", "login_success"),
                new KeyValuePair("http.durationMs", 37L),
                new KeyValuePair("owner.document", DECOY_DOCUMENT)));

    assertThat(redacted)
        .extracting(pair -> pair.value)
        .containsExactly("login_success", 37L, LogRedactor.MASK);
  }

  @Test
  void toleratesNullAndEmptyStructuredFields() {
    assertThat(LogRedactor.redactMdc(null)).isNull();
    assertThat(LogRedactor.redactMdc(Map.of())).isEmpty();
    assertThat(LogRedactor.redactKeyValuePairs(null)).isNull();
    assertThat(LogRedactor.redactKeyValuePairs(List.of())).isEmpty();
  }
}
