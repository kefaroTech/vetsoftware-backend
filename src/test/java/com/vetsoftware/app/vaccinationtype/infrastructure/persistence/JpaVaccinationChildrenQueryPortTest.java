package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaVaccinationChildrenQueryPort — adaptador sobre VaccinationJpaRepository")
class JpaVaccinationChildrenQueryPortTest {

    @Mock
    private VaccinationJpaRepository jpaRepository;

    @InjectMocks
    private JpaVaccinationChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByVaccinationType_Id del repositorio de Spring Data")
    void delega_en_exists_by_vaccination_type_id() {
        when(jpaRepository.existsByVaccinationType_Id(VaccinationTypeMother.TYPE_ID))
                .thenReturn(true);

        assertThat(port.existsActiveByVaccinationTypeId(VaccinationTypeMother.TYPE_ID)).isTrue();
    }

    @Test
    @DisplayName("sin vacunas activas devuelve false")
    void sin_vacunas_activas_devuelve_false() {
        when(jpaRepository.existsByVaccinationType_Id(VaccinationTypeMother.TYPE_ID))
                .thenReturn(false);

        assertThat(port.existsActiveByVaccinationTypeId(VaccinationTypeMother.TYPE_ID)).isFalse();
    }
}
