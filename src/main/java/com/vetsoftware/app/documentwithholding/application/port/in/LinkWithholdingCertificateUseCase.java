package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.command.LinkWithholdingCertificateCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface LinkWithholdingCertificateUseCase {

    /**
     * Apunta una retencion a su certificado: la unica escritura posterior que la
     * ficha admite, y la que la vuelve descontable.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> El command lleva un
     * {@code id} que alguien escribe en la URL, asi que
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} (BE-COV) obliga a una de
     * dos: recibir la empresa y revalidarla, o cerrarse a plataforma. Aqui vale la
     * segunda por lo mismo que el registro —es tesoreria quien recibe el
     * certificado del cliente y lo cruza contra lo retenido—; el {@code companyId}
     * viaja igualmente en el command, pero como filtro de la carga, no como
     * autorizacion.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    DocumentWithholdingDto execute(LinkWithholdingCertificateCommand command);
}
