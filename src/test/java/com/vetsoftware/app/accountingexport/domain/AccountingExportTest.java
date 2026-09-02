package com.vetsoftware.app.accountingexport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("AccountingExport — invariantes y ciclo de vida del asiento resumen")
class AccountingExportTest {

    private static final LocalDateTime GENERATED_AT = LocalDateTime.of(2026, 4, 1, 9, 0);
    private static final String VALID_HASH = "0123456789abcdef".repeat(4);

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. El estado por defecto
     * es GENERATED sin desenlace, que es el unico que pasa
     * {@code validateLifecycle} sin tocar fechas de resolucion.
     */
    private static final class Builder {
        private Long id = 1L;
        private String periodKey = "2026-03";
        private AccountingExportKind exportKind = AccountingExportKind.JOURNAL_SUMMARY;
        private int attemptNumber = 1;
        private AccountingExportStatus status = AccountingExportStatus.GENERATED;
        private LocalDateTime generatedAt = GENERATED_AT;
        private Long generatedBySystemUserId = 3L;
        private BigDecimal totalDebit = new BigDecimal("1000.00");
        private BigDecimal totalCredit = new BigDecimal("1000.00");
        private String totalsHash = VALID_HASH;
        private String fileRef = "s3://vetsoftware-exports/2026-03-journal-1.csv";
        private LocalDateTime deliveredAt;
        private LocalDateTime rejectedAt;
        private String rejectionReason;
        private LocalDateTime createdDate = GENERATED_AT;
        private Long version = 0L;

        private Builder periodKey(String v) {
            this.periodKey = v;
            return this;
        }

        private Builder exportKind(AccountingExportKind v) {
            this.exportKind = v;
            return this;
        }

        private Builder attemptNumber(int v) {
            this.attemptNumber = v;
            return this;
        }

        private Builder status(AccountingExportStatus v) {
            this.status = v;
            return this;
        }

        private Builder generatedAt(LocalDateTime v) {
            this.generatedAt = v;
            return this;
        }

        private Builder generatedBySystemUserId(Long v) {
            this.generatedBySystemUserId = v;
            return this;
        }

        private Builder totalDebit(BigDecimal v) {
            this.totalDebit = v;
            return this;
        }

        private Builder totalCredit(BigDecimal v) {
            this.totalCredit = v;
            return this;
        }

        private Builder totalsHash(String v) {
            this.totalsHash = v;
            return this;
        }

        private Builder fileRef(String v) {
            this.fileRef = v;
            return this;
        }

        private Builder deliveredAt(LocalDateTime v) {
            this.deliveredAt = v;
            return this;
        }

        private Builder rejectedAt(LocalDateTime v) {
            this.rejectedAt = v;
            return this;
        }

        private Builder rejectionReason(String v) {
            this.rejectionReason = v;
            return this;
        }

        private Builder createdDate(LocalDateTime v) {
            this.createdDate = v;
            return this;
        }

