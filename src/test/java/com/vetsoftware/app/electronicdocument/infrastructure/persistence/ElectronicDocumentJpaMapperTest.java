package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.BRANCH_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.CUFE;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.EMPLOYEE_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.bd;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.customer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.efectivo;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendiente;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.issuer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.notaCreditoTotal;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.unaLineaGravada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit puro, ida y vuelta dominio &lt;-&gt; entidad, sin mocks (regla de la
 * capa persistence/XxxJpaMapper del CLAUDE.md).
 */
@DisplayName("ElectronicDocumentJpaMapper")
class ElectronicDocumentJpaMapperTest {

    private final ElectronicDocumentJpaMapper mapper = new ElectronicDocumentJpaMapper();

    private static CompanyJpaEntity empresa(Long id) throws ReflectiveOperationException {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setId(id);
        return company;
    }

    private static ElectronicDocument documentoCon(IssuerSnapshot issuer,
            CustomerSnapshot customer) {
        return new ElectronicDocument(1L, COMPANY_ID, OPEN_ACCOUNT_ID,
                ElectronicDocumentType.FE_VENTA, "SETP", 990L, "18760000001",
                LocalDate.of(2026, 3, 10), "10:15:00-05:00", CUFE, "cude-1", "uuid-1", null, null,
                null, null, DianStatus.VALIDADO, LocalDateTime.of(2026, 3, 10, 10, 16), issuer,
                customer, bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                PaymentForm.CONTADO, unaLineaGravada(), efectivo("1190.00"),
                LocalDateTime.of(2026, 3, 10, 10, 15), true, null, null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, EMPLOYEE_ID, BRANCH_ID);
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("mapea cabecera, snapshots y montos al vuelo")
        void mapea_cabecera_snapshots_y_montos() throws Exception {
            ElectronicDocument doc = facturaValidada(1L);

            ElectronicDocumentJpaEntity entity = mapper.toJpa(doc, empresa(COMPANY_ID));

            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getCompany().getId()).isEqualTo(COMPANY_ID);
            assertThat(entity.getOpenAccountId()).isEqualTo(OPEN_ACCOUNT_ID);
            assertThat(entity.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(entity.getDocumentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(entity.getPrefix()).isEqualTo("SETP");
            assertThat(entity.getConsecutive()).isEqualTo(990L);
            assertThat(entity.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(entity.getCufe()).isEqualTo(CUFE);
            assertThat(entity.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(entity.getIssuerDocumentId()).isEqualTo("900123456");
            assertThat(entity.getIssuerLegalName()).isEqualTo("Veterinaria Vet SAS");
            assertThat(entity.getIssuerResponsibilities()).isEqualTo("O-13;O-15");
            assertThat(entity.getCustomerDocumentId()).isEqualTo("1020304050");
            assertThat(entity.getCustomerTaxRegime()).isEqualTo("NO_RESPONSABLE_IVA");
            assertThat(entity.getCustomerFiscalResponsibility()).isEqualTo("NO_APLICA");
            assertThat(entity.getPayableAmount()).isEqualByComparingTo("1190.00");
            assertThat(entity.getPaymentForm()).isEqualTo(PaymentForm.CONTADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getClientRequestId()).isNull();
            assertThat(entity.getIssuedByEmployeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("mapea las lineas y los pagos con el backreference al documento")
        void mapea_lineas_y_pagos_con_backreference() throws Exception {
            ElectronicDocument doc = facturaPendiente();

            ElectronicDocumentJpaEntity entity = mapper.toJpa(doc, empresa(COMPANY_ID));

            assertThat(entity.getLines()).hasSize(1);
            ElectronicDocumentLineJpaEntity linea = entity.getLines().iterator().next();
            assertThat(linea.getDocument()).isSameAs(entity);
            assertThat(linea.getLineNumber()).isEqualTo(1);
            assertThat(linea.getTaxCategory()).isEqualTo(TaxCategory.GRAVADO);
            assertThat(linea.getTaxAmount()).isEqualByComparingTo("190.00");

            assertThat(entity.getPayments()).hasSize(1);
            ElectronicDocumentPaymentJpaEntity pago = entity.getPayments().iterator().next();
            assertThat(pago.getDocument()).isSameAs(entity);
            assertThat(pago.getPaymentMeans()).isEqualTo(PaymentMeans.EFECTIVO);
            assertThat(pago.getAmount()).isEqualByComparingTo("1190.00");
        }

        @Test
        @DisplayName("responsabilidades del emisor vacias se guardan como null, no como cadena vacia")
        void responsabilidades_del_emisor_vacias_se_guardan_como_null() throws Exception {
            IssuerSnapshot sinResponsabilidades = new IssuerSnapshot("NIT", "900123456", "7",
                    "Veterinaria Vet SAS", "RESPONSABLE", "facturacion@vet.co", List.of());
            ElectronicDocument doc = documentoCon(sinResponsabilidades, customer());

            ElectronicDocumentJpaEntity entity = mapper.toJpa(doc, empresa(COMPANY_ID));

            assertThat(entity.getIssuerResponsibilities()).isNull();
        }

        @Test
        @DisplayName("regimen y responsabilidad fiscal del adquiriente ausentes quedan null")
        void regimen_y_responsabilidad_del_adquiriente_ausentes_quedan_null() throws Exception {
            CustomerSnapshot sinRegimenNiResponsabilidad = new CustomerSnapshot("CEDULA_CIUDADANIA",
                    "1020304050", "3", "NATURAL", null, "Ana Perez", "ana@correo.co", "05001", null,
                    null);
            ElectronicDocument doc = documentoCon(issuer(), sinRegimenNiResponsabilidad);

            ElectronicDocumentJpaEntity entity = mapper.toJpa(doc, empresa(COMPANY_ID));

            assertThat(entity.getCustomerTaxRegime()).isNull();
            assertThat(entity.getCustomerFiscalResponsibility()).isNull();
        }

        @Test
        @DisplayName("una factura (no una nota) no setea los campos de referencia")
        void factura_no_setea_campos_de_referencia() throws Exception {
            ElectronicDocument doc = facturaValidada(1L);

            ElectronicDocumentJpaEntity entity = mapper.toJpa(doc, empresa(COMPANY_ID));

            assertThat(entity.getReferencedCufe()).isNull();
            assertThat(entity.getReferencedPrefix()).isNull();
            assertThat(entity.getReferencedNumber()).isNull();
            assertThat(entity.getReferencedIssueDate()).isNull();
        }

        @Test
        @DisplayName("una nota credito copia la referencia al documento anulado")
        void nota_credito_copia_la_referencia() throws Exception {
            ElectronicDocument nota = notaCreditoTotal(2L);

            ElectronicDocumentJpaEntity entity = mapper.toJpa(nota, empresa(COMPANY_ID));

            assertThat(entity.getReferencedCufe()).isEqualTo(nota.getReference().cufe());
            assertThat(entity.getReferencedPrefix()).isEqualTo(nota.getReference().prefix());
            assertThat(entity.getReferencedNumber()).isEqualTo(nota.getReference().number());
            assertThat(entity.getReferencedIssueDate()).isEqualTo(nota.getReference().issueDate());
            assertThat(entity.getNoteReasonCode()).isEqualTo(nota.getNoteReasonCode());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        private ElectronicDocumentJpaEntity entidadBase() throws ReflectiveOperationException {
            ElectronicDocumentJpaEntity entity = ReflectionEntities
                    .newInstance(ElectronicDocumentJpaEntity.class);
            entity.setId(1L);
            entity.setCompany(empresa(COMPANY_ID));
            entity.setOpenAccountId(OPEN_ACCOUNT_ID);
            entity.setBranchId(BRANCH_ID);
            entity.setDocumentType(ElectronicDocumentType.FE_VENTA);
            entity.setPrefix("SETP");
            entity.setConsecutive(990L);
            entity.setResolutionNumber("18760000001");
            entity.setIssueDate(LocalDate.of(2026, 3, 10));
            entity.setIssueTime("10:15:00-05:00");
            entity.setCufe(CUFE);
            entity.setCude("cude-1");
            entity.setUuid("uuid-1");
            entity.setDianStatus(DianStatus.VALIDADO);
            entity.setDianValidationDate(LocalDateTime.of(2026, 3, 10, 10, 16));
            entity.setIssuerDocumentType("NIT");
            entity.setIssuerDocumentId("900123456");
            entity.setIssuerVerificationDigit("7");
            entity.setIssuerLegalName("Veterinaria Vet SAS");
            entity.setIssuerTaxRegime("RESPONSABLE");
            entity.setIssuerEmail("facturacion@vet.co");
            entity.setIssuerResponsibilities("O-13;O-15");
            entity.setCustomerDocumentType("CEDULA_CIUDADANIA");
            entity.setCustomerDocumentId("1020304050");
            entity.setCustomerVerificationDigit("3");
            entity.setCustomerPersonType("NATURAL");
            entity.setCustomerName("Ana Perez");
            entity.setCustomerEmail("ana@correo.co");
            entity.setCustomerCityDane("05001");
            entity.setCustomerTaxRegime("NO_RESPONSABLE_IVA");
            entity.setCustomerFiscalResponsibility("NO_APLICA");
            entity.setLineExtensionAmount(bd("1000.00"));
            entity.setTaxExclusiveAmount(bd("1000.00"));
            entity.setTaxInclusiveAmount(bd("1190.00"));
            entity.setPayableAmount(bd("1190.00"));
            entity.setReteFuenteAmount(BigDecimal.ZERO);
            entity.setReteIvaAmount(BigDecimal.ZERO);
            entity.setReteIcaAmount(BigDecimal.ZERO);
            entity.setPaymentForm(PaymentForm.CONTADO);
            entity.setCreatedDate(LocalDateTime.of(2026, 3, 10, 10, 15));
            entity.setEnabled(true);
            entity.setIssuedByEmployeeId(EMPLOYEE_ID);
            agregarLinea(entity, 1, "Gravado 1");
            agregarPago(entity);
            return entity;
        }

        private void agregarLinea(ElectronicDocumentJpaEntity entity, int numero,
                String descripcion) throws ReflectiveOperationException {
            ElectronicDocumentLineJpaEntity linea = ReflectionEntities
                    .newInstance(ElectronicDocumentLineJpaEntity.class);
            linea.setDocument(entity);
            linea.setLineNumber(numero);
            linea.setDescription(descripcion);
            linea.setQuantity(BigDecimal.ONE);
            linea.setUnitMeasureCode("94");
            linea.setUnitPrice(bd("1000.00"));
            linea.setLineExtensionAmount(bd("1000.00"));
            linea.setTaxCategory(TaxCategory.GRAVADO);
            linea.setTaxScheme(TaxScheme.IVA);
            linea.setTaxRate(bd("19"));
            linea.setTaxAmount(bd("190.00"));
            linea.setTotalAmount(bd("1190.00"));
            entity.getLines().add(linea);
        }

        private void agregarPago(ElectronicDocumentJpaEntity entity)
                throws ReflectiveOperationException {
            ElectronicDocumentPaymentJpaEntity pago = ReflectionEntities
                    .newInstance(ElectronicDocumentPaymentJpaEntity.class);
            pago.setDocument(entity);
            pago.setPaymentMeans(PaymentMeans.EFECTIVO);
            pago.setAmount(bd("1190.00"));
            entity.getPayments().add(pago);
        }

        @Test
        @DisplayName("reconstruye cabecera, snapshots y montos desde la entidad")
        void reconstruye_cabecera_snapshots_y_montos() throws Exception {
            ElectronicDocument doc = mapper.toDomain(entidadBase());

            assertThat(doc.getId()).isEqualTo(1L);
            assertThat(doc.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(doc.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(doc.getDocumentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(doc.getCufe()).isEqualTo(CUFE);
            assertThat(doc.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(doc.getIssuer().documentId()).isEqualTo("900123456");
            assertThat(doc.getIssuer().responsibilities()).containsExactly("O-13", "O-15");
            assertThat(doc.getCustomer().documentId()).isEqualTo("1020304050");
            assertThat(doc.getCustomer().taxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(doc.getCustomer().fiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.NO_APLICA);
            assertThat(doc.getPayableAmount()).isEqualByComparingTo("1190.00");
            assertThat(doc.getIssuedByEmployeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("sin company asociada, el mapeo falla porque el dominio exige companyId")
        void sin_company_el_dominio_rechaza_companyId_nulo() throws Exception {
            ElectronicDocumentJpaEntity entity = entidadBase();
            entity.setCompany(null);

            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("ordena las lineas por lineNumber aunque el set las entregue en otro orden")
        void ordena_las_lineas_por_lineNumber() throws Exception {
            ElectronicDocumentJpaEntity entity = ReflectionEntities
                    .newInstance(ElectronicDocumentJpaEntity.class);
            entity.setId(1L);
            entity.setCompany(empresa(COMPANY_ID));
            entity.setOpenAccountId(OPEN_ACCOUNT_ID);
            entity.setBranchId(BRANCH_ID);
            entity.setDocumentType(ElectronicDocumentType.FE_VENTA);
            entity.setIssueDate(LocalDate.of(2026, 3, 10));
            entity.setIssueTime("10:15:00-05:00");
            entity.setDianStatus(DianStatus.PENDIENTE);
            entity.setIssuerDocumentId("900123456");
            entity.setIssuerLegalName("Veterinaria Vet SAS");
            entity.setCustomerDocumentId("1020304050");
            entity.setCustomerName("Ana Perez");
            entity.setLineExtensionAmount(bd("3000.00"));
            entity.setTaxExclusiveAmount(bd("3000.00"));
            entity.setTaxInclusiveAmount(bd("3570.00"));
            entity.setPayableAmount(bd("3570.00"));
            entity.setReteFuenteAmount(BigDecimal.ZERO);
            entity.setReteIvaAmount(BigDecimal.ZERO);
            entity.setReteIcaAmount(BigDecimal.ZERO);
            entity.setPaymentForm(PaymentForm.CONTADO);
            entity.setCreatedDate(LocalDateTime.of(2026, 3, 10, 10, 15));
            entity.setEnabled(true);
            agregarLinea(entity, 3, "Tercera");
            agregarLinea(entity, 1, "Primera");
            agregarLinea(entity, 2, "Segunda");

            ElectronicDocument doc = mapper.toDomain(entity);

            assertThat(doc.getLines()).extracting("lineNumber").containsExactly(1, 2, 3);
            assertThat(doc.getLines()).extracting("description").containsExactly("Primera",
                    "Segunda", "Tercera");
        }

        @Test
        @DisplayName("regimen y responsabilidad fiscal desconocidos o vacios resuelven a null")
        void regimen_y_responsabilidad_vacios_resuelven_a_null() throws Exception {
            ElectronicDocumentJpaEntity entity = entidadBase();
            entity.setCustomerTaxRegime(null);
            entity.setCustomerFiscalResponsibility("");

            ElectronicDocument doc = mapper.toDomain(entity);

            assertThat(doc.getCustomer().taxRegime()).isNull();
            assertThat(doc.getCustomer().fiscalResponsibility()).isNull();
        }

        @Test
        @DisplayName("sin cufe referenciado, el documento no lleva referencia (no es nota)")
        void sin_cufe_referenciado_no_hay_referencia() throws Exception {
            ElectronicDocument doc = mapper.toDomain(entidadBase());

            assertThat(doc.getReference()).isNull();
        }

        @Test
        @DisplayName("codigos de responsabilidad con espacios alrededor del separador se recortan")
        void codigos_de_responsabilidad_se_recortan() throws Exception {
            ElectronicDocumentJpaEntity entity = entidadBase();
            entity.setIssuerResponsibilities(" O-13 ; O-15 ");

            ElectronicDocument doc = mapper.toDomain(entity);

            assertThat(doc.getIssuer().responsibilities()).containsExactly("O-13", "O-15");
        }

        @Test
        @DisplayName("responsabilidades en blanco no producen un codigo vacio")
        void responsabilidades_en_blanco_quedan_vacias() throws Exception {
            ElectronicDocumentJpaEntity entity = entidadBase();
            entity.setIssuerResponsibilities("   ");

            ElectronicDocument doc = mapper.toDomain(entity);

            assertThat(doc.getIssuer().responsibilities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ida y vuelta dominio -> entidad -> dominio")
    class IdaYVuelta {

        @Test
        @DisplayName("una factura validada sobrevive el viaje redondo sin perder datos")
        void factura_sobrevive_el_viaje_redondo() throws Exception {
            ElectronicDocument original = facturaValidada(1L);

            ElectronicDocumentJpaEntity entity = mapper.toJpa(original, empresa(COMPANY_ID));
            ElectronicDocument reconstruido = mapper.toDomain(entity);

            assertThat(reconstruido.getId()).isEqualTo(original.getId());
            assertThat(reconstruido.getCompanyId()).isEqualTo(original.getCompanyId());
            assertThat(reconstruido.getCufe()).isEqualTo(original.getCufe());
            assertThat(reconstruido.getPayableAmount())
                    .isEqualByComparingTo(original.getPayableAmount());
            assertThat(reconstruido.getIssuer()).isEqualTo(original.getIssuer());
            assertThat(reconstruido.getCustomer()).isEqualTo(original.getCustomer());
            assertThat(reconstruido.getLines()).hasSameSizeAs(original.getLines());
        }

        @Test
        @DisplayName("una nota credito conserva su referencia tras el viaje redondo")
        void nota_credito_conserva_su_referencia() throws Exception {
            ElectronicDocument original = notaCreditoTotal(2L);

            ElectronicDocumentJpaEntity entity = mapper.toJpa(original, empresa(COMPANY_ID));
            ElectronicDocument reconstruido = mapper.toDomain(entity);

            assertThat(reconstruido.getReference()).isEqualTo(original.getReference());
            assertThat(reconstruido.getNoteReasonCode()).isEqualTo(original.getNoteReasonCode());
            assertThat(reconstruido.isReversed()).isEqualTo(original.isReversed());
        }
    }
}
