package com.vetsoftware.app.taxreturn.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Las diez comprobaciones del constructor de {@link TaxReturn}.
 *
 * <p>
 * <b>Cada una tiene dos ramas y las dos importan.</b> «ICA exige municipio» sin
 * su gemela «lo demas no lo lleva» deja pasar dos filas para el mismo supuesto
 * —una con municipio y otra sin el— que {@code uq_tax_returns_current} no ve
 * como iguales. Por eso las matrices van con {@link EnumSource}: la rama de
 * exclusion se prueba contra <em>todos</em> los valores que no son el
 * privilegiado, y el dia que alguien anada un {@code TaxKind} nuevo al
 * {@code switch} sin darle forma de periodo, el caso parametrizado lo caza sin
 * que nadie tenga que acordarse.
 *
 * <p>
 * <b>Nada de mockear la entidad.</b> Un {@code TaxReturn} mockeado no valida
 * nada, y toda esta clase seria un adorno.
 */
@DisplayName("TaxReturn — las validaciones del constructor")
class TaxReturnTest {

    @Nested
    @DisplayName("Municipio (chk_tax_returns_municipality)")
    class Municipio {

        @Test
        @DisplayName("ICA sin municipio no se construye")
        void ica_sin_municipio_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.ICA, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-B01", 1, null, null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("municipalityCode is required for ICA");
        }

