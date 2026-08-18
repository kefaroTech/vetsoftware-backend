package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompaniesService")
class ListCompaniesServiceTest {

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private ListCompaniesService service;

    @Test
    @DisplayName("devuelve el DTO de cada empresa del repositorio")
    void devuelve_el_dto_de_cada_empresa() {
        when(repository.findAll())
                .thenReturn(List.of(CompanyMother.clinicaNorte(), CompanyMother.clinicaNorte(77L)));

        List<CompanyDto> dtos = service.listAll();

        assertThat(dtos).extracting(CompanyDto::id).containsExactly(CompanyMother.COMPANY_ID, 77L);
    }

    @Test
    @DisplayName("sin empresas registradas, devuelve lista vacia")
    void sin_empresas_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
