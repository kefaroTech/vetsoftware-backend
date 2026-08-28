package com.vetsoftware.app.withholdingcertificate.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>Aqui conviven la carga ancha y la acotada, y eso es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la ancha y no la acotada, y exime al servicio al
 * que solo llega un principal SYSTEM. Las dos escrituras de segunda pasada
 * -recibir y adjuntar el sustituto- son exactamente eso: sus puertos son
 * {@code hasRole('SYSTEM')} a secas y un principal SYSTEM no tiene empresa de
 * la que tirar, asi que {@link #findById(Long)} es su camino correcto. Todo lo
 * que puede alcanzar un empleado pasa por
 * {@link #findByIdAndCompanyId(Long, Long)}.
 *
 * <p>
 * <strong>Sin {@code delete} y sin reactivacion.</strong> La tabla no tiene
 * {@code enabled}: un certificado no se oculta, porque la ausencia del papel es
 * justo lo que hay que poder listar.
 */
public interface WithholdingCertificateRepository {

    WithholdingCertificate save(WithholdingCertificate certificate);

    /** Carga ancha. Solo la consumen los servicios cerrados a {@code SYSTEM}. */
    Optional<WithholdingCertificate> findById(Long id);

    Optional<WithholdingCertificate> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<WithholdingCertificate> findAllByCompanyId(Long companyId, int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<WithholdingCertificate> findAll(int page, int pageSize);

    /**
     * Los que vencen antes de una fecha y todavia no han llegado, de todas las
     * empresas. Solo lo consume un puerto SYSTEM: una fecha no acota un tenant.
     */
    PageResult<WithholdingCertificate> findAllMissing(LocalDate deadlineBefore, int page,
            int pageSize);

    /** El mismo barrido con la empresa encima, que es lo que ve la clinica. */
    PageResult<WithholdingCertificate> findAllMissingByCompanyId(Long companyId,
            LocalDate deadlineBefore, int page, int pageSize);
}
