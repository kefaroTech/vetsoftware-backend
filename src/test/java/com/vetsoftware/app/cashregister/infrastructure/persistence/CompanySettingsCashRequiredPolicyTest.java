package com.vetsoftware.app.cashregister.infrastructure.persistence;

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

/**
 * Tests del flag {@code cashregister.required} por empresa. Mismo patrón que
 * {@code CompanySettingsNegativeStockPolicyTest} (inventory), con la polaridad
 * invertida: aquí la ausencia de fila BLOQUEA el cobro (por defecto se exige
 * caja abierta).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanySettingsCashRequiredPolicy — el flag cashregister.required por empresa")
class CompanySettingsCashRequiredPolicyTest {

    private static final Long COMPANY_ID = 1L;
    private static final String KEY = "cashregister.required";

    @Mock
    private CompanySettingJpaRepository companySettingJpaRepository;

    @InjectMocks
    private CompanySettingsCashRequiredPolicy policy;

    private static CompanySettingJpaEntity setting(String value) {
        CompanySettingJpaEntity entity = mock(CompanySettingJpaEntity.class);
        when(entity.getValue()).thenReturn(value);
        return entity;
    }

    @Test
    @DisplayName("una empresa null exige caja igual, sin tocar el repositorio")
    void companyId_null_exige_caja_y_no_consulta_el_repositorio() {
        assertThat(policy.isCashRequired(null)).isTrue();

        verifyNoInteractions(companySettingJpaRepository);
    }

    @Test
    @DisplayName("sin fila configurada, exige caja por defecto")
    void sin_fila_configurada_exige_caja_por_defecto() {
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(Optional.empty());

        assertThat(policy.isCashRequired(COMPANY_ID)).isTrue();
    }

    @Test
    @DisplayName("con el valor 'false' no exige caja")
    void valor_false_no_exige_caja() {
        // La fixture se resuelve ANTES del when(): construirla dentro de
        // thenReturn(...) mezclaría el stubbing de dos mocks a la vez y Mockito lo
        // rechaza con UnfinishedStubbingException.
        Optional<CompanySettingJpaEntity> fila = Optional.of(setting("false"));
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(fila);

        assertThat(policy.isCashRequired(COMPANY_ID)).isFalse();
    }

    @Test
    @DisplayName("el valor 'false' se compara sin distinguir mayusculas")
    void el_valor_false_se_compara_sin_distinguir_mayusculas() {
        Optional<CompanySettingJpaEntity> fila = Optional.of(setting("FALSE"));
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(fila);

        assertThat(policy.isCashRequired(COMPANY_ID)).isFalse();
    }

    @Test
    @DisplayName("cualquier otro valor exige caja")
    void cualquier_otro_valor_exige_caja() {
        Optional<CompanySettingJpaEntity> fila = Optional.of(setting("true"));
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(COMPANY_ID, KEY))
                .thenReturn(fila);

        assertThat(policy.isCashRequired(COMPANY_ID)).isTrue();
    }
}
