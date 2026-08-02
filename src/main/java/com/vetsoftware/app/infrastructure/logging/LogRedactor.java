package com.vetsoftware.app.infrastructure.logging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.event.KeyValuePair;

/**
 * Motor central de redacción de logs. Sin dependencias de Logback ni de Spring: es una función pura
 * y por tanto verificable con valores señuelo (ver {@code LogRedactorTest}).
 *
 * <p>Cubre las dos superficies por las que un dato sensible puede escapar a archivos y a Loki:
 *
 * <ol>
 *   <li><b>Campos estructurados</b> (MDC y {@code KeyValuePair}) — gobernados por <b>allowlist</b>
 *       ({@link LogFieldPolicy}). Lo que no está declarado se enmascara. Es una garantía cerrada:
 *       un campo nuevo es opaco por defecto.
 *   <li><b>Texto libre</b> (mensaje formateado y mensajes de excepción) — gobernado por
 *       <b>patrones</b>. Aquí no cabe una allowlist, así que es defensa en profundidad de mejor
 *       esfuerzo: detecta las formas de alta confianza (credenciales por clave, JWT, {@code
 *       Bearer}, correos, teléfonos internacionales, tarjetas válidas por Luhn, documentos y campos
 *       clínicos nombrados). No pretende detectar prosa clínica sin clave — para eso la regla sigue
 *       siendo no registrar entidades ni payloads completos.
 * </ol>
 *
 * <p><b>Orden de las reglas de texto:</b> importa y no debe alterarse a la ligera. Las de forma
 * (JWT, {@code Bearer}, Luhn) van <em>antes</em> que la de clave-valor, porque {@code
 * Authorization: Bearer eyJ...} tiene un valor con espacios: si la regla de clave-valor corriera
 * primero, cortaría en el espacio y solo enmascararía la palabra {@code Bearer}, dejando el token
 * intacto.
 *
 * <p>Todas las reglas son idempotentes: aplicar la redacción a un texto ya redactado no lo cambia.
 *
 * @see LogFieldPolicy
 * @see RedactingAppender
 */
public final class LogRedactor {

  /** Reemplazo único para todo valor suprimido. Buscable en Grafana como señal de fuga evitada. */
  public static final String MASK = "***";

  private LogRedactor() {}

  // ---------------------------------------------------------------------------------------------
  // Vocabulario de claves sensibles (para la regla clave-valor sobre texto libre)
  // ---------------------------------------------------------------------------------------------

  /**
   * Claves cuyo valor es un secreto. La alternación no necesita las variantes con prefijo ({@code
   * refresh_token}, {@code client_secret}): el lookbehind de {@link #KEYED_VALUE} excluye solo
   * {@code [A-Za-z0-9]}, así que {@code _} y {@code .} ya funcionan como frontera y {@code token}
   * casa dentro de {@code refresh_token}.
   */
  private static final String SECRET_KEYS =
      String.join(
          "|",
          "password",
          "passwd",
          "pwd",
          "contrasena",
          "contrase\u00f1a",
          "clave",
          "secret",
          "token",
          "jwt",
          "apikey",
          "api_key",
          "api-key",
          "authorization",
          "auth",
          "credential",
          "credentials",
          "privatekey",
          "private_key",
          "signature",
          "otp",
          "pin",
          "cvv",
          "cvc",
          "enckey",
          "encryptionkey");

  /**
   * Claves cuyo valor es dato personal, tributario, de pago o clínico. Se enmascara el valor
   * completo: a diferencia de un secreto, aquí no hay forma reconocible que permita ser más fino.
   */
  private static final String PII_KEYS =
      String.join(
          "|",
          // Contacto
          "email",
          "e-mail",
          "correo",
          "mail",
          "phone",
          "telephone",
          "telefono",
          "tel\u00e9fono",
          "celular",
          "mobile",
          "cellphone",
          "whatsapp",
          "address",
          "direccion",
          "direcci\u00f3n",
          // Documentos e identidad
          "document",
          "documento",
          "identification",
          "cedula",
          "c\u00e9dula",
          "passport",
          "pasaporte",
          "dni",
          "birthdate",
          "fechanacimiento",
          "dob",
          // Tributario y de pago
          "nit",
          "taxid",
          "tax_id",
          "rut",
          "iban",
          "accountnumber",
          "cuenta",
          "card",
          "cardnumber",
          "tarjeta",
          "pan",
          // Payload clínico
          "diagnosis",
          "diagnostico",
          "diagn\u00f3stico",
          "anamnesis",
          "symptoms",
          "sintomas",
          "s\u00edntomas",
          "treatment",
          "tratamiento",
          "prescription",
          "receta",
          "medication",
          "medicamento",
          "subjective",
          "objective",
          "assessment",
          "clinicalnotes",
          "observaciones",
          "notes");

  // ---------------------------------------------------------------------------------------------
  // Patrones
  // ---------------------------------------------------------------------------------------------

  /** {@code scheme://usuario:contraseña@host} en URLs de conexión. */
  private static final Pattern URL_CREDENTIALS = Pattern.compile("://[^\\s:/@]+:[^\\s:/@]+@");

