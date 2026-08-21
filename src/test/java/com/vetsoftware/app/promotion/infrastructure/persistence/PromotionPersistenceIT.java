package com.vetsoftware.app.promotion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las promociones contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Casi todo el comportamiento de este
 * adaptador lo pone Hibernate en SQL, no el codigo Java:
 *
 * <ul>
 * <li>El borrado es un {@code @SQLDelete} que hace
 * {@code UPDATE ... SET enabled
 * = false}, y un {@code @SQLRestriction("enabled = true")} esconde la fila de
 * todas las consultas. Un repositorio en memoria borraria de verdad y el test
 * pasaria igual el dia que alguien quite la anotacion —y ese dia se perderia el
 * historial de promociones.</li>
 * <li>{@code reactivate} es SQL nativo <i>precisamente</i> porque el
 * {@code @SQLRestriction} le impide a JPA volver a ver la fila para
 * reactivarla. Que ese UPDATE nativo esquive la restriccion solo se puede
 * comprobar contra la base.</li>
 * <li>El {@code CompanyRef} de una lectura no existe en ningun objeto Java
 * previo: lo arma el mapper con los datos que trae el {@code @EntityGraph} de
 * la tabla {@code companies}.</li>
 * <li>Los enums viajan como texto por {@code @Enumerated(STRING)}. Si alguien
 * lo cambiara a ordinal, reordenar el enum repuntaria en silencio todas las
 * promociones guardadas; eso solo se ve mirando la columna.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que este adaptador NO hace</b>, contra lo que sugiere el nombre: no
 * filtra por vigencia. Ni {@code findAllByCompanyId} ni {@code findById} miran
 * {@code start_date}/{@code end_date}. El filtro por fecha que consume el POS
 * vive en {@code JpaSalePromotionQueryPort} y tiene su propia rodaja.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPromotionRepository — promociones contra MySQL real")
class PromotionPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY, "Veterinaria ajena",
            "900654321");

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 31, 23, 59);
    private static final LocalDateTime CREADA = LocalDateTime.of(2025, 12, 20, 9, 0);

    @Autowired
    private JpaPromotionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private Promotion guardar(String nombre, CompanyRef empresa, PromotionStatus estado) {
        return repository.save(new Promotion(null, nombre, PromotionType.DISCOUNT,
                ApplicationType.CATEGORY, SchemaSeed.CATEGORY_ID, ValueType.PERCENTAGE,
                new BigDecimal("15.00"), INICIO, FIN, estado, empresa, CREADA, null, true));
    }

    private Promotion eneroPerruno() {
        return guardar("Enero perruno", EMPRESA, PromotionStatus.ACTIVE);
    }

    /**
     * Vacia el contexto de persistencia para que la lectura vaya de verdad a la
     * base.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private Number contarFilasCrudas(Long id) {
        return (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM promotions WHERE id = ?")
                .setParameter(1, id).getSingleResult();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y la lectura resuelve la empresa desde su tabla")
        void guardar_asigna_id_y_resuelve_la_empresa() {
            Promotion guardada = eneroPerruno();
            assertThat(guardada.getId()).isNotNull();

            releerDesdeLaBase();
            Promotion leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getName()).isEqualTo("Enero perruno");
            assertThat(leida.getPromotionType()).isEqualTo(PromotionType.DISCOUNT);
            assertThat(leida.getApplicationType()).isEqualTo(ApplicationType.CATEGORY);
            assertThat(leida.getApplicationItem()).isEqualTo(SchemaSeed.CATEGORY_ID);
            assertThat(leida.getValueType()).isEqualTo(ValueType.PERCENTAGE);
            assertThat(leida.getValue()).isEqualByComparingTo("15.00");
            assertThat(leida.getStartDate()).isEqualTo(INICIO);
            assertThat(leida.getEndDate()).isEqualTo(FIN);
            assertThat(leida.getPromotionStatus()).isEqualTo(PromotionStatus.ACTIVE);
            // El nombre y el NIT no viajaron en el save: los trae el @EntityGraph.
            assertThat(leida.getCompany()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("los enums se guardan como texto, no como ordinal")
        void los_enums_se_guardan_como_texto() {
            Promotion guardada = eneroPerruno();
            releerDesdeLaBase();

            Object[] fila = (Object[]) entityManager.createNativeQuery("""
                    SELECT promotion_type, application_type, value_type, promotion_status
                    FROM promotions WHERE id = ?
                    """).setParameter(1, guardada.getId()).getSingleResult();

            // Con ordinales, reordenar cualquiera de estos enums repuntaria en silencio
            // todas las promociones ya guardadas.
            assertThat(fila).containsExactly("DISCOUNT", "CATEGORY", "PERCENTAGE", "ACTIVE");
        }

        @Test
        @DisplayName("guardar de nuevo con id actualiza la fila en vez de insertar otra")
        void guardar_de_nuevo_actualiza_la_fila() {
            Promotion guardada = eneroPerruno();
            guardada.update("Febrero felino", PromotionType.SPECIAL_PRICE, ApplicationType.PRODUCT,
                    SchemaSeed.PRODUCT_ID, ValueType.VALUE, new BigDecimal("8000.00"), INICIO, FIN,
                    PromotionStatus.INACTIVE, EMPRESA);
            repository.save(guardada);
            releerDesdeLaBase();

            Promotion leida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(leida.getName()).isEqualTo("Febrero felino");
            assertThat(leida.getPromotionStatus()).isEqualTo(PromotionStatus.INACTIVE);
            assertThat(contarFilasCrudas(guardada.getId()).intValue()).isOne();
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("una promocion de otra empresa no se lee por id")
        void una_promocion_ajena_no_se_lee_por_id() {
            Promotion guardada = eneroPerruno();
            releerDesdeLaBase();

            // Sin el scope por empresa, adivinar un id daria la promocion de otro tenant.
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), OTRA_COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("el listado por empresa no mezcla tenants")
        void el_listado_por_empresa_no_mezcla_tenants() {
            eneroPerruno();
            guardar("Promo ajena", OTRA_EMPRESA, PromotionStatus.ACTIVE);
            releerDesdeLaBase();

            assertThat(repository.findAllByCompanyId(COMPANY)).extracting(Promotion::getName)
                    .containsExactly("Enero perruno");
            assertThat(repository.findAllByCompanyId(OTRA_COMPANY)).extracting(Promotion::getName)
                    .containsExactly("Promo ajena");
        }
    }

    @Nested
    @DisplayName("borrado logico y reactivacion")
    class BorradoLogico {

        @Test
        @DisplayName("borrar no elimina la fila: la marca deshabilitada y la esconde")
        void borrar_marca_deshabilitada_y_esconde() {
            Promotion guardada = eneroPerruno();
            releerDesdeLaBase();

            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).isEmpty();
            assertThat(repository.findAllByCompanyId(COMPANY)).isEmpty();
            // La fila sigue ahi: el historial de precios aplicados depende de ella.
            assertThat(contarFilasCrudas(guardada.getId()).intValue()).isOne();
            assertThat(enabledCrudo(guardada.getId())).isZero();
        }

        @Test
        @DisplayName("reactivar devuelve la promocion al listado esquivando la restriccion")
        void reactivar_devuelve_la_promocion_al_listado() {
            Promotion guardada = eneroPerruno();
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            // JPA ya no ve la fila (@SQLRestriction), asi que reactivarla con un save
            // seria imposible: por eso el adaptador usa un UPDATE nativo.
            assertThat(repository.reactivate(guardada.getId(), COMPANY)).isOne();

            assertThat(repository.findById(guardada.getId())).isPresent();
            assertThat(repository.findAllByCompanyId(COMPANY)).hasSize(1);
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta ninguna fila")
        void reactivar_un_id_inexistente_no_afecta_nada() {
            assertThat(repository.reactivate(999_999L, COMPANY)).isZero();
        }

        @Test
        @DisplayName("reactivar con el companyId de OTRA empresa afecta 0 filas y la deja oculta")
        void reactivar_con_la_empresa_ajena_no_afecta_nada() {
            // El WHERE es la unica barrera: sin el, otra empresa revivia esta promocion y
            // con ella los precios que se cobran aqui.
            Promotion guardada = eneroPerruno();
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.reactivate(guardada.getId(), OTRA_COMPANY)).isZero();
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        private int enabledCrudo(Long id) {
            return ((Number) entityManager
                    .createNativeQuery("SELECT enabled FROM promotions WHERE id = ?")
                    .setParameter(1, id).getSingleResult()).intValue();
        }
    }
}
