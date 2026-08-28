package com.vetsoftware.app.revenuerecognitionline.application.port.in;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindRevenueRecognitionLineUseCase {

    /**
     * Un renglon por su id.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y aqui el gate es lo unico que
     * separa esto de una fuga.</strong> Las filas de esta tabla <b>si</b>
     * pertenecen a una empresa, asi que
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} mira este metodo: un id lo
     * escribe el cliente en la URL, y un puerto que señala una fila de alguien sin
     * recibir de quien solo lo puede servir un principal cross-tenant. La
     * alternativa —abrirlo por permiso con {@code @authz.isMyCompany(#companyId)}—
     * se descarto a proposito: el reconocimiento de ingreso es contabilidad de
     * VetSoftware, no un informe de cliente.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    RevenueRecognitionLineDto findById(Long id);
}
