package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.domain.OwnerRef;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOwnerQueryPort (appointment) — adaptador sobre OwnerJpaRepository")
class JpaOwnerQueryPortTest {

    private static final Long OWNER_ID = 3L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private OwnerJpaRepository ownerJpaRepository;
    @InjectMocks
    private JpaOwnerQueryPort port;

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class BusquedaDelPropietario {

        @Test
        @DisplayName("mapea el propietario encontrado a su companion VO")
        void mapea_el_propietario_encontrado() {
            OwnerJpaEntity entity = mock(OwnerJpaEntity.class);
            when(entity.getId()).thenReturn(OWNER_ID);
            when(entity.getName()).thenReturn("Ana Ruiz");
            when(ownerJpaRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.findByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .contains(new OwnerRef(OWNER_ID, "Ana Ruiz"));
        }

        @Test
        @DisplayName("un propietario de otra empresa no se entrega")
        void un_propietario_de_otra_empresa_no_se_entrega() {
            when(ownerJpaRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(OWNER_ID, COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findEmailByIdAndCompanyId")
    class CorreoDelPropietario {

        @Test
        @DisplayName("devuelve el correo del propietario cuando lo tiene registrado")
        void devuelve_el_correo_cuando_lo_tiene() {
            OwnerJpaEntity entity = mock(OwnerJpaEntity.class);
            when(entity.getEmail()).thenReturn("ana@example.com");
            when(ownerJpaRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.findEmailByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .contains("ana@example.com");
        }

        @Test
        @DisplayName("un correo nulo en la entidad colapsa a Optional vacio, no a Optional.of(null)")
        void un_correo_nulo_colapsa_a_vacio() {
            OwnerJpaEntity entity = mock(OwnerJpaEntity.class);
            when(entity.getEmail()).thenReturn(null);
            when(ownerJpaRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entity));

            assertThat(port.findEmailByIdAndCompanyId(OWNER_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("un propietario inexistente o de otra empresa no entrega correo")
        void un_propietario_inexistente_no_entrega_correo() {
            when(ownerJpaRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findEmailByIdAndCompanyId(OWNER_ID, COMPANY_ID)).isEmpty();
        }
    }
}
