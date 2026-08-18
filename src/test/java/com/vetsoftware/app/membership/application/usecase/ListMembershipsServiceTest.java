package com.vetsoftware.app.membership.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListMembershipsService")
class ListMembershipsServiceTest {

    @Mock
    private MembershipRepository repository;
    @InjectMocks
    private ListMembershipsService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada membresia del repositorio a dto")
        void mapea_cada_membresia_del_repositorio_a_dto() {
            when(repository.findAll()).thenReturn(List.of(MembershipMother.activa()));

            List<MembershipDto> resultado = service.listAll();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).name()).isEqualTo("Plan Oro");
        }

        @Test
        @DisplayName("un catalogo vacio devuelve una lista vacia, no null")
        void un_catalogo_vacio_devuelve_una_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
