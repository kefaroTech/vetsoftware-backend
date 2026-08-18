package com.vetsoftware.app.laboratorytestfile.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestFileUseCase {
    /**
     * El borrado arrastra el objeto en S3, asi que una fuga aqui es irreversible:
     * la empresa viaja hasta la lectura previa para que un id ajeno sea un 404 y no
     * un borrado del fichero de otro tenant.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.delete')"
            + " and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
