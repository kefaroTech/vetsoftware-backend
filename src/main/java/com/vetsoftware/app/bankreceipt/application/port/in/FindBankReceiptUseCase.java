package com.vetsoftware.app.bankreceipt.application.port.in;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBankReceiptUseCase {

    /**
     * <strong>La lectura por id tambien va cerrada a {@code hasRole('SYSTEM')} a
     * secas, sin la alternativa por permiso que llevan las lecturas de otros
     * bloques.</strong> Un {@code id} lo escribe el cliente en la URL y aqui no hay
     * ningun {@code companyId} con el que acotar la fila —la tabla no tiene
     * empresa—, asi que abrirla por {@code hasAuthority} daria a cualquier empleado
     * autenticado el importe, la fecha y la referencia de cualquier consignacion
     * que haya recibido la plataforma, incluidas las de sus competidoras.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BankReceiptDto findById(Long id);
}
