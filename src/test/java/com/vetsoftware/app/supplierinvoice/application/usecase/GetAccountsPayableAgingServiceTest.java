package com.vetsoftware.app.supplierinvoice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.AccountsPayableAgingDto;
import com.vetsoftware.app.supplierinvoice.application.dto.PageResult;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Test del aging: reparte el saldo de las facturas pendientes en los tramos según los días vencidos
 * vs asOf.
 */
class GetAccountsPayableAgingServiceTest {

  private static final CompanyRef CO = new CompanyRef(1L, "Vet SAS", "900123456-7");
  private static final BranchRef BR = new BranchRef(10L, "Principal");
  private static final SupplierRef SUP = new SupplierRef(5L, "Distribuidora", "800111222-3");
  private static final LocalDate AS_OF = LocalDate.of(2026, 7, 31);

  private static SupplierInvoice inv(String number, LocalDate due, String amount) {
    LocalDate issue = due.minusDays(30);
    return SupplierInvoice.create(
        CO,
        BR,
        SUP,
        null,
        null,
        number,
        issue,
        due,
        new BigDecimal(amount),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        7L);
  }

  /** Repo que solo responde findOutstandingByCompany; el resto no se usa aquí. */
  private SupplierInvoiceRepository repoWith(List<SupplierInvoice> outstanding) {
    return new SupplierInvoiceRepository() {
      public SupplierInvoice save(SupplierInvoice i) {
        return i;
      }

      public Optional<SupplierInvoice> findByIdAndCompanyId(Long id, Long companyId) {
        return Optional.empty();
      }

      public PageResult<SupplierInvoice> search(SearchSupplierInvoicesCommand c) {
        return new PageResult<>(List.of(), 0, 20, 0, 0);
      }

      public List<SupplierInvoice> findOutstandingByCompany(Long companyId, Long branchId) {
        return outstanding;
      }

      public boolean existsByCompanySupplierAndNumber(Long c, Long s, String n) {
        return false;
      }

      public void delete(Long id) {}
    };
  }

  @Test
  void reparte_saldos_por_tramo_de_antiguedad() {
    List<SupplierInvoice> outstanding =
        List.of(
            inv("A", LocalDate.of(2026, 8, 15), "1000"), // futuro → al día
            inv("B", LocalDate.of(2026, 7, 20), "500"), // 11 días vencida → 1–30
            inv("C", LocalDate.of(2026, 4, 1), "300")); // >90 días → +90
    var service = new GetAccountsPayableAgingService(repoWith(outstanding));

    AccountsPayableAgingDto dto = service.get(1L, null, AS_OF);

    assertThat(dto.asOf()).isEqualTo(AS_OF);
    assertThat(dto.suppliers()).hasSize(1);
    AccountsPayableAgingDto.Bucket b = dto.suppliers().get(0).bucket();
    assertThat(b.current()).isEqualByComparingTo("1000");
    assertThat(b.days1to30()).isEqualByComparingTo("500");
    assertThat(b.days31to60()).isEqualByComparingTo("0");
    assertThat(b.over90()).isEqualByComparingTo("300");
    assertThat(b.total()).isEqualByComparingTo("1800");
    assertThat(dto.totals().total()).isEqualByComparingTo("1800");
  }

  @Test
  void sin_pendientes_devuelve_totales_en_cero() {
    var service = new GetAccountsPayableAgingService(repoWith(List.of()));
    AccountsPayableAgingDto dto = service.get(1L, null, AS_OF);
    assertThat(dto.suppliers()).isEmpty();
    assertThat(dto.totals().total()).isEqualByComparingTo("0");
  }
}
