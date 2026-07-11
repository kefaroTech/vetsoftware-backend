package com.vetsoftware.app.branch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** READ lista scoped a la empresa: mapea todas las sucursales (activas e inactivas) preservando el orden. */
@ExtendWith(MockitoExtension.class)
class ListBranchesServiceTest {

    @Mock private BranchRepository repository;
    @InjectMocks private ListBranchesService service;

    private final CityRef city = new CityRef(5L, "Bogotá");
    private final CompanyRef company = new CompanyRef(9L, "Vet SAS", "900123456");

    @Test
    void mapea_todas_las_sucursales_incluyendo_inactivas_en_orden() {
        Branch activa = new Branch(1L, "Principal", "PRINCIPAL", null, null, city, company,
            LocalDateTime.of(2020, 1, 1, 10, 0), true);
        Branch inactiva = new Branch(2L, "Sede Sur", "SUR", null, null, city, company,
            LocalDateTime.of(2020, 1, 2, 10, 0), false);
        when(repository.findAllByCompanyId(9L)).thenReturn(List.of(activa, inactiva));

        List<BranchDto> result = service.listAll(9L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BranchDto::code).containsExactly("PRINCIPAL", "SUR");
        assertThat(result).extracting(BranchDto::active).containsExactly(true, false);
    }

    @Test
    void devuelve_vacio_cuando_no_hay_sucursales() {
        when(repository.findAllByCompanyId(9L)).thenReturn(List.of());

        assertThat(service.listAll(9L)).isEmpty();
    }
}
