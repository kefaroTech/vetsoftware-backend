package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSurgeryChildrenQueryPort")
class JpaSurgeryChildrenQueryPortTest {

    private static final Long SURGERY_TYPE_ID = 700L;

    @Mock
    private SurgeryJpaRepository surgeryJpaRepository;

    @InjectMocks
    private JpaSurgeryChildrenQueryPort port;

    @Nested
    @DisplayName("existsActiveBySurgeryTypeId")
    class ExistsActiveBySurgeryTypeId {

        @Test
        @DisplayName("hay hijos activos si alguna cirugia usa el tipo")
        void hay_hijos_activos() {
            when(surgeryJpaRepository.existsBySurgeryType_Id(SURGERY_TYPE_ID)).thenReturn(true);

            assertThat(port.existsActiveBySurgeryTypeId(SURGERY_TYPE_ID)).isTrue();
        }

        @Test
        @DisplayName("no hay hijos activos si ninguna cirugia usa el tipo")
        void no_hay_hijos_activos() {
            when(surgeryJpaRepository.existsBySurgeryType_Id(SURGERY_TYPE_ID)).thenReturn(false);

            assertThat(port.existsActiveBySurgeryTypeId(SURGERY_TYPE_ID)).isFalse();
        }
    }
}
