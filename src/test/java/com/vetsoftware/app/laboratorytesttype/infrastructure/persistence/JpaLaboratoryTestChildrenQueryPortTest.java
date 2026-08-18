package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaLaboratoryTestChildrenQueryPort — adaptador sobre LaboratoryTestJpaRepository")
class JpaLaboratoryTestChildrenQueryPortTest {

    @Mock
    private LaboratoryTestJpaRepository jpaRepository;

    @InjectMocks
    private JpaLaboratoryTestChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByTestType_Id del repositorio JPA")
    void delega_en_exists_by_test_type_id() {
        when(jpaRepository.existsByTestType_Id(70L)).thenReturn(true);

        assertThat(port.existsActiveByLaboratoryTestTypeId(70L)).isTrue();
    }

    @Test
    @DisplayName("propaga false cuando no hay examenes con ese tipo")
    void propaga_false_cuando_no_hay_examenes() {
        when(jpaRepository.existsByTestType_Id(70L)).thenReturn(false);

        assertThat(port.existsActiveByLaboratoryTestTypeId(70L)).isFalse();
    }
}