        private AccountingExport build() {
            return new AccountingExport(id, periodKey, exportKind, attemptNumber, status,
                    generatedAt, generatedBySystemUserId, totalDebit, totalCredit, totalsHash,
                    fileRef, deliveredAt, rejectedAt, rejectionReason, createdDate, version);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            AccountingExport export = valido().build();

            assertThat(export.getId()).isEqualTo(1L);
            assertThat(export.getPeriodKey()).isEqualTo("2026-03");
            assertThat(export.getExportKind()).isEqualTo(AccountingExportKind.JOURNAL_SUMMARY);
            assertThat(export.getAttemptNumber()).isEqualTo(1);
            assertThat(export.getStatus()).isEqualTo(AccountingExportStatus.GENERATED);
            assertThat(export.getGeneratedAt()).isEqualTo(GENERATED_AT);
            assertThat(export.getGeneratedBySystemUserId()).isEqualTo(3L);
            assertThat(export.getTotalDebit()).isEqualByComparingTo("1000.00");
            assertThat(export.getTotalCredit()).isEqualByComparingTo("1000.00");
            assertThat(export.getTotalsHash()).isEqualTo(VALID_HASH);
            assertThat(export.getFileRef())
                    .isEqualTo("s3://vetsoftware-exports/2026-03-journal-1.csv");
            assertThat(export.getDeliveredAt()).isNull();
            assertThat(export.getRejectedAt()).isNull();
            assertThat(export.getRejectionReason()).isNull();
            assertThat(export.getCreatedDate()).isEqualTo(GENERATED_AT);
            assertThat(export.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("generate — fichero recien generado")
    class Generacion {

        @Test
        @DisplayName("nace en GENERATED, sin desenlace, sin id y sin version")
        void nace_en_generated_sin_desenlace_sin_id_y_sin_version() {
            AccountingExport export = AccountingExport.generate(AccountingExportMother.PERIOD_KEY,
                    AccountingExportMother.KIND, AccountingExportMother.ATTEMPT_NUMBER,
                    AccountingExportMother.GENERATED_AT, AccountingExportMother.GENERATED_BY,
                    AccountingExportMother.TOTAL, AccountingExportMother.TOTAL,
                    AccountingExportMother.TOTALS_HASH, AccountingExportMother.FILE_REF,
                    AccountingExportMother.CREATED);

            assertThat(export.getId()).isNull();
            assertThat(export.getStatus()).isEqualTo(AccountingExportStatus.GENERATED);
            assertThat(export.getDeliveredAt()).isNull();
            assertThat(export.getRejectedAt()).isNull();
            assertThat(export.getRejectionReason()).isNull();
            assertThat(export.getVersion()).isNull();
            assertThat(export.isCurrent()).isTrue();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("periodKey null",
                            (ThrowingCallable) () -> valido().periodKey(null).build(),
                            "periodKey is required"),
                    arguments("periodKey con formato invalido",
                            (ThrowingCallable) () -> valido().periodKey("2026-13").build(),
                            "periodKey must have the form yyyy-MM"),
                    arguments("exportKind null",
                            (ThrowingCallable) () -> valido().exportKind(null).build(),
                            "exportKind is required"),
                    arguments("attemptNumber cero",
                            (ThrowingCallable) () -> valido().attemptNumber(0).build(),
                            "attemptNumber must be 1 or greater"),
                    arguments("status null", (ThrowingCallable) () -> valido().status(null).build(),
                            "status is required"),
                    arguments("generatedAt null",
                            (ThrowingCallable) () -> valido().generatedAt(null).build(),
                            "generatedAt is required"),
                    arguments("generatedBySystemUserId null",
                            (ThrowingCallable) () -> valido().generatedBySystemUserId(null).build(),
                            "generatedBySystemUserId is required"),
                    arguments("totalDebit null",
                            (ThrowingCallable) () -> valido().totalDebit(null).build(),
                            "totalDebit and totalCredit are required"),
                    arguments("totalCredit null",
                            (ThrowingCallable) () -> valido().totalCredit(null).build(),
                            "totalDebit and totalCredit are required"),
                    arguments("totalDebit con tres decimales: MySQL redondearia sin avisar",
                            (ThrowingCallable) () -> valido().totalDebit(new BigDecimal("1000.001"))
                                    .build(),
                            "totals must have 2 decimals or fewer"),
                    arguments("totalCredit con tres decimales",
                            (ThrowingCallable) () -> valido()
                                    .totalCredit(new BigDecimal("1000.001")).build(),
                            "totals must have 2 decimals or fewer"),
                    arguments("totalDebit negativo",
                            (ThrowingCallable) () -> valido().totalDebit(new BigDecimal("-100.00"))
                                    .build(),
                            "totalDebit must not be negative"),
                    arguments("totales que no cuadran: chk_accounting_exports_balanced",
                            (ThrowingCallable) () -> valido().totalCredit(new BigDecimal("999.99"))
                                    .build(),
                            "the export does not balance"),
                    arguments("totalsHash null",
                            (ThrowingCallable) () -> valido().totalsHash(null).build(),
                            "must be 64 lowercase hex characters"),
                    arguments("totalsHash en mayusculas",
                            (ThrowingCallable) () -> valido().totalsHash("F".repeat(64)).build(),
                            "must be 64 lowercase hex characters"),
                    arguments("totalsHash con longitud incorrecta",
                            (ThrowingCallable) () -> valido().totalsHash("abc123").build(),
                            "must be 64 lowercase hex characters"),
                    arguments("fileRef null",
                            (ThrowingCallable) () -> valido().fileRef(null).build(),
                            "fileRef is required"),
                    arguments("fileRef en blanco",
                            (ThrowingCallable) () -> valido().fileRef("   ").build(),
                            "fileRef is required"),
                    arguments("fileRef de 256 caracteres",
                            (ThrowingCallable) () -> valido().fileRef("x".repeat(256)).build(),
                            "fileRef must be 255 chars or less"),
                    arguments("createdDate null",
                            (ThrowingCallable) () -> valido().createdDate(null).build(),
                            "createdDate is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("cuadra con distinta escala: compareTo, no equals, es lo que compara")
        void cuadra_con_distinta_escala() {
            assertThatCode(() -> valido().totalDebit(new BigDecimal("1000.0"))
                    .totalCredit(new BigDecimal("1000.00")).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fileRef de 255 caracteres se acepta en el limite")
        void file_ref_de_255_caracteres_se_acepta() {
            assertThatCode(() -> valido().fileRef("x".repeat(255)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("chk_accounting_exports_lifecycle — coherencia entre estado y desenlace")
    class CicloDeVida {

        @Test
        @DisplayName("GENERATED con fecha de entrega colgada es incoherente y se rechaza")
        void generated_con_fecha_de_entrega_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.GENERATED)
                    .deliveredAt(GENERATED_AT).build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "an export without an outcome must not carry delivery or rejection "
                                    + "data");
        }

        @Test
        @DisplayName("GENERATED con motivo de rechazo colgado es incoherente y se rechaza")
        void generated_con_motivo_de_rechazo_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.GENERATED)
                    .rejectionReason("motivo").build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "an export without an outcome must not carry delivery or rejection "
                                    + "data");
        }

        @Test
        @DisplayName("DELIVERED sin fecha de entrega es incoherente y se rechaza")
        void delivered_sin_fecha_de_entrega_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.DELIVERED).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a DELIVERED export needs deliveredAt and nothing else");
        }

        @Test
        @DisplayName("DELIVERED con motivo de rechazo colgado es incoherente y se rechaza")
        void delivered_con_motivo_de_rechazo_colgado_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.DELIVERED)
                    .deliveredAt(GENERATED_AT).rejectionReason("motivo").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a DELIVERED export needs deliveredAt and nothing else");
        }

        @Test
        @DisplayName("DELIVERED anterior a generatedAt es incoherente y se rechaza")
        void delivered_anterior_a_generated_at_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.DELIVERED)
                    .deliveredAt(GENERATED_AT.minusMinutes(1)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deliveredAt must not be before generatedAt");
        }

