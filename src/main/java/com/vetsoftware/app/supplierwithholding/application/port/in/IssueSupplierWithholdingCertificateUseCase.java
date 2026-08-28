package com.vetsoftware.app.supplierwithholding.application.port.in;

import com.vetsoftware.app.supplierwithholding.application.command.IssueSupplierWithholdingCertificateCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueSupplierWithholdingCertificateUseCase {

    /**
     * Emite el certificado que hay que entregarle al proveedor.
     *
     * <p>
     * <strong>Se niega si ya estaba emitido.</strong> El numero del certificado es
     * el que el proveedor usa para descontarse la retencion en su propia
     * declaracion; reescribirlo deja dos documentos incompatibles en circulacion, y
     * la base no lo impide —{@code chk_sw_certificate} solo exige que fecha y
     * referencia vayan juntas—.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SupplierWithholdingDto execute(IssueSupplierWithholdingCertificateCommand command);
}
