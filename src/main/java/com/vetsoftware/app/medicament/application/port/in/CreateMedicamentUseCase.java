package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateMedicamentUseCase {
    /**
     * <b>Sin disyuncion de {@code hasRole('SYSTEM')}, a proposito</b> (#593). La
     * llevaba, y con ella un principal de plataforma que enviara la cabecera
     * {@code X-Company-Id: 7} escribia el vademecum PRIVADO de la empresa 7 por
     * esta misma via: {@code Authz.currentCompanyId()} resuelve
     * {@code SystemUserContext} con esa cabecera, asi que la empresa llegaba al
     * command como valida y el {@code isMyCompany} ni se evaluaba —la disyuncion de
     * SYSTEM iba primera y sin condicion—.
     *
     * <p>
     * La decision de producto es que el superusuario administra SOLO el catalogo
     * global: los medicamentos propios de una empresa se LISTAN para dar contexto y
     * no se escriben nunca desde la consola. Ese camino ya no necesita colarse por
     * el puerto del empleado porque tiene el suyo,
     * {@link CreateGlobalMedicamentUseCase}. Los puertos de LECTURA de esta slice
     * conservan su {@code hasRole('SYSTEM')}: leer si esta permitido.
     */
    @PreAuthorize("hasAuthority('prescription.create')"
            + " and @authz.isMyCompany(#command.companyId)")
    MedicamentDto execute(CreateMedicamentCommand command);
}
