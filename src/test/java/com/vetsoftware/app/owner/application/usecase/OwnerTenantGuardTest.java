package com.vetsoftware.app.owner.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.owner.application.port.out.CityQueryPort;
import com.vetsoftware.app.owner.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerTenantGuardTest {

    private static final long OWNER_ID = 10L;
    private static final long COMPANY_ID = 20L;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private AnimalChildrenQueryPort animalChildrenQueryPort;

    @Mock
    private CityQueryPort cityQueryPort;

    @Mock
    private CompanyQueryPort companyQueryPort;

    @Test
    void findByIdRequiresOwnerInCurrentCompany() {
        when(ownerRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID)).thenReturn(Optional.empty());

        FindOwnerService service = new FindOwnerService(ownerRepository);

        assertThatThrownBy(() -> service.findById(OWNER_ID, COMPANY_ID))
                .isInstanceOf(OwnerNotFoundException.class);
    }

    @Test
    void listAllUsesCurrentCompanyScope() {
        when(ownerRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

        ListOwnersService service = new ListOwnersService(ownerRepository);

        assertThat(service.listAll(COMPANY_ID)).isEmpty();
        verify(ownerRepository).findAllByCompanyId(COMPANY_ID);
    }

    @Test
    void updateDoesNotLoadCityOrCompanyWhenOwnerIsOutsideCurrentCompany() {
        when(ownerRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID)).thenReturn(Optional.empty());
        UpdateOwnerCommand command = new UpdateOwnerCommand(
                OWNER_ID, null, null, null, null, null, null, null,
                null, null, 30L, COMPANY_ID, false, null, null);

        UpdateOwnerService service = new UpdateOwnerService(ownerRepository, cityQueryPort, companyQueryPort);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(OwnerNotFoundException.class);
        verify(cityQueryPort, never()).findById(30L);
        verify(companyQueryPort, never()).findById(COMPANY_ID);
        verify(ownerRepository, never()).save(any());
    }

    @Test
    void deleteDoesNotCheckChildrenOrDeleteWhenOwnerIsOutsideCurrentCompany() {
        when(ownerRepository.findByIdAndCompanyId(OWNER_ID, COMPANY_ID)).thenReturn(Optional.empty());

        DeleteOwnerService service = new DeleteOwnerService(ownerRepository, animalChildrenQueryPort);

        assertThatThrownBy(() -> service.execute(OWNER_ID, COMPANY_ID))
                .isInstanceOf(OwnerNotFoundException.class);
        verify(animalChildrenQueryPort, never()).existsActiveByOwnerId(OWNER_ID);
        verify(ownerRepository, never()).delete(OWNER_ID, COMPANY_ID);
    }

    @Test
    void reactivateRequiresOwnerInCurrentCompany() {
        when(ownerRepository.reactivate(OWNER_ID, COMPANY_ID)).thenReturn(0);

        ReactivateOwnerService service = new ReactivateOwnerService(ownerRepository);

        assertThatThrownBy(() -> service.execute(OWNER_ID, COMPANY_ID))
                .isInstanceOf(OwnerNotFoundException.class);
        verify(ownerRepository, never()).findByIdAndCompanyId(OWNER_ID, COMPANY_ID);
    }
}
