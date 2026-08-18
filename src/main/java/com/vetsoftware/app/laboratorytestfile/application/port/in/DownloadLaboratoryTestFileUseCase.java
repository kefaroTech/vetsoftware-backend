package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DownloadLaboratoryTestFileUseCase {
    /**
     * La empresa es parte de la firma porque este endpoint devuelve el CONTENIDO
     * del fichero: sin acotar, {@code laboratoryTest.read} bastaba para descargar
     * el PDF de un resultado de laboratorio de otro tenant. No es escritura
     * indebida, es exfiltracion de historia clinica.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.read')"
            + " and @authz.isMyCompany(#companyId))")
    LaboratoryTestFileDownloadDto download(Long id, Long companyId);
}
