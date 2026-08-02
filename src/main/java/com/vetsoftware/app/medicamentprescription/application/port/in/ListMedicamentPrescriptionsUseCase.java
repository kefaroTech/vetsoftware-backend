package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMedicamentPrescriptionsUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  List<MedicamentPrescriptionDto> listAll();
}
