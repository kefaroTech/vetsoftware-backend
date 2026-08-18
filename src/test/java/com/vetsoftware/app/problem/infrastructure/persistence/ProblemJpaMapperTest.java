package com.vetsoftware.app.problem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.problem.domain.AnimalRef;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * Las entidades JPA de otras features se mockean porque su constructor sin
 * argumentos es {@code protected} y no son instanciables desde este paquete. No
 * tienen logica: son portadores de datos, y mockearlas no oculta
 * comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemJpaMapper")
class ProblemJpaMapperTest {

    private final ProblemJpaMapper mapper = new ProblemJpaMapper();

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Problem problem = ProblemMother.activo();

            ProblemJpaEntity entity = mapper.toJpa(problem, animalEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(ProblemMother.PROBLEM_ID);
            assertThat(entity.getDescription()).isEqualTo("Dermatitis alergica");
            assertThat(entity.getStatus()).isEqualTo(ProblemStatus.ACTIVE);
            assertThat(entity.getOnsetDate()).isEqualTo(ProblemMother.INICIO);
            assertThat(entity.getResolvedDate()).isNull();
            assertThat(entity.getNotes()).isEqualTo("Revisar en dos semanas");
            assertThat(entity.getCreatedDate()).isEqualTo(ProblemMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha animal y empresa en su slot")
        void engancha_animal_y_empresa_en_su_slot() {
            ProblemJpaEntity entity = mapper.toJpa(ProblemMother.activo(), animalEntity,
                    companyEntity);

            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("onsetDate y resolvedDate no se cruzan — ambos son LocalDate")
        void onset_date_y_resolved_date_no_se_cruzan() {
            LocalDate resolucion = LocalDate.of(2026, 2, 1);
            Problem problem = ProblemMother.resuelto(resolucion);

            ProblemJpaEntity entity = mapper.toJpa(problem, animalEntity, companyEntity);

            assertThat(entity.getOnsetDate()).isEqualTo(ProblemMother.INICIO);
            assertThat(entity.getResolvedDate()).isEqualTo(resolucion);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getAnimal(), Hibernate lanzaria un SELECT extra por save.
            ProblemJpaEntity entity = mapper.toJpa(ProblemMother.activo(), animalEntity,
                    companyEntity);

            Problem reconstruido = mapper.toDomain(entity, ProblemMother.FIRULAIS,
                    ProblemMother.CLINICA);

            assertThat(reconstruido).usingRecursiveComparison().isEqualTo(ProblemMother.activo());
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Problem original = ProblemMother.resuelto(LocalDate.of(2026, 2, 1));

            ProblemJpaEntity entity = mapper.toJpa(original, animalEntity, companyEntity);
            Problem vuelta = mapper.toDomain(entity, ProblemMother.FIRULAIS, ProblemMother.CLINICA);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye animal y empresa desde su propia asociacion")
        void construye_animal_y_empresa_desde_su_asociacion() {
            when(animalEntity.getId()).thenReturn(ProblemMother.ANIMAL_ID);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");
            when(companyEntity.getId()).thenReturn(ProblemMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("NIT-900123");

            ProblemJpaEntity entity = mapper.toJpa(ProblemMother.activo(), animalEntity,
                    companyEntity);

            Problem problem = mapper.toDomain(entity);

            assertThat(problem.getAnimal())
                    .isEqualTo(new AnimalRef(ProblemMother.ANIMAL_ID, "Firulais", "A-001"));
            assertThat(problem.getCompany()).isEqualTo(
                    new CompanyRef(ProblemMother.COMPANY_ID, "Clinica Norte", "NIT-900123"));
        }
    }
}