  /** JWT compacto, con o sin prefijo {@code Bearer}. Los tres segmentos son base64url. */
  private static final Pattern JWT =
      Pattern.compile("\\beyJ[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{4,}(?:\\.[A-Za-z0-9_-]*)?");

  /** Credencial de esquema HTTP: {@code Bearer <token>} / {@code Basic <base64>}. */
  private static final Pattern HTTP_AUTH_SCHEME =
      Pattern.compile("\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]{8,}", Pattern.CASE_INSENSITIVE);

  /** Candidato a PAN: 13–19 dígitos con separadores opcionales. Se confirma con Luhn. */
  private static final Pattern CARD_CANDIDATE =
      Pattern.compile("(?<![0-9A-Za-z])(?:[0-9][ -]?){12,18}[0-9](?![0-9A-Za-z])");

  /**
   * Clave sensible seguida de su valor. El grupo 1 es el valor: entrecomillado (se conservan las
   * comillas) o hasta el primer delimitador. El sufijo {@code [A-Za-z0-9_]*} tras la clave hace que
   * {@code passwordHash} o {@code tokenValue} también casen.
   */
  private static final Pattern KEYED_VALUE =
      Pattern.compile(
          "(?<![A-Za-z0-9])(?:"
              + SECRET_KEYS
              + "|"
              + PII_KEYS
              + ")[A-Za-z0-9_]*"
              + "\"?\\s*(?::|=>|=)\\s*"
              + "(\"[^\"]*\"|'[^']*'|[^\\s,;&}\\])\"']+)",
          Pattern.CASE_INSENSITIVE);

