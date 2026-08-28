package com.vetsoftware.app.companytrialwindow.application.port.out;

import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import java.util.Optional;

/**
 * Adaptador de salida del reloj de la empresa.
 *
 * <p>
 * Todas las lecturas van acotadas por empresa y no hay ninguna «por id» suelta:
 * una ventana es de alguien, y cargarla por un id que escribe el cliente sin
 * decir de quién es exactamente la familia de fugas que BE-COV cerró.
 */
public interface CompanyTrialWindowRepository {

    CompanyTrialWindow save(CompanyTrialWindow window);

    /** La ventana viva de la empresa, si la tiene. */
    Optional<CompanyTrialWindow> findOpenByCompanyId(Long companyId);

    Optional<CompanyTrialWindow> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsOpenByCompanyId(Long companyId);
}
