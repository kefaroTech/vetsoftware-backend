package com.vetsoftware.app.laboratorytest.application.dto;

import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BACTERIOLOGA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CLINICA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CONSULTA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CREADO;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.FECHA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.FIRULAIS;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.HEMOGRAMA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.PROCESADO;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El DTO es el contrato que sale del caso de uso. Un campo cruzado aqui no lo
 * detecta ninguna otra capa: compila, viaja y solo se ve en pantalla.
 */
@DisplayName("DTOs de salida del modulo laboratorytest")
class LaboratoryTestDtoTest {

    @Nested
    @DisplayName("LaboratoryTestDto.from")
    class Desde {

        @Test
        @DisplayName("copia cada campo del agregado en su posicion")
        void copia_cada_campo_del_agregado() {
            LaboratoryTestDto dto = LaboratoryTestDto.from(LaboratoryTestMother.validada());

            assertThat(dto.id()).isEqualTo(LaboratoryTestMother.ID);
            assertThat(dto.date()).isEqualTo(FECHA);
            assertThat(dto.testType()).isEqualTo(new LaboratoryTestTypeSummaryDto(4L, "Hemograma"));
            assertThat(dto.quantity()).isEqualTo(2);
            assertThat(dto.diagnosis()).isEqualTo("Anemia regenerativa");
            assertThat(dto.status()).isEqualTo("COMPLETED");
            assertThat(dto.prioridad()).isEqualTo("URGENTE");
            assertThat(dto.animal()).isEqualTo(new AnimalSummaryDto(7L, "Firulais", "A-001"));
            assertThat(dto.consultation())
                    .isEqualTo(new ConsultationSummaryDto(11L, LocalDate.of(2026, 3, 14)));
            assertThat(dto.company())
                    .isEqualTo(new CompanySummaryDto(9L, "Clinica Kefaro", "900123456"));
            assertThat(dto.processedBy())
                    .isEqualTo(new EmployeeSummaryDto(3L, "EMP-003", "Ana Ruiz"));
            assertThat(dto.processedDate()).isEqualTo(PROCESADO);
            assertThat(dto.createdDate()).isEqualTo(CREADO);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("el estado y la prioridad viajan como texto, no como enum")
        void el_estado_y_la_prioridad_viajan_como_texto() {
            LaboratoryTestDto dto = LaboratoryTestDto.from(LaboratoryTestMother.pendienteDeToma());

            assertThat(dto.status()).isEqualTo(LaboratoryTestStatus.PENDING_COLLECTION.name());
            assertThat(dto.prioridad()).isEqualTo(LaboratoryTestPriority.NORMAL.name());
        }

        @Test
        @DisplayName("sin consulta ni procesador deja esos bloques en null, no en objetos vacios")
        void sin_consulta_ni_procesador_deja_esos_bloques_en_null() {
            LaboratoryTestDto dto = LaboratoryTestDto
                    .from(LaboratoryTestMother.sinAsociacionesOpcionales());

            assertThat(dto.consultation()).isNull();
            assertThat(dto.processedBy()).isNull();
            assertThat(dto.processedDate()).isNull();
            assertThat(dto.diagnosis()).isNull();
        }

        @Test
        @DisplayName("una muestra deshabilitada lo declara en el DTO")
        void una_muestra_deshabilitada_lo_declara() {
            assertThat(LaboratoryTestDto.from(LaboratoryTestMother.deshabilitada()).enabled())
                    .isFalse();
        }

        @Test
        @DisplayName("una muestra sin id todavia produce un DTO valido")
        void una_muestra_sin_id_todavia_produce_un_dto_valido() {
            LaboratoryTest nueva = LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", FIRULAIS,
                    CONSULTA, CLINICA, LaboratoryTestMother.BRANCH_ID);

            LaboratoryTestDto dto = LaboratoryTestDto.from(nueva);

            assertThat(dto.id()).isNull();
            assertThat(dto.animal().id()).isEqualTo(7L);
        }

        @Test
        @DisplayName("la sede no se expone: la bandeja la filtra en servidor")
        void la_sede_no_se_expone() {
            assertThat(LaboratoryTestDto.class.getRecordComponents())
                    .noneMatch(component -> "branchId".equals(component.getName()));
        }
    }

    @Nested
    @DisplayName("Resumenes anidados")
    class Resumenes {

        @Test
        @DisplayName("AnimalSummaryDto copia id, nombre y codigo")
        void animal_summary_copia_los_tres_campos() {
            AnimalSummaryDto dto = AnimalSummaryDto.from(new AnimalRef(7L, "Firulais", "A-001"));

            assertThat(dto.id()).isEqualTo(7L);
            assertThat(dto.name()).isEqualTo("Firulais");
            assertThat(dto.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("AnimalSummaryDto conserva el codigo nulo del animal sin codigo")
        void animal_summary_conserva_el_codigo_nulo() {
            assertThat(AnimalSummaryDto.from(new AnimalRef(7L, "Firulais", null)).code()).isNull();
        }

        @Test
        @DisplayName("CompanySummaryDto copia id, nombre e identificador")
        void company_summary_copia_los_tres_campos() {
            CompanySummaryDto dto = CompanySummaryDto.from(CLINICA);

            assertThat(dto.id()).isEqualTo(9L);
            assertThat(dto.name()).isEqualTo("Clinica Kefaro");
            assertThat(dto.identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("ConsultationSummaryDto copia id y fecha")
        void consultation_summary_copia_id_y_fecha() {
            ConsultationSummaryDto dto = ConsultationSummaryDto
                    .from(new ConsultationRef(11L, LocalDate.of(2026, 3, 14)));

            assertThat(dto.id()).isEqualTo(11L);
            assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 3, 14));
        }

        @Test
        @DisplayName("EmployeeSummaryDto copia id, codigo y nombre")
        void employee_summary_copia_los_tres_campos() {
            EmployeeSummaryDto dto = EmployeeSummaryDto.from(BACTERIOLOGA);

            assertThat(dto.id()).isEqualTo(3L);
            assertThat(dto.employeeCode()).isEqualTo("EMP-003");
            assertThat(dto.name()).isEqualTo("Ana Ruiz");
        }

        @Test
        @DisplayName("LaboratoryTestTypeSummaryDto copia id y nombre")
        void test_type_summary_copia_id_y_nombre() {
            LaboratoryTestTypeSummaryDto dto = LaboratoryTestTypeSummaryDto.from(HEMOGRAMA);

            assertThat(dto.id()).isEqualTo(4L);
            assertThat(dto.name()).isEqualTo("Hemograma");
        }

        @Test
        @DisplayName("los resumenes no arrastran campos que el dominio no expone")
        void los_resumenes_no_arrastran_campos_extra() {
            EmployeeRef otro = new EmployeeRef(99L, "EMP-099", "Luis Paz");
            LaboratoryTestTypeRef tipo = new LaboratoryTestTypeRef(8L, "Uroanalisis");
            CompanyRef empresa = new CompanyRef(1L, "Otra", "800000000");

            assertThat(EmployeeSummaryDto.from(otro))
                    .isEqualTo(new EmployeeSummaryDto(99L, "EMP-099", "Luis Paz"));
            assertThat(LaboratoryTestTypeSummaryDto.from(tipo))
                    .isEqualTo(new LaboratoryTestTypeSummaryDto(8L, "Uroanalisis"));
            assertThat(CompanySummaryDto.from(empresa))
                    .isEqualTo(new CompanySummaryDto(1L, "Otra", "800000000"));
        }
    }
}
