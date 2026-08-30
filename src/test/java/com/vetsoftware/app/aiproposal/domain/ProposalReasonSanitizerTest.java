package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las nueve reglas de S6.4.1.
 *
 * <p>
 * <b>El nucleo de este test es la regla 3</b>, y el caso que la justifica esta
 * escrito literalmente: el prompt <em>obliga</em> al modelo a citar al cliente,
 * y el cliente colombiano escribe "facturamos 40 millones al mes". Un saneador
 * que solo casara rachas de cuatro o mas digitos —lo que decia el borrador
 * anterior— dejaria pasar esa frase entera, y la cifra acabaria pintada bajo un
 * precio real, indistinguible del calculado.
 */
@DisplayName("ProposalReasonSanitizer — las nueve reglas del motivo")
class ProposalReasonSanitizerTest {

    private static final String FALLBACK = "Historia clinica del paciente y catalogo de medicamentos";

    @Nested
    @DisplayName("El caso que motiva la regla 3: una cifra en el motivo")
    class UnaCifra {

        @Test
        @DisplayName("«facturamos 40 millones al mes» se descarta por el digito")
        void la_frase_del_cliente_colombiano_se_descarta() {
            SanitizedReason saneado = ProposalReasonSanitizer
                    .sanitize("Porque facturamos 40 millones al mes y necesitas control", FALLBACK);

            assertThat(saneado.rule()).isEqualTo(ReasonRejection.R3_CIFRA);
            assertThat(saneado.substituted()).isTrue();
            assertThat(saneado.text()).isEqualTo(FALLBACK);
        }

        @Test
        @DisplayName("y escrita con letras cae igual, por la regla 4")
        void la_misma_frase_sin_digitos_cae_por_dinero() {
            SanitizedReason saneado = ProposalReasonSanitizer
                    .sanitize("Porque facturas varios millones al mes y llevas cuentas", FALLBACK);

            assertThat(saneado.rule()).isEqualTo(ReasonRejection.R4_DINERO);
            assertThat(saneado.text()).isEqualTo(FALLBACK);
        }

        @ParameterizedTest(name = "«{0}»")
        @DisplayName("las cifras de tres y dos digitos que el borrador anterior dejaba pasar")
        @ValueSource(strings = {"El arriendo son 900 al mes segun me contaste",
                "Tienes 12 empleados atendiendo consultas cada dia",
                "El impuesto del 19 por ciento lo calcula el sistema solo",
                "Atiendes 8 pacientes diarios en tu consultorio de barrio"})
        void ninguna_cifra_pasa(String motivo) {
            assertThat(ProposalReasonSanitizer.sanitize(motivo, FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R3_CIFRA);
        }

        @Test
        @DisplayName("el ejemplo bueno del anexo E no lleva ni una cifra y pasa intacto")
        void el_ejemplo_bueno_pasa() {
            String bueno = "Le vendes a credito a una fundacion, y facturar a credito en"
                    + " Colombia exige factura electronica.";
            SanitizedReason saneado = ProposalReasonSanitizer.sanitize(bueno, FALLBACK);

            assertThat(saneado.rule()).isNull();
            assertThat(saneado.substituted()).isFalse();
            assertThat(saneado.text()).isEqualTo(bueno);
        }
    }

    @Nested
    @DisplayName("Dinero escrito con letras (regla 4)")
    class Dinero {

        @ParameterizedTest(name = "«{0}»")
        @ValueSource(strings = {"Porque manejas muchos pesos en efectivo cada dia",
                "Te ahorra varios millones al ano en papeleo del negocio",
                "Cuesta apenas unos mil al mes segun lo que describiste",
                "Porque tu margen ronda el % que declaraste en el texto",
                "Le vendes en USD a clientes de fuera y necesitas control",
                "Cobras en COP y necesitas la caja cuadrada cada tarde"})
        void el_dinero_con_letras_se_descarta(String motivo) {
            assertThat(ProposalReasonSanitizer.sanitize(motivo, FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R4_DINERO);
        }

        @Test
        @DisplayName("«familia» contiene «mil» y NO dispara: la frontera de palabra importa")
        void familia_no_dispara() {
            SanitizedReason saneado = ProposalReasonSanitizer
                    .sanitize("Porque atiendes a la familia entera de cada mascota", FALLBACK);

            assertThat(saneado.rule()).isNull();
        }

        @Test
        @DisplayName("«espeso» contiene «peso» y tampoco dispara")
        void espeso_no_dispara() {
            assertThat(ProposalReasonSanitizer
                    .sanitize("Porque llevas un registro espeso de cada tratamiento", FALLBACK)
                    .rule()).isNull();
        }
    }

    @Nested
    @DisplayName("Las otras reglas")
    class Otras {

