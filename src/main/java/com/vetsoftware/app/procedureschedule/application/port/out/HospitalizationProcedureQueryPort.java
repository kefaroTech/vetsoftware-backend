package com.vetsoftware.app.procedureschedule.application.port.out;

import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import java.util.Optional;

public interface HospitalizationProcedureQueryPort {
    Optional<ProcedureOrderParams> findById(Long hospitalizationProcedureId);

    /**
     * La ejecucion no tiene empresa propia: su unico vinculo con un tenant es la
     * orden de procedimiento, y la empresa cuelga de la hospitalizacion padre de
     * esa orden. Acotar por {@code hospitalization_procedure_id} NO prueba nada —es
     * una FK ajena, el paciente es de alguien—, asi que este es el finder que sube
     * hasta la empresa y el unico que autoriza escribir sobre una ejecucion.
     */
    Optional<ProcedureOrderParams> findByIdAndCompanyId(Long hospitalizationProcedureId,
            Long companyId);
}
