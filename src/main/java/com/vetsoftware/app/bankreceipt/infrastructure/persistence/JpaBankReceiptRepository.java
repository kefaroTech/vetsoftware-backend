package com.vetsoftware.app.bankreceipt.infrastructure.persistence;

import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBankReceiptRepository implements BankReceiptRepository {

    private final BankReceiptJpaRepository jpaRepository;
    private final BankReceiptJpaMapper mapper;

    public JpaBankReceiptRepository(BankReceiptJpaRepository jpaRepository,
            BankReceiptJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public BankReceipt save(BankReceipt receipt) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(receipt)));
    }

    @Override
    public Optional<BankReceipt> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByBankReferenceAndReceivedOn(String bankReference, LocalDate receivedOn) {
        return jpaRepository.existsByBankReferenceAndReceivedOn(bankReference, receivedOn);
    }

    @Override
    public PageResult<BankReceipt> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, masReciente())),
                mapper::toDomain);
    }

    @Override
    public PageResult<BankReceipt> findAllByStatus(BankReceiptStatus status, int page,
            int pageSize) {
        return Pages.result(
                jpaRepository.findAllByStatus(status, Pages.request(page, pageSize, masAntigua())),
                mapper::toDomain);
    }

    /**
     * Extracto completo: lo ultimo que llego primero, que es como se revisa un
     * extracto. Desempate por {@code id} descendente para que el orden sea total —
     * un extracto trae decenas de lineas con la <em>misma</em> fecha, asi que sin
     * desempate dos paginas consecutivas repetirian u omitirian filas, y aqui eso
     * significa perder una consignacion del cuadre.
     */
    private static Sort masReciente() {
        return Sort.by(Sort.Direction.DESC, "receivedOn").and(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Bandeja: <strong>ascendente</strong>, al reves que el listado completo y a
     * proposito. Lo pendiente se atiende por antiguedad —una consignacion que lleva
     * tres semanas sin explicar es un cliente que puede estar reclamando— y ordenar
     * por lo ultimo que llego la dejaria al final para siempre. El desempate por
     * {@code id} ascendente mantiene el orden total y respeta el orden de carga
     * dentro del mismo dia.
     */
    private static Sort masAntigua() {
        return Sort.by(Sort.Direction.ASC, "receivedOn").and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
