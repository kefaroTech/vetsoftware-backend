package com.vetsoftware.app.electronicdocument.domain;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.documento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Maquina de estados DIAN: forward-only y con dos estados terminales
 * (VALIDADO/RECHAZADO). Es lo que impide que una respuesta tardia del proveedor
 * "des-valide" un documento ya sellado, o que un rechazo se pise con otro
 * intento. La unica correccion valida de un terminal es una nota.
 */
@DisplayName("ElectronicDocument — maquina de estados DIAN")
class ElectronicDocumentStateMachineTest {

    private static final LocalDateTime VALIDACION = LocalDateTime.of(2026, 3, 10, 10, 30);

    private static ElectronicDocument enEstado(DianStatus status) {
        return documento(55L, 9L, ElectronicDocumentType.FE_VENTA, status, null, null, null, null,
                false, 100L);
    }

    private static ElectronicDocument numeradoEnEstado(DianStatus status) {
        return documento(55L, 9L, ElectronicDocumentType.FE_VENTA, status, "SETP", 990L, null, null,
                false, 100L);
    }

    @Nested
    @DisplayName("numeracion fiscal")
    class Numeracion {

        @Test
        @DisplayName("assignNumber estampa resolucion, prefijo y consecutivo sobre un PENDIENTE")
        void assignNumber_estampa_los_tres_datos() {
            ElectronicDocument documento = enEstado(DianStatus.PENDIENTE);

            documento.assignNumber("18760000001", "SETP", 991L);

            assertThat(documento.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(documento.getPrefix()).isEqualTo("SETP");
            assertThat(documento.getConsecutive()).isEqualTo(991L);
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
        }

        @Test
        @DisplayName("assignNumber admite prefijo null: hay resoluciones sin prefijo")
        void assignNumber_admite_prefijo_null() {
            ElectronicDocument documento = enEstado(DianStatus.PENDIENTE);

            documento.assignNumber("18760000001", null, 991L);

            assertThat(documento.getPrefix()).isNull();
            assertThat(documento.getConsecutive()).isEqualTo(991L);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("assignNumber exige un numero de resolucion no vacio")
        void assignNumber_exige_resolucion_no_vacia(String resolutionNumber) {
            ElectronicDocument documento = enEstado(DianStatus.PENDIENTE);

            assertThatThrownBy(() -> documento.assignNumber(resolutionNumber, "SETP", 991L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("resolutionNumber is required");
        }

        @Test
        @DisplayName("assignNumber exige un numero de resolucion (null tampoco vale)")
        void assignNumber_exige_resolucion_no_null() {
            ElectronicDocument documento = enEstado(DianStatus.PENDIENTE);

            assertThatThrownBy(() -> documento.assignNumber(null, "SETP", 991L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("resolutionNumber is required");
        }

        @Test
        @DisplayName("assignNumber exige consecutivo: numerar sin numero no tiene sentido")
        void assignNumber_exige_consecutivo() {
            ElectronicDocument documento = enEstado(DianStatus.PENDIENTE);

            assertThatThrownBy(() -> documento.assignNumber("18760000001", "SETP", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consecutive is required");
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"VALIDADO", "RECHAZADO", "CONTINGENCIA",
                "NO_ELECTRONICO"})
        @DisplayName("solo se puede numerar un documento PENDIENTE")
        void solo_se_puede_numerar_un_pendiente(DianStatus status) {
            ElectronicDocument documento = enEstado(status);

            assertThatThrownBy(() -> documento.assignNumber("18760000001", "SETP", 991L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se puede numerar un documento PENDIENTE");
        }
    }

    @Nested
    @DisplayName("numeracion del proveedor (POS)")
    class NumeracionDelProveedor {

        @Test
        @DisplayName("assignResolutionOnly estampa resolucion y prefijo sin consumir consecutivo")
        void assignResolutionOnly_no_consume_consecutivo() {
            ElectronicDocument pos = documento(55L, 9L, ElectronicDocumentType.DOC_EQUIV_POS,
                    DianStatus.PENDIENTE, null, null, null, null, false, null);

            pos.assignResolutionOnly("18760000009", "POS");

            assertThat(pos.getResolutionNumber()).isEqualTo("18760000009");
            assertThat(pos.getPrefix()).isEqualTo("POS");
            assertThat(pos.getConsecutive()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " "})
        @DisplayName("assignResolutionOnly exige un numero de resolucion no vacio")
        void assignResolutionOnly_exige_resolucion(String resolutionNumber) {
            ElectronicDocument pos = documento(55L, 9L, ElectronicDocumentType.DOC_EQUIV_POS,
                    DianStatus.PENDIENTE, null, null, null, null, false, null);

            assertThatThrownBy(() -> pos.assignResolutionOnly(resolutionNumber, "POS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("resolutionNumber is required");
        }

        @Test
        @DisplayName("assignResolutionOnly no re-numera un documento que ya tiene consecutivo")
        void assignResolutionOnly_no_renumera() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.PENDIENTE);

            assertThatThrownBy(() -> documento.assignResolutionOnly("18760000009", "POS"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("el documento ya tiene consecutivo asignado");
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"VALIDADO", "RECHAZADO", "CONTINGENCIA",
                "NO_ELECTRONICO"})
        @DisplayName("assignResolutionOnly tambien exige estado PENDIENTE")
        void assignResolutionOnly_exige_pendiente(DianStatus status) {
            ElectronicDocument documento = enEstado(status);

            assertThatThrownBy(() -> documento.assignResolutionOnly("18760000009", "POS"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se puede numerar un documento PENDIENTE");
        }

        @Test
        @DisplayName("la factura electronica NO delega el consecutivo: lo numeramos nosotros")
        void la_factura_no_delega_el_consecutivo() {
            assertThat(enEstado(DianStatus.PENDIENTE).usesProviderAssignedConsecutive()).isFalse();
        }

        @Test
        @DisplayName("las notas credito y debito tampoco delegan el consecutivo")
        void las_notas_no_delegan_el_consecutivo() {
            assertThat(ElectronicDocumentMother.notaCreditoTotal(70L)
                    .usesProviderAssignedConsecutive()).isFalse();
            assertThat(ElectronicDocumentMother.notaDebito(71L).usesProviderAssignedConsecutive())
                    .isFalse();
        }

        @Test
        @DisplayName("el tiquete POS si delega el consecutivo en el proveedor")
        void el_tiquete_pos_delega_el_consecutivo() {
            ElectronicDocument pos = documento(55L, 9L, ElectronicDocumentType.DOC_EQUIV_POS,
                    DianStatus.PENDIENTE, null, null, null, null, false, null);

            assertThat(pos.usesProviderAssignedConsecutive()).isTrue();
        }
    }

    @Nested
    @DisplayName("validacion DIAN")
    class Validacion {

        @Test
        @DisplayName("markValidated sella el documento y estampa la fecha de validacion")
        void markValidated_sella_el_documento() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.PENDIENTE);

            documento.markValidated(null, null, "CUFE-X", "CUDE-X", "uuid-X", "<xml/>", "qr-data",
                    "https://qr", "s3/key.pdf", VALIDACION);

            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(documento.getCufe()).isEqualTo("CUFE-X");
            assertThat(documento.getCude()).isEqualTo("CUDE-X");
            assertThat(documento.getUuid()).isEqualTo("uuid-X");
            assertThat(documento.getXmlSigned()).isEqualTo("<xml/>");
            assertThat(documento.getQrData()).isEqualTo("qr-data");
            assertThat(documento.getQrUrl()).isEqualTo("https://qr");
            assertThat(documento.getPdfRepresentation()).isEqualTo("s3/key.pdf");
            assertThat(documento.getDianValidationDate()).isEqualTo(VALIDACION);
        }

        @Test
        @DisplayName("si el proveedor no devuelve el numero, se conserva el que asignamos nosotros")
        void conserva_el_numero_propio_si_el_proveedor_no_lo_devuelve() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.PENDIENTE);

            documento.markValidated(null, null, "CUFE-X", null, null, null, null, null, null,
                    VALIDACION);

            assertThat(documento.getPrefix()).isEqualTo("SETP");
            assertThat(documento.getConsecutive()).isEqualTo(990L);
        }

        @Test
        @DisplayName("si el proveedor devuelve el numero (POS), ese es el que queda")
        void aplica_el_numero_que_devuelve_el_proveedor() {
            ElectronicDocument pos = documento(55L, 9L, ElectronicDocumentType.DOC_EQUIV_POS,
                    DianStatus.PENDIENTE, "POS", null, null, null, false, null);

            pos.markValidated("POSDIAN", 12345L, null, "CUDE-X", null, null, null, null, null,
                    VALIDACION);

            assertThat(pos.getPrefix()).isEqualTo("POSDIAN");
            assertThat(pos.getConsecutive()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("un documento en CONTINGENCIA se puede regularizar a VALIDADO")
        void un_documento_en_contingencia_se_regulariza() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.CONTINGENCIA);

            documento.markValidated(null, null, "CUFE-X", null, null, null, null, null, null,
                    VALIDACION);

            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"VALIDADO", "RECHAZADO"})
        @DisplayName("un documento en estado terminal no se puede volver a validar")
        void un_terminal_no_se_puede_volver_a_validar(DianStatus terminal) {
            ElectronicDocument documento = numeradoEnEstado(terminal);

            assertThatThrownBy(() -> documento.markValidated(null, null, "CUFE-X", null, null, null,
                    null, null, null, VALIDACION)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya está en estado terminal (" + terminal + ")");
        }
    }

    @Nested
    @DisplayName("rechazo y liberacion del consecutivo")
    class RechazoYLiberacion {

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"PENDIENTE", "CONTINGENCIA"})
        @DisplayName("un documento no terminal se puede marcar RECHAZADO")
        void un_no_terminal_se_puede_rechazar(DianStatus status) {
            ElectronicDocument documento = numeradoEnEstado(status);

            documento.markRejected();

            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.RECHAZADO);
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"VALIDADO", "RECHAZADO"})
        @DisplayName("un documento en estado terminal no se puede volver a rechazar")
        void un_terminal_no_se_puede_volver_a_rechazar(DianStatus terminal) {
            ElectronicDocument documento = numeradoEnEstado(terminal);

            assertThatThrownBy(documento::markRejected).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("corrige por nota crédito/débito");
        }

        @Test
        @DisplayName("liberar la numeracion borra resolucion, prefijo y consecutivo a la vez")
        void liberar_la_numeracion_borra_los_tres_datos() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.PENDIENTE);
            documento.markRejected();

            documento.releaseFiscalNumber();

            assertThat(documento.getPrefix()).isNull();
            assertThat(documento.getConsecutive()).isNull();
            assertThat(documento.getResolutionNumber()).isNull();
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.RECHAZADO);
        }

        @Test
        @DisplayName("tras liberar, el documento rechazado se puede volver a numerar? no: sigue "
                + "RECHAZADO")
        void tras_liberar_sigue_rechazado_y_no_se_renumera() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.PENDIENTE);
            documento.markRejected();
            documento.releaseFiscalNumber();

