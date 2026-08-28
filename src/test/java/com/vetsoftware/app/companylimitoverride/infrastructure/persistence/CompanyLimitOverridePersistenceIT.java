package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import com.vetsoftware.app.companylimitoverride.domain.OverrideReasonCode;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyLimitOverrideRepository — la excepción negociada contra MySQL real")
class CompanyLimitOverridePersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate CATORCE_DE_MARZO = LocalDate.of(2026, 3, 14);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 3, 14, 16, 0);

    @Autowired
    private JpaCompanyLimitOverrideRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resuelto, no sembrado: los ocho ejes llegan poblados por el changeset 313.
     */
    private Long ejeAnimal;

    /** El segundo eje: es lo que distingue «por eje» de «por empresa». */
    private Long ejeUsuarios;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
        ejeAnimal = SchemaSeed.limitDimensionId(entityManager, "ANIMAL");
        ejeUsuarios = SchemaSeed.limitDimensionId(entityManager, "USER");
    }

    private CompanyLimitOverride trescientasMascotas() {
        return CompanyLimitOverride.grant(SchemaSeed.COMPANY_ID, ejeAnimal, 300, CATORCE_DE_MARZO,
                OverrideReasonCode.RETENTION,
                "Retención — llamada del 14/03, aprobada por Dirección Comercial",
                SchemaSeed.SYSTEM_USER_ID, CREADA);
    }

    @Test
    @DisplayName("guarda la excepción con su motivo y su firma, y la vuelve a leer como viva")
    void guarda_la_excepcion_con_su_motivo_y_su_firma() {
        repository.save(trescientasMascotas());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAliveByCompanyIdAndLimitDimensionId(SchemaSeed.COMPANY_ID,
                ejeAnimal)).get().satisfies(leida -> {
                    assertThat(leida.getLimitQuantity()).isEqualTo(300);
                    assertThat(leida.getReasonCode()).isEqualTo(OverrideReasonCode.RETENTION);
                    assertThat(leida.getGrantedBySystemUserId())
                            .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                    assertThat(leida.isAlive()).isTrue();
                });
    }

    @Test
    @DisplayName("R-LIMIT-20 · dos excepciones vivas sobre el mismo eje mueren en el motor")
    void dos_excepciones_abiertas_sobre_el_eje_ANIMAL_fallan_en_el_motor() {
        repository.save(trescientasMascotas());
        entityManager.flush();

        assertViolates("uq_company_limit_overrides_alive", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_limit_overrides (company_id, limit_dimension_id,
                                                         limit_quantity, valid_from, valid_to,
                                                         reason_code, reason,
                                                         granted_by_system_user_id,
                                                         revoked_by_system_user_id, revoked_at,
                                                         revoked_reason_code, revoked_reason,
                                                         created_date, enabled, version)
                    VALUES (:companyId, :dimensionId, 500, '2026-04-01', NULL, 'RETENTION',
                            'Segunda excepcion', :userId, NULL, NULL, NULL, NULL, NOW(), true, 0)
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("dimensionId", ejeAnimal)
                    .setParameter("userId", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
            entityManager.flush();
        });
    }

    /**
     * <b>La mitad de R-LIMIT-20 que el motor tiene que sostener, y que solo estaba
     * probada contra un repositorio simulado.</b>
     * {@code GrantCompanyLimitOverrideServiceTest} afirma que negociar 300 mascotas
     * y 5 usuarios en la misma llamada produce dos excepciones vivas, pero lo hace
     * sobre un doble que responde lo que se le diga: con el indice unico escrito
     * sobre {@code alive_company_marker} a secas —el error exacto que la regla
     * existe para evitar, y una linea de mas de lo que hay hoy— aquella prueba
     * seguiria verde y la segunda negociacion moriria en produccion.
     *
     * <p>
     * Aqui las dos filas van contra MySQL. Que entren es lo que demuestra que
     * {@code uq_company_limit_overrides_alive} es
     * {@code (alive_company_marker, limit_dimension_id)} y no solo el marcador.
     */
    @Test
    @DisplayName("R-LIMIT-20 · dos excepciones vivas sobre ejes distintos de la misma empresa sí"
            + " coexisten: la unicidad es por eje, no por empresa")
    void dos_excepciones_vivas_sobre_ejes_distintos_de_la_misma_empresa_coexisten() {
        repository.save(trescientasMascotas());
        repository.save(CompanyLimitOverride.grant(SchemaSeed.COMPANY_ID, ejeUsuarios, 5,
                CATORCE_DE_MARZO, OverrideReasonCode.RETENTION,
                "Retención — misma llamada del 14/03, cinco usuarios", SchemaSeed.SYSTEM_USER_ID,
                CREADA));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAliveByCompanyIdAndLimitDimensionId(SchemaSeed.COMPANY_ID,
                ejeAnimal)).get()
                .satisfies(mascotas -> assertThat(mascotas.getLimitQuantity()).isEqualTo(300));
        assertThat(repository.findAliveByCompanyIdAndLimitDimensionId(SchemaSeed.COMPANY_ID,
                ejeUsuarios)).get()
                .satisfies(usuarios -> assertThat(usuarios.getLimitQuantity()).isEqualTo(5));
        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).hasSize(2);
    }

    @Test
    @DisplayName("R-LIMIT-35 · una excepción revocada no bloquea abrir otra sobre el mismo eje")
    void una_excepcion_revocada_no_bloquea_abrir_otra_sobre_el_mismo_eje() {
        CompanyLimitOverride viva = repository.save(trescientasMascotas());
        repository.save(viva.revoke(LocalDateTime.of(2026, 6, 1, 9, 0), SchemaSeed.SYSTEM_USER_ID,
                OverrideReasonCode.COMMERCIAL_AGREEMENT, "Pasa a plan de pago"));
        entityManager.flush();

        CompanyLimitOverride nueva = repository
                .save(CompanyLimitOverride.grant(SchemaSeed.COMPANY_ID, ejeAnimal, 500,
                        LocalDate.of(2026, 6, 2), OverrideReasonCode.RETENTION, "Segunda retención",
                        SchemaSeed.SYSTEM_USER_ID, LocalDateTime.of(2026, 6, 2, 10, 0)));
        entityManager.flush();
        entityManager.clear();

        assertThat(nueva.getId()).isNotNull();
        assertThat(repository.findAliveByCompanyIdAndLimitDimensionId(SchemaSeed.COMPANY_ID,
                ejeAnimal)).get()
                .satisfies(leida -> assertThat(leida.getLimitQuantity()).isEqualTo(500));
        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).hasSize(2);
    }

    @Test
    @DisplayName("R-LIMIT-34 · una excepción sin motivo escrito muere en el motor")
    void una_excepcion_sin_motivo_escrito_muere_en_el_motor() {
        assertViolates("Column 'reason' cannot be null", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_limit_overrides (company_id, limit_dimension_id,
                                                         limit_quantity, valid_from, valid_to,
                                                         reason_code, reason,
                                                         granted_by_system_user_id, created_date,
                                                         enabled, version)
                    VALUES (:companyId, :dimensionId, 300, '2026-03-14', NULL, 'RETENTION', NULL,
                            :userId, NOW(), true, 0)
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("dimensionId", ejeAnimal)
                    .setParameter("userId", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("la excepción de una clínica no se ve desde otra")
    void la_excepcion_de_una_clinica_no_se_ve_desde_otra() {
        CompanyLimitOverride guardada = repository.save(trescientasMascotas());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                .isPresent();
        assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
        assertThat(repository.existsAliveByCompanyIdAndLimitDimensionId(SchemaSeed.OTRA_COMPANY_ID,
                ejeAnimal)).isFalse();
    }
}
