package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageCompanyRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaExternalInvoicingOutageCompanyRepository} contra MySQL
 * real: el reparto de una caida entre las clinicas alcanzadas.
 *
 * <p>
 * <b>Lo que solo se ve con el motor delante</b> son tres cosas. Que
 * {@code uq_eioc_pair} sea {@code (outage_id, company_id)} <b>sin ambito</b>
 * —al reves que la puente de incidentes de seguridad, donde el ambito si
 * multiplica—: aqui una caida alcanza a una clinica de una sola forma, y una
 * segunda fila para el mismo par seria contar dos veces los documentos que se
 * quedaron sin transmitir. Que las dos claves foraneas sean {@code RESTRICT} de
 * verdad. Y que el {@code @EntityGraph} hidrate la caida asociada, sin la cual
 * el {@code @ManyToOne(LAZY)} dispararia un N+1 dentro del listado.
 *
 * <p>
 * <b>La empresa va como escalar y no como {@code @ManyToOne}</b>, asi que la
 * unica prueba posible de que {@code fk_eioc_company} existe es escribir una
 * fila cruda con una empresa que no esta. Es justo el caso que se pierde cuando
 * alguien confunde «no hay navegacion desde Java» con «no hay clave foranea».
 *
 * <p>
 * Se siembra con {@code SchemaSeed} por esa clave foranea: sin empresas reales
 * no se puede repartir nada.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaExternalInvoicingOutageCompanyRepository — el reparto por clinica")
class ExternalInvoicingOutageCompanyPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja, para las escrituras crudas. */
    private static final Long REPARTO_CRUDO = 8750L;

    private static final Long EMPRESA_INEXISTENTE = 999999L;
    private static final Long CAIDA_INEXISTENTE = 999999L;

    private static final LocalDateTime EMPEZO = LocalDateTime.of(2026, 3, 10, 8, 15, 0);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 10, 8, 16, 0);

    @Autowired
    private ExternalInvoicingOutageCompanyJpaRepository springDataRepository;
    @Autowired
    private ExternalInvoicingOutageJpaRepository outageJpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaExternalInvoicingOutageCompanyRepository repository;
    private Long caidaId;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        repository = new JpaExternalInvoicingOutageCompanyRepository(springDataRepository,
                outageJpaRepository, new ExternalInvoicingOutageCompanyJpaMapper());
        JpaExternalInvoicingOutageRepository caidas = new JpaExternalInvoicingOutageRepository(
                outageJpaRepository, new ExternalInvoicingOutageJpaMapper());
        ExternalInvoicingOutage caida = caidas
                .save(ExternalInvoicingOutage.open(EMPEZO, CauseParty.EXTERNAL_ISSUER,
                        "El proveedor no responde", 2, "INC-2026-0310", CREADO_EL));
        entityManager.flush();
        caidaId = caida.getId();
    }

    @Nested
    @DisplayName("Reparto")
    class Reparto {

        @Test
        @DisplayName("guarda la clinica alcanzada y la recupera campo a campo")
        void guarda_la_clinica_alcanzada_y_la_recupera() {
            ExternalInvoicingOutageCompany guardada = repository
                    .save(ExternalInvoicingOutageCompany.register(caidaId, SchemaSeed.COMPANY_ID,
                            17, OutageResolution.CONTINGENCY_NUMBERING));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByOutageId(caidaId, 0, 20).content()).singleElement()
                    .satisfies(recuperada -> {
                        assertThat(recuperada.getId()).isEqualTo(guardada.getId());
                        // El @EntityGraph hidrato la caida: si no lo hiciera, este
                        // getter dispararia una consulta por fila dentro del listado.
                        assertThat(recuperada.getOutageId()).isEqualTo(caidaId);
                        assertThat(recuperada.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(recuperada.getFailedDocumentCount()).isEqualTo(17);
                        assertThat(recuperada.getResolvedBy())
                                .isEqualTo(OutageResolution.CONTINGENCY_NUMBERING);
                        assertThat(recuperada.usedContingencyNumbering()).isTrue();
                    });
        }

        @Test
        @DisplayName("cero documentos fallidos es legitimo: la clinica estuvo dentro del alcance")
        void cero_documentos_fallidos_es_legitimo() {
            // La comprobacion del dominio es < 0 y no <= 0 a proposito: una clinica
            // puede estar alcanzada sin haber intentado emitir nada en esa franja, y
            // dejarla fuera del reparto la borraria del expediente.
            repository.save(ExternalInvoicingOutageCompany.register(caidaId, SchemaSeed.COMPANY_ID,
                    0, OutageResolution.RETRIED));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByOutageId(caidaId, 0, 20).content()).singleElement()
                    .satisfies(fila -> assertThat(fila.getFailedDocumentCount()).isZero());
        }

        @Test
        @DisplayName("el listado ordena por documentos fallidos descendente, con desempate por id")
        void el_listado_ordena_por_documentos_fallidos() {
            // Quien mas documentos perdio encabeza la lista: es la clinica cuya
            // reclamacion hay que atender primero.
            repository.save(ExternalInvoicingOutageCompany.register(caidaId, SchemaSeed.COMPANY_ID,
                    3, OutageResolution.RETRIED));
            repository.save(ExternalInvoicingOutageCompany.register(caidaId,
                    SchemaSeed.OTRA_COMPANY_ID, 41, OutageResolution.MANUAL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByOutageId(caidaId, 0, 20).content())
                    .extracting(ExternalInvoicingOutageCompany::getFailedDocumentCount)
                    .containsExactly(41, 3);
        }

        @Test
        @DisplayName("la comprobacion previa distingue la clinica repartida de la que no")
        void la_comprobacion_previa_distingue_la_clinica_repartida() {
            // Es lo que el service consulta ANTES de insertar, para convertir el
            // duplicado en un 409 legible en vez de un Duplicate entry del driver.
            repository.save(ExternalInvoicingOutageCompany.register(caidaId, SchemaSeed.COMPANY_ID,
                    5, OutageResolution.RETRIED));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsByOutageIdAndCompanyId(caidaId, SchemaSeed.COMPANY_ID))
                    .isTrue();
            assertThat(repository.existsByOutageIdAndCompanyId(caidaId, SchemaSeed.OTRA_COMPANY_ID))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("la misma clinica dos veces en la misma caida la para uq_eioc_pair")
        void la_misma_clinica_dos_veces_la_para_la_unicidad() {
            // Sin esta unicidad, los documentos fallidos de una clinica se contarian
            // dos veces y la cifra que se coteja contra la del proveedor dejaria de
            // cuadrar.
            repository.save(ExternalInvoicingOutageCompany.register(caidaId, SchemaSeed.COMPANY_ID,
                    17, OutageResolution.CONTINGENCY_NUMBERING));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_eioc_pair", () -> {
                repository.save(ExternalInvoicingOutageCompany.register(caidaId,
                        SchemaSeed.COMPANY_ID, 4, OutageResolution.MANUAL));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos clinicas distintas en la misma caida si caben")
        void dos_clinicas_distintas_en_la_misma_caida_caben() {
            repository.save(ExternalInvoicingOutageCompany.register(caidaId, SchemaSeed.COMPANY_ID,
                    17, OutageResolution.CONTINGENCY_NUMBERING));
            repository.save(ExternalInvoicingOutageCompany.register(caidaId,
                    SchemaSeed.OTRA_COMPANY_ID, 2, OutageResolution.RETRIED));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByOutageId(caidaId, 0, 20).content())
                    .extracting(ExternalInvoicingOutageCompany::getCompanyId)
                    .containsExactlyInAnyOrder(SchemaSeed.COMPANY_ID, SchemaSeed.OTRA_COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un contador de documentos negativo lo para chk_eioc_count")
        void un_contador_negativo_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_eioc_count", () -> insertarCruda(REPARTO_CRUDO,
                    caidaId, SchemaSeed.COMPANY_ID, -1, "RETRIED"));
        }

        @Test
        @DisplayName("una resolucion fuera de las tres la para chk_eioc_resolved")
        void una_resolucion_desconocida_la_para_el_check() {
            // Si alguien anade un valor a OutageResolution sin el changeset que lo
            // admita, el INSERT muere. Importa mas de lo que parece:
            // CONTINGENCY_NUMBERING es el unico que hay que sostener ante la
            // autoridad, y un cuarto valor mal escrito lo dejaria sin clasificar.
            EngineConstraint.assertViolates("chk_eioc_resolved",
                    () -> insertarCruda(REPARTO_CRUDO + 1, caidaId, SchemaSeed.COMPANY_ID, 3,
                            "IGNORED"));
        }

        @Test
        @DisplayName("una empresa que no existe la para fk_eioc_company")
        void una_empresa_inexistente_la_para_la_clave_foranea() {
            // La empresa viaja como escalar Long y NO como @ManyToOne —colgar la
            // asociacion activaria las cuatro reglas de BE-COV sobre la feature
            // entera—, pero la clave foranea sigue viva y vigilando en la base. Este
            // caso es la unica prueba de que sigue ahi.
            EngineConstraint.assertViolates("fk_eioc_company",
                    () -> insertarCruda(REPARTO_CRUDO + 2, caidaId, EMPRESA_INEXISTENTE, 3,
                            "RETRIED"));
        }

        @Test
        @DisplayName("una caida que no existe la para fk_eioc_outage")
        void una_caida_inexistente_la_para_la_clave_foranea() {
            EngineConstraint.assertViolates("fk_eioc_outage", () -> insertarCruda(REPARTO_CRUDO + 3,
                    CAIDA_INEXISTENTE, SchemaSeed.COMPANY_ID, 3, "RETRIED"));
        }
    }

    @Nested
    @DisplayName("Sin borrado")
    class SinBorrado {

        @Test
        @DisplayName("el puerto no declara ningun borrado, y esa ausencia es la decision")
        void el_puerto_no_declara_ningun_borrado() {
            // No es una prueba de estilo. Quitar una clinica de la lista de alcanzadas
            // destruye la prueba de que se le aviso y de por que uso numeracion de
            // contingencia — que es justo lo que hay que poder ensenar ante la
            // autoridad. Este caso se pone rojo el dia que alguien anada el metodo,
            // que es cuando hay que discutirlo y no despues.
            assertThat(Arrays.stream(ExternalInvoicingOutageCompanyRepository.class.getMethods())
                    .map(Method::getName).toList())
                    .noneMatch(nombre -> nombre.toLowerCase().contains("delete")
                            || nombre.toLowerCase().contains("remove"));
        }
    }

    /**
     * Escritura cruda que se salta el agregado. La puente no lleva
     * {@code created_date}, ni {@code enabled}, ni {@code version}: se escribe una
     * sola vez al repartir y ningun caso de uso la reescribe.
     */
    private void insertarCruda(Long id, Long outageId, Long companyId, int fallidos,
            String resolucion) {
        entityManager.createNativeQuery("""
                INSERT INTO external_invoicing_outage_companies
                        (id, outage_id, company_id, failed_document_count, resolved_by)
                VALUES (:id, :outageId, :companyId, :fallidos, :resolucion)
                """).setParameter("id", id).setParameter("outageId", outageId)
                .setParameter("companyId", companyId).setParameter("fallidos", fallidos)
                .setParameter("resolucion", resolucion).executeUpdate();
    }
}
