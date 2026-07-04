package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.port.out.AnimalColorQueryPort;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationTypeQueryPort;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnimalConsultationTenantGuardTest {

    private static final long RESOURCE_ID = 10L;
    private static final long COMPANY_ID = 20L;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecieQueryPort specieQueryPort;

    @Mock
    private BreedQueryPort breedQueryPort;

    @Mock
    private OwnerQueryPort ownerQueryPort;

    @Mock
    private CompanyQueryPort companyQueryPort;

    @Mock
    private AnimalColorQueryPort animalColorQueryPort;

    @Mock
    private VaccinationChildrenQueryPort vaccinationChildrenQueryPort;

    @Mock
    private DewormingChildrenQueryPort dewormingChildrenQueryPort;

    @Mock
    private SurgeryChildrenQueryPort surgeryChildrenQueryPort;

    @Mock
    private HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort;

    @Mock
    private DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;

    @Mock
    private LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;

    @Mock
    private SpaChildrenQueryPort spaChildrenQueryPort;

    @Mock
    private DayCareChildrenQueryPort dayCareChildrenQueryPort;

    @Mock
    private ConsultationChildrenQueryPort consultationChildrenQueryPort;

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private ConsultationTypeQueryPort consultationTypeQueryPort;

    @Mock
    private com.vetsoftware.app.consultation.application.port.out.AnimalQueryPort consultationAnimalQueryPort;

    @Mock
    private com.vetsoftware.app.consultation.application.port.out.CompanyQueryPort consultationCompanyQueryPort;

    @Test
    void updateAnimalDoesNotLoadReferencesWhenAnimalIsOutsideCurrentCompany() {
        when(animalRepository.findByIdAndCompanyId(RESOURCE_ID, COMPANY_ID)).thenReturn(Optional.empty());
        UpdateAnimalCommand command = new UpdateAnimalCommand(
            RESOURCE_ID, null, null, 1L, 2L, 3L, null, null, null, null, 4L,
            null, null, null, false, null, COMPANY_ID);

        UpdateAnimalService service = new UpdateAnimalService(
            animalRepository, specieQueryPort, breedQueryPort, ownerQueryPort,
            companyQueryPort, animalColorQueryPort);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(AnimalNotFoundException.class);
        verify(specieQueryPort, never()).findById(1L);
        verify(ownerQueryPort, never()).findByIdAndCompanyId(3L, COMPANY_ID);
        verify(animalRepository, never()).save(any());
    }

    @Test
    void deleteAnimalDoesNotCheckChildrenWhenAnimalIsOutsideCurrentCompany() {
        when(animalRepository.findByIdAndCompanyId(RESOURCE_ID, COMPANY_ID)).thenReturn(Optional.empty());

        DeleteAnimalService service = new DeleteAnimalService(
            animalRepository, vaccinationChildrenQueryPort, dewormingChildrenQueryPort,
            surgeryChildrenQueryPort, hospitalizationChildrenQueryPort, diagnosticImagingChildrenQueryPort,
            laboratoryTestChildrenQueryPort, spaChildrenQueryPort, dayCareChildrenQueryPort,
            consultationChildrenQueryPort);

        assertThatThrownBy(() -> service.execute(RESOURCE_ID, COMPANY_ID))
            .isInstanceOf(AnimalNotFoundException.class);
        verify(vaccinationChildrenQueryPort, never()).existsActiveByAnimalId(RESOURCE_ID);
        verify(animalRepository, never()).delete(RESOURCE_ID, COMPANY_ID);
    }

    @Test
    void updateConsultationDoesNotLoadReferencesWhenConsultationIsOutsideCurrentCompany() {
        when(consultationRepository.findByIdAndCompanyId(RESOURCE_ID, COMPANY_ID)).thenReturn(Optional.empty());
        UpdateConsultationCommand command = new UpdateConsultationCommand(
            RESOURCE_ID, LocalDate.now(), 1L, null, null, null, null, 2L, COMPANY_ID,
            null, null, null, null, null, null, null, null, null, null);

        com.vetsoftware.app.consultation.application.usecase.UpdateConsultationService service =
            new com.vetsoftware.app.consultation.application.usecase.UpdateConsultationService(
                consultationRepository, consultationTypeQueryPort, consultationAnimalQueryPort,
                consultationCompanyQueryPort);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(ConsultationNotFoundException.class);
        verify(consultationTypeQueryPort, never()).findById(1L);
        verify(consultationAnimalQueryPort, never()).findByIdAndCompanyId(2L, COMPANY_ID);
        verify(consultationRepository, never()).save(any());
    }
}