        @Test
        @DisplayName("DELIVERED con deliveredAt igual a generatedAt es valido")
        void delivered_con_delivered_at_igual_a_generated_at_es_valido() {
            assertThatCode(() -> valido().status(AccountingExportStatus.DELIVERED)
                    .deliveredAt(GENERATED_AT).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("REJECTED sin fecha de rechazo es incoherente y se rechaza")
        void rejected_sin_fecha_de_rechazo_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectionReason("motivo").build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a REJECTED export needs rejectedAt and rejectionReason");
        }

        @Test
        @DisplayName("REJECTED sin motivo escrito es incoherente: obliga a rehacer a ciegas")
        void rejected_sin_motivo_escrito_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectedAt(GENERATED_AT).build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a REJECTED export needs rejectedAt and rejectionReason");
        }

        @Test
        @DisplayName("REJECTED con motivo en blanco es incoherente igual que sin motivo")
        void rejected_con_motivo_en_blanco_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectedAt(GENERATED_AT).rejectionReason("   ").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a REJECTED export needs rejectedAt and rejectionReason");
        }

        @Test
        @DisplayName("REJECTED que conserva la fecha de entrega es incoherente y se rechaza")
        void rejected_que_conserva_la_fecha_de_entrega_es_incoherente() {
            assertThatThrownBy(
                    () -> valido().status(AccountingExportStatus.REJECTED).rejectedAt(GENERATED_AT)
                            .rejectionReason("motivo").deliveredAt(GENERATED_AT).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a REJECTED export must not keep a delivery date");
        }

        @Test
        @DisplayName("REJECTED anterior a generatedAt es incoherente y se rechaza")
        void rejected_anterior_a_generated_at_es_incoherente() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectedAt(GENERATED_AT.minusMinutes(1)).rejectionReason("motivo").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rejectedAt must not be before generatedAt");
        }

        @Test
        @DisplayName("un motivo de rechazo de 256 caracteres se rechaza sea cual sea el estado")
        void un_motivo_de_256_caracteres_se_rechaza() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectedAt(GENERATED_AT).rejectionReason("x".repeat(256)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rejectionReason must be 255 chars or less");
        }

