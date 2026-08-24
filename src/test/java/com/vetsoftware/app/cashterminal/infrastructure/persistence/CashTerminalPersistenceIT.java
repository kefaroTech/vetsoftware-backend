package com.vetsoftware.app.cashterminal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCashTerminalRepository - terminales contra MySQL real")
class CashTerminalPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTHER_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;

    @Autowired
    private JpaCashTerminalRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("save asigna id y find relee todos los campos del dominio")
    void save_y_find_hacen_ida_y_vuelta() {
        CashTerminal saved = repository.save(newTerminal("Caja farmacia", "farmacia"));
        entityManager.flush();
        entityManager.clear();

        CashTerminal found = repository.findByIdAndCompanyId(saved.getId(), COMPANY).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Caja farmacia");
        assertThat(found.getCode()).isEqualTo("FARMACIA");
        assertThat(found.getBranchId()).isEqualTo(BRANCH);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getVersion()).isZero();
    }

    @Test
    @DisplayName("find por id no deja cruzar la frontera de empresa")
    void find_por_id_aisla_por_empresa() {
        CashTerminal saved = repository.save(newTerminal("Caja farmacia", "FARMACIA"));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByIdAndCompanyId(saved.getId(), OTHER_COMPANY)).isEmpty();
        assertThat(repository.findByIdAndCompanyId(saved.getId(), COMPANY)).isPresent();
    }

    @Test
    @DisplayName("activeOnly excluye terminales inactivas sin ocultarlas del listado completo")
    void active_only_filtra_estado_real() {
        CashTerminal principal = repository.findByIdAndCompanyId(SchemaSeed.TERMINAL_ID, COMPANY)
                .orElseThrow();
        principal.setActive(false);
        repository.save(principal);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByBranch(COMPANY, BRANCH, true))
                .extracting(CashTerminal::getCode).contains("CAJA-2").doesNotContain("PRINCIPAL");
        assertThat(repository.findAllByBranch(COMPANY, BRANCH, false))
                .extracting(CashTerminal::getCode).contains("CAJA-2", "PRINCIPAL");
    }

    @Test
    @DisplayName("unicidad por codigo ignora mayusculas y excluye la propia fila al editar")
    void exists_code_respeta_scope_y_exclusion() {
        assertThat(repository.existsCode(COMPANY, BRANCH, "principal")).isTrue();
        assertThat(repository.existsCode(OTHER_COMPANY, BRANCH, "principal")).isFalse();
        assertThat(repository.existsOtherWithCode(COMPANY, BRANCH, "PRINCIPAL",
                SchemaSeed.TERMINAL_ID)).isFalse();
        assertThat(repository.existsOtherWithCode(COMPANY, BRANCH, "PRINCIPAL",
                SchemaSeed.OTRO_TERMINAL_ID)).isTrue();
    }

    private static CashTerminal newTerminal(String name, String code) {
        return CashTerminal.create(COMPANY, BRANCH, name, code,
                LocalDateTime.of(2026, 2, 1, 9, 30));
    }
}
