package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPermissionsByCompanyService")
class ListPermissionsByCompanyServiceTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private ListPermissionsByCompanyService service;

    @Test
    @DisplayName("lista los permisos de la empresa mapeados a DTO")
    void lista_los_permisos_de_la_empresa() {
        when(repository.findAllByCompanyId(PermissionMother.COMPANY_ID))
                .thenReturn(List.of(PermissionMother.permisoValido()));

        List<PermissionDto> dtos = service.listByCompany(PermissionMother.COMPANY_ID);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).company().id()).isEqualTo(PermissionMother.COMPANY_ID);
    }

    @Test
    @DisplayName("otra empresa sin permisos devuelve lista vacia")
    void otra_empresa_sin_permisos_devuelve_lista_vacia() {
        when(repository.findAllByCompanyId(PermissionMother.OTRA_CLINICA.id()))
                .thenReturn(List.of());

        assertThat(service.listByCompany(PermissionMother.OTRA_CLINICA.id())).isEmpty();
    }
}
