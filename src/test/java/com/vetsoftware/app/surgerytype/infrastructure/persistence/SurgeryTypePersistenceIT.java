package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del tipo de cirugia contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> El
 * {@code @SQLDelete}/{@code @SQLRestriction} que convierte el borrado en un
 * {@code enabled = false} invisible, la reactivacion por UPDATE nativo, y la
 * consulta {@code findAvailableById} que mezcla filas generales y de la empresa
 * con un {@code LEFT JOIN} las verifica el motor, no el mapper.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSurgeryTypeRepository — tipos de cirugia contra MySQL real")
class SurgeryTypePersistenceIT extends AbstractDataJpaTest {

    private static final CompanyRef EMPRESA = new CompanyRef(SchemaSeed.COMPANY_ID,
            "Veterinaria de prueba", "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(SchemaSeed.OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    @Autowired
    private JpaSurgeryTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private SurgeryType guardarPropio(String name) {
        return repository.save(SurgeryType.create(name, "desc", EMPRESA, false));
    }

    private SurgeryType guardarGeneral(String name) {
        return repository.save(SurgeryType.create(name, "desc", null, true));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        releerDesdeLaBase();
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer un tipo propio conserva cada campo, incluida la empresa")
        void guardar_y_releer_un_tipo_propio_conserva_cada_campo() {
            SurgeryType guardado = guardarPropio("Castracion");
            releerDesdeLaBase();

            SurgeryType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Castracion");
            assertThat(leido.getDescription()).isEqualTo("desc");
            assertThat(leido.getCompany()).isEqualTo(EMPRESA);
            assertThat(leido.isGeneral()).isFalse();
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("guardar y releer un tipo general no trae empresa")
        void guardar_y_releer_un_tipo_general_no_trae_empresa() {
            SurgeryType guardado = guardarGeneral("Cirugia general");
            releerDesdeLaBase();

            SurgeryType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getCompany()).isNull();
            assertThat(leido.isGeneral()).isTrue();
        }

        @Test
        @DisplayName("el save devuelve el id generado")
        void el_save_devuelve_el_id_generado() {
            SurgeryType guardado = guardarPropio("Castracion");

            assertThat(guardado.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("disponibilidad para la empresa")
    class DisponibilidadParaLaEmpresa {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra un tipo general aunque sea de otra empresa")
        void find_by_id_and_company_id_encuentra_un_tipo_general() {
            SurgeryType general = guardarGeneral("Cirugia general");
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(general.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .map(SurgeryType::getId).contains(general.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no encuentra un tipo propio de otra empresa")
        void find_by_id_and_company_id_no_encuentra_un_tipo_ajeno() {
            SurgeryType propio = guardarPropio("Castracion");
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("findAllAvailableForCompany mezcla los generales con los propios, sin los ajenos")
        void find_all_available_mezcla_generales_y_propios() {
            SurgeryType propio = guardarPropio("Castracion");
            SurgeryType general = guardarGeneral("Cirugia general");
            SurgeryType ajeno = repository
                    .save(SurgeryType.create("Ajeno", "desc", OTRA_EMPRESA, false));
            releerDesdeLaBase();

            // Contencion y no igualdad exacta: la migracion 295 siembra 81 tipos
            // GENERALES en el catalogo de plataforma, y esta consulta los devuelve
            // todos por definicion. Lo que se afirma es la regla —los propios y los
            // generales SI, los de otra empresa NO—, no cuantas filas hay sembradas.
            assertThat(repository.findAllAvailableForCompany(SchemaSeed.COMPANY_ID))
                    .extracting(SurgeryType::getId).contains(propio.getId(), general.getId())
                    .doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll trae tipos de todas las empresas — es el listado SYSTEM")
        void find_all_trae_tipos_de_todas_las_empresas() {
            SurgeryType propio = guardarPropio("Castracion");
            SurgeryType ajeno = repository
                    .save(SurgeryType.create("Ajeno", "desc", OTRA_EMPRESA, false));
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(SurgeryType::getId).contains(propio.getId(),
                    ajeno.getId());
        }
    }

    @Nested
    @DisplayName("borrado")
    class BorradoYReactivacion {

        @Test
        @DisplayName("el borrado es logico: la fila deja de verse por id")
        void el_borrado_es_logico() {
            SurgeryType borrado = guardarPropio("Castracion");
            releerDesdeLaBase();

            repository.delete(borrado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(borrado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la general ni la ajena")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye las generales: si las incluyera,
            // el update les pondria el company_id del llamador.
            SurgeryType propio = guardarPropio("Castracion");
            SurgeryType general = guardarGeneral("Cirugia general");
            SurgeryType ajeno = repository
                    .save(SurgeryType.create("Ajeno", "desc", OTRA_EMPRESA, false));
            releerDesdeLaBase();

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(general.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findOwnedByIdAndCompanyId(ajeno.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            // La general sigue siendo legible por el finder de disponibles.
            assertThat(repository.findByIdAndCompanyId(general.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
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
            SurgeryType pausado = guardarPropio("Castracion de prueba");
            releerDesdeLaBase();

            deshabilitar(pausado.getId());

            // El contraste es la prueba: por el camino de entidad la fila ya no existe.
            assertThat(repository.findById(pausado.getId())).isEmpty();
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Castracion de prueba",
                    SchemaSeed.COMPANY_ID)).map(SurgeryType::getId).contains(pausado.getId());
        }

        @Test
        @DisplayName("la collation ignora acentos y caja: «Ovariohisterectomía» se encuentra como «ovariohisterectomia»")
        void la_collation_ignora_acentos_y_caja() {
            // utf8mb4_0900_ai_ci. Es el mismo criterio con el que el indice unico
            // decide, asi que una guarda que comparase en Java diria «libre» y la base
            // rechazaria el INSERT despues: 409 crudo en vez del mensaje de negocio.
            SurgeryType guardado = guardarPropio("Ovariohisterectomía");
            releerDesdeLaBase();

            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("ovariohisterectomia",
                    SchemaSeed.COMPANY_ID)).map(SurgeryType::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("las dos ramas del adaptador no se cruzan: la de empresa y la global")
        void las_dos_ramas_del_adaptador_no_se_cruzan() {
            // El ternario de JpaSurgeryTypeRepository. El mismo nombre puede convivir en
            // una empresa y en el catalogo de plataforma —owner_scope los separa— y cada
            // rama tiene que devolver el suyo, no el del otro.
            SurgeryType propio = guardarPropio("Doble ambito");
            SurgeryType global = guardarGeneral("Doble ambito");
            releerDesdeLaBase();

            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Doble ambito",
                    SchemaSeed.COMPANY_ID)).map(SurgeryType::getId).contains(propio.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Doble ambito", null))
                    .map(SurgeryType::getId).contains(global.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Doble ambito",
                    SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("existsActive deja de contar la fila en cuanto se da de baja: la baja libera el nombre")
        void exists_active_no_cuenta_la_fila_dada_de_baja() {
            SurgeryType ocupante = guardarPropio("Nombre liberado");
            SurgeryType otro = guardarPropio("Otro tipo");
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Nombre liberado",
                    SchemaSeed.COMPANY_ID, otro.getId())).isTrue();

            deshabilitar(ocupante.getId());

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Nombre liberado",
                    SchemaSeed.COMPANY_ID, otro.getId())).isFalse();
        }

        @Test
        @DisplayName("al excluir su propio id, una fila no choca consigo misma ni con otra caja")
        void una_fila_no_choca_consigo_misma() {
            SurgeryType propio = guardarPropio("Nombre propio");
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("NOMBRE PROPIO",
                    SchemaSeed.COMPANY_ID, propio.getId())).isFalse();
        }

        @Test
        @DisplayName("la rama global de existsActive solo cuenta las filas sin empresa")
        void la_rama_global_de_exists_active_solo_cuenta_las_sin_empresa() {
            SurgeryType global = guardarGeneral("Global exclusivo");
            SurgeryType propio = guardarPropio("Global exclusivo");
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
            SurgeryType primera = guardarPropio("Nombre reciclado");
            deshabilitar(primera.getId());
            SurgeryType segunda = guardarPropio("Nombre reciclado");
            deshabilitar(segunda.getId());
            SurgeryType activa = guardarPropio("Nombre reciclado");
            releerDesdeLaBase();

            // La activa primero: es la unica que de verdad ocupa el nombre y la que
            // tiene que hacer saltar el conflicto en vez de una reactivacion.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Nombre reciclado",
                    SchemaSeed.COMPANY_ID)).map(SurgeryType::getId).contains(activa.getId());
        }

        @Test
        @DisplayName("sin ninguna activa, entre dos dadas de baja homonimas devuelve la de id mayor")
        void entre_dos_de_baja_devuelve_la_mas_reciente() {
            SurgeryType vieja = guardarPropio("Nombre reciclado");
            deshabilitar(vieja.getId());
            SurgeryType reciente = guardarPropio("Nombre reciclado");
            deshabilitar(reciente.getId());

            // La mas reciente es la que el usuario espera recuperar al volver a dar de
            // alta ese nombre.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Nombre reciclado",
                    SchemaSeed.COMPANY_ID)).map(SurgeryType::getId).contains(reciente.getId());
        }
    }

    @Nested
    @DisplayName("reactivacion por UPDATE nativo")
    class Reactivacion {

        @Test
        @DisplayName("deja enabled = true, aplica la descripcion nueva y sube version")
        void reactivar_deja_la_fila_activa_con_los_datos_nuevos() {
            SurgeryType pausado = guardarPropio("Castracion reactivable");
            releerDesdeLaBase();
            Long versionAntes = repository.findById(pausado.getId()).orElseThrow().getVersion();
            deshabilitar(pausado.getId());

            int filas = repository.reactivateWithDetails(pausado.getId(), SchemaSeed.COMPANY_ID,
                    "Castracion reactivable", "Descripcion nueva");
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            SurgeryType reactivado = repository.findById(pausado.getId()).orElseThrow();
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
            SurgeryType propio = guardarPropio("Ambito acotado");
            SurgeryType global = guardarGeneral("Ambito acotado");
            releerDesdeLaBase();
            deshabilitar(propio.getId());
            deshabilitar(global.getId());

            // Cruzadas: cero filas afectadas. Sin el `company_id IS NULL` del WHERE, el
            // camino de plataforma resucitaria la fila privada de un tenant.
            assertThat(repository.reactivateWithDetails(propio.getId(), null, "Ambito acotado",
                    "Cruzada")).isZero();
            assertThat(repository.reactivateWithDetails(global.getId(), SchemaSeed.COMPANY_ID,
                    "Ambito acotado", "Cruzada")).isZero();

            // Cada una en su ambito: una fila.
            assertThat(repository.reactivateWithDetails(propio.getId(), SchemaSeed.COMPANY_ID,
                    "Ambito acotado", "De la empresa")).isEqualTo(1);
            assertThat(repository.reactivateWithDetails(global.getId(), null, "Ambito acotado",
                    "De plataforma")).isEqualTo(1);
        }
    }
}
