package com.vetsoftware.app.supplierwithholding.application.usecase;

import com.vetsoftware.app.supplierwithholding.application.command.IssueSupplierWithholdingCertificateCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.in.IssueSupplierWithholdingCertificateUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite el certificado de retencion del proveedor.
 *
 * <p>
 * <strong>La fecha la pone el reloj inyectado, no el cliente.</strong> Es un
 * dato probatorio —el proveedor lo usa para descontarse la retencion en su
 * declaracion— y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build ante un
 * {@code now()} pelado.
 *
 * <p>
 * Que el certificado no estuviera ya emitido lo decide el dominio: la base
 * <b>no lo cuida</b>, porque {@code chk_sw_certificate} solo exige que la fecha
 * y la referencia vayan juntas y una segunda emision las cambiaria las dos a la
 * vez, pasando la constraint sin una queja.
 */
@Observed(name = "supplier.withholding.certificate.issue")
@Service
public class IssueSupplierWithholdingCertificateService
        implements
            IssueSupplierWithholdingCertificateUseCase {

    private final SupplierWithholdingRepository repository;
    private final Clock clock;

    public IssueSupplierWithholdingCertificateService(SupplierWithholdingRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SupplierWithholdingDto execute(IssueSupplierWithholdingCertificateCommand command) {
        SupplierWithholding withholding = repository.findById(command.id())
                .orElseThrow(() -> new SupplierWithholdingNotFoundException(command.id()));
        return SupplierWithholdingDto.from(repository.save(
                withholding.issueCertificate(LocalDateTime.now(clock), command.certificateRef())));
    }
}
