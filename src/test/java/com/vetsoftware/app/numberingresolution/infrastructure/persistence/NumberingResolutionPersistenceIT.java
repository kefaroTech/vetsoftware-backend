package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la resolucion de numeracion contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve.</b> Todo lo que hace especial a este adaptador
 * esta en el schema o en anotaciones de Hibernate, no en su codigo:
 *
 * <ul>
 * <li>El borrado es <b>logico</b>: una {@code @SQLDelete} convierte el
 * {@code DELETE} en {@code enabled = false} y una {@code @SQLRestriction}
 * esconde la fila despues. Un doble que quita el elemento del mapa da verde
 * igual, y no detecta que la fila —con su rango y su consecutivo— sigue
 * ahi.</li>
 * <li>Justamente por esa {@code @SQLRestriction}, reactivar NO se puede hacer
 * con {@code findById(...).enable()}: la consulta no ve lo desactivado. Por eso
 * {@code reactivate} es un {@code UPDATE} nativo. Que esa via siga viendo la
 * fila oculta solo se comprueba contra el motor.</li>
 * <li>El invariante «una sola resolucion activa por (empresa, sede, tipo)» lo
 * sostiene un <b>indice unico sobre una columna generada</b>
 * ({@code active_scope = CONCAT(COALESCE(branch_id,0), ':', document_type)}
 * cuando esta activa, NULL si no). Es SQL puro: ni la entidad ni el service
 * saben que existe, y es lo unico que cierra la ventana de carrera del
 * check-then-act.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaNumberingResolutionRepository — baja logica, reactivacion y unicidad contra MySQL real")
class NumberingResolutionPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef EMPRESA_AJENA = new CompanyRef(OTRA_COMPANY,
            "Veterinaria ajena", "900654321");

    private static final LocalDate EXPEDIDA = LocalDate.of(2026, 1, 2);
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2027, 12, 31);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 2, 9, 0, 0);

    @Autowired
    private JpaNumberingResolutionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private NumberingResolution resolucion(CompanyRef empresa, Long branchId,
            ElectronicDocumentType tipo, String numero, String prefijo) {
        return new NumberingResolution(null, empresa, tipo, numero, EXPEDIDA, prefijo, 100L, 199L,
                DESDE, HASTA, "clave-tecnica", 100L, CREADA, true, branchId);
    }

    private NumberingResolution guardadaDeEmpresa() {
        return repository.save(
                resolucion(EMPRESA, null, ElectronicDocumentType.FE_VENTA, "18760000001", "SETP"));
    }

    private long filasConId(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM numbering_resolutions WHERE id = ?")
                .setParameter(1, id).getSingleResult()).longValue();
    }

    private long filasDesactivadasConId(Long id) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM numbering_resolutions WHERE id = ? AND enabled = false")
                .setParameter(1, id).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y la resolucion vuelve entera")
        void guardar_asigna_id_y_la_resolucion_vuelve_entera() {
            NumberingResolution guardada = guardadaDeEmpresa();

            assertThat(guardada.getId()).isNotNull();

            NumberingResolution leida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(leida.getDocumentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(leida.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(leida.getPrefix()).isEqualTo("SETP");
            assertThat(leida.getRangeFrom()).isEqualTo(100L);
            assertThat(leida.getRangeTo()).isEqualTo(199L);
            assertThat(leida.getValidFrom()).isEqualTo(DESDE);
            assertThat(leida.getValidTo()).isEqualTo(HASTA);
            assertThat(leida.getTechnicalKey()).isEqualTo("clave-tecnica");
            assertThat(leida.getCurrentNumber()).isEqualTo(100L);
            assertThat(leida.getCreatedDate()).isEqualTo(CREADA);
            assertThat(leida.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la lectura resuelve el nombre y el NIT de la empresa")
        void la_lectura_resuelve_el_nombre_y_el_nit_de_la_empresa() {
            NumberingResolution guardada = guardadaDeEmpresa();
            entityManager.flush();
            entityManager.clear();

            // El save escribe con getReferenceById (un proxy sin SELECT). Estos datos
            // salen del @EntityGraph de la lectura: sin el, el proxy sin sesion reventaria
            // al pedir el nombre.
            CompanyRef empresa = repository.findById(guardada.getId()).orElseThrow().getCompany();
            assertThat(empresa.id()).isEqualTo(COMPANY);
            assertThat(empresa.name()).isEqualTo("Veterinaria de prueba");
            assertThat(empresa.identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("la resolucion de una sede guarda su branchId; la de empresa lo deja nulo")
        void la_resolucion_de_una_sede_guarda_su_branch_id() {
            NumberingResolution deEmpresa = guardadaDeEmpresa();
            NumberingResolution deSede = repository.save(resolucion(EMPRESA, BRANCH,
                    ElectronicDocumentType.FE_VENTA, "18760000002", "SEDE"));

            assertThat(repository.findById(deEmpresa.getId()).orElseThrow().getBranchId()).isNull();
            assertThat(repository.findById(deSede.getId()).orElseThrow().getBranchId())
                    .isEqualTo(BRANCH);
        }

        @Test
        @DisplayName("actualizar conserva el id y el consecutivo ya consumido")
        void actualizar_conserva_el_id_y_el_consecutivo() {
            NumberingResolution guardada = guardadaDeEmpresa();
            guardada.update(EMPRESA, ElectronicDocumentType.FE_VENTA, "18760000001", EXPEDIDA,
                    "NUEVO", 100L, 299L, DESDE, HASTA, "otra-clave", BRANCH);

            NumberingResolution actualizada = repository.save(guardada);

            assertThat(actualizada.getId()).isEqualTo(guardada.getId());
            NumberingResolution leida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(leida.getPrefix()).isEqualTo("NUEVO");
            assertThat(leida.getRangeTo()).isEqualTo(299L);
            assertThat(leida.getBranchId()).isEqualTo(BRANCH);
            // El CRUD no gestiona el consecutivo: lo mueve la emision. Si el update lo
            // reiniciara, se repetirian numeros ya usados en facturas emitidas.
            assertThat(leida.getCurrentNumber()).isEqualTo(100L);
            assertThat(filasConId(guardada.getId())).as("actualiza, no inserta otra").isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una resolucion de otra empresa no se lee por id")
        void una_resolucion_de_otra_empresa_no_se_lee_por_id() {
            NumberingResolution guardada = guardadaDeEmpresa();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), OTRA_COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("el listado por empresa no ve las de otra")
        void el_listado_por_empresa_no_ve_las_de_otra() {
            guardadaDeEmpresa();
            repository.save(resolucion(EMPRESA_AJENA, null, ElectronicDocumentType.FE_VENTA,
                    "18760000099", "AJEN"));

            assertThat(repository.findAllByCompanyId(COMPANY))
                    .extracting(NumberingResolution::getResolutionNumber)
                    .containsExactly("18760000001");
            assertThat(repository.findAllByCompanyId(OTRA_COMPANY))
                    .extracting(NumberingResolution::getResolutionNumber)
                    .containsExactly("18760000099");
        }
    }

    @Nested
    @DisplayName("baja logica y reactivacion")
    class BajaLogica {

        @Test
        @DisplayName("borrar no elimina la fila: la desactiva")
        void borrar_no_elimina_la_fila() {
            NumberingResolution guardada = guardadaDeEmpresa();

            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            // Un DELETE real perderia el rango autorizado por la DIAN y el consecutivo
            // ya consumido, que hay que conservar aunque la resolucion se retire.
            assertThat(filasConId(guardada.getId())).isEqualTo(1L);
            assertThat(filasDesactivadasConId(guardada.getId())).isEqualTo(1L);
        }

        @Test
        @DisplayName("una resolucion desactivada deja de verse por las consultas normales")
        void una_resolucion_desactivada_deja_de_verse() {
            NumberingResolution guardada = guardadaDeEmpresa();

            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).isEmpty();
            assertThat(repository.findAllByCompanyId(COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("reactivar SI ve la fila oculta y la devuelve al servicio")
        void reactivar_ve_la_fila_oculta_y_la_devuelve() {
            NumberingResolution guardada = guardadaDeEmpresa();
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            // La @SQLRestriction esconde la fila de todo el mapeo JPA. Solo el UPDATE
            // nativo puede volver a activarla; con findById().enable() esto seria
            // imposible y nadie podria recuperar una resolucion dada de baja por error.
            assertThat(repository.reactivate(guardada.getId(), COMPANY)).isEqualTo(1);
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).map(NumberingResolution::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivar una resolucion inexistente no afecta a ninguna fila")
        void reactivar_una_resolucion_inexistente_no_afecta_nada() {
            assertThat(repository.reactivate(777777L, COMPANY)).isZero();
        }

        @Test
        @DisplayName("reactivar con el companyId de OTRA empresa afecta 0 filas y la deja oculta")
        void reactivar_con_la_empresa_ajena_no_afecta_filas() {
            // La unica barrera de la reactivacion es este WHERE: no hay lectura previa que
            // valide la propiedad. Con el company_id ajeno la numeracion fiscal de esta
            // empresa sigue dada de baja.
            NumberingResolution guardada = guardadaDeEmpresa();
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.reactivate(guardada.getId(), OTRA_COMPANY)).isZero();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("una sola resolucion activa por alcance")
    class Unicidad {

        @Test
        @DisplayName("reconoce el alcance exacto: empresa, sede y tipo")
        void reconoce_el_alcance_exacto() {
            repository.save(
                    resolucion(EMPRESA, BRANCH, ElectronicDocumentType.FE_VENTA, "1876A", "SEDE"));

            assertThat(repository.existsActiveByCompanyBranchAndType(COMPANY, BRANCH,
                    ElectronicDocumentType.FE_VENTA)).isTrue();
            assertThat(repository.existsActiveByCompanyBranchAndType(COMPANY, OTRA_BRANCH,
                    ElectronicDocumentType.FE_VENTA)).as("otra sede es otro alcance").isFalse();
            assertThat(repository.existsActiveByCompanyBranchAndType(COMPANY, BRANCH,
                    ElectronicDocumentType.NOTA_CREDITO)).as("otro tipo es otro alcance").isFalse();
            assertThat(repository.existsActiveByCompanyBranchAndType(OTRA_COMPANY, BRANCH,
                    ElectronicDocumentType.FE_VENTA)).as("otra empresa es otro alcance").isFalse();
        }

        @Test
        @DisplayName("el alcance de EMPRESA (sin sede) es distinto del de una sede")
        void el_alcance_de_empresa_es_distinto_del_de_una_sede() {
            guardadaDeEmpresa();

            // branchId null significa "todas las sedes", no "cualquier sede": si estos
            // dos alcances se confundieran, crear el prefijo de una sucursal seria
            // imposible mientras existiera el de empresa.
            assertThat(repository.existsActiveByCompanyBranchAndType(COMPANY, null,
                    ElectronicDocumentType.FE_VENTA)).isTrue();
            assertThat(repository.existsActiveByCompanyBranchAndType(COMPANY, BRANCH,
                    ElectronicDocumentType.FE_VENTA)).isFalse();
        }

        @Test
        @DisplayName("una desactivada libera el alcance y ya no cuenta como activa")
        void una_desactivada_libera_el_alcance() {
            NumberingResolution guardada = guardadaDeEmpresa();
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsActiveByCompanyBranchAndType(COMPANY, null,
                    ElectronicDocumentType.FE_VENTA)).isFalse();
            // Y el hueco queda de verdad libre: la columna generada vale NULL cuando
            // enabled es false, y MySQL no deduplica los NULL.
            assertThat(repository.save(resolucion(EMPRESA, null, ElectronicDocumentType.FE_VENTA,
                    "18760000002", "NUEV")).getId()).isNotNull();
        }

        @Test
        @DisplayName("la de empresa y la de cada sede conviven sin chocar")
        void la_de_empresa_y_la_de_cada_sede_conviven() {
            guardadaDeEmpresa();
            repository.save(
                    resolucion(EMPRESA, BRANCH, ElectronicDocumentType.FE_VENTA, "1876A", "SEDA"));
            repository.save(resolucion(EMPRESA, OTRA_BRANCH, ElectronicDocumentType.FE_VENTA,
                    "1876B", "SEDB"));

            // Tres activas del mismo tipo y la misma empresa: legitimo, porque el alcance
            // del indice unico incluye la sede.
            assertThat(repository.findAllByCompanyId(COMPANY)).hasSize(3);
        }

        @Test
        @DisplayName("dos activas para el MISMO alcance las rechaza la base")
        void dos_activas_para_el_mismo_alcance_las_rechaza_la_base() {
            guardadaDeEmpresa();

            // El check del service es check-then-act. Sin este indice, dos peticiones
            // concurrentes dejan dos resoluciones activas y la asignacion de consecutivo
            // (ORDER BY id LIMIT 1) elige una en silencio, ignorando el rango de la otra.
            assertThatThrownBy(() -> repository.save(resolucion(EMPRESA, null,
                    ElectronicDocumentType.FE_VENTA, "18760000002", "OTRO")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("dos activas para la MISMA sede tambien las rechaza la base")
        void dos_activas_para_la_misma_sede_tambien_las_rechaza() {
            repository.save(
                    resolucion(EMPRESA, BRANCH, ElectronicDocumentType.FE_VENTA, "1876A", "SEDA"));

            assertThatThrownBy(() -> repository.save(
                    resolucion(EMPRESA, BRANCH, ElectronicDocumentType.FE_VENTA, "1876C", "SEDC")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
