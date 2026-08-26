package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
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
 * Rodaja de persistencia de tipos de examen de laboratorio contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b>
 *
 * <ul>
 * <li><b>El {@code @SQLDelete} + {@code @SQLRestriction}</b> convierten el
 * borrado en {@code UPDATE enabled = false} y esconden la fila del resto de
 * consultas de entidad; un repositorio en memoria no reproduce ese
 * comportamiento.</li>
 * <li><b>El {@code @EntityGraph} en {@code company}</b> es lo que evita el N+1
 * al listar y al buscar, y solo se ve pasando por Hibernate.</li>
 * <li><b>{@code findAllByGeneralTrueOrCompany_Id}</b> mezcla dos condiciones en
 * el WHERE (tipos generales + tipos propios de la empresa): es SQL real, no
 * logica Java.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaLaboratoryTestTypeRepository — disponibilidad general/propia y soft delete contra MySQL real")
class LaboratoryTestTypePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    /**
     * Los nombres GLOBALES de esta rodaja llevan sufijo de prueba a proposito. La
     * semilla {@code 296_seed_laboratory_test_types_catalog} deja 87 tipos de
     * plataforma en la base del contenedor y el indice
     * {@code uq_laboratory_test_types_owner_active_name} es unico sobre
     * {@code (COALESCE(company_id, 0), name)} de las filas ACTIVAS: un nombre
     * canonico —«Perfil renal» esta sembrado tal cual— revienta el INSERT del
     * fixture antes de llegar a la asercion. Los nombres propios de empresa no
     * tienen ese problema: caen en otro {@code owner_scope}.
     */
    private static final String NOMBRE_GENERAL = "Perfil renal de prueba";

    /** Nombre global libre para los casos de ocupacion y reactivacion. */
    private static final String OTRO_NOMBRE_GENERAL = "Panel metabólico de prueba";

    /**
     * Nombre PROPIO de empresa, y tambien sintetico, pero por un motivo DISTINTO
     * del de {@link #NOMBRE_GENERAL}. Aqui el INSERT nunca choca: la fila cae en
     * {@code owner_scope = COMPANY_ID} y la semilla 296 siembra en el global. Lo
     * que se rompe es la ASERCION de que el ambito global esta VACIO para ese
     * nombre: con «Perfil hepatico» el finder global devolvia la fila SEMBRADA
     * —«Perfil hepático», el mismo nombre bajo {@code utf8mb4_0900_ai_ci}— y el
     * {@code isEmpty()} fallaba por una razon que no tiene nada que ver con lo que
     * el caso quiere probar.
     *
     * <p>
     * La regla, para el proximo fixture: hay que buscar nombre sintetico solo si se
     * guarda una fila GLOBAL, o si se afirma que el ambito GLOBAL esta vacio. Un
     * nombre propio de empresa que no aparece en ninguna asercion global puede ser
     * el real.
     */
    private static final String NOMBRE_PROPIO = "Perfil hepatico de prueba";

    @Autowired
    private JpaLaboratoryTestTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private LaboratoryTestType propio(String nombre, CompanyRef empresa) {
        return new LaboratoryTestType(null, nombre, "Tipo de prueba", empresa, false, CREADO, null,
                true);
    }

    private LaboratoryTestType general(String nombre) {
        return new LaboratoryTestType(null, nombre, "Tipo general de prueba", null, true, CREADO,
                null, true);
    }

    private LaboratoryTestType guardarPropio(String nombre) {
        return repository.save(propio(nombre, EMPRESA));
    }

    private LaboratoryTestType guardarGeneral(String nombre) {
        return repository.save(general(nombre));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        sincronizar();
    }

    /**
     * Vacia el contexto de persistencia. Importa para los finders NATIVOS: si la
     * fila sigue gestionada en la sesion, Hibernate devuelve esa instancia y la
     * asercion no mira lo que de verdad hay en la base.
     */
    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer un tipo propio conserva cada campo y la empresa hidratada")
        void guardar_y_releer_un_tipo_propio_conserva_cada_campo() {
            LaboratoryTestType guardado = guardarPropio("Hemograma");

            LaboratoryTestType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Hemograma");
            assertThat(leido.getDescription()).isEqualTo("Tipo de prueba");
            assertThat(leido.isGeneral()).isFalse();
            assertThat(leido.isEnabled()).isTrue();
            // El CompanyRef no se guarda: se reconstruye desde el @ManyToOne al leer, y
            // sus invariantes rechazan un nombre o un identificador vacios.
            assertThat(leido.getCompany())
                    .isEqualTo(new CompanyRef(COMPANY_ID, "Veterinaria de prueba", "900123456"));
        }

        @Test
        @DisplayName("guardar y releer un tipo general conserva company nula")
        void guardar_y_releer_un_tipo_general_conserva_company_nula() {
            LaboratoryTestType guardado = guardarGeneral(NOMBRE_GENERAL);

            LaboratoryTestType leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.isGeneral()).isTrue();
            assertThat(leido.getCompany()).isNull();
        }
    }

    @Nested
    @DisplayName("disponibilidad por empresa")
    class Disponibilidad {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra un tipo general aunque la empresa sea otra")
        void find_by_id_and_company_id_encuentra_un_tipo_general() {
            LaboratoryTestType general = guardarGeneral(NOMBRE_GENERAL);

            assertThat(repository.findByIdAndCompanyId(general.getId(), OTRA_COMPANY_ID))
                    .map(LaboratoryTestType::getId).contains(general.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve el tipo propio de otra empresa")
        void find_by_id_and_company_id_no_devuelve_el_de_otra_empresa() {
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findAllAvailableForCompany mezcla los generales con los propios, sin los ajenos")
        void find_all_available_mezcla_generales_y_propios() {
            LaboratoryTestType general = guardarGeneral(NOMBRE_GENERAL);
            LaboratoryTestType propio = guardarPropio("Hemograma");
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).contains(general.getId(), propio.getId())
                    .doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("soft delete")
    class SoftDelete {

        @Test
        @DisplayName("el listado disponible deja de ver el tipo deshabilitado")
        void el_listado_disponible_no_ve_el_deshabilitado() {
            LaboratoryTestType pausado = guardarPropio("Hemograma");

            deshabilitar(pausado.getId());

            assertThat(repository.findAllAvailableForCompany(COMPANY_ID))
                    .extracting(LaboratoryTestType::getId).doesNotContain(pausado.getId());
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la general ni la ajena")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye las generales: si las incluyera,
            // el update les pondria el company_id del llamador.
            LaboratoryTestType propio = guardarPropio("Hemograma");
            LaboratoryTestType general = guardarGeneral(NOMBRE_GENERAL);
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), COMPANY_ID))
                    .isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(general.getId(), COMPANY_ID)).isEmpty();
            assertThat(repository.findOwnedByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
            // La general sigue siendo legible por el finder de disponibles.
            assertThat(repository.findByIdAndCompanyId(general.getId(), COMPANY_ID)).isPresent();
        }
    }

    @Nested
    @DisplayName("nombre ocupado dentro del ambito")
    class NombreOcupado {

        @Test
        @DisplayName("findByNameAndCompanyIdIncludingDisabled VE la fila deshabilitada que el resto de consultas esconde")
        void ve_la_fila_deshabilitada_que_el_sql_restriction_esconde() {
            // Es la razon de ser de la consulta nativa: el @SQLRestriction("enabled =
            // true") oculta la fila a findById, asi que el alta creia el nombre libre,
            // insertaba y chocaba contra el indice unico con un 409 sin campo (#559).
            LaboratoryTestType pausado = guardarPropio("Hemograma");
            deshabilitar(pausado.getId());

            assertThat(repository.findById(pausado.getId())).isEmpty();
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma", COMPANY_ID))
                    .get().satisfies(fila -> {
                        assertThat(fila.getId()).isEqualTo(pausado.getId());
                        assertThat(fila.isEnabled()).isFalse();
                    });
        }

        @Test
        @DisplayName("la comparacion de nombre es insensible a acentos y a caja, igual que el indice unico")
        void la_comparacion_de_nombre_es_insensible_a_acentos_y_caja() {
            // La collation de la columna es utf8mb4_0900_ai_ci y es la misma con la que
            // decide el indice unico. Comparar en Java daria «libre» a «Coprologico» y la
            // base rechazaria el INSERT despues, con el mensaje generico que #559 vino a
            // quitar.
            LaboratoryTestType guardado = guardarPropio("Coprológico");

            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled("coprologico", COMPANY_ID))
                    .map(LaboratoryTestType::getId).contains(guardado.getId());
            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled("COPROLOGICO", COMPANY_ID))
                    .map(LaboratoryTestType::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("los dos ambitos no se ven entre si: la rama global no alcanza la fila de una empresa")
        void los_dos_ambitos_no_se_ven_entre_si() {
            // Es la rama companyId == null del adaptador. Va aparte y no con un parametro
            // nulable porque "= NULL" nunca casa en SQL: con un unico finder el catalogo
            // de plataforma se quedaria sin guarda en silencio.
            LaboratoryTestType propioDeLaEmpresa = guardarPropio(NOMBRE_PROPIO);
            LaboratoryTestType delCatalogo = guardarGeneral(OTRO_NOMBRE_GENERAL);

            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled(OTRO_NOMBRE_GENERAL, null))
                    .map(LaboratoryTestType::getId).contains(delCatalogo.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(OTRO_NOMBRE_GENERAL,
                    COMPANY_ID)).isEmpty();
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(NOMBRE_PROPIO, null))
                    .isEmpty();
            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled(NOMBRE_PROPIO, COMPANY_ID))
                    .map(LaboratoryTestType::getId).contains(propioDeLaEmpresa.getId());
        }

        @Test
        @DisplayName("existsActiveByNameAndCompanyIdExcludingId cuenta solo las ACTIVAS y se salta la fila que se edita")
        void exists_active_cuenta_solo_las_activas_y_excluye_la_fila_editada() {
            LaboratoryTestType ocupante = guardarPropio("Hemograma");
            LaboratoryTestType laQueSeEdita = guardarPropio("Coprológico");

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Hemograma", COMPANY_ID,
                    laQueSeEdita.getId())).isTrue();
            // Conservar el propio nombre no es un choque contra uno mismo.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Hemograma", COMPANY_ID,
                    ocupante.getId())).isFalse();

            deshabilitar(ocupante.getId());

            // Dada de baja, la fila LIBERA el nombre: el indice unico solo cubre activas.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Hemograma", COMPANY_ID,
                    laQueSeEdita.getId())).isFalse();
        }

        @Test
        @DisplayName("existsActiveByNameAndCompanyIdExcludingId separa el catalogo de plataforma de la empresa")
        void exists_active_separa_el_catalogo_de_plataforma_de_la_empresa() {
            LaboratoryTestType delCatalogo = guardarGeneral(OTRO_NOMBRE_GENERAL);
            LaboratoryTestType laQueSeEdita = guardarPropio("Coprológico");

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(OTRO_NOMBRE_GENERAL,
                    null, laQueSeEdita.getId())).isTrue();
            // El mismo nombre esta libre DENTRO de la empresa: los ambitos no compiten.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(OTRO_NOMBRE_GENERAL,
                    COMPANY_ID, laQueSeEdita.getId())).isFalse();
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(OTRO_NOMBRE_GENERAL,
                    null, delCatalogo.getId())).isFalse();
        }

        @Test
        @DisplayName("con una fila activa y dos homonimas de baja devuelve la ACTIVA, sin reventar por resultado multiple")
        void con_una_activa_y_dos_de_baja_devuelve_la_activa() {
            // El indice unico cubre solo las ACTIVAS: active_name vale NULL cuando
            // enabled = false y MySQL no deduplica NULL, asi que la tabla admite UNA
            // activa y N de baja con el mismo nombre. Sin ORDER BY + LIMIT 1 la segunda
            // baja homonima convertia el finder en IncorrectResultSizeDataAccessException
            // -un 500- y dejaba ese nombre inutilizable para siempre (#580).
            LaboratoryTestType primeraBaja = guardarPropio("Hemograma");
            deshabilitar(primeraBaja.getId());
            LaboratoryTestType segundaBaja = guardarPropio("Hemograma");
            deshabilitar(segundaBaja.getId());
            LaboratoryTestType activa = guardarPropio("Hemograma");
            sincronizar();

            // La activa primero porque es la unica que de verdad OCUPA el nombre: es la
            // que tiene que hacer saltar el conflicto en vez de una reactivacion.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma", COMPANY_ID))
                    .get().satisfies(fila -> {
                        assertThat(fila.getId()).isEqualTo(activa.getId());
                        assertThat(fila.isEnabled()).isTrue();
                    });
        }

        @Test
        @DisplayName("sin ninguna activa, entre dos homonimas de baja devuelve la de id mayor")
        void sin_activa_entre_dos_de_baja_devuelve_la_de_id_mayor() {
            LaboratoryTestType antigua = guardarPropio("Hemograma");
            deshabilitar(antigua.getId());
            LaboratoryTestType reciente = guardarPropio("Hemograma");
            deshabilitar(reciente.getId());

            assertThat(reciente.getId()).isGreaterThan(antigua.getId());
            // La mas reciente es la que la usuaria espera recuperar al volver a dar de
            // alta ese nombre: es la que llevaba sus ultimos datos.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma", COMPANY_ID))
                    .get().satisfies(fila -> {
                        assertThat(fila.getId()).isEqualTo(reciente.getId());
                        assertThat(fila.isEnabled()).isFalse();
                    });
        }
    }

    @Nested
    @DisplayName("reactivacion con detalles")
    class Reactivacion {

        @Test
        @DisplayName("reactivateWithDetails deja la fila de la empresa activa, con la descripcion nueva y la version subida")
        void reactivate_with_details_reactiva_la_fila_de_la_empresa() {
            LaboratoryTestType pausado = guardarPropio("Hemograma");
            deshabilitar(pausado.getId());
            LaboratoryTestType antes = repository
                    .findByNameAndCompanyIdIncludingDisabled("Hemograma", COMPANY_ID).orElseThrow();

            int filas = repository.reactivateWithDetails(pausado.getId(), COMPANY_ID, "Hemograma",
                    "Hemograma con recuento de plaquetas");

            assertThat(filas).isEqualTo(1);
            LaboratoryTestType despues = repository.findById(pausado.getId()).orElseThrow();
            assertThat(despues.isEnabled()).isTrue();
            assertThat(despues.getDescription()).isEqualTo("Hemograma con recuento de plaquetas");
            // La version sube a mano porque una consulta nativa ni la comprueba ni la
            // incrementa: sin eso, un save cargado antes reescribiria la fila con su
            // enabled = false y deshaceria la reactivacion en silencio.
            assertThat(despues.getVersion()).isGreaterThan(antes.getVersion());
        }

        @Test
        @DisplayName("reactivateWithDetails con companyId nulo solo alcanza el catalogo de plataforma")
        void reactivate_with_details_con_company_id_nulo_solo_alcanza_el_catalogo() {
            LaboratoryTestType delCatalogo = guardarGeneral(OTRO_NOMBRE_GENERAL);
            LaboratoryTestType deLaEmpresa = guardarPropio("Hemograma");
            deshabilitar(delCatalogo.getId());
            deshabilitar(deLaEmpresa.getId());

            int filasDelCatalogo = repository.reactivateWithDetails(delCatalogo.getId(), null,
                    OTRO_NOMBRE_GENERAL, "Panel metabólico ampliado");
            // El WHERE nombra company_id IS NULL: la fila privada de un tenant queda
            // fuera del alcance de este camino aunque le pasen su id.
            int filasDeLaEmpresa = repository.reactivateWithDetails(deLaEmpresa.getId(), null,
                    "Hemograma", "Robado");

            assertThat(filasDelCatalogo).isEqualTo(1);
            assertThat(filasDeLaEmpresa).isZero();
            assertThat(repository.findById(delCatalogo.getId())).get()
                    .satisfies(fila -> assertThat(fila.getDescription())
                            .isEqualTo("Panel metabólico ampliado"));
            assertThat(repository.findById(deLaEmpresa.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("findAll incluye tipos propios y generales de todas las empresas")
        void find_all_incluye_propios_y_generales_de_todas_las_empresas() {
            LaboratoryTestType general = guardarGeneral(NOMBRE_GENERAL);
            LaboratoryTestType ajeno = repository.save(propio("Coprologico", OTRA_EMPRESA));

            assertThat(repository.findAll()).extracting(LaboratoryTestType::getId)
                    .contains(general.getId(), ajeno.getId());
        }
    }
}
