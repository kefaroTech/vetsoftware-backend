package com.vetsoftware.app.animalalert.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.domain.AnimalRef;
import com.vetsoftware.app.animalalert.domain.CompanyRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las alertas clinicas contra MySQL real.
 *
 * <p>
 * {@code AnimalAlertJpaEntity} tiene FKs obligatorias a {@code animals} y
 * {@code companies}, y a su vez {@code animals} exige specie, breed, owner y
 * color propios. {@link SchemaSeed} no siembra un animal, asi que este test
 * completa la cadena con sus propias filas (ids 960+, fuera del rango de
 * {@link SchemaSeed}) para poder guardar una alerta real.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAnimalAlertRepository — alertas clinicas contra MySQL real")
class AnimalAlertPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long SPECIE_ID = 960L;
    private static final Long BREED_ID = 961L;
    private static final Long OWNER_ID = 962L;
    private static final Long COLOR_ID = 963L;
    private static final Long ANIMAL_ID = 964L;
    private static final Long OTRO_ANIMAL_ID = 965L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    private static final AnimalRef MICHI = new AnimalRef(OTRO_ANIMAL_ID, "Michi", "A-002");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");

    @Autowired
    private JpaAnimalAlertRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Perro-AA', '2026-01-01 00:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Labrador-AA', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-AA', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-AA-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
        animal(ANIMAL_ID, "Firulais", "A-001", EMPRESA);
        animal(OTRO_ANIMAL_ID, "Michi", "A-002", EMPRESA);
        entityManager.flush();
    }

    private void animal(Long id, String nombre, String codigo, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, :nombre, :codigo, :specie, :breed, :owner, 'MALE', 'KILOGRAMS', 'NONE',
                        'STERILIZED', :color, false, :empresa, '2026-01-01 00:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER_ID)
                .setParameter("color", COLOR_ID).setParameter("empresa", companyId).executeUpdate();
    }

    private AnimalAlert nuevaAlerta(AnimalRef animal, String descripcion, LocalDateTime creada) {
        return new AnimalAlert(null, animal, CLINICA, AlertType.ALLERGY, descripcion,
                AlertSeverity.HIGH, creada, null, true);
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda la alerta y la relee con animal y empresa resueltos")
        void guarda_la_alerta_y_la_relee() {
            AnimalAlert guardada = repository.save(nuevaAlerta(FIRULAIS, "Alergia a la penicilina",
                    LocalDateTime.of(2026, 1, 15, 10, 30)));
            entityManager.flush();
            entityManager.clear();

            Optional<AnimalAlert> releida = repository.findByIdAndCompanyId(guardada.getId(),
                    EMPRESA);

            assertThat(releida).isPresent();
            assertThat(releida.get().getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releida.get().getCompany()).isEqualTo(CLINICA);
            assertThat(releida.get().getType()).isEqualTo(AlertType.ALLERGY);
            assertThat(releida.get().getDescription()).isEqualTo("Alergia a la penicilina");
            assertThat(releida.get().getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(releida.get().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("una alerta no se lee con la empresa equivocada")
        void una_alerta_no_se_lee_con_la_empresa_equivocada() {
            AnimalAlert guardada = repository
                    .save(nuevaAlerta(FIRULAIS, "Alergia", LocalDateTime.of(2026, 1, 15, 10, 30)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado por animal")
    class ListadoPorAnimal {

        @Test
        @DisplayName("devuelve solo las alertas del animal pedido, mas recientes primero")
        void devuelve_solo_las_alertas_del_animal_ordenadas_por_fecha_desc() {
            AnimalAlert antigua = repository.save(
                    nuevaAlerta(FIRULAIS, "Alergia antigua", LocalDateTime.of(2026, 1, 1, 8, 0)));
            AnimalAlert reciente = repository.save(
                    nuevaAlerta(FIRULAIS, "Alergia reciente", LocalDateTime.of(2026, 2, 1, 8, 0)));
            repository.save(nuevaAlerta(MICHI, "Alergia de otro animal",
                    LocalDateTime.of(2026, 1, 15, 8, 0)));
            entityManager.flush();
            entityManager.clear();

            List<AnimalAlert> encontradas = repository.findByAnimalIdAndCompanyId(ANIMAL_ID,
                    EMPRESA);

            assertThat(encontradas).extracting(AnimalAlert::getId).containsExactly(reciente.getId(),
                    antigua.getId());
        }

        @Test
        @DisplayName("una empresa sin alertas para ese animal recibe una lista vacia")
        void una_empresa_sin_alertas_recibe_lista_vacia() {
            repository.save(nuevaAlerta(FIRULAIS, "Alergia", LocalDateTime.of(2026, 1, 15, 8, 0)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByAnimalIdAndCompanyId(ANIMAL_ID, OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("borrado logico")
    class BorradoLogico {

        @Test
        @DisplayName("borrar deshabilita la fila: deja de aparecer en las lecturas")
        void borrar_deshabilita_la_fila() {
            AnimalAlert guardada = repository
                    .save(nuevaAlerta(FIRULAIS, "Alergia", LocalDateTime.of(2026, 1, 15, 8, 0)));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardada.getId(), EMPRESA);
            entityManager.flush();
            entityManager.clear();

            // @SQLDelete + @SQLRestriction("enabled = true"): la fila sigue en la tabla
            // pero ninguna lectura de la entidad la vuelve a traer.
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), EMPRESA)).isEmpty();
            assertThat(repository.findByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA)).isEmpty();
        }

        @Test
        @DisplayName("borrar una alerta de otra empresa no hace nada")
        void borrar_una_alerta_de_otra_empresa_no_hace_nada() {
            AnimalAlert guardada = repository
                    .save(nuevaAlerta(FIRULAIS, "Alergia", LocalDateTime.of(2026, 1, 15, 8, 0)));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardada.getId(), OTRA_EMPRESA);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), EMPRESA)).isPresent();
        }
    }
}
