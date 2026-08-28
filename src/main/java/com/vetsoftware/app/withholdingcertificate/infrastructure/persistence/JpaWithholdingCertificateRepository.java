package com.vetsoftware.app.withholdingcertificate.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWithholdingCertificateRepository implements WithholdingCertificateRepository {

    private final WithholdingCertificateJpaRepository jpaRepository;
    private final WithholdingCertificateJpaMapper mapper;

    public JpaWithholdingCertificateRepository(WithholdingCertificateJpaRepository jpaRepository,
            WithholdingCertificateJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>La rama de actualizacion recarga la fila y vuelca sobre ella, en vez
     * de construir una entidad nueva con el mismo id.</strong> Una instancia nueva
     * llega con {@code version} a nulo; Hibernate la tomaria por fila nueva, haria
     * un {@code merge} que no compara nada y el {@code @Version} de la tabla
     * dejaria de proteger justo el escenario para el que existe -recibir el papel y
     * adjuntar el sustituto son dos escrituras sobre la misma fila-.
     */
    @Override
    public WithholdingCertificate save(WithholdingCertificate certificate) {
        if (certificate.getId() == null)
            return mapper.toDomain(jpaRepository.save(mapper.toJpa(certificate)));
        WithholdingCertificateJpaEntity managed = jpaRepository.findById(certificate.getId())
                .orElseGet(WithholdingCertificateJpaEntity::new);
        mapper.apply(certificate, managed);
        return mapper.toDomain(jpaRepository.save(managed));
    }

    @Override
    public Optional<WithholdingCertificate> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<WithholdingCertificate> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<WithholdingCertificate> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    @Override
    public PageResult<WithholdingCertificate> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    @Override
    public PageResult<WithholdingCertificate> findAllMissing(LocalDate deadlineBefore, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByLegalDeadlineOnLessThanAndReceivedOnIsNull(
                deadlineBefore, Pages.request(page, pageSize, deadlineOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<WithholdingCertificate> findAllMissingByCompanyId(Long companyId,
            LocalDate deadlineBefore, int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyIdAndLegalDeadlineOnLessThanAndReceivedOnIsNull(
                        companyId, deadlineBefore, Pages.request(page, pageSize, deadlineOrder())),
                mapper::toDomain);
    }

    /**
     * Orden total y estable del expediente: lo mas reciente primero por ano
     * gravable y por fecha de expedicion, con el {@code id} de desempate. Sin
     * desempate, dos certificados del mismo ano expedidos el mismo dia -que es lo
     * normal cuando un cliente los emite todos de golpe- pueden salir en dos
     * paginas o en ninguna.
     */
    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "fiscalYear")
                .and(Sort.by(Sort.Direction.DESC, "issuedOn"))
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * El barrido de vencimientos se lee al reves: <strong>lo que vence antes va
     * primero</strong>, porque el listado existe para actuar sobre lo mas urgente y
     * quien lo mira casi nunca pasa de la primera pagina. El desempate por
     * {@code id} ascendente completa el orden -todos los de una empresa comparten
     * el mismo ultimo dia habil de marzo, asi que los empates aqui no son la
     * excepcion sino la norma-.
     */
    private static Sort deadlineOrder() {
        return Sort.by(Sort.Direction.ASC, "legalDeadlineOn")
                .and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
