package com.vetsoftware.app.companyactivitymonth.application.port.in;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDormantCompaniesUseCase {

    /**
     * <strong>El barrido por el que existe {@code ix_cam_dormant}</strong>: las
     * clinicas que en un mes dado no pasaron de {@code activeDaysThreshold} dias de
     * uso.
     *
     * <p>
     * Es la consulta que convierte la tabla en una senal accionable. Una clinica
     * con cero dias activos en el mes en curso sigue figurando como cliente al
     * corriente en todos los demas informes; aqui aparece antes de que cancele, que
     * es el unico momento en que se puede hacer algo.
     *
     * <p>
     * <strong>El indice no lleva la empresa delante, y eso no es un
     * descuido.</strong> {@code ix_cam_dormant (period_key, active_days)} responde
     * «una igualdad y un rango, en ese orden», que es exactamente la forma de esta
     * consulta. Ponerle {@code company_id} delante lo haria inutil justo para el
     * barrido, que es lo unico para lo que se creo.
     *
     * <p>
     * Y por no llevar empresa, el caso de uso solo lo puede servir
     * {@code hasRole('SYSTEM')} a secas ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     * No hay hermano acotado por empresa porque no tendria sentido: preguntarle a
     * una clinica si esta dormida es leer su propia fila, y para eso esta
     * {@code FindCompanyActivityMonthUseCase}.
     *
     * @param activeDaysThreshold
     *            umbral inclusivo de dias activos. No hay un valor universalmente
     *            correcto —«dormido» son tres dias para quien mira retencion y cero
     *            para quien mira bajas—, asi que lo trae quien pregunta
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyActivityMonthDto> listDormant(String periodKey, int activeDaysThreshold,
            int page, int pageSize);
}
