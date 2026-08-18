package com.vetsoftware.app.membershipsubmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListMembershipSubModulesService")
class ListMembershipSubModulesServiceTest {

    @Mock
    private MembershipSubModuleRepository repository;

    private ListMembershipSubModulesService service;

    @BeforeEach
    void crearServicio() {
        service = new ListMembershipSubModulesService(repository);
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada relacion a su dto")
        void mapea_cada_relacion_a_su_dto() {
            when(repository.findAll()).thenReturn(List.of(MembershipSubModuleMother.activa()));

            List<MembershipSubModuleDto> resultado = service.listAll();

            assertThat(resultado).extracting(MembershipSubModuleDto::id)
                    .containsExactly(MembershipSubModuleMother.RELATION_ID);
        }

        @Test
        @DisplayName("sin relaciones devuelve una lista vacia")
        void sin_relaciones_devuelve_una_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            List<MembershipSubModuleDto> resultado = service.listAll();

            assertThat(resultado).isEmpty();
        }
    }
}
