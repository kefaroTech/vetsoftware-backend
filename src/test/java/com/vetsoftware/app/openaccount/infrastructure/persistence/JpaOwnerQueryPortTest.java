package com.vetsoftware.app.openaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.domain.OwnerRef;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOwnerQueryPort")
class JpaOwnerQueryPortTest {

    @Mock
    private OwnerJpaRepository ownerJpaRepository;
    @InjectMocks
    private JpaOwnerQueryPort port;

    private static final Long COMPANY_ID = 9L;

    @Test
    @DisplayName("mapea el propietario encontrado a OwnerRef")
    void mapea_el_propietario_encontrado() {
        OwnerJpaEntity entity = mock(OwnerJpaEntity.class);
        when(entity.getId()).thenReturn(2L);
        when(entity.getName()).thenReturn("Juan Perez");
        when(entity.getDocument()).thenReturn("CC123");
        when(ownerJpaRepository.findByIdAndCompanyId(2L, COMPANY_ID))
                .thenReturn(Optional.of(entity));

        assertThat(port.findByIdAndCompanyId(2L, COMPANY_ID))
                .contains(new OwnerRef(2L, "Juan Perez", "CC123"));
    }

    /**
     * Mismo desenlace para «no existe» y «existe en otra empresa»: el adaptador
     * delega en el finder acotado, asi que el propietario ajeno es indistinguible
     * de uno inexistente.
     */
    @Test
    @DisplayName("devuelve vacio si el propietario no existe o es de otra empresa")
    void devuelve_vacio_si_no_existe_o_es_de_otra_empresa() {
        when(ownerJpaRepository.findByIdAndCompanyId(2L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(2L, COMPANY_ID)).isEmpty();
    }
}
