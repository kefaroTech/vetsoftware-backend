package com.vetsoftware.app.branch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * READ por id scoped a la empresa (defensa IDOR): devuelve DTO fiel o lanza si
 * no pertenece.
 */
@ExtendWith(MockitoExtension.class)
class FindBranchServiceTest {

    @Mock
    private BranchRepository repository;
    @InjectMocks
    private FindBranchService service;

    @Test
    void devuelve_dto_cuando_pertenece_a_la_empresa() {
        Branch branch = new Branch(3L, "Sede Norte", "NORTE", "addr", "phone",
                new CityRef(5L, "Bogotá"), new CompanyRef(9L, "Vet SAS", "900123456"),
                LocalDateTime.of(2020, 1, 1, 10, 0), null, true);
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(branch));

        BranchDto dto = service.findById(3L, 9L);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.code()).isEqualTo("NORTE");
        assertThat(dto.city().name()).isEqualTo("Bogotá");
        assertThat(dto.active()).isTrue();
    }

    @Test
    void lanza_cuando_no_existe_o_es_de_otra_empresa() {
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(3L, 9L))
                .isInstanceOf(BranchNotFoundException.class).hasMessageContaining("3");
    }
}
