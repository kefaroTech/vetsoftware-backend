package com.vetsoftware.app.securityincident.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSecurityIncidentsService")
class ListSecurityIncidentsServiceTest {

    @Mock
    private SecurityIncidentRepository repository;

    @InjectMocks
    private ListSecurityIncidentsService service;

    @Test
    @DisplayName("mapea el barrido de plataforma conservando la paginacion del repositorio")
    void mapea_el_barrido_conservando_la_paginacion() {
        SecurityIncident primero = SecurityIncidentMother
                .registrado(SecurityIncidentMother.INCIDENT_ID);
        SecurityIncident segundo = SecurityIncidentMother
                .registrado(SecurityIncidentMother.INCIDENT_ID + 1);
        when(repository.findAll(1, 10))
                .thenReturn(new PageResult<>(List.of(primero, segundo), 1, 10, 25L, 3));

        PageResult<SecurityIncidentDto> resultado = service.listAll(1, 10);

        assertThat(resultado.content()).extracting(SecurityIncidentDto::id).containsExactly(
                SecurityIncidentMother.INCIDENT_ID, SecurityIncidentMother.INCIDENT_ID + 1);
        assertThat(resultado.page()).isEqualTo(1);
        assertThat(resultado.totalElements()).isEqualTo(25L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("una pagina vacia no es un error")
    void una_pagina_vacia_no_es_un_error() {
        when(repository.findAll(9, 20)).thenReturn(new PageResult<>(List.of(), 9, 20, 25L, 3));

        PageResult<SecurityIncidentDto> resultado = service.listAll(9, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(25L);
    }
}
