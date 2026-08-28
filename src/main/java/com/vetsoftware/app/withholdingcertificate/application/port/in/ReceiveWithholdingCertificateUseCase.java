package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.withholdingcertificate.application.command.ReceiveWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReceiveWithholdingCertificateUseCase {

    /**
     * Registra que el certificado llego: su fecha y el archivo guardado.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y no es una eleccion de estilo: es
     * la unica salida que deja {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}
     * (BE-COV).</strong> El command lleva un {@code id} que el cliente escribe en
     * la URL y <em>no</em> lleva {@code companyId}, asi que nada en la peticion
     * dice de quien es la fila; abrirlo por {@code hasAuthority} dejaria a
     * cualquier empleado cerrar el expediente de otra clinica escribiendo su id.
     *
     * <p>
     * La alternativa -recibir tambien la empresa y validarla con
     * {@code @authz.isMyCompany}- se descarto por lo mismo que la escritura de
     * {@link RegisterWithholdingCertificateUseCase}: quien afirma que el papel
     * llego es tesoreria, no el beneficiario de la retencion.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    WithholdingCertificateDto execute(ReceiveWithholdingCertificateCommand command);
}
