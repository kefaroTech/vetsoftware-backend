package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ReactivateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.charge.open.account.reactivate")
@Service
public class ReactivateProductChargeOpenAccountService
        implements
            ReactivateProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final OpenAccountRefresher refresher;

    public ReactivateProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.refresher = refresher;
    }

    /**
     * Reactivar un cargo es una operacion de DINERO: vuelve a sumarlo al total de
     * la cuenta, porque {@code sumChargesByOpenAccountId} filtra
     * {@code enabled = true}. Por eso sigue el mismo orden que anular y editar
     * —resolver la cuenta, bloquearla, comprobar que sigue abierta y solo entonces
     * mutar— y no el atajo de un UPDATE a ciegas.
     */
    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(Long id, Long companyId) {
        // El cargo a reactivar esta deshabilitado y el @SQLRestriction("enabled =
        // true") de la entidad lo esconde de TODOS los finders JPA, asi que la unica
        // forma de saber que cuenta hay que bloquear antes de encenderlo es la consulta
        // nativa acotada por empresa. Ese era el motivo real de que este caso de uso
        // reactivara primero y preguntara despues.
        Long openAccountId = repository.findOpenAccountIdIncludingDisabled(id, companyId)
                .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(id));
        // Lock pesimista de la cuenta antes de leer su estado, igual que en la
        // anulacion: cierra el TOCTOU entre el isOpen de la linea siguiente y el
        // UPDATE.
        // Sin el, la cuenta se cierra entre la comprobacion y la reactivacion y el
        // cargo
        // revive igual sobre una cuenta ya cerrada.
        openAccountQueryPort.lockForUpdate(openAccountId, companyId);
        // LA GUARDA QUE FALTABA (#239). Reactivar sobre una cuenta CLOSE sube su total
        // y rompe la invariante contable de changeStatus —saldo cero al cerrar—, que
        // SOLO se comprueba al cerrar y nunca despues: la cuenta queda cerrada y con
        // saldo pendiente que ya nadie puede cobrar. Como no deja el saldo en negativo,
        // la guarda de OpenAccount.recalculate tampoco lo ve, asi que se corrompe en
        // silencio y aparece al descuadrar semanas despues. Mismo error que usan las
        // demas operaciones de dinero cuando la cuenta no admite cambios.
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new ProductChargeOpenAccountNotFoundException(id);
        ProductChargeOpenAccount charge = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(id));
        refresher.refresh(companyId, openAccountId);
        return ProductChargeOpenAccountDto.from(charge);
    }
}