        @Test
        @DisplayName("ICA con un municipio que no tiene cinco caracteres no se construye")
        void ica_con_municipio_de_longitud_equivocada_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.ICA, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-B01", 1, "5001", null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("municipalityCode must be 5 characters");
        }

        @ParameterizedTest
        @EnumSource(value = TaxKind.class, names = "ICA", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("cualquier impuesto que no sea ICA rechaza el municipio")
        void los_que_no_son_ica_rechazan_el_municipio(TaxKind taxKind) {
            // La segunda rama del CHECK. Sin ella, la misma declaracion nacional cabe
            // dos veces —con municipio y sin el— y la unicidad no las ve iguales.
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, taxKind, TaxReturnMother.ANIO,
                    periodoValido(taxKind), 1, TaxReturnMother.MUNICIPIO_ICA,
                    frecuenciaValida(taxKind), TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("municipalityCode must be absent");
        }

        @Test
        @DisplayName("una declaracion nacional expone el centinela que calcula la base")
        void una_declaracion_nacional_expone_el_centinela() {
            assertThat(TaxReturnMother.borradorDeRetencion().municipalityKey())
                    .isEqualTo(TaxReturn.NATIONAL_MUNICIPALITY_KEY);
            assertThat(TaxReturnMother.borradorDeIca().municipalityKey())
                    .isEqualTo(TaxReturnMother.MUNICIPIO_ICA);
        }
    }

    @Nested
    @DisplayName("Periodicidad de IVA (chk_tax_returns_vat_freq)")
    class PeriodicidadDeIva {

        @Test
        @DisplayName("IVA sin periodicidad no se construye")
        void iva_sin_periodicidad_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.VAT, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-B01", 1, null, null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("vatFrequency is required for VAT");
        }

        @ParameterizedTest
        @EnumSource(value = TaxKind.class, names = "VAT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("cualquier impuesto que no sea IVA rechaza la periodicidad")
        void los_que_no_son_iva_rechazan_la_periodicidad(TaxKind taxKind) {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, taxKind, TaxReturnMother.ANIO,
                    periodoValido(taxKind), 1, municipioValido(taxKind), VatFrequency.BIMONTHLY,
                    TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("vatFrequency must be absent");
        }
    }

    @Nested
    @DisplayName("Forma de la clave de periodo (chk_tax_returns_period)")
    class ClaveDePeriodo {

        @ParameterizedTest
        @EnumSource(TaxKind.class)
        @DisplayName("cada impuesto acepta la forma que le toca")
        void cada_impuesto_acepta_la_forma_que_le_toca(TaxKind taxKind) {
            // El caso en verde de la matriz. Su valor esta en el switch: un TaxKind
            // nuevo sin rama en validatePeriodKey no compila el switch exhaustivo,
            // pero uno con rama mal escrita si, y esto lo caza.
            assertThatCode(() -> TaxReturnMother.crudo(null, taxKind, TaxReturnMother.ANIO,
                    periodoValido(taxKind), 1, municipioValido(taxKind), frecuenciaValida(taxKind),
                    TaxReturnStatus.DRAFT, null)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @CsvSource({"INCOME_TAX, 2026-M03", "WITHHOLDING, 2026-A", "WITHHOLDING, 2026-B01",
                "WITHHOLDING, 2026-M13", "ICA, 2026-M03", "ICA, 2026-B07"})
        @DisplayName("una clave con la forma de otro impuesto no se construye")
        void una_clave_con_la_forma_de_otro_impuesto_no_se_construye(TaxKind taxKind,
                String clave) {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, taxKind, TaxReturnMother.ANIO,
                    clave, 1, municipioValido(taxKind), frecuenciaValida(taxKind),
                    TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("does not match the shape required for");
        }

        @Test
        @DisplayName("una retencion de diciembre no se declara en el ano siguiente")
        void una_retencion_de_diciembre_no_se_declara_en_el_ano_siguiente() {
            // El caso que el CHECK existe para impedir: la clave dice 2025 y el ano
            // fiscal 2026. Sin esta comprobacion se presenta fuera de plazo y nada lo
            // delata hasta la sancion por extemporaneidad.
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING, 2026,
                    "2025-M12", 1, null, null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("does not match the shape required for");
        }

        @ParameterizedTest
        @CsvSource({"BIMONTHLY, 2026-C01", "FOURMONTHLY, 2026-B01", "ANNUAL, 2026-B01"})
        @DisplayName("la forma del IVA depende ademas de su periodicidad")
        void la_forma_del_iva_depende_de_su_periodicidad(VatFrequency frecuencia, String clave) {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.VAT, TaxReturnMother.ANIO,
                    clave, 1, null, frecuencia, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("does not match the shape required for");
        }

        @Test
        @DisplayName("sin clave de periodo no se construye")
        void sin_clave_de_periodo_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING,
                    TaxReturnMother.ANIO, "  ", 1, null, null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("fiscalPeriodKey is required");
        }
    }

    @Nested
    @DisplayName("Ano fiscal (chk_tax_returns_year)")
    class AnoFiscal {

        @ParameterizedTest
        @CsvSource({"2019", "2101"})
        @DisplayName("fuera del rango 2020-2100 no se construye")
        void fuera_del_rango_no_se_construye(int ano) {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING, ano,
                    ano + "-M03", 1, null, null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("fiscalYear must be between 2020 and 2100");
        }

        @ParameterizedTest
        @CsvSource({"2020", "2100"})
        @DisplayName("los dos extremos del rango si se construyen")
        void los_extremos_del_rango_si_se_construyen(int ano) {
            assertThatCode(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING, ano, ano + "-M03",
                    1, null, null, TaxReturnStatus.DRAFT, null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Cadena de correcciones (chk_tax_returns_correction)")
    class CadenaDeCorrecciones {

        @Test
        @DisplayName("la primera declaracion no corrige a nadie")
        void la_primera_declaracion_no_corrige_a_nadie() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING,
                    TaxReturnMother.ANIO, TaxReturnMother.ANIO + "-M03", 1, null, null,
                    TaxReturnStatus.DRAFT, 41L))
                    .hasMessageContaining("the first tax return corrects nothing");
        }

        @Test
        @DisplayName("una correccion tiene que nombrar a la que corrige")
        void una_correccion_tiene_que_nombrar_a_la_que_corrige() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING,
                    TaxReturnMother.ANIO, TaxReturnMother.ANIO + "-M03", 2, null, null,
                    TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("a correction must name the return it corrects");
        }

        @Test
        @DisplayName("un consecutivo menor que uno no se construye")
        void un_consecutivo_menor_que_uno_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING,
                    TaxReturnMother.ANIO, TaxReturnMother.ANIO + "-M03", 0, null, null,
                    TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("sequenceNumber must be 1 or greater");
        }

        @Test
        @DisplayName("una declaracion reconstruida no puede corregirse a si misma")
        void una_declaracion_reconstruida_no_puede_corregirse_a_si_misma() {
            // OJO CON ESTE CASO. Por la via de correctionDraft() la comprobacion es
            // inalcanzable: la correccion nace sin id y correctsReturnId es inmutable.
            // Pero el constructor NO es solo el de las factorias: es por donde
            // TaxReturnJpaMapper.toDomain reconstruye una fila leida de la base, con
            // su id ya puesto. Una fila con id = corrects_return_id -que ninguna
            // constraint impide, porque el manual prohibe referenciar una columna
            // AUTO_INCREMENT dentro de un CHECK- se para AQUI al leerla. Por eso el
            // caso se escribe por el camino del mapper y no por el de la factoria:
            // es el unico donde la regla es alcanzable, y no es vacio.
            assertThatThrownBy(() -> TaxReturnMother.crudo(77L, TaxKind.WITHHOLDING,
                    TaxReturnMother.ANIO, TaxReturnMother.ANIO + "-M03", 2, null, null,
                    TaxReturnStatus.DRAFT, 77L))
                    .isInstanceOf(TaxReturnCannotCorrectItselfException.class)
                    .hasMessageContaining("77");
        }

        @Test
        @DisplayName("el borrador de la correccion lleva el consecutivo siguiente y apunta atras")
        void el_borrador_de_la_correccion_apunta_atras() {
            TaxReturn presentada = TaxReturnMother.retencionPresentada(55L);

            TaxReturn correccion = presentada.correctionDraft(new BigDecimal("4400000.00"),
                    BigDecimal.ZERO, new BigDecimal("4400000.00"), BigDecimal.ZERO,
                    LocalDateTime.of(2026, 5, 2, 8, 0));

            assertThat(correccion.getId()).isNull();
            assertThat(correccion.getSequenceNumber()).isEqualTo(2);
            assertThat(correccion.getCorrectsReturnId()).isEqualTo(55L);
            assertThat(correccion.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
            assertThat(correccion.getFiledAt()).isNull();
            assertThat(correccion.getFirmezaUntil()).isNull();
        }

        @Test
        @DisplayName("una declaracion sin persistir todavia no se puede corregir")
        void una_declaracion_sin_persistir_no_se_puede_corregir() {
            assertThatThrownBy(() -> TaxReturnMother.borradorDeRetencion().correctionDraft(
                    BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                    TaxReturnMother.CREADA_EL)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("only a persisted tax return can be corrected");
        }
    }

    @Nested
    @DisplayName("Importes")
    class Importes {

        @ParameterizedTest
        @CsvSource({"totalGenerated", "totalDeductible", "balancePayable", "balanceCredit"})
        @DisplayName("ningun importe admite un valor negativo")
        void ningun_importe_admite_un_valor_negativo(String campo) {
            BigDecimal menosUno = new BigDecimal("-1.00");
            BigDecimal cero = BigDecimal.ZERO;

            assertThatThrownBy(() -> new TaxReturn(null, TaxKind.WITHHOLDING, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, TaxReturnStatus.DRAFT, null, null,
                    null, null, "totalGenerated".equals(campo) ? menosUno : cero,
                    "totalDeductible".equals(campo) ? menosUno : cero,
                    "balancePayable".equals(campo) ? menosUno : cero,
                    "balanceCredit".equals(campo) ? menosUno : cero, null, null,
                    TaxReturnMother.CREADA_EL, null)).hasMessageContaining(campo);
        }

        @Test
        @DisplayName("una declaracion no deja saldo a pagar y saldo a favor a la vez")
        void no_deja_saldo_a_pagar_y_saldo_a_favor_a_la_vez() {
            // Los dos a la vez es aritmeticamente imposible y contablemente ambiguo:
            // no se sabria si hay que pagar o si hay que arrastrar el credito.
            assertThatThrownBy(() -> new TaxReturn(null, TaxKind.WITHHOLDING, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, TaxReturnStatus.DRAFT, null, null,
                    null, null, new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("60.00"),
                    new BigDecimal("40.00"), null, null, TaxReturnMother.CREADA_EL, null))
                    .hasMessageContaining("either a payable balance or a credit balance");
        }

        @Test
        @DisplayName("los dos saldos en cero si se construyen: es una declaracion en ceros")
        void los_dos_saldos_en_cero_si_se_construyen() {
            assertThatCode(() -> new TaxReturn(null, TaxKind.WITHHOLDING, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, TaxReturnStatus.DRAFT, null, null,
                    null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, TaxReturnMother.CREADA_EL, null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Datos de presentacion (chk_tax_returns_filed)")
    class DatosDePresentacion {

        @ParameterizedTest
        @EnumSource(value = TaxReturnStatus.class, names = {"DRAFT", "ANNULLED"})
        @DisplayName("una declaracion no presentada no carga datos de presentacion")
        void una_no_presentada_no_carga_datos_de_presentacion(TaxReturnStatus estado) {
            assertThatThrownBy(() -> new TaxReturn(9L, TaxKind.WITHHOLDING, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, estado,
                    LocalDateTime.of(2026, 4, 12, 10, 30), 990L, "REC-1", null,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                    BigDecimal.ZERO, null, null, TaxReturnMother.CREADA_EL, null))
                    .hasMessageContaining("must not carry filing data");
        }

        @ParameterizedTest
        @EnumSource(value = TaxReturnStatus.class, names = {"FILED", "CORRECTED"})
        @DisplayName("una presentada exige las cinco cosas a la vez")
        void una_presentada_exige_las_cinco_cosas(TaxReturnStatus estado) {
            assertThatThrownBy(() -> new TaxReturn(9L, TaxKind.WITHHOLDING, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, estado,
                    LocalDateTime.of(2026, 4, 12, 10, 30), 990L, "REC-1", null,
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                    BigDecimal.ZERO, LocalDate.of(2029, 4, 12), null, TaxReturnMother.CREADA_EL,
                    null)).hasMessageContaining("needs filedAt");
        }

        @Test
        @DisplayName("la firmeza tiene que ser posterior a la presentacion")
        void la_firmeza_tiene_que_ser_posterior_a_la_presentacion() {
            assertThatThrownBy(
                    () -> TaxReturnMother.conId(9L, TaxReturnMother.borradorDeRetencion()).file(
                            LocalDateTime.of(2026, 4, 12, 10, 30), 990L, "REC-1", "s3://f.pdf",
                            LocalDate.of(2026, 4, 12)))
                    .hasMessageContaining("firmezaUntil must be after the filing date");
        }

        @Test
        @DisplayName("presentar deja la declaracion con sus cinco datos y en FILED")
        void presentar_deja_la_declaracion_en_filed() {
            TaxReturn presentada = TaxReturnMother.retencionPresentada(55L);

            assertThat(presentada.getStatus()).isEqualTo(TaxReturnStatus.FILED);
            assertThat(presentada.getFiledAt()).isEqualTo(LocalDateTime.of(2026, 4, 12, 10, 30));
            assertThat(presentada.getFiledBySystemUserId()).isEqualTo(990L);
            assertThat(presentada.getReceiptRef()).isEqualTo("REC-2026-000123");
            assertThat(presentada.getFileRef()).isEqualTo("s3://declaraciones/2026/M03.pdf");
            assertThat(presentada.getFirmezaUntil()).isEqualTo(LocalDate.of(2029, 4, 12));
            assertThat(presentada.isCurrent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @ParameterizedTest
        @EnumSource(value = TaxReturnStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("solo un borrador admite que le reediten los importes")
        void solo_un_borrador_admite_que_le_reediten_los_importes(TaxReturnStatus estado) {
            // Sin esta barandilla, reeditar una presentada produce una fila que el
            // motor acepta y unos numeros que ya no coinciden con el formulario
            // radicado: chk_tax_returns_filed mira la fila, no de donde venia.
            TaxReturn declaracion = enEstado(estado);

            assertThatThrownBy(() -> declaracion.updateAmounts(new BigDecimal("1.00"),
                    BigDecimal.ZERO, new BigDecimal("1.00"), BigDecimal.ZERO))
                    .isInstanceOf(TaxReturnNotEditableException.class);
        }

        @ParameterizedTest
        @EnumSource(value = TaxReturnStatus.class, names = "FILED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("solo una presentada se puede marcar como corregida")
        void solo_una_presentada_se_puede_marcar_como_corregida(TaxReturnStatus estado) {
            TaxReturn declaracion = enEstado(estado);

            assertThatThrownBy(declaracion::markCorrected)
                    .isInstanceOf(TaxReturnNotEditableException.class);
        }

        @Test
        @DisplayName("marcar corregida conserva la presentacion y libera el hueco vigente")
        void marcar_corregida_conserva_la_presentacion() {
            TaxReturn corregida = TaxReturnMother.retencionPresentada(55L).markCorrected();

            assertThat(corregida.getStatus()).isEqualTo(TaxReturnStatus.CORRECTED);
            assertThat(corregida.getFiledAt()).isNotNull();
            assertThat(corregida.getReceiptRef()).isEqualTo("REC-2026-000123");
            assertThat(corregida.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("anular borra presentacion y firmeza pero conserva la copia del fichero")
        void anular_borra_presentacion_y_firmeza() {
            TaxReturn anulada = TaxReturnMother.conId(55L, TaxReturnMother.borradorDeIva()).annul();

            assertThat(anulada.getStatus()).isEqualTo(TaxReturnStatus.ANNULLED);
            assertThat(anulada.getFiledAt()).isNull();
            assertThat(anulada.getFiledBySystemUserId()).isNull();
            assertThat(anulada.getReceiptRef()).isNull();
            assertThat(anulada.getFirmezaUntil()).isNull();
            assertThat(anulada.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("reeditar los importes de un borrador si se permite")
        void reeditar_los_importes_de_un_borrador_si_se_permite() {
            TaxReturn reeditada = TaxReturnMother.borradorDeRetencion().updateAmounts(
                    new BigDecimal("5000000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("120000.00"));

            assertThat(reeditada.getTotalGenerated()).isEqualByComparingTo("5000000.00");
            assertThat(reeditada.getBalanceCredit()).isEqualByComparingTo("120000.00");
            assertThat(reeditada.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Obligatorios sueltos")
    class ObligatoriosSueltos {

        @Test
        @DisplayName("sin impuesto no se construye")
        void sin_impuesto_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, null, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, TaxReturnStatus.DRAFT, null))
                    .hasMessageContaining("taxKind is required");
        }

        @Test
        @DisplayName("sin estado no se construye")
        void sin_estado_no_se_construye() {
            assertThatThrownBy(() -> TaxReturnMother.crudo(null, TaxKind.WITHHOLDING,
                    TaxReturnMother.ANIO, TaxReturnMother.ANIO + "-M03", 1, null, null, null, null))
                    .hasMessageContaining("status is required");
        }

        @Test
        @DisplayName("sin fecha de creacion no se construye")
        void sin_fecha_de_creacion_no_se_construye() {
            assertThatThrownBy(() -> new TaxReturn(null, TaxKind.WITHHOLDING, TaxReturnMother.ANIO,
                    TaxReturnMother.ANIO + "-M03", 1, null, null, TaxReturnStatus.DRAFT, null, null,
                    null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, null, null)).hasMessageContaining("createdDate is required");
        }
    }

    private static TaxReturn enEstado(TaxReturnStatus estado) {
        return switch (estado) {
            case DRAFT -> TaxReturnMother.conId(55L, TaxReturnMother.borradorDeRetencion());
            case FILED -> TaxReturnMother.retencionPresentada(55L);
            case CORRECTED -> TaxReturnMother.retencionPresentada(55L).markCorrected();
            case ANNULLED ->
                TaxReturnMother.conId(55L, TaxReturnMother.borradorDeRetencion()).annul();
        };
    }

    private static String periodoValido(TaxKind taxKind) {
        return switch (taxKind) {
            case INCOME_TAX -> TaxReturnMother.ANIO + "-A";
            case WITHHOLDING -> TaxReturnMother.ANIO + "-M03";
            case ICA, VAT -> TaxReturnMother.ANIO + "-B01";
        };
    }

    private static String municipioValido(TaxKind taxKind) {
        return taxKind == TaxKind.ICA ? TaxReturnMother.MUNICIPIO_ICA : null;
    }

    private static VatFrequency frecuenciaValida(TaxKind taxKind) {
        return taxKind == TaxKind.VAT ? VatFrequency.BIMONTHLY : null;
    }
}
