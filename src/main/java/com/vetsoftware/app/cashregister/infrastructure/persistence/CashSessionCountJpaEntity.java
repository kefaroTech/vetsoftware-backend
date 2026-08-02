package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Conteo de cierre por método: esperado vs contado (difference derivada en el dominio, persistida
 * para el reporte).
 */
@Entity
@Table(name = "cash_session_count")
public class CashSessionCountJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private CashSessionJpaEntity session;

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false, length = 10)
  private CashPaymentMethod method;

  @Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal expectedAmount;

  @Column(name = "counted_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal countedAmount;

  @Column(name = "difference", nullable = false, precision = 12, scale = 2)
  private BigDecimal difference;

  protected CashSessionCountJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CashSessionJpaEntity getSession() {
    return session;
  }

  public void setSession(CashSessionJpaEntity session) {
    this.session = session;
  }

  public CashPaymentMethod getMethod() {
    return method;
  }

  public void setMethod(CashPaymentMethod method) {
    this.method = method;
  }

  public BigDecimal getExpectedAmount() {
    return expectedAmount;
  }

  public void setExpectedAmount(BigDecimal expectedAmount) {
    this.expectedAmount = expectedAmount;
  }

  public BigDecimal getCountedAmount() {
    return countedAmount;
  }

  public void setCountedAmount(BigDecimal countedAmount) {
    this.countedAmount = countedAmount;
  }

  public BigDecimal getDifference() {
    return difference;
  }

  public void setDifference(BigDecimal difference) {
    this.difference = difference;
  }
}
