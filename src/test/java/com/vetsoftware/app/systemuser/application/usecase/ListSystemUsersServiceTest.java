package com.vetsoftware.app.systemuser.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSystemUsersService")
class ListSystemUsersServiceTest {

    @Mock
    private SystemUserRepository repository;

    @InjectMocks
    private ListSystemUsersService service;

    @Test
    @DisplayName("mapea cada usuario del repositorio a su DTO, en el mismo orden")
    void mapea_cada_usuario_a_su_dto_en_el_mismo_orden() {
        when(repository.findAll())
                .thenReturn(List.of(SystemUserMother.activo(100L), SystemUserMother.activo(200L)));

        List<SystemUserDto> resultado = service.listAll();

        assertThat(resultado).extracting(SystemUserDto::id).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("sin usuarios, devuelve una lista vacia")
    void sin_usuarios_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
