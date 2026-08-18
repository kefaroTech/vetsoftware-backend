package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSubModuleQueryPort — adaptador sobre SubModuleJpaRepository")
class JpaSubModuleQueryPortTest {

    @Mock
    private SubModuleJpaRepository subModuleJpaRepository;
    @Mock
    private SubModuleJpaEntity subModuleEntity;

    private JpaSubModuleQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaSubModuleQueryPort(subModuleJpaRepository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a su companion VO")
        void mapea_la_entidad_encontrada_a_su_companion_vo() {
            when(subModuleEntity.getId()).thenReturn(MembershipSubModuleMother.SUB_MODULE_ID);
            when(subModuleEntity.getName())
                    .thenReturn(MembershipSubModuleMother.FACTURACION.name());
            when(subModuleEntity.getCode())
                    .thenReturn(MembershipSubModuleMother.FACTURACION.code());
            when(subModuleJpaRepository.findById(MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(subModuleEntity));

            Optional<SubModuleRef> resultado = port
                    .findById(MembershipSubModuleMother.SUB_MODULE_ID);

            assertThat(resultado).contains(MembershipSubModuleMother.FACTURACION);
        }

        @Test
        @DisplayName("un submodulo inexistente devuelve vacio")
        void un_sub_modulo_inexistente_devuelve_vacio() {
            when(subModuleJpaRepository.findById(MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.empty());

            Optional<SubModuleRef> resultado = port
                    .findById(MembershipSubModuleMother.SUB_MODULE_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
