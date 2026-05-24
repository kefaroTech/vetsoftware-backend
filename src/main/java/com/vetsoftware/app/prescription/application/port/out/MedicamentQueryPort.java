package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.MedicamentRef;
import java.util.List;

public interface MedicamentQueryPort {
    List<MedicamentRef> findByPrescriptionId(Long prescriptionId);
}