  /** Dirección de correo. Se conserva el dominio (no es dato personal y sirve para diagnóstico). */
  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+)");

  /** Teléfono en formato internacional explícito ({@code +57 300 123 4567}). */
  private static final Pattern INTERNATIONAL_PHONE =
      Pattern.compile("(?<![0-9A-Za-z])\\+[0-9][0-9 ().-]{6,18}[0-9](?![0-9])");

  /**
   * Corrida de 10 dígitos o más <b>aislada</b>: cédulas, NIT con dígito de verificación y celulares
   * nacionales. El umbral 10 es deliberado — por debajo se solapa con importes e ids de entidad,
   * que son la mayoría de los números que se registran. Los documentos más cortos quedan cubiertos
   * por {@link #KEYED_VALUE} cuando vienen con su clave.
   *
   * <p>Los lookarounds excluyen letras además de dígitos, de modo que la regla no muerda dentro de
   * un hash, un UUID o un id alfanumérico. Sin eso, un hash hex de 64 caracteres tiene ~50 % de
   * probabilidad de contener 10 dígitos seguidos por azar y los checkpoints de la cadena de
   * auditoría saldrían mutilados.
   */
  private static final Pattern LONG_DIGIT_RUN =
      Pattern.compile("(?<![0-9A-Za-z])[0-9]{10,}(?![0-9A-Za-z])");

  // ---------------------------------------------------------------------------------------------
  // Texto libre
  // ---------------------------------------------------------------------------------------------

  /**
   * Aplica el enmascarado de texto. Devuelve la <b>misma instancia</b> si no hubo cambios, para que
   * el appender pueda evitar copiar el evento en el caso normal.
   *
   * @param text texto a redactar; puede ser {@code null}
   * @return el texto redactado, o el original si ninguna regla aplicó
   */
  public static String redact(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    // Un solo barrido para decidir qué reglas pueden casar. Evita 8 pasadas de regex sobre la
    // gran mayoría de mensajes, que no contienen ningún candidato.
    boolean digit = false;
    boolean at = false;
    boolean separator = false;
    boolean plus = false;
    boolean slash = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= '0' && c <= '9') digit = true;
      else if (c == '@') at = true;
      else if (c == ':' || c == '=') separator = true;
      else if (c == '+') plus = true;
      else if (c == '/') slash = true;
    }

    String result = text;
    if (separator && slash) {
      result = replaceAll(result, URL_CREDENTIALS, "://" + MASK + ":" + MASK + "@");
    }
    if (result.indexOf("eyJ") >= 0) {
      result = replaceAll(result, JWT, MASK);
    }
    result = maskHttpAuthScheme(result);
    if (digit) {
      result = maskCardNumbers(result);
    }
    if (separator) {
      result = maskKeyedValues(result);
    }
    if (at) {
      result = replaceAll(result, EMAIL, MASK + "@$1");
    }
    if (plus) {
      result = replaceAll(result, INTERNATIONAL_PHONE, MASK);
    }
    if (digit) {
      result = replaceAll(result, LONG_DIGIT_RUN, MASK);
    }
    return result;
  }

  private static String replaceAll(String text, Pattern pattern, String replacement) {
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? matcher.reset().replaceAll(replacement) : text;
  }

  /** {@code Bearer <token>} → {@code Bearer ***}, conservando el esquema tal como se escribió. */
  private static String maskHttpAuthScheme(String text) {
    Matcher matcher = HTTP_AUTH_SCHEME.matcher(text);
    if (!matcher.find()) {
      return text;
    }
    return matcher.reset().replaceAll("$1 " + MASK);
  }

  /**
   * Enmascara solo el valor de cada clave sensible, dejando la clave visible: quien lea el log sabe
   * qué campo se suprimió sin conocer su contenido.
   */
  private static String maskKeyedValues(String text) {
    Matcher matcher = KEYED_VALUE.matcher(text);
    if (!matcher.find()) {
      return text;
    }
    StringBuilder out = new StringBuilder(text.length());
    int copiedUpTo = 0;
    do {
      int valueStart = matcher.start(1);
      int valueEnd = matcher.end(1);
      out.append(text, copiedUpTo, valueStart);
      char quote = text.charAt(valueStart);
      boolean quoted =
          (quote == '"' || quote == '\'')
              && valueEnd - valueStart >= 2
              && text.charAt(valueEnd - 1) == quote;
      if (quoted) {
        out.append(quote).append(MASK).append(quote);
      } else {
        out.append(MASK);
      }
      copiedUpTo = valueEnd;
    } while (matcher.find());
    out.append(text, copiedUpTo, text.length());
    return out.toString();
  }

  /**
   * Enmascara números de tarjeta conservando los últimos 4 dígitos (lo que PCI DSS permite
   * mostrar). Solo actúa si el candidato pasa Luhn; si no lo pasa, la corrida de dígitos queda para
   * {@link #LONG_DIGIT_RUN}, que la suprime completa.
   */
  private static String maskCardNumbers(String text) {
    Matcher matcher = CARD_CANDIDATE.matcher(text);
    if (!matcher.find()) {
      return text;
    }
    StringBuilder out = new StringBuilder(text.length());
    int copiedUpTo = 0;
    do {
      String candidate = matcher.group();
      String digits = candidate.replaceAll("[ -]", "");
      if (digits.length() >= 13 && digits.length() <= 19 && passesLuhn(digits)) {
        out.append(text, copiedUpTo, matcher.start());
        out.append(MASK).append(digits, digits.length() - 4, digits.length());
        copiedUpTo = matcher.end();
      }
    } while (matcher.find());
    if (copiedUpTo == 0) {
      return text;
    }
    out.append(text, copiedUpTo, text.length());
    return out.toString();
  }

  private static boolean passesLuhn(String digits) {
    int sum = 0;
    boolean doubling = false;
    for (int i = digits.length() - 1; i >= 0; i--) {
      int value = digits.charAt(i) - '0';
      if (doubling) {
        value *= 2;
        if (value > 9) {
          value -= 9;
        }
      }
      sum += value;
      doubling = !doubling;
    }
    return sum % 10 == 0;
  }

  // ---------------------------------------------------------------------------------------------
  // Campos estructurados (allowlist)
  // ---------------------------------------------------------------------------------------------

  /**
   * Aplica la allowlist al mapa MDC. Devuelve la <b>misma instancia</b> si ningún valor cambió.
   *
   * @param mdc mapa MDC del evento; puede ser {@code null} o vacío
   */
  public static Map<String, String> redactMdc(Map<String, String> mdc) {
    if (mdc == null || mdc.isEmpty()) {
      return mdc;
    }
    Map<String, String> redacted = null;
    for (Map.Entry<String, String> entry : mdc.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      String safe =
          LogFieldPolicy.isVerbatim(key)
              ? value
              : LogFieldPolicy.isScanned(key) ? redact(value) : MASK;
      if (redacted == null && !Objects.equals(safe, value)) {
        redacted = new LinkedHashMap<>(mdc);
      }
      if (redacted != null) {
        redacted.put(key, safe);
      }
    }
    return redacted == null ? mdc : redacted;
  }

  /**
   * Aplica la allowlist a los {@code KeyValuePair} estructurados (los de {@code addKeyValue(...)}).
   * Devuelve la <b>misma instancia</b> si ningún valor cambió.
   *
   * @param pairs lista de pares del evento; puede ser {@code null} o vacía
   */
  public static List<KeyValuePair> redactKeyValuePairs(List<KeyValuePair> pairs) {
    if (pairs == null || pairs.isEmpty()) {
      return pairs;
    }
    List<KeyValuePair> redacted = null;
    for (int i = 0; i < pairs.size(); i++) {
      KeyValuePair pair = pairs.get(i);
      Object safe = redactFieldValue(pair.key, pair.value);
      if (redacted == null && safe != pair.value) {
        redacted = new ArrayList<>(pairs);
      }
      if (redacted != null) {
        redacted.set(i, new KeyValuePair(pair.key, safe));
      }
    }
    return redacted == null ? pairs : redacted;
  }

  /**
   * Política de un único valor estructurado. Los valores no {@code String} de claves permitidas
   * (ids numéricos, duraciones, enums) se dejan intactos: su tipo ya acota su forma.
   */
  private static Object redactFieldValue(String key, Object value) {
    if (LogFieldPolicy.isVerbatim(key)) {
      return value;
    }
    if (!LogFieldPolicy.isScanned(key)) {
      return MASK;
    }
    return value instanceof String text ? redact(text) : value;
  }
}
