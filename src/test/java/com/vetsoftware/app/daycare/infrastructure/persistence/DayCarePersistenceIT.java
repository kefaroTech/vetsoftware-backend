package com.vetsoftware.app.daycare.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de guarderia/hotel contra MySQL real.
 *
 * <p>
 * {@code DayCareJpaEntity} tiene FKs obligatorias a {@code animals} y
 * {@code companies}, y a su vez {@code animals} exige specie, breed, owner y
 * color propios. {@link SchemaSeed} no siembra un animal, asi que este test
 * completa la cadena con sus propias filas (ids 970+, fuera del rango de
 * {@link SchemaSeed} y del resto de features que ya usan el rango 960-969).
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDayCareRepository — guarderia/hotel contra MySQL real")
class DayCarePersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;

    private static final Long SPECIE_ID = 970L;
    private static final Long BREED_ID = 971L;
    private static final Long OWNER_ID = 972L;
    private static final Long COLOR_ID = 973L;
    private static final Long ANIMAL_ID = 974L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais-DC", "A-DC-001");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");

    @Autowired
    private JpaDayCareRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Perro-DC', '2026-01-01 00:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Labrador-DC', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-DC', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-DC-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
        animal(ANIMAL_ID, "Firulais-DC", "A-DC-001");
        entityManager.flush();
    }

    private void animal(Long id, String nombre, String codigo) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, :nombre, :codigo, :specie, :breed, :owner, 'MALE', 'KILOGRAMS', 'NONE',
                        'STERILIZED', :color, false, :empresa, '2026-01-01 00:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER_ID)
                .setParameter("color", COLOR_ID).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private DayCare nuevoDayCare(AnimalRef animal, DayCareType tipo) {
        return DayCare.create(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), tipo, "Correa, plato", "Sin novedades", animal, CLINICA);
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el daycare y lo relee con animal y empresa resueltos")
        void guarda_y_relee_con_animal_y_empresa_resueltos() {
            DayCare guardado = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            Optional<DayCare> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releido.get().getCompany()).isEqualTo(CLINICA);
            assertThat(releido.get().getType()).isEqualTo(DayCareType.DAYCARE);
            assertThat(releido.get().getObjects()).isEqualTo("Correa, plato");
            assertThat(releido.get().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("findAll — listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("incluye daycares de todas las empresas")
        void incluye_daycares_de_todas_las_empresas() {
            DayCare deEstaEmpresa = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            List<DayCare> todos = repository.findAll();

            assertThat(todos).extracting(DayCare::getId).contains(deEstaEmpresa.getId());
        }
    }
}
