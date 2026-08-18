package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionReportModel;
import com.vetsoftware.app.prescription.application.dto.PrescriptionSignalment;
import com.vetsoftware.app.prescription.application.port.in.ExportPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriberQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionPdfPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionReportQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "prescription.export")
@Service
public class ExportPrescriptionService implements ExportPrescriptionUseCase {

    private final PrescriptionRepository repository;
    private final PrescriptionReportQueryPort reportQueryPort;
    private final MedicamentQueryPort medicamentQueryPort;
    private final PrescriberQueryPort prescriberQueryPort;
    private final PrescriptionPdfPort pdfPort;

    public ExportPrescriptionService(PrescriptionRepository repository,
            PrescriptionReportQueryPort reportQueryPort, MedicamentQueryPort medicamentQueryPort,
            PrescriberQueryPort prescriberQueryPort, PrescriptionPdfPort pdfPort) {
        this.repository = repository;
        this.reportQueryPort = reportQueryPort;
        this.medicamentQueryPort = medicamentQueryPort;
        this.prescriberQueryPort = prescriberQueryPort;
        this.pdfPort = pdfPort;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] execute(Long prescriptionId, Long companyId, Long employeeId) {
        // El filtro va en la consulta, no en Java: el .filter() posterior rechazaba
        // igual, pero la fila ajena se cargaba entera antes de descartarse. El
        // companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId()), asi que nunca es null aqui.
        Prescription prescription = repository.findByIdAndCompanyId(prescriptionId, companyId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

        PrescriptionSignalment signalment = reportQueryPort
                .loadByAnimal(prescription.getAnimal().id(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Animal "
                        + prescription.getAnimal().id() + " not found for current company"));

        List<MedicamentRef> medicaments = medicamentQueryPort.findByPrescriptionId(prescriptionId);

        String prescriberName = employeeId == null
                ? null
                : prescriberQueryPort.findName(employeeId).orElse(null);

        PrescriptionReportModel model = new PrescriptionReportModel(signalment, prescriberName,
                prescription.getDate(), prescription.getDiagnosis(), prescription.getObservations(),
                medicaments, LocalDateTime.now());
        return pdfPort.render(model);
    }
}
