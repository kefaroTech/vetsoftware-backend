package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
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
 * Rodaja de persistencia de {@link JpaDiagnosticImagingTypeRepository} contra
 * MySQL real: ejercita el {@code getReferenceById} de la company, el
 * {@code @SQLRestriction} de soft-delete, {@code reactivate} (UPDATE nativo) y
 * {@code findAvailableById}/{@code findAllByGeneralTrueOrCompany_Id}, que
 * mezclan filas globales y privadas por empresa — nada de eso lo ve un test en
 * memoria.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDiagnosticImagingTypeRepository — catalogo de tipos de imagen diagnostica contra MySQL real")
class DiagnosticImagingTypePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaDiagnosticImagingTypeRepository repository;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private CompanyJpaEntity company;

    @BeforeEach
    void sembrarLaEmpresa() {
        SchemaSeed.seed(entityManager);
        company = companyJpaRepository.getReferenceById(COMPANY);
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
        company = companyJpaRepository.getReferenceById(COMPANY);
    }

    private DiagnosticImagingType tipoGeneralValido() {
        return DiagnosticImagingType.create("Radiografia", "Radiografia simple digital", null,
                true);
    }

    private DiagnosticImagingType tipoDeEmpresaValido() {
        return DiagnosticImagingType.create("Ecografia abdominal", "Ecografia de rutina",
                new CompanyRef(company.getId(), company.getName(), company.getIdentifier()), false);
    }

    /**
     * Nombre GLOBAL sintetico a proposito. La semilla
     * {@code 297_seed_diagnostic_imaging_types_catalog} deja 21 tipos de plataforma
     * en la base del contenedor y el indice
     * {@code uq_diagnostic_imaging_types_owner_active_name} es unico sobre
     * {@code (COALESCE(company_id, 0), name)} de las filas ACTIVAS: un nombre
     * canonico del catalogo revienta el INSERT del fixture antes de llegar a la
     * asercion. Los nombres propios de empresa caen en otro {@code owner_scope} y
     * no tienen ese problema.
     */
    private static final String NOMBRE_GLOBAL = "Estudio global de prueba";

    private DiagnosticImagingType estudioGlobalDePrueba() {
        return DiagnosticImagingType.create(NOMBRE_GLOBAL, "Estudio global de prueba", null, true);
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste un tipo general (sin company) y devuelve el id asignado")
        void persiste_un_tipo_general_sin_company() {
            DiagnosticImagingType guardado = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            assertThat(guardado.getId()).isNotNull();
            DiagnosticImagingType releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.getCompany()).isNull();
            assertThat(releido.isGeneral()).isTrue();
        }

        @Test
        @DisplayName("persiste un tipo propio de empresa con la asociacion resuelta por getReferenceById")
        void persiste_un_tipo_propio_de_empresa() {
            DiagnosticImagingType guardado = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();

            DiagnosticImagingType releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.getCompany().id()).isEqualTo(COMPANY);
            assertThat(releido.isGeneral()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId / findAvailableById")
    class BusquedaDisponible {

        @Test
        @DisplayName("un tipo general esta disponible para cualquier empresa")
        void un_tipo_general_esta_disponible_para_cualquier_empresa() {
            DiagnosticImagingType guardado = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("un tipo propio de otra empresa no se entrega")
        void un_tipo_propio_de_otra_empresa_no_se_entrega() {
            DiagnosticImagingType guardado = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("findAllAvailableForCompany")
    class ListadoDisponible {

        @Test
        @DisplayName("mezcla los tipos generales con los propios de la empresa, sin los de otras")
        void mezcla_generales_con_los_propios_sin_los_de_otras_empresas() {
            DiagnosticImagingType general = repository.save(tipoGeneralValido());
            releerDesdeLaBase();
            DiagnosticImagingType propio = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            DiagnosticImagingType ajeno = repository
                    .save(DiagnosticImagingType.create("Tomografia", "desc",
                            new CompanyRef(OTRA_COMPANY,
                                    companyJpaRepository.getReferenceById(OTRA_COMPANY).getName(),
                                    "900654321"),
                            false));
            releerDesdeLaBase();

            var disponibles = repository.findAllAvailableForCompany(COMPANY);

            assertThat(disponibles).extracting(DiagnosticImagingType::getId)
                    .contains(general.getId(), propio.getId()).doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("delete")
    class BorradoYReactivacion {

        @Test
        @DisplayName("un tipo borrado desaparece de findById (SQLRestriction)")
        void tipo_borrado_desaparece() {
            DiagnosticImagingType guardado = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            repository.delete(guardado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findOwnedByIdAndCompanyId ve la fila propia pero NO la general")
        void find_owned_solo_ve_la_fila_propia() {
            // Es el finder de los caminos de ESCRITURA. A diferencia de
            // findByIdAndCompanyId (disponibles), excluye las generales: si las incluyera,
            // el update les pondria el company_id del llamador.
            DiagnosticImagingType propio = repository.save(tipoDeEmpresaValido());
            DiagnosticImagingType general = repository.save(tipoGeneralValido());
            releerDesdeLaBase();

            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), COMPANY)).isPresent();
            assertThat(repository.findOwnedByIdAndCompanyId(general.getId(), COMPANY)).isEmpty();
            assertThat(repository.findOwnedByIdAndCompanyId(propio.getId(), OTRA_COMPANY))
                    .isEmpty();
            // La general sigue siendo legible por el finder de disponibles.
            assertThat(repository.findByIdAndCompanyId(general.getId(), COMPANY)).isPresent();
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
            DiagnosticImagingType pausado = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(pausado.getId())).isEmpty();
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal",
                    COMPANY)).get().satisfies(fila -> {
                        assertThat(fila.getId()).isEqualTo(pausado.getId());
                        assertThat(fila.isEnabled()).isFalse();
                    });
        }

        @Test
        @DisplayName("la comparacion de nombre es insensible a acentos y a caja, igual que el indice unico")
        void la_comparacion_de_nombre_es_insensible_a_acentos_y_caja() {
            // La collation de la columna es utf8mb4_0900_ai_ci y es la misma con la que
            // decide el indice unico. Comparar en Java daria «libre» a «Radiografia» y la
            // base rechazaria el INSERT despues, con el mensaje generico que #559 vino a
            // quitar.
            DiagnosticImagingType guardado = repository.save(DiagnosticImagingType.create(
                    "Ecografía torácica", "Estudio de torax",
                    new CompanyRef(company.getId(), company.getName(), company.getIdentifier()),
                    false));
            releerDesdeLaBase();

            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("ecografia toracica",
                    COMPANY)).map(DiagnosticImagingType::getId).contains(guardado.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("ECOGRAFIA TORACICA",
                    COMPANY)).map(DiagnosticImagingType::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("los dos ambitos no se ven entre si: la rama global no alcanza la fila de una empresa")
        void los_dos_ambitos_no_se_ven_entre_si() {
            // Es la rama companyId == null del adaptador. Va aparte y no con un parametro
            // nulable porque "= NULL" nunca casa en SQL: con un unico finder el catalogo
            // de plataforma se quedaria sin guarda en silencio.
            DiagnosticImagingType propioDeLaEmpresa = repository.save(tipoDeEmpresaValido());
            DiagnosticImagingType delCatalogo = repository.save(estudioGlobalDePrueba());
            releerDesdeLaBase();

            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(NOMBRE_GLOBAL, null))
                    .map(DiagnosticImagingType::getId).contains(delCatalogo.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(NOMBRE_GLOBAL, COMPANY))
                    .isEmpty();
            assertThat(
                    repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal", null))
                    .isEmpty();
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal",
                    COMPANY)).map(DiagnosticImagingType::getId).contains(propioDeLaEmpresa.getId());
        }

        @Test
        @DisplayName("existsActiveByNameAndCompanyIdExcludingId cuenta solo las ACTIVAS y se salta la fila que se edita")
        void exists_active_cuenta_solo_las_activas_y_excluye_la_fila_editada() {
            DiagnosticImagingType ocupante = repository.save(tipoDeEmpresaValido());
            DiagnosticImagingType laQueSeEdita = repository.save(DiagnosticImagingType.create(
                    "Tomografia", "Estudio de prueba",
                    new CompanyRef(company.getId(), company.getName(), company.getIdentifier()),
                    false));
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Ecografia abdominal",
                    COMPANY, laQueSeEdita.getId())).isTrue();
            // Conservar el propio nombre no es un choque contra uno mismo.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Ecografia abdominal",
                    COMPANY, ocupante.getId())).isFalse();

            repository.delete(ocupante.getId());
            releerDesdeLaBase();

            // Dada de baja, la fila LIBERA el nombre: el indice unico solo cubre activas.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Ecografia abdominal",
                    COMPANY, laQueSeEdita.getId())).isFalse();
        }

        @Test
        @DisplayName("existsActiveByNameAndCompanyIdExcludingId separa el catalogo de plataforma de la empresa")
        void exists_active_separa_el_catalogo_de_plataforma_de_la_empresa() {
            DiagnosticImagingType delCatalogo = repository.save(estudioGlobalDePrueba());
            DiagnosticImagingType laQueSeEdita = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(NOMBRE_GLOBAL, null,
                    laQueSeEdita.getId())).isTrue();
            // El mismo nombre esta libre DENTRO de la empresa: los ambitos no compiten.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(NOMBRE_GLOBAL, COMPANY,
                    laQueSeEdita.getId())).isFalse();
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(NOMBRE_GLOBAL, null,
                    delCatalogo.getId())).isFalse();
        }

        @Test
        @DisplayName("con una fila activa y dos homonimas de baja devuelve la ACTIVA, sin reventar por resultado multiple")
        void con_una_activa_y_dos_de_baja_devuelve_la_activa() {
            // El indice unico cubre solo las ACTIVAS: active_name vale NULL cuando
            // enabled = false y MySQL no deduplica NULL, asi que la tabla admite UNA
            // activa y N de baja con el mismo nombre. Sin ORDER BY + LIMIT 1 la segunda
            // baja homonima convertia el finder en IncorrectResultSizeDataAccessException
            // -un 500- y dejaba ese nombre inutilizable para siempre (#580).
            DiagnosticImagingType primeraBaja = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(primeraBaja.getId());
            releerDesdeLaBase();
            DiagnosticImagingType segundaBaja = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(segundaBaja.getId());
            releerDesdeLaBase();
            DiagnosticImagingType activa = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();

            // La activa primero porque es la unica que de verdad OCUPA el nombre: es la
            // que tiene que hacer saltar el conflicto en vez de una reactivacion.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal",
                    COMPANY)).get().satisfies(fila -> {
                        assertThat(fila.getId()).isEqualTo(activa.getId());
                        assertThat(fila.isEnabled()).isTrue();
                    });
        }

        @Test
        @DisplayName("sin ninguna activa, entre dos homonimas de baja devuelve la de id mayor")
        void sin_activa_entre_dos_de_baja_devuelve_la_de_id_mayor() {
            DiagnosticImagingType antigua = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(antigua.getId());
            releerDesdeLaBase();
            DiagnosticImagingType reciente = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(reciente.getId());
            releerDesdeLaBase();

            assertThat(reciente.getId()).isGreaterThan(antigua.getId());
            // La mas reciente es la que la usuaria espera recuperar al volver a dar de
            // alta ese nombre: es la que llevaba sus ultimos datos.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal",
                    COMPANY)).get().satisfies(fila -> {
                        assertThat(fila.getId()).isEqualTo(reciente.getId());
                        assertThat(fila.isEnabled()).isFalse();
                    });
        }
    }

    @Nested
    @DisplayName("reactivacion con detalles")
    class ReactivacionConDetalles {

        @Test
        @DisplayName("reactivateWithDetails deja la fila de la empresa activa, con la descripcion nueva y la version subida")
        void reactivate_with_details_reactiva_la_fila_de_la_empresa() {
            DiagnosticImagingType pausado = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();
            DiagnosticImagingType antes = repository
                    .findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal", COMPANY)
                    .orElseThrow();

            int filas = repository.reactivateWithDetails(pausado.getId(), COMPANY,
                    "Ecografia abdominal", "Ecografia con Doppler");

            assertThat(filas).isEqualTo(1);
            DiagnosticImagingType despues = repository.findById(pausado.getId()).orElseThrow();
            assertThat(despues.isEnabled()).isTrue();
            assertThat(despues.getDescription()).isEqualTo("Ecografia con Doppler");
            // La version sube a mano porque una consulta nativa ni la comprueba ni la
            // incrementa: sin eso, un save cargado antes reescribiria la fila con su
            // enabled = false y deshaceria la reactivacion en silencio.
            assertThat(despues.getVersion()).isGreaterThan(antes.getVersion());
        }

        @Test
        @DisplayName("reactivateWithDetails con companyId nulo solo alcanza el catalogo de plataforma")
        void reactivate_with_details_con_company_id_nulo_solo_alcanza_el_catalogo() {
            DiagnosticImagingType delCatalogo = repository.save(estudioGlobalDePrueba());
            DiagnosticImagingType deLaEmpresa = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            repository.delete(delCatalogo.getId());
            repository.delete(deLaEmpresa.getId());
            releerDesdeLaBase();

            int filasDelCatalogo = repository.reactivateWithDetails(delCatalogo.getId(), null,
                    NOMBRE_GLOBAL, "Estudio global ampliado");
            // El WHERE nombra company_id IS NULL: la fila privada de un tenant queda
            // fuera del alcance de este camino aunque le pasen su id.
            int filasDeLaEmpresa = repository.reactivateWithDetails(deLaEmpresa.getId(), null,
                    "Ecografia abdominal", "Robado");

            assertThat(filasDelCatalogo).isEqualTo(1);
            assertThat(filasDeLaEmpresa).isZero();
            assertThat(repository.findById(delCatalogo.getId())).get().satisfies(
                    fila -> assertThat(fila.getDescription()).isEqualTo("Estudio global ampliado"));
            assertThat(repository.findById(deLaEmpresa.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class ListadoGlobal {

        @Test
        @DisplayName("devuelve tipos de todas las empresas, sin acotar tenant")
        void devuelve_tipos_de_todas_las_empresas() {
            DiagnosticImagingType propio = repository.save(tipoDeEmpresaValido());
            releerDesdeLaBase();
            DiagnosticImagingType ajeno = repository
                    .save(DiagnosticImagingType.create("Tomografia", "desc",
                            new CompanyRef(OTRA_COMPANY,
                                    companyJpaRepository.getReferenceById(OTRA_COMPANY).getName(),
                                    "900654321"),
                            false));
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(DiagnosticImagingType::getId)
                    .contains(propio.getId(), ajeno.getId());
        }
    }
}
