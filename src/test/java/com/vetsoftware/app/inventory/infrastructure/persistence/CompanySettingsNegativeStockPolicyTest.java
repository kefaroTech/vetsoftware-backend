package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaEntity;
import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanySettingsNegativeStockPolicy — el flag inventory.allow_negative_stock por empresa")
class CompanySettingsNegativeStockPolicyTest {

    private static final Long COMPANY_ID = 1L;
    private static final String KEY = "inventory.allow_negative_stock";

    @Mock
    private CompanySettingJpaRepository companySettingJpaRepository;

    @InjectMocks
    private CompanySettingsNegativeStockPolicy policy;

    private static CompanySettingJpaEntity setting(String value) {
        CompanySettingJpaEntity entity = mock(CompanySettingJpaEntity.class);
        when(entity.getValue()).thenReturn(value);
        return entity;
    }

    @Test
    @DisplayName("una empresa null nunca permite negativo, sin tocar el repositorio")
    void companyId_null_no_permite_negativo_y_no_consulta_el_repositorio() {
        assertThat(policy.allowsNegative(null)).isFalse();

        verifyNoInteractions(companySettingJpaRepository);
    }

    @Test
    @DisplayName("sin fila configurada, bloquea el stock negativo por defecto")
    void sin_fila_configurada_bloquea_por_defecto() {
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(Optional.empty());

        assertThat(policy.allowsNegative(COMPANY_ID)).isFalse();
    }

    @Test
    @DisplayName("con el valor 'true' permite negativo")
    void valor_true_permite_negativo() {
        // El mock se arma ANTES del when(): construirlo dentro de thenReturn(...)
        // dejaria un when() anidado mientras el de companySettingJpaRepository sigue
        // abierto.
        CompanySettingJpaEntity fila = setting("true");
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(Optional.of(fila));

        assertThat(policy.allowsNegative(COMPANY_ID)).isTrue();
    }

    @Test
    @DisplayName("el valor se compara sin distinguir mayusculas")
    void el_valor_se_compara_sin_distinguir_mayusculas() {
        CompanySettingJpaEntity fila = setting("TRUE");
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(Optional.of(fila));

        assertThat(policy.allowsNegative(COMPANY_ID)).isTrue();
    }

    @Test
    @DisplayName("cualquier otro valor bloquea el stock negativo")
    void cualquier_otro_valor_bloquea() {
        CompanySettingJpaEntity fila = setting("false");
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(Optional.of(fila));

        assertThat(policy.allowsNegative(COMPANY_ID)).isFalse();
    }
}
