package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de tipos de vacuna contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Lo que sostiene esta feature lo decide
 * el motor, no el codigo Java:
 *
 * <ul>
 * <li><b>El catalogo mezcla filas generales y por empresa.</b> El
 * {@code @Query} JPQL con {@code LEFT JOIN} de {@code findAvailableById} y el
 * derivado {@code findAllByGeneralTrueOrCompany_Id} son los que deciden que un
 * tipo general se vea desde cualquier empresa: eso solo lo verifica Hibernate
 * contra datos reales.</li>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b> El
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de todas las consultas de entidad,
 * menos de la nativa de {@code reactivate}.</li>
 * <li><b>El {@code @EntityGraph} en {@code company}</b> es lo que evita el N+1
 * al listar.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaVaccinationTypeRepository — catalogo general/por empresa y soft delete contra MySQL real")
class VaccinationTypePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaVaccinationTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private VaccinationType propia(String nombre, CompanyRef empresa) {
        return new VaccinationType(null, nombre, "Vacuna de prueba", empresa, false, CREADO, null,
                true);
    }

    private VaccinationType general(String nombre) {
        return new VaccinationType(null, nombre, "Vacuna de prueba", null, true, CREADO, null,
                true);
    }

    private VaccinationType guardar(String nombre) {
        return repository.save(propia(nombre, EMPRESA));
    }

    /**
     * Vacia el contexto de persistencia para que la siguiente lectura venga de la
     * base.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        releerDesdeLaBase();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y la empresa hidratada")
        void guardar_y_releer_conserva_cada_campo() {
            VaccinationType guardado = guardar("Rabia");
            releerDesdeLaBase();

            VaccinationType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Rabia");
            assertThat(leido.getDescription()).isEqualTo("Vacuna de prueba");
            assertThat(leido.isGeneral()).isFalse();
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCompany()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("un tipo general se guarda y relee sin compania")
        void un_tipo_general_se_guarda_y_relee_sin_compania() {
            VaccinationType guardado = repository.save(general("Vacuna universal"));
            releerDesdeLaBase();

            VaccinationType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getCompany()).isNull();
            assertThat(leido.isGeneral()).isTrue();
        }
    }

    @Nested
    @DisplayName("disponibilidad por empresa")
    class Disponibilidad {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra un tipo propio de la empresa")
        void find_by_id_and_company_id_encuentra_un_tipo_propio() {
            VaccinationType propio = guardar("Rabia");
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), COMPANY_ID))
                    .map(VaccinationType::getId).contains(propio.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve un tipo propio de otra empresa")
        void find_by_id_and_company_id_no_devuelve_el_de_otra_empresa() {
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findByIdAndCompanyId si devuelve un tipo general aunque sea de otra empresa")
        void find_by_id_and_company_id_devuelve_el_general() {
            VaccinationType comun = repository.save(general("Vacuna universal"));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(comun.getId(), COMPANY_ID))
                    .map(VaccinationType::getId).contains(comun.getId());
        }

        @Test
        @DisplayName("findAllAvailableForCompany trae los generales y los propios, no los ajenos")
        void find_all_available_for_company_trae_generales_y_propios() {
            VaccinationType propio = guardar("Rabia");
            VaccinationType comun = repository.save(general("Vacuna universal"));
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            // Contencion y no igualdad exacta: la migracion 294 siembra 33 tipos
            // GENERALES en el catalogo de plataforma, y esta consulta los devuelve
            // todos por definicion. Lo que se afirma es la regla —los propios y los
            // generales SI, los de otra empresa NO—, no cuantas filas hay sembradas.
            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(VaccinationType::getId).contains(propio.getId(), comun.getId())
                    .doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll trae los tipos de todas las empresas")
        void find_all_trae_los_tipos_de_todas_las_empresas() {
            VaccinationType propio = guardar("Rabia");
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(VaccinationType::getId)
                    .contains(propio.getId(), ajeno.getId());
        }
    }

    @Nested
    @DisplayName("soft delete")
    class SoftDeleteYReactivacion {

        @Test
        @DisplayName("el borrado es logico: el tipo deja de verse por id")
        void el_borrado_es_logico() {
            VaccinationType pausado = guardar("Rabia");

            deshabilitar(pausado.getId());

            assertThat(repository.findById(pausado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la ajena")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye lo que la empresa solo puede
            // consultar: si no, el update le pondria su company_id a la fila ajena.
            VaccinationType propio = guardar("Rabia");
            VaccinationType ajeno = repository.save(propia("Rabia ajena", OTRA_EMPRESA));
            releerDesdeLaBase();

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), COMPANY_ID))
                    .isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }
    }

    /**
     * La guarda de nombre de #559. Todo lo que decide aqui lo decide MySQL: que la
     * fila dada de baja siga siendo visible pese al {@code @SQLRestriction}, y que
     * la igualdad de nombres sea la de la collation y no la de {@code String}.
     */
    @Nested
    @DisplayName("nombre ocupado, incluidas las filas dadas de baja")
    class NombreOcupado {

        @Test
        @DisplayName("ve la fila deshabilitada que el @SQLRestriction esconde a todo lo demas")
        void ve_la_fila_deshabilitada() {
            VaccinationType pausado = guardar("Rabia de prueba");

            deshabilitar(pausado.getId());

            // El contraste es la prueba: por el camino de entidad la fila ya no existe.
            assertThat(repository.findById(pausado.getId())).isEmpty();
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Rabia de prueba",
                    COMPANY_ID)).map(VaccinationType::getId).contains(pausado.getId());
        }

        @Test
        @DisplayName("la collation ignora acentos y caja: «Antirrábica» se encuentra como «antirrabica»")
        void la_collation_ignora_acentos_y_caja() {
            // utf8mb4_0900_ai_ci. Es el mismo criterio con el que el indice unico
            // decide, asi que una guarda que comparase en Java diria «libre» y la base
            // rechazaria el INSERT despues: 409 crudo en vez del mensaje de negocio.
            VaccinationType guardado = guardar("Antirrábica");
            releerDesdeLaBase();

            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled("antirrabica", COMPANY_ID))
                    .map(VaccinationType::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("las dos ramas del adaptador no se cruzan: la de empresa y la global")
        void las_dos_ramas_del_adaptador_no_se_cruzan() {
            // El ternario de JpaVaccinationTypeRepository. El mismo nombre puede
            // convivir en una empresa y en el catalogo de plataforma —owner_scope los
            // separa— y cada rama tiene que devolver el suyo, no el del otro.
            VaccinationType propio = guardar("Doble ambito");
            VaccinationType global = repository.save(general("Doble ambito"));
            releerDesdeLaBase();

            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled("Doble ambito", COMPANY_ID))
                    .map(VaccinationType::getId).contains(propio.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Doble ambito", null))
                    .map(VaccinationType::getId).contains(global.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Doble ambito",
                    OTRA_COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("existsActive deja de contar la fila en cuanto se da de baja: la baja libera el nombre")
        void exists_active_no_cuenta_la_fila_dada_de_baja() {
            VaccinationType ocupante = guardar("Nombre liberado");
            VaccinationType otro = guardar("Otro tipo");
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Nombre liberado",
                    COMPANY_ID, otro.getId())).isTrue();

            deshabilitar(ocupante.getId());

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Nombre liberado",
                    COMPANY_ID, otro.getId())).isFalse();
        }

        @Test
        @DisplayName("al excluir su propio id, una fila no choca consigo misma ni con otra caja")
        void una_fila_no_choca_consigo_misma() {
            VaccinationType propio = guardar("Nombre propio");
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("NOMBRE PROPIO",
                    COMPANY_ID, propio.getId())).isFalse();
        }

        @Test
        @DisplayName("la rama global de existsActive solo cuenta las filas sin empresa")
        void la_rama_global_de_exists_active_solo_cuenta_las_sin_empresa() {
            VaccinationType global = repository.save(general("Global exclusivo"));
            VaccinationType propio = guardar("Global exclusivo");
            releerDesdeLaBase();

            // Excluyendo la fila de la EMPRESA sigue quedando la global: el ambito de
            // plataforma esta ocupado.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Global exclusivo",
                    null, propio.getId())).isTrue();
            // Excluyendo la propia fila global no queda ninguna otra: la fila de la
            // empresa NO cuenta en el ambito de plataforma.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Global exclusivo",
                    null, global.getId())).isFalse();
        }

        @Test
        @DisplayName("con una activa y dos dadas de baja homonimas devuelve la ACTIVA, sin reventar (#580)")
        void con_varias_homonimas_devuelve_la_activa() {
            // El indice unico solo cubre las ACTIVAS —active_name vale NULL cuando
            // enabled = false y MySQL no deduplica NULL—, asi que la tabla admite UNA
            // activa y N dadas de baja con el mismo nombre. Sin ORDER BY + LIMIT 1 la
            // segunda baja homonima convertia esta consulta en un
            // IncorrectResultSizeDataAccessException —un 500— y dejaba ese nombre
            // inutilizable para siempre (#580).
            VaccinationType primera = guardar("Nombre reciclado");
            deshabilitar(primera.getId());
            VaccinationType segunda = guardar("Nombre reciclado");
            deshabilitar(segunda.getId());
            VaccinationType activa = guardar("Nombre reciclado");
            releerDesdeLaBase();

            // La activa primero: es la unica que de verdad ocupa el nombre y la que
            // tiene que hacer saltar el conflicto en vez de una reactivacion.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Nombre reciclado",
                    COMPANY_ID)).map(VaccinationType::getId).contains(activa.getId());
        }

        @Test
        @DisplayName("sin ninguna activa, entre dos dadas de baja homonimas devuelve la de id mayor")
        void entre_dos_de_baja_devuelve_la_mas_reciente() {
            VaccinationType vieja = guardar("Nombre reciclado");
            deshabilitar(vieja.getId());
            VaccinationType reciente = guardar("Nombre reciclado");
            deshabilitar(reciente.getId());

            // La mas reciente es la que el usuario espera recuperar al volver a dar de
            // alta ese nombre.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Nombre reciclado",
                    COMPANY_ID)).map(VaccinationType::getId).contains(reciente.getId());
        }
    }

    @Nested
    @DisplayName("reactivacion por UPDATE nativo")
    class Reactivacion {

        @Test
        @DisplayName("deja enabled = true, aplica la descripcion nueva y sube version")
        void reactivar_deja_la_fila_activa_con_los_datos_nuevos() {
            VaccinationType pausado = guardar("Rabia reactivable");
            releerDesdeLaBase();
            Long versionAntes = repository.findById(pausado.getId()).orElseThrow().getVersion();
            deshabilitar(pausado.getId());

            int filas = repository.reactivateWithDetails(pausado.getId(), COMPANY_ID,
                    "Rabia reactivable", "Descripcion nueva");
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            VaccinationType reactivado = repository.findById(pausado.getId()).orElseThrow();
            assertThat(reactivado.isEnabled()).isTrue();
            assertThat(reactivado.getDescription()).isEqualTo("Descripcion nueva");
            // La version SUBE a proposito: una nativa ni la comprueba ni la incrementa,
            // asi que un save cargado antes reescribiria la fila con su enabled = false
            // y su WHERE version = ? casaria igual, deshaciendo la reactivacion en
            // silencio. Movida, ese save no encuentra fila y salta el 409.
            assertThat(reactivado.getVersion()).isGreaterThan(versionAntes);
        }

        @Test
        @DisplayName("cada rama alcanza solo su ambito: la de empresa no resucita la global ni al reves")
        void cada_rama_alcanza_solo_su_ambito() {
            VaccinationType propio = guardar("Ambito acotado");
            VaccinationType global = repository.save(general("Ambito acotado"));
            releerDesdeLaBase();
            deshabilitar(propio.getId());
            deshabilitar(global.getId());

            // Cruzadas: cero filas afectadas. Sin el `company_id IS NULL` del WHERE, el
            // camino de plataforma resucitaria la fila privada de un tenant.
            assertThat(repository.reactivateWithDetails(propio.getId(), null, "Ambito acotado",
                    "Cruzada")).isZero();
            assertThat(repository.reactivateWithDetails(global.getId(), COMPANY_ID,
                    "Ambito acotado", "Cruzada")).isZero();

            // Cada una en su ambito: una fila.
            assertThat(repository.reactivateWithDetails(propio.getId(), COMPANY_ID,
                    "Ambito acotado", "De la empresa")).isEqualTo(1);
            assertThat(repository.reactivateWithDetails(global.getId(), null, "Ambito acotado",
                    "De plataforma")).isEqualTo(1);
        }
    }
}
