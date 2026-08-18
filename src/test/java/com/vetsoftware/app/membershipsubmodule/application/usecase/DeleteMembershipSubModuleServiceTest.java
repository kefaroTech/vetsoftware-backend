package com.vetsoftware.app.membershipsubmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteMembershipSubModuleService")
class DeleteMembershipSubModuleServiceTest {

    @Mock
    private MembershipSubModuleRepository repository;

    private DeleteMembershipSubModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteMembershipSubModuleService(repository);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la relacion existente")
        void borra_la_relacion_existente() {
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.activa()));

            service.execute(MembershipSubModuleMother.RELATION_ID);

            verify(repository).delete(MembershipSubModuleMother.RELATION_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no borra si la relacion no existe")
        void no_borra_si_la_relacion_no_existe() {
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipSubModuleMother.RELATION_ID))
                    .isInstanceOf(MembershipSubModuleNotFoundException.class)
                    .hasMessageContaining("MembershipSubModule not found: "
                            + MembershipSubModuleMother.RELATION_ID);

            verify(repository, never()).delete(any());
        }
    }
}
