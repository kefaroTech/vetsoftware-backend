package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de salida de la rodaja {@code aiproposal}.
 *
 * <p>
 * <strong>Sin una sola consulta nativa y sin un solo {@code @Query} de
 * escritura.</strong> Todo lo que hace esta clase pasa por el ciclo gestionado
 * de Hibernate, que es donde {@code @Version} protege de verdad. El barrido de
 * retencion -que si necesita {@code UPDATE} masivos y por lotes- llega en su
 * propia fase, y cuando llegue tendra que llevar {@code version = version + 1}
 * en el {@code SET} de los tres pasos ({@code UPDATE_MASIVO_MUEVE_LA_VERSION},
 * dura).
 *
 * <p>
 * <strong>Sin cargar padres.</strong> Turno y linea guardan su FK como columna
 * {@code Long}, no como {@code @ManyToOne}, asi que escribir un turno o un
 * bloque de lineas cuesta exactamente sus {@code INSERT} -ni un
 * {@code getReferenceById}, ni un proxy, ni el {@code @EntityGraph} que
 * {@code REPOS_CON_ENTITYGRAPH} exigiria si la asociacion existiera-.
 */
@Repository
public class JpaAiProposalRepository implements AiProposalRepository {

    private final AiProposalJpaRepository proposalJpaRepository;
    private final AiProposalTurnJpaRepository turnJpaRepository;
    private final AiProposalLineJpaRepository lineJpaRepository;
    private final AiProposalJpaMapper mapper;

    public JpaAiProposalRepository(AiProposalJpaRepository proposalJpaRepository,
            AiProposalTurnJpaRepository turnJpaRepository,
            AiProposalLineJpaRepository lineJpaRepository, AiProposalJpaMapper mapper) {
        this.proposalJpaRepository = proposalJpaRepository;
        this.turnJpaRepository = turnJpaRepository;
        this.lineJpaRepository = lineJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AiProposal save(AiProposal proposal) {
        return mapper.toDomain(proposalJpaRepository.save(mapper.toJpa(proposal)));
    }

    @Override
    public Optional<AiProposal> findById(Long id) {
        return proposalJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AiProposal> findByPublicToken(String publicToken) {
        if (publicToken == null || publicToken.isBlank())
            return Optional.empty();
        return proposalJpaRepository.findByPublicToken(publicToken).map(mapper::toDomain);
    }

    @Override
    public Optional<AiProposal> findByIdempotency(String contactEmail, String idempotencyKey) {
        if (contactEmail == null || contactEmail.isBlank() || idempotencyKey == null
                || idempotencyKey.isBlank())
            return Optional.empty();
        return proposalJpaRepository
                .findByIdempotencyKeyAndContactEmail(idempotencyKey, contactEmail)
                .map(mapper::toDomain);
    }

    @Override
    public ProposalTurn saveTurn(ProposalTurn turn) {
        return mapper.toDomain(turnJpaRepository.save(mapper.toJpa(turn)));
    }

    @Override
    public Optional<ProposalTurn> findTurnById(Long turnId) {
        return turnJpaRepository.findById(turnId).map(mapper::toDomain);
    }

    @Override
    public List<ProposalTurn> findTurnsByProposalId(Long proposalId) {
        return turnJpaRepository.findByProposalIdOrderByTurnNumberAsc(proposalId).stream()
                .map(mapper::toDomain).toList();
    }

    /**
     * Las lineas se guardan en bloque porque el motor las produce en bloque, y
     * porque el unico {@code (turn_id, item_code)} tiene que evaluarse sobre el
     * conjunto entero: guardarlas de una en una dejaria la mitad escrita si la
     * novena choca.
     */
    @Override
    public List<ProposalLine> saveLines(List<ProposalLine> lines) {
        if (lines == null || lines.isEmpty())
            return List.of();
        List<AiProposalLineJpaEntity> entidades = lines.stream().map(mapper::toJpa).toList();
        return lineJpaRepository.saveAll(entidades).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProposalLine> findLinesByTurnId(Long turnId) {
        return lineJpaRepository.findByTurnIdOrderBySortOrderAscIdAsc(turnId).stream()
                .map(mapper::toDomain).toList();
    }

    /**
     * Borrado logico: el {@code @SQLDelete} de la entidad lo convierte en
     * {@code UPDATE ... SET enabled = false WHERE id = ? AND version = ?}. La fila
     * no se puede borrar de verdad -las FK de turnos y lineas van
     * {@code ON DELETE RESTRICT}- y una propuesta convertida no se borra nunca.
     */
    @Override
    public void delete(Long id) {
        proposalJpaRepository.findById(id).ifPresent(proposalJpaRepository::delete);
    }
}
