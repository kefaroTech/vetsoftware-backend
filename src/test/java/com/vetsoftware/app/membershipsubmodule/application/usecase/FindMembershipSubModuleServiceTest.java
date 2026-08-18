package com.vetsoftware.app.membershipsubmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindMembershipSubModuleService")
class FindMembershipSubModuleServiceTest {

    @Mock
    private MembershipSubModuleRepository repository;

    private FindMembershipSubModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new FindMembershipSubModuleService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el dto de la relacion encontrada")
        void devuelve_el_dto_de_la_relacion_encontrada() {
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.activa()));

            MembershipSubModuleDto dto = service.findById(MembershipSubModuleMother.RELATION_ID);

            assertThat(dto.id()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(dto.membership().id()).isEqualTo(MembershipSubModuleMother.MEMBERSHIP_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("lanza no encontrada si no existe")
        void lanza_no_encontrada_si_no_existe() {
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(MembershipSubModuleMother.RELATION_ID))
                    .isInstanceOf(MembershipSubModuleNotFoundException.class)
                    .hasMessageContaining("MembershipSubModule not found: "
                            + MembershipSubModuleMother.RELATION_ID);
        }
    }
}
