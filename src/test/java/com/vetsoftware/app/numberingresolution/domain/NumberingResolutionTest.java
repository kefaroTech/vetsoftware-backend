package com.vetsoftware.app.numberingresolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("NumberingResolution")
class NumberingResolutionTest {

    private static final CompanyRef COMPANY = NumberingResolutionMother.EMPRESA;
    private static final ElectronicDocumentType TYPE = ElectronicDocumentType.FE_VENTA;
    private static final String NUMBER = "18760000001";
    private static final LocalDate DATE = LocalDate.of(2026, 1, 2);
    private static final String PREFIX = "SETP";
    private static final Long FROM = 100L;
    private static final Long TO = 199L;
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);
    private static final String TECH_KEY = "clave-tecnica";
    private static final Long CURRENT = 100L;

    private static NumberingResolution crear(CompanyRef company, ElectronicDocumentType type,
            String number, LocalDate date, String prefix, Long from, Long to, LocalDate validFrom,
            LocalDate validTo, String techKey, Long current) {
        return new NumberingResolution(null, company, type, number, date, prefix, from, to,
                validFrom, validTo, techKey, current, LocalDateTime.of(2026, 1, 2, 9, 0), true,
                null);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("create() arranca el consecutivo en rangeFrom, habilitada y sin id")
        void create_arranca_el_consecutivo_en_range_from() {
            NumberingResolution creada = NumberingResolution.create(COMPANY, TYPE, NUMBER, DATE,
                    PREFIX, FROM, TO, VALID_FROM, VALID_TO, TECH_KEY, null);

            assertThat(creada.getId()).isNull();
            assertThat(creada.getCurrentNumber()).isEqualTo(FROM);
            assertThat(creada.isEnabled()).isTrue();
            assertThat(creada.getBranchId()).isNull();
            assertThat(creada.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("create() de una sede conserva el branchId")
        void create_de_una_sede_conserva_el_branch_id() {
            NumberingResolution creada = NumberingResolution.create(COMPANY, TYPE, NUMBER, DATE,
                    PREFIX, FROM, TO, VALID_FROM, VALID_TO, TECH_KEY,
                    NumberingResolutionMother.BRANCH_ID);

            assertThat(creada.getBranchId()).isEqualTo(NumberingResolutionMother.BRANCH_ID);
        }

        @Test
        @DisplayName("create() con datos invalidos falla igual que el constructor")
        void create_con_datos_invalidos_falla() {
            assertThatThrownBy(() -> NumberingResolution.create(null, TYPE, NUMBER, DATE, PREFIX,
                    FROM, TO, VALID_FROM, VALID_TO, TECH_KEY, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update() conserva id, consecutivo y fecha de creacion")
        void update_conserva_id_consecutivo_y_fecha_de_creacion() {
            NumberingResolution existente = NumberingResolutionMother.activaDeEmpresa();

            existente.update(NumberingResolutionMother.EMPRESA, ElectronicDocumentType.NOTA_CREDITO,
                    "18760000002", DATE, "NUEV", 100L, 299L, VALID_FROM, VALID_TO, "otra-clave",
                    NumberingResolutionMother.BRANCH_ID);

            assertThat(existente.getId()).isEqualTo(NumberingResolutionMother.RESOLUTION_ID);
            assertThat(existente.getCurrentNumber()).isEqualTo(100L);
            assertThat(existente.getCreatedDate()).isEqualTo(NumberingResolutionMother.CREADA);
            assertThat(existente.getDocumentType()).isEqualTo(ElectronicDocumentType.NOTA_CREDITO);
            assertThat(existente.getPrefix()).isEqualTo("NUEV");
            assertThat(existente.getRangeTo()).isEqualTo(299L);
            assertThat(existente.getBranchId()).isEqualTo(NumberingResolutionMother.BRANCH_ID);
        }

        @Test
        @DisplayName("update() con datos invalidos falla y no muta el estado previo a la validacion")
        void update_con_datos_invalidos_falla() {
            NumberingResolution existente = NumberingResolutionMother.activaDeEmpresa();

            assertThatThrownBy(() -> existente.update(NumberingResolutionMother.EMPRESA, TYPE, "",
                    DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO, TECH_KEY, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("resolutionNumber is required");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable() apaga la resolucion y enable() la vuelve a prender")
        void disable_y_enable_alternan_el_estado() {
            NumberingResolution resolucion = NumberingResolutionMother.activaDeEmpresa();

            resolucion.disable();
            assertThat(resolucion.isEnabled()).isFalse();

            resolucion.enable();
            assertThat(resolucion.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("invariantes de construccion")
    class Validaciones {

        @ParameterizedTest(name = "{11}")
        @MethodSource("com.vetsoftware.app.numberingresolution.domain.NumberingResolutionTest#invariantesInvalidas")
        @DisplayName("rechaza datos que violan un invariante")
        void rechaza_datos_invalidos(CompanyRef company, ElectronicDocumentType type, String number,
                LocalDate date, String prefix, Long from, Long to, LocalDate validFrom,
                LocalDate validTo, String techKey, Long current, String mensajeEsperado) {
            assertThatThrownBy(() -> crear(company, type, number, date, prefix, from, to, validFrom,
                    validTo, techKey, current)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }

        @ParameterizedTest(name = "{5}")
        @MethodSource("com.vetsoftware.app.numberingresolution.domain.NumberingResolutionTest#casosLimiteValidos")
        @DisplayName("acepta los valores limite de rango, vigencia y campos opcionales")
        void acepta_valores_limite(Long from, Long to, LocalDate validFrom, LocalDate validTo,
                Long current, String descripcion) {
            NumberingResolution resolucion = crear(COMPANY, TYPE, NUMBER, DATE, PREFIX, from, to,
                    validFrom, validTo, TECH_KEY, current);

            assertThat(resolucion.getRangeFrom()).isEqualTo(from);
        }

        @Test
        @DisplayName("acepta prefijo nulo: es opcional")
        void acepta_prefijo_nulo() {
            NumberingResolution resolucion = crear(COMPANY, TYPE, NUMBER, DATE, null, FROM, TO,
                    VALID_FROM, VALID_TO, TECH_KEY, CURRENT);

            assertThat(resolucion.getPrefix()).isNull();
        }

        @Test
        @DisplayName("acepta clave tecnica nula: es opcional")
        void acepta_clave_tecnica_nula() {
            NumberingResolution resolucion = crear(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO,
                    VALID_FROM, VALID_TO, null, CURRENT);

            assertThat(resolucion.getTechnicalKey()).isNull();
        }

        @Test
        @DisplayName("acepta consecutivo nulo: aun no se ha emitido ningun documento")
        void acepta_consecutivo_nulo() {
            NumberingResolution resolucion = crear(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO,
                    VALID_FROM, VALID_TO, TECH_KEY, null);

            assertThat(resolucion.getCurrentNumber()).isNull();
        }
    }

    static Stream<Arguments> invariantesInvalidas() {
        return Stream.of(
                Arguments.of(null, TYPE, NUMBER, DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "company is required"),
                Arguments.of(COMPANY, null, NUMBER, DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "documentType is required"),
                Arguments.of(COMPANY, TYPE, null, DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "resolutionNumber is required"),
                Arguments.of(COMPANY, TYPE, "   ", DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "resolutionNumber is required"),
                Arguments.of(COMPANY, TYPE, "1".repeat(51), DATE, PREFIX, FROM, TO, VALID_FROM,
                        VALID_TO, TECH_KEY, CURRENT, "resolutionNumber must be 50 chars or less"),
                Arguments.of(COMPANY, TYPE, NUMBER, null, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "resolutionDate is required"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, "P".repeat(11), FROM, TO, VALID_FROM,
                        VALID_TO, TECH_KEY, CURRENT, "prefix must be 10 chars or less"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, null, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "rangeFrom is required"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, 0L, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "rangeFrom must be 1 or greater"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, null, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "rangeTo is required"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, 99L, VALID_FROM, VALID_TO,
                        TECH_KEY, CURRENT, "rangeTo must be greater than or equal to rangeFrom"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO, null, VALID_TO,
                        TECH_KEY, CURRENT, "validFrom is required"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO, VALID_FROM, null,
                        TECH_KEY, CURRENT, "validTo is required"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO, VALID_TO, VALID_FROM,
                        TECH_KEY, CURRENT, "validTo cannot be before validFrom"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        "K".repeat(256), CURRENT, "technicalKey must be 255 chars or less"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, 99L, "currentNumber must be within the resolution range"),
                Arguments.of(COMPANY, TYPE, NUMBER, DATE, PREFIX, FROM, TO, VALID_FROM, VALID_TO,
                        TECH_KEY, 200L, "currentNumber must be within the resolution range"));
    }

    static Stream<Arguments> casosLimiteValidos() {
        return Stream.of(
                Arguments.of(1L, 1L, VALID_FROM, VALID_TO, 1L, "rango de un solo numero desde 1"),
                Arguments.of(FROM, TO, VALID_FROM, VALID_TO, FROM,
                        "consecutivo en el primer numero del rango"),
                Arguments.of(FROM, TO, VALID_FROM, VALID_TO, TO,
                        "consecutivo en el ultimo numero del rango"),
                Arguments.of(FROM, TO, VALID_FROM, VALID_FROM, CURRENT,
                        "vigencia de un solo dia (validTo == validFrom)"));
    }
}
