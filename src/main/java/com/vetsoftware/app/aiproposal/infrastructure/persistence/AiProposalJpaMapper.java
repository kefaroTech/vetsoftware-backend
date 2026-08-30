package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y las tres entidades
 * JPA de la rodaja.
 *
 * <p>
 * <strong>Ninguna de las tres traducciones necesita cargar un padre.</strong>
 * Turno y linea guardan su FK como {@code Long} y no como {@code @ManyToOne}
 * -ver el javadoc de sus entidades-, asi que escribir cuesta exactamente sus
 * {@code INSERT}: ni un {@code SELECT} de mas, ni un proxy que inicializar, ni
 * un {@code @EntityGraph} que mantener.
 */
@Component
public class AiProposalJpaMapper {

    public AiProposalJpaEntity toJpa(AiProposal proposal) {
        AiProposalJpaEntity entity = new AiProposalJpaEntity();
        entity.setId(proposal.getId());
        entity.setPublicToken(proposal.getPublicToken());
        entity.setStatus(proposal.getStatus());
        entity.setPriceListId(proposal.getPriceListId());
        entity.setBillingCycle(proposal.getBillingCycle());
        entity.setCatalogSnapshotHash(proposal.getCatalogSnapshotHash());
        entity.setPrivacyNoticeVersionId(proposal.getPrivacyNoticeVersionId());
        entity.setIdempotencyKey(proposal.getIdempotencyKey());
        entity.setContactEmail(proposal.getContactEmail());
        entity.setLocale(proposal.getLocale());
        entity.setTurnCount(proposal.getTurnCount());
        entity.setTotalInputTokens(proposal.getTotalInputTokens());
        entity.setTotalOutputTokens(proposal.getTotalOutputTokens());
        entity.setFirstSeenAt(proposal.getFirstSeenAt());
        entity.setLastActivityAt(proposal.getLastActivityAt());
        entity.setExpiresAt(proposal.getExpiresAt());
        entity.setAnonymizedAt(proposal.getAnonymizedAt());
        entity.setCreatedDate(proposal.getCreatedDate());
        entity.setVersion(proposal.getVersion());
        entity.setEnabled(proposal.isEnabled());
        return entity;
    }

    public AiProposal toDomain(AiProposalJpaEntity entity) {
        return new AiProposal(entity.getId(), entity.getPublicToken(), entity.getStatus(),
                entity.getPriceListId(), entity.getBillingCycle(), entity.getCatalogSnapshotHash(),
                entity.getPrivacyNoticeVersionId(), entity.getIdempotencyKey(),
                entity.getContactEmail(), entity.getLocale(), entity.getTurnCount(),
                entity.getTotalInputTokens(), entity.getTotalOutputTokens(),
                entity.getFirstSeenAt(), entity.getLastActivityAt(), entity.getExpiresAt(),
                entity.getAnonymizedAt(), entity.getCreatedDate(), entity.getVersion(),
                entity.isEnabled());
    }

    public AiProposalTurnJpaEntity toJpa(ProposalTurn turn) {
        AiProposalTurnJpaEntity entity = new AiProposalTurnJpaEntity();
        entity.setId(turn.getId());
        entity.setProposalId(turn.getProposalId());
        entity.setTurnNumber(turn.getTurnNumber());
        entity.setTurnType(turn.getTurnType());
        entity.setStatus(turn.getStatus());
        entity.setInputText(turn.getInputText());
        entity.setInputTextChars(turn.getInputTextChars());
        entity.setModelId(turn.getModelId());
        entity.setPromptVersion(turn.getPromptVersion());
        entity.setInputTokens(turn.getInputTokens());
        entity.setOutputTokens(turn.getOutputTokens());
        entity.setLatencyMs(turn.getLatencyMs());
        entity.setStopReason(turn.getStopReason());
        entity.setRawResponse(turn.getRawResponse());
        entity.setFailureCode(turn.getFailureCode());
        entity.setPresentation(turn.getPresentation());
        entity.setClientRequestId(turn.getClientRequestId());
        entity.setCreatedDate(turn.getCreatedDate());
        entity.setCompletedAt(turn.getCompletedAt());
        entity.setVersion(turn.getVersion());
        return entity;
    }

    public ProposalTurn toDomain(AiProposalTurnJpaEntity entity) {
        return new ProposalTurn(entity.getId(), entity.getProposalId(), entity.getTurnNumber(),
                entity.getTurnType(), entity.getStatus(), entity.getInputText(),
                entity.getInputTextChars(), entity.getModelId(), entity.getPromptVersion(),
                entity.getInputTokens(), entity.getOutputTokens(), entity.getLatencyMs(),
                entity.getStopReason(), entity.getRawResponse(), entity.getFailureCode(),
                entity.getPresentation(), entity.getClientRequestId(), entity.getCreatedDate(),
                entity.getCompletedAt(), entity.getVersion());
    }

    public AiProposalLineJpaEntity toJpa(ProposalLine line) {
        AiProposalLineJpaEntity entity = new AiProposalLineJpaEntity();
        entity.setId(line.getId());
        entity.setTurnId(line.getTurnId());
        entity.setItemCode(line.getItemCode());
        entity.setCatalogItemId(line.getCatalogItemId());
        entity.setAction(line.getAction());
        entity.setSource(line.getSource());
        entity.setVerdict(line.getVerdict());
        entity.setQuantity(line.getQuantity());
        entity.setUnitAmount(line.getUnitAmount());
        entity.setReason(line.getReason());
        entity.setReasonRedactedAt(line.getReasonRedactedAt());
        entity.setSortOrder(line.getSortOrder());
        entity.setCreatedDate(line.getCreatedDate());
        entity.setVersion(line.getVersion());
        return entity;
    }

    public ProposalLine toDomain(AiProposalLineJpaEntity entity) {
        return new ProposalLine(entity.getId(), entity.getTurnId(), entity.getItemCode(),
                entity.getCatalogItemId(), entity.getAction(), entity.getSource(),
                entity.getVerdict(), entity.getQuantity(), entity.getUnitAmount(),
                entity.getReason(), entity.getReasonRedactedAt(), entity.getSortOrder(),
                entity.getCreatedDate(), entity.getVersion());
    }
}
