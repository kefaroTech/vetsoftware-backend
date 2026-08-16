package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListHospitalizationsByCompanyService")
class ListHospitalizationsByCompanyServiceTest {

    @Mock
    private HospitalizationRepository repository;

    @InjectMocks
    private ListHospitalizationsByCompanyService service;

    @Test
    @DisplayName("pide al repositorio solo las estancias de la empresa recibida")
    void pide_solo_las_estancias_de_la_empresa() {
        when(repository.findAllByCompanyId(HospitalizationMother.COMPANY_ID))
                .thenReturn(List.of(HospitalizationMother.internado()));

        List<HospitalizationDto> resultado = service
                .listByCompany(HospitalizationMother.COMPANY_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).company().id()).isEqualTo(HospitalizationMother.COMPANY_ID);
        verify(repository).findAllByCompanyId(HospitalizationMother.COMPANY_ID);
    }

    @Test
    @DisplayName("sin estancias devuelve la lista vacia")
    void sin_estancias_devuelve_lista_vacia() {
        when(repository.findAllByCompanyId(HospitalizationMother.COMPANY_ID)).thenReturn(List.of());

        assertThat(service.listByCompany(HospitalizationMother.COMPANY_ID)).isEmpty();
    }
}
