package com.vetsoftware.app.medicament.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MedicamentJpaMapper")
class MedicamentJpaMapperTest {

    private final MedicamentJpaMapper mapper = new MedicamentJpaMapper();

    @Nested
    @DisplayName("toJpa / toDomain — medicamento propio de una empresa")
    class MedicamentoDeEmpresa {

        @Test
        @DisplayName("ida y vuelta conserva name, description, general y company")
        void ida_y_vuelta_conserva_los_campos() {
            CompanyRef companyRef = new CompanyRef(9L, "Clinica Norte", "900123456");
            Medicament medicamento = Medicament.create("Suero", "Formula propia", companyRef,
                    false);

            CompanyJpaEntity companyJpaEntity = mock(CompanyJpaEntity.class);
            when(companyJpaEntity.getId()).thenReturn(9L);
            when(companyJpaEntity.getName()).thenReturn("Clinica Norte");
            when(companyJpaEntity.getIdentifier()).thenReturn("900123456");

            MedicamentJpaEntity entity = mapper.toJpa(medicamento, companyJpaEntity);
            entity.setCreatedDate(LocalDateTime.of(2026, 1, 1, 0, 0));

            assertThat(entity.getName()).isEqualTo("Suero");
            assertThat(entity.getDescription()).isEqualTo("Formula propia");
            assertThat(entity.getGeneral()).isFalse();
            assertThat(entity.getCompany()).isSameAs(companyJpaEntity);

            Medicament vuelta = mapper.toDomain(entity);

            assertThat(vuelta.getName()).isEqualTo("Suero");
            assertThat(vuelta.getCompany()).isEqualTo(companyRef);
            assertThat(vuelta.isGeneral()).isFalse();
        }
    }

    @Nested
    @DisplayName("toJpa / toDomain — medicamento general sin empresa")
    class MedicamentoGeneral {

        @Test
        @DisplayName("toJpa con company nula y toDomain devuelve company nula")
        void toJpa_con_company_nula() {
            Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);

            MedicamentJpaEntity entity = mapper.toJpa(medicamento, null);
            entity.setCreatedDate(LocalDateTime.of(2026, 1, 1, 0, 0));

            assertThat(entity.getCompany()).isNull();
            assertThat(entity.getGeneral()).isTrue();

            Medicament vuelta = mapper.toDomain(entity);

            assertThat(vuelta.getCompany()).isNull();
            assertThat(vuelta.isGeneral()).isTrue();
        }

        @Test
        @DisplayName("toDomain trata general=null como false (Boolean.TRUE.equals)")
        void toDomain_trata_general_nulo_como_false() {
            // El invariante de Medicament exige company cuando general=false, asi que la
            // entidad necesita una company para que el mapeo con general=null sea valido.
            CompanyRef companyRef = new CompanyRef(9L, "Clinica Norte", "900123456");
            CompanyJpaEntity companyJpaEntity = mock(CompanyJpaEntity.class);
            when(companyJpaEntity.getId()).thenReturn(9L);
            when(companyJpaEntity.getName()).thenReturn("Clinica Norte");
            when(companyJpaEntity.getIdentifier()).thenReturn("900123456");
            MedicamentJpaEntity entity = mapper.toJpa(
                    Medicament.create("Amoxicilina", null, companyRef, false), companyJpaEntity);
            entity.setGeneral(null);
            entity.setCreatedDate(LocalDateTime.of(2026, 1, 1, 0, 0));

            // toDomain(entity) de un solo argumento SI deriva el CompanyRef de
            // entity.getCompany(); el overload de dos argumentos usa el ref tal cual se le
            // pase, así que aquí hace falta el de un argumento para respetar el invariante.
            assertThat(mapper.toDomain(entity).isGeneral()).isFalse();
        }
    }

    @Test
    @DisplayName("toDomain(entity, ref) reusa el CompanyRef precargado sin tocar el proxy")
    void toDomain_con_ref_reusa_el_precargado() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "900123456");
        Medicament medicamento = Medicament.create("Suero", null, ref, false);
        MedicamentJpaEntity entity = mapper.toJpa(medicamento, null);
        entity.setCreatedDate(LocalDateTime.of(2026, 1, 1, 0, 0));

        Medicament vuelta = mapper.toDomain(entity, ref);

        assertThat(vuelta.getCompany()).isSameAs(ref);
    }
}
