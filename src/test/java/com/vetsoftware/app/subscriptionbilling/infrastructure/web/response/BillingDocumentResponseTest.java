package com.vetsoftware.app.subscriptionbilling.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BillingDocumentResponse.from")
class BillingDocumentResponseTest {

    @Test
    @DisplayName("expone companyId sin confundirlo con documento o suscripcion")
    void expone_company_id_del_documento() {
        BillingDocumentDto dto = new BillingDocumentDto(7L, "DC-2026-0007", 42L, 91L,
                DocumentKind.INVOICE, BillingReason.RECURRING_CYCLE, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), IssueStatus.DRAFT, null, null, null, null, null, null,
                null, LocalDate.of(2026, 9, 5), new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), new BigDecimal("119000.00"), BigDecimal.ZERO,
                new BigDecimal("119000.00"), List.of(), LocalDateTime.of(2026, 8, 31, 23, 0), 0L);

        BillingDocumentResponse response = BillingDocumentResponse.from(dto);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.companyId()).isEqualTo(42L);
        assertThat(response.documentNumber()).isEqualTo("DC-2026-0007");
        assertThat(response.subscriptionId()).isEqualTo(91L);
    }
}