        @Test
        @DisplayName("R1: menos de diez caracteres utiles no explica nada")
        void demasiado_corto() {
            assertThat(ProposalReasonSanitizer.sanitize("   Porque   ", FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R1_CORTO);
            assertThat(ProposalReasonSanitizer.sanitize(null, FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R1_CORTO);
        }

        @Test
        @DisplayName("R2: mas de 140 se TRUNCA, y es la unica que no cae al determinista")
        void demasiado_largo_se_trunca() {
            String largo = "Porque atiendes consultas generales y vacunacion en tu clinica de"
                    + " barrio y necesitas guardar la historia de cada paciente para poder"
                    + " seguirle el tratamiento completo";
            SanitizedReason saneado = ProposalReasonSanitizer.sanitize(largo, FALLBACK);

            assertThat(saneado.rule()).isEqualTo(ReasonRejection.R2_LARGO);
            assertThat(saneado.substituted()).isFalse();
            assertThat(saneado.text()).hasSizeLessThanOrEqualTo(140).endsWith("…")
                    .doesNotContain("  ");
            assertThat(ReasonRejection.R2_LARGO.sustituye()).isFalse();
        }

        @Test
        @DisplayName("un motivo largo CON cifra se descarta, no se trunca: el orden importa")
        void el_truncado_va_al_final() {
            String largoConCifra = "Porque atiendes consultas generales y vacunacion en tu"
                    + " clinica de barrio y facturas 40 millones al mes, asi que necesitas"
                    + " el control completo del paciente";
            SanitizedReason saneado = ProposalReasonSanitizer.sanitize(largoConCifra, FALLBACK);

            // Truncar primero cortaria justo la cifra y la dejaria pasar.
            assertThat(saneado.rule()).isEqualTo(ReasonRejection.R3_CIFRA);
            assertThat(saneado.text()).isEqualTo(FALLBACK);
        }

        @Test
        @DisplayName("R5: etiquetas y esquemas de URI")
        void marcado() {
            assertThat(ProposalReasonSanitizer
                    .sanitize("Porque necesitas <b>historia</b> de cada paciente", FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R5_MARCADO);
            assertThat(ProposalReasonSanitizer
                    .sanitize("Porque atiendes javascript:alert de cada paciente", FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R5_MARCADO);
        }

        @Test
        @DisplayName("R6: el vector de phishing")
        void enlaces() {
            assertThat(ProposalReasonSanitizer
                    .sanitize("Entra en https://premios-vet.example para reclamar", FALLBACK)
                    .rule()).isEqualTo(ReasonRejection.R6_ENLACE);
            assertThat(ProposalReasonSanitizer
                    .sanitize("Visita www.regalos-para-tu-clinica.example ahora", FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R6_ENLACE);
            assertThat(ProposalReasonSanitizer
                    .sanitize("Escribe a soporte-falso.com y te lo activamos", FALLBACK).rule())
                    .isEqualTo(ReasonRejection.R6_ENLACE);
        }

        @Test
        @DisplayName("R7: un motivo que nombra un codigo es el oraculo de S6.5 en prosa")
        void codigos_del_catalogo() {
            assertThat(ProposalReasonSanitizer
                    .sanitize("Porque te conviene mas el PACK_FULL que las piezas", FALLBACK)
                    .rule()).isEqualTo(ReasonRejection.R7_CODIGO);
        }

        @Test
        @DisplayName("R8: la arroba delata un correo aunque no haya digitos")
        void contacto() {
            assertThat(ProposalReasonSanitizer
                    .sanitize("Te escribimos a laura@vetchapinero.example con el detalle", FALLBACK)
                    .rule()).isEqualTo(ReasonRejection.R8_CONTACTO);
        }

        @Test
        @DisplayName("R9: la misma frase en mas de tres lineas del turno")
        void repeticion() {
            Map<String, String> crudos = new LinkedHashMap<>();
            String atasco = "Porque lo necesitas para tu operacion diaria";
            crudos.put("CORE", atasco);
            crudos.put("SCHEDULING", atasco);
            crudos.put("CLINICAL_HISTORY", atasco);
            crudos.put("CASH_REGISTER", atasco);
            crudos.put("INVENTORY", "Porque llevas existencias de medicamentos en bodega");

            Map<String, SanitizedReason> saneados = ProposalReasonSanitizer.sanitizeTurn(crudos,
                    Map.of("CORE", FALLBACK));

            assertThat(saneados.get("CORE").rule()).isEqualTo(ReasonRejection.R9_REPETIDO);
            assertThat(saneados.get("CASH_REGISTER").rule()).isEqualTo(ReasonRejection.R9_REPETIDO);
            assertThat(saneados.get("INVENTORY").rule()).isNull();
        }

        @Test
        @DisplayName("exactamente tres repeticiones todavia no son un atasco")
        void tres_repeticiones_pasan() {
            Map<String, String> crudos = new LinkedHashMap<>();
            String frase = "Porque lo necesitas para tu operacion diaria";
            crudos.put("CORE", frase);
            crudos.put("SCHEDULING", frase);
            crudos.put("CLINICAL_HISTORY", frase);

            assertThat(ProposalReasonSanitizer.sanitizeTurn(crudos, Map.of()).values())
                    .allSatisfy(saneado -> assertThat(saneado.rule()).isNull());
        }
    }

    @Nested
    @DisplayName("El fallback")
    class Fallback {

        @Test
        @DisplayName("sin short_description cae en texto fijo que NO hace eco del codigo")
        void sin_fallback_texto_fijo() {
            SanitizedReason saneado = ProposalReasonSanitizer.sanitize("tiene 900 cosas", null);

            assertThat(saneado.text()).isEqualTo(ProposalCart.MOTIVO_AUSENTE).doesNotContain("900");
        }

        @Test
        @DisplayName("un motivo sustituido queda marcado para la metrica")
        void queda_marcado() {
            assertThat(ProposalReasonSanitizer.sanitize("x", FALLBACK).hayQueRegistrar()).isTrue();
            assertThat(ProposalReasonSanitizer
                    .sanitize("Porque atiendes consultas y vacunas cada dia", FALLBACK)
                    .hayQueRegistrar()).isFalse();
        }
    }
}
