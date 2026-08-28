package com.vetsoftware.app.supplierwithholding.application.usecase;

import com.vetsoftware.app.supplierwithholding.application.command.RegisterSupplierWithholdingPaymentCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.in.RegisterSupplierWithholdingPaymentUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota la prueba de la consignacion de lo retenido.
 *
 * <p>
 * <strong>A diferencia del certificado, este si se puede reescribir</strong>, y
 * es deliberado: el acuse de la consignacion llega tarde y a veces corregido
 * por el banco. Lo que no se puede reescribir es el numero del certificado,
 * porque ese ya esta en manos del proveedor.
 *
 * <p>
 * Leer, modificar y guardar dentro de una transaccion, con {@code @Version} de
 * por medio.
 */
@Observed(name = "supplier.withholding.payment.register")
@Service
public class RegisterSupplierWithholdingPaymentService
        implements
            RegisterSupplierWithholdingPaymentUseCase {

    private final SupplierWithholdingRepository repository;

    public RegisterSupplierWithholdingPaymentService(SupplierWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SupplierWithholdingDto execute(RegisterSupplierWithholdingPaymentCommand command) {
        SupplierWithholding withholding = repository.findById(command.id())
                .orElseThrow(() -> new SupplierWithholdingNotFoundException(command.id()));
        return SupplierWithholdingDto.from(
                repository.save(withholding.registerPaymentReceipt(command.paymentReceiptRef())));
    }
}
