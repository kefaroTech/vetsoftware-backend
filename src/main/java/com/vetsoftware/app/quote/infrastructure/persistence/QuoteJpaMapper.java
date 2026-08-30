package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.quote.domain.CompanyRef;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.QuoteSummary;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Unico sitio que conoce a la vez el dominio de la cotizacion y su forma JPA.
 */
@Component
public class QuoteJpaMapper {

    public QuoteJpaEntity toJpa(Quote quote, CompanyJpaEntity company) {
        QuoteJpaEntity entity = new QuoteJpaEntity();
        entity.setId(quote.getId());
        entity.setQuoteNumber(quote.getQuoteNumber());
        entity.setCompany(company);
        entity.setProspectName(quote.getProspectName());
        entity.setProspectEmail(quote.getProspectEmail());
        entity.setProspectDocument(quote.getProspectDocument());
        entity.setProspectPhone(quote.getProspectPhone());
        entity.setPriceListId(quote.getPriceListId());
        entity.setBillingCycle(quote.getBillingCycle());
        entity.setSubtotalAmount(quote.getSubtotalAmount());
        entity.setDiscountAmount(quote.getDiscountAmount());
        entity.setTaxAmount(quote.getTaxAmount());
        entity.setTotalAmount(quote.getTotalAmount());
        entity.setStatus(quote.getStatus());
        entity.setValidUntil(quote.getValidUntil());
        entity.setTrialDays(quote.getTrialDays());
        entity.setAcceptedAt(quote.getAcceptedAt());
        entity.setAcceptedByEmail(quote.getAcceptedByEmail());
        entity.setAcceptedIp(quote.getAcceptedIp());
        entity.setClientRequestId(quote.getClientRequestId());
        entity.setAiProposalId(quote.getAiProposalId());
        entity.setCreatedDate(quote.getCreatedDate());
        entity.setVersion(quote.getVersion());
        entity.setEnabled(quote.isEnabled());
        entity.setLines(toJpaLines(quote.getLines()));
        return entity;
    }

    private static Set<QuoteLineJpaEntity> toJpaLines(List<QuoteLine> lines) {
        Set<QuoteLineJpaEntity> result = new LinkedHashSet<>();
        for (QuoteLine line : lines) {
            QuoteLineJpaEntity entity = new QuoteLineJpaEntity();
            entity.setId(line.getId());
            entity.setCatalogItemId(line.getCatalogItemId());
            entity.setLineNumber(line.getLineNumber());
            entity.setItemCode(line.getItemCode());
            entity.setItemName(line.getItemName());
            entity.setItemType(line.getItemType());
            entity.setTierMin(line.getTierMin());
            entity.setTierMax(line.getTierMax());
            entity.setContractedQuantity(line.getContractedQuantity());
            entity.setIncludedQuantity(line.getIncludedQuantity());
            entity.setQuantity(line.getQuantity());
            entity.setUnitAmount(line.getUnitAmount());
            entity.setDiscountPercent(line.getDiscountPercent());
            entity.setDiscountAmount(line.getDiscountAmount());
            entity.setDiscountIsConditional(line.isDiscountConditional());
            entity.setTaxRate(line.getTaxRate());
            entity.setTaxTreatment(line.getTaxTreatment());
            entity.setTaxAmount(line.getTaxAmount());
            entity.setLineTotal(line.getLineTotal());
            entity.setCreatedDate(line.getCreatedDate());
            entity.setEnabled(line.isEnabled());
            result.add(entity);
        }
        return result;
    }

    /**
     * Camino de lectura: el {@code @EntityGraph} ya hidrato la empresa y las
     * lineas.
     */
    public Quote toDomain(QuoteJpaEntity entity) {
        return toDomain(entity, companyRefOf(entity.getCompany()));
    }

    /**
     * Camino de escritura: reusa el ref ya cargado para no inicializar el proxy que
     * devolvio {@code getReferenceById}, que dispararia un SELECT extra por cada
     * guardado.
     */
    public Quote toDomain(QuoteJpaEntity entity, CompanyRef companyRef) {
        return new Quote(entity.getId(), entity.getQuoteNumber(), companyRef,
                entity.getProspectName(), entity.getProspectEmail(), entity.getProspectDocument(),
                entity.getProspectPhone(), entity.getPriceListId(), entity.getBillingCycle(),
                entity.getSubtotalAmount(), entity.getDiscountAmount(), entity.getTaxAmount(),
                entity.getTotalAmount(), entity.getStatus(), entity.getValidUntil(),
                entity.getTrialDays(), entity.getAcceptedAt(), entity.getAcceptedByEmail(),
                entity.getAcceptedIp(), entity.getClientRequestId(), entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled(), toDomainLines(entity.getLines()),
                entity.getAiProposalId());
    }

    /**
     * Proyeccion de cabecera para los listados. No toca las colecciones: si las
     * tocara, cada fila del listado dispararia dos consultas mas.
     */
    public QuoteSummary toSummary(QuoteJpaEntity entity) {
        return new QuoteSummary(entity.getId(), entity.getQuoteNumber(),
                companyRefOf(entity.getCompany()), entity.getProspectName(),
                entity.getProspectEmail(), entity.getPriceListId(), entity.getBillingCycle(),
                entity.getSubtotalAmount(), entity.getDiscountAmount(), entity.getTaxAmount(),
                entity.getTotalAmount(), entity.getStatus(), entity.getValidUntil(),
                entity.getTrialDays(), entity.getAcceptedAt(), entity.getCreatedDate(),
                entity.isEnabled());
    }

    /**
     * Null es legitimo aqui: la cotizacion a un prospecto no tiene empresa todavia.
     */
    private static CompanyRef companyRefOf(CompanyJpaEntity company) {
        return company == null
                ? null
                : new CompanyRef(company.getId(), company.getName(), company.getIdentifier());
    }

    /**
     * El orden de impresion sale de {@code line_number}, que es un dato: sin el, el
     * orden de la cotizacion impresa dependeria del orden de recuperacion, que no
     * es determinista y no es un contrato.
     */
    private static List<QuoteLine> toDomainLines(Set<QuoteLineJpaEntity> lines) {
        return lines.stream().map(e -> new QuoteLine(e.getId(), e.getLineNumber(),
                e.getCatalogItemId(), e.getItemCode(), e.getItemName(), e.getItemType(),
                e.getTierMin(), e.getTierMax(), e.getContractedQuantity(), e.getIncludedQuantity(),
                e.getQuantity(), e.getUnitAmount(), e.getDiscountPercent(), e.getDiscountAmount(),
                e.isDiscountIsConditional(), e.getTaxRate(), e.getTaxTreatment(), e.getTaxAmount(),
                e.getLineTotal(), e.getCreatedDate(), e.isEnabled()))
                .sorted(Comparator.comparingInt(QuoteLine::getLineNumber)).toList();
    }
}