        @Test
        @DisplayName("un motivo de rechazo de 255 caracteres se acepta en el limite")
        void un_motivo_de_255_caracteres_se_acepta() {
            assertThatCode(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectedAt(GENERATED_AT).rejectionReason("x".repeat(255)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("REJECTED valido no lanza")
        void rejected_valido_no_lanza() {
            assertThatCode(() -> valido().status(AccountingExportStatus.REJECTED)
                    .rejectedAt(GENERATED_AT).rejectionReason("Totales no cuadran").build())
                    .doesNotThrowAnyException();
        }

        /**
         * En la base, {@code chk_accounting_exports_lifecycle} no impone ninguna
         * condicion a la rama SUPERSEDED (puede llegar desde cualquier estado, y el
         * CHECK solo mira la fila resultante). <strong>El constructor Java es mas
         * estricto</strong>: reusa el mismo {@code requireEmpty} que GENERATED, asi que
         * este caso SI es observable desde una unidad de dominio pura — a diferencia de
         * lo que la ausencia de condicion en el CHECK sugeriria.
         */
        @Test
        @DisplayName("SUPERSEDED con una fecha de entrega colgada es incoherente para el "
                + "dominio Java, aunque el CHECK de la base no lo exija")
        void superseded_con_fecha_de_entrega_colgada_es_incoherente_para_java() {
            assertThatThrownBy(() -> valido().status(AccountingExportStatus.SUPERSEDED)
                    .deliveredAt(GENERATED_AT).build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "an export without an outcome must not carry delivery or rejection "
                                    + "data");
        }

        @Test
        @DisplayName("SUPERSEDED sin desenlace previo no lanza")
        void superseded_sin_desenlace_no_lanza() {
            assertThatCode(() -> valido().status(AccountingExportStatus.SUPERSEDED).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("markDelivered")
    class MarcarEntregado {

        @Test
        @DisplayName("desde GENERATED pasa a DELIVERED y conserva la version")
        void desde_generated_pasa_a_delivered_y_conserva_la_version() {
            AccountingExport export = AccountingExportMother.generado();
            LocalDateTime entrega = AccountingExportMother.GENERATED_AT.plusDays(1);

            AccountingExport entregado = export.markDelivered(entrega);

            assertThat(entregado.getStatus()).isEqualTo(AccountingExportStatus.DELIVERED);
            assertThat(entregado.getDeliveredAt()).isEqualTo(entrega);
            assertThat(entregado.getRejectedAt()).isNull();
            assertThat(entregado.getRejectionReason()).isNull();
            assertThat(entregado.getVersion()).isEqualTo(AccountingExportMother.VERSION);
        }

        @Test
        @DisplayName("una entrega anterior a generatedAt se rechaza")
        void una_entrega_anterior_a_generated_at_se_rechaza() {
            AccountingExport export = AccountingExportMother.generado();

            assertThatThrownBy(
                    () -> export.markDelivered(AccountingExportMother.GENERATED_AT.minusMinutes(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deliveredAt must not be before generatedAt");
        }

        @Test
        @DisplayName("una entrega sin fecha se rechaza")
        void una_entrega_sin_fecha_se_rechaza() {
            AccountingExport export = AccountingExportMother.generado();

            assertThatThrownBy(() -> export.markDelivered(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deliveredAt must not be before generatedAt");
        }

        @ParameterizedTest
        @EnumSource(value = AccountingExportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "GENERATED")
        @DisplayName("una exportacion que no esta GENERATED rechaza un nuevo desenlace")
        void una_exportacion_que_no_esta_generated_rechaza_un_nuevo_desenlace(
                AccountingExportStatus status) {
            AccountingExport export = AccountingExportMother.paraEstado(status);

            assertThatThrownBy(() -> export.markDelivered(AccountingExportMother.GENERATED_AT))
                    .isInstanceOf(AccountingExportAlreadyResolvedException.class)
                    .hasMessageContaining("is already resolved with status " + status);
        }
    }

    @Nested
    @DisplayName("markRejected")
    class MarcarRechazado {

        @Test
        @DisplayName("desde GENERATED pasa a REJECTED con motivo y fecha")
        void desde_generated_pasa_a_rejected_con_motivo_y_fecha() {
            AccountingExport export = AccountingExportMother.generado();
            LocalDateTime rechazo = AccountingExportMother.GENERATED_AT.plusHours(2);

            AccountingExport rechazado = export.markRejected(rechazo, "Totales no cuadran");

            assertThat(rechazado.getStatus()).isEqualTo(AccountingExportStatus.REJECTED);
            assertThat(rechazado.getRejectedAt()).isEqualTo(rechazo);
            assertThat(rechazado.getRejectionReason()).isEqualTo("Totales no cuadran");
            assertThat(rechazado.getDeliveredAt()).isNull();
        }

        @Test
        @DisplayName("un rechazo sin motivo escrito se rechaza")
        void un_rechazo_sin_motivo_se_rechaza() {
            AccountingExport export = AccountingExportMother.generado();

            assertThatThrownBy(() -> export.markRejected(AccountingExportMother.GENERATED_AT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rejectionReason is required when rejecting");
        }

        @Test
        @DisplayName("un rechazo con motivo en blanco se rechaza igual que sin motivo")
        void un_rechazo_con_motivo_en_blanco_se_rechaza() {
            AccountingExport export = AccountingExportMother.generado();

            assertThatThrownBy(
                    () -> export.markRejected(AccountingExportMother.GENERATED_AT, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rejectionReason is required when rejecting");
        }

        @Test
        @DisplayName("un rechazo anterior a generatedAt se rechaza")
        void un_rechazo_anterior_a_generated_at_se_rechaza() {
            AccountingExport export = AccountingExportMother.generado();

            assertThatThrownBy(() -> export
                    .markRejected(AccountingExportMother.GENERATED_AT.minusMinutes(1), "motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rejectedAt must not be before generatedAt");
        }

        @ParameterizedTest
        @EnumSource(value = AccountingExportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "GENERATED")
        @DisplayName("una exportacion que no esta GENERATED rechaza el rechazo")
        void una_exportacion_que_no_esta_generated_rechaza_el_rechazo(
                AccountingExportStatus status) {
            AccountingExport export = AccountingExportMother.paraEstado(status);

            assertThatThrownBy(
                    () -> export.markRejected(AccountingExportMother.GENERATED_AT, "motivo"))
                    .isInstanceOf(AccountingExportAlreadyResolvedException.class)
                    .hasMessageContaining("is already resolved with status " + status);
        }
    }

    @Nested
    @DisplayName("supersede")
    class Reemplazo {

        @Test
        @DisplayName("desde DELIVERED borra la fecha de entrega, no solo cambia el estado")
        void desde_delivered_borra_la_fecha_de_entrega() {
            AccountingExport entregado = AccountingExportMother
                    .entregado(AccountingExportMother.GENERATED_AT.plusDays(1));

            AccountingExport reemplazado = entregado.supersede();

            assertThat(reemplazado.getStatus()).isEqualTo(AccountingExportStatus.SUPERSEDED);
            assertThat(reemplazado.getDeliveredAt()).isNull();
            assertThat(reemplazado.getRejectedAt()).isNull();
            assertThat(reemplazado.getRejectionReason()).isNull();
        }

        @Test
        @DisplayName("desde GENERATED tambien reemplaza, sin desenlace previo que borrar")
        void desde_generated_tambien_reemplaza() {
            AccountingExport reemplazado = AccountingExportMother.generado().supersede();

            assertThat(reemplazado.getStatus()).isEqualTo(AccountingExportStatus.SUPERSEDED);
        }

        @Test
        @DisplayName("una exportacion ya reemplazada rechaza un segundo reemplazo")
        void una_exportacion_ya_reemplazada_rechaza_un_segundo_reemplazo() {
            AccountingExport reemplazado = AccountingExportMother.reemplazado();

            assertThatThrownBy(reemplazado::supersede)
                    .isInstanceOf(AccountingExportAlreadyResolvedException.class)
                    .hasMessageContaining("is already resolved with status SUPERSEDED");
        }
    }

    @Nested
    @DisplayName("isCurrent — el hueco de uq_accounting_exports_current")
    class Vigencia {

        @ParameterizedTest
        @EnumSource(value = AccountingExportStatus.class, names = {"GENERATED", "DELIVERED"})
        @DisplayName("GENERATED y DELIVERED ocupan el hueco")
        void generated_y_delivered_ocupan_el_hueco(AccountingExportStatus status) {
            assertThat(AccountingExportMother.paraEstado(status).isCurrent()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = AccountingExportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {
                "GENERATED", "DELIVERED"})
        @DisplayName("cualquier otro estado libera el hueco")
        void cualquier_otro_estado_libera_el_hueco(AccountingExportStatus status) {
            assertThat(AccountingExportMother.paraEstado(status).isCurrent()).isFalse();
        }
    }
}