            assertThatThrownBy(() -> documento.assignNumber("18760000001", "SETP", 992L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se puede numerar un documento PENDIENTE");
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"PENDIENTE", "VALIDADO", "CONTINGENCIA",
                "NO_ELECTRONICO"})
        @DisplayName("solo se libera la numeracion de un documento RECHAZADO")
        void solo_se_libera_la_numeracion_de_un_rechazado(DianStatus status) {
            ElectronicDocument documento = numeradoEnEstado(status);

            assertThatThrownBy(documento::releaseFiscalNumber)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se libera la numeración de un documento RECHAZADO");
        }
    }

    @Nested
    @DisplayName("contingencia")
    class Contingencia {

        @Test
        @DisplayName("un PENDIENTE pasa a CONTINGENCIA cuando el proveedor no responde")
        void un_pendiente_pasa_a_contingencia() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.PENDIENTE);

            documento.markContingency();

            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.CONTINGENCIA);
        }

        @Test
        @DisplayName("marcar contingencia dos veces no rompe: el reintento tambien puede fallar")
        void marcar_contingencia_dos_veces_no_rompe() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.CONTINGENCIA);

            assertThatCode(documento::markContingency).doesNotThrowAnyException();
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.CONTINGENCIA);
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"VALIDADO", "RECHAZADO"})
        @DisplayName("un terminal nunca cae a contingencia")
        void un_terminal_nunca_cae_a_contingencia(DianStatus terminal) {
            ElectronicDocument documento = numeradoEnEstado(terminal);

            assertThatThrownBy(documento::markContingency).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("estado terminal");
        }

        @Test
        @DisplayName("en contingencia y sin sellos, lo entregado al cliente es provisional")
        void en_contingencia_sin_sellos_es_provisional() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.CONTINGENCIA);

            assertThat(documento.isProvisional()).isTrue();
        }

        @Test
        @DisplayName("en contingencia PERO con CUFE ya no es provisional: el sello fiscal existe")
        void en_contingencia_con_cufe_no_es_provisional() {
            ElectronicDocument documento = documento(55L, 9L, ElectronicDocumentType.FE_VENTA,
                    DianStatus.CONTINGENCIA, "SETP", 990L, "CUFE-1", null, false, 100L);

            assertThat(documento.isProvisional()).isFalse();
        }

        @Test
        @DisplayName("en contingencia PERO con CUDE tampoco es provisional")
        void en_contingencia_con_cude_no_es_provisional() {
            ElectronicDocument documento = documento(55L, 9L, ElectronicDocumentType.DOC_EQUIV_POS,
                    DianStatus.CONTINGENCIA, "POS", 55L, null, "CUDE-1", false, null);

            assertThat(documento.isProvisional()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"PENDIENTE", "VALIDADO", "RECHAZADO",
                "NO_ELECTRONICO"})
        @DisplayName("fuera de contingencia nada es provisional")
        void fuera_de_contingencia_nada_es_provisional(DianStatus status) {
            assertThat(enEstado(status).isProvisional()).isFalse();
        }
    }

    @Nested
    @DisplayName("documento local (empresa sin facturacion electronica)")
    class DocumentoLocal {

        @Test
        @DisplayName("un PENDIENTE se degrada a NO_ELECTRONICO y se queda en la casa")
        void un_pendiente_se_degrada_a_no_electronico() {
            ElectronicDocument documento = enEstado(DianStatus.PENDIENTE);

            documento.markLocal();

            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.NO_ELECTRONICO);
        }

        @ParameterizedTest
        @EnumSource(value = DianStatus.class, names = {"VALIDADO", "RECHAZADO", "CONTINGENCIA",
                "NO_ELECTRONICO"})
        @DisplayName("un documento ya numerado o sellado nunca se degrada a NO_ELECTRONICO")
        void un_documento_sellado_nunca_se_degrada(DianStatus status) {
            ElectronicDocument documento = enEstado(status);

            assertThatThrownBy(documento::markLocal).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "solo un documento PENDIENTE puede marcarse como NO_ELECTRONICO");
        }
    }

    @Nested
    @DisplayName("representacion grafica y reverso")
    class RepresentacionYReverso {

        @Test
        @DisplayName("adjuntar la representacion guarda la clave del PDF sin tocar el estado")
        void adjuntar_la_representacion_no_toca_el_estado() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.VALIDADO);

            documento.attachRepresentation("invoices/9/55/SETP990.pdf");

            assertThat(documento.getPdfRepresentation()).isEqualTo("invoices/9/55/SETP990.pdf");
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
        }

        @Test
        @DisplayName("adjuntar de nuevo reemplaza la clave anterior")
        void adjuntar_de_nuevo_reemplaza_la_clave() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.VALIDADO);
            documento.attachRepresentation("vieja.pdf");

            documento.attachRepresentation("nueva.pdf");

            assertThat(documento.getPdfRepresentation()).isEqualTo("nueva.pdf");
        }

        @Test
        @DisplayName("una factura nace sin marca de reverso")
        void una_factura_nace_sin_marca_de_reverso() {
            assertThat(numeradoEnEstado(DianStatus.VALIDADO).isReversed()).isFalse();
        }

        @Test
        @DisplayName("marcar reversada no cambia el estado DIAN: no es un soft-delete")
        void marcar_reversada_no_cambia_el_estado_dian() {
            ElectronicDocument documento = numeradoEnEstado(DianStatus.VALIDADO);

            documento.markReversed();

            assertThat(documento.isReversed()).isTrue();
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
        }
    }
}
