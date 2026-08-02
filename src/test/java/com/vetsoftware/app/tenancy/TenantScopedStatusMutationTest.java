package com.vetsoftware.app.tenancy;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.command.ChangeDiagnosticImagingStatusCommand;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.application.usecase.ChangeDiagnosticImagingStatusService;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.spa.application.command.ChangeSpaStatusCommand;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.application.usecase.ChangeSpaStatusService;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.surgery.application.command.ChangeSurgeryStatusCommand;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.application.usecase.ChangeSurgeryStatusService;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantScopedStatusMutationTest {

    @Test
    void spaStatusDoesNotFallBackToGlobalLookupForEmployee() {
        SpaRepository repository = mock(SpaRepository.class);
        when(repository.findByIdAndCompanyId(9L, 3L)).thenReturn(Optional.empty());

        var service = new ChangeSpaStatusService(repository);

        assertThrows(SpaNotFoundException.class,
                () -> service.execute(new ChangeSpaStatusCommand(9L, "COMPLETED", 3L)));
        verify(repository, never()).findById(9L);
    }

    @Test
    void surgeryStatusDoesNotFallBackToGlobalLookupForEmployee() {
        SurgeryRepository repository = mock(SurgeryRepository.class);
        when(repository.findByIdAndCompanyId(9L, 3L)).thenReturn(Optional.empty());

        var service = new ChangeSurgeryStatusService(repository);

        assertThrows(SurgeryNotFoundException.class,
                () -> service.execute(new ChangeSurgeryStatusCommand(9L, "COMPLETED", 3L)));
        verify(repository, never()).findById(9L);
    }

    @Test
    void diagnosticStatusDoesNotFallBackToGlobalLookupForEmployee() {
        DiagnosticImagingRepository repository = mock(DiagnosticImagingRepository.class);
        when(repository.findByIdAndCompanyId(9L, 3L)).thenReturn(Optional.empty());

        var service = new ChangeDiagnosticImagingStatusService(repository);

        assertThrows(DiagnosticImagingNotFoundException.class, () -> service
                .execute(new ChangeDiagnosticImagingStatusCommand(9L, "COMPLETED", 3L)));
        verify(repository, never()).findById(9L);
    }
}
