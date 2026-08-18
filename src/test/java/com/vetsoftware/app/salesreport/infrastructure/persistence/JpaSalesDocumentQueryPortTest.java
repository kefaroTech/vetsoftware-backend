package com.vetsoftware.app.salesreport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaEntity;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaRepository;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentLineJpaEntity;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentPaymentJpaEntity;
import com.vetsoftware.app.salesreport.application.port.out.SalesDocumentQueryPort.SalesDocumentView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador fino sobre {@code ElectronicDocumentJpaRepository}: no ejecuta SQL
 * propio (por eso Mockito y no {@code @DataJpaTest}), pero si concentra toda la
 * logica de filtrado por rango, orden y el mapeo de enums del dominio ajeno a
 * texto. Las entidades JPA de electronicdocument tienen constructor protegido
 * en otro paquete, asi que se mockean, igual que hace
 * {@code JpaOwnerQueryPortTest} con {@code OwnerJpaEntity}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSalesDocumentQueryPort — adaptador sobre ElectronicDocumentJpaRepository")
class JpaSalesDocumentQueryPortTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long BRANCH_ID = 31L;
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    @Mock
    private ElectronicDocumentJpaRepository documentJpaRepository;

    @InjectMocks
    private JpaSalesDocumentQueryPort port;

    /** Documento fuera de rango: solo se consulta su fecha antes de descartarlo. */
    private static ElectronicDocumentJpaEntity documentoConFecha(LocalDate issueDate) {
        ElectronicDocumentJpaEntity e = mock(ElectronicDocumentJpaEntity.class);
        when(e.getIssueDate()).thenReturn(issueDate);
        return e;
    }

    /**
     * Documento que va a pasar el filtro de rango: toView() recorre todos sus
     * getters.
     */
    private static ElectronicDocumentJpaEntity documentoEnRango(Long id, LocalDate issueDate,
            ElectronicDocumentType documentType, DianStatus dianStatus,
            Set<ElectronicDocumentLineJpaEntity> lines,
            Set<ElectronicDocumentPaymentJpaEntity> payments) {
        ElectronicDocumentJpaEntity e = mock(ElectronicDocumentJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getIssueDate()).thenReturn(issueDate);
        when(e.getDocumentType()).thenReturn(documentType);
        when(e.getPrefix()).thenReturn("SETP");
        when(e.getConsecutive()).thenReturn(990L + id);
        when(e.getCustomerDocumentId()).thenReturn("900123456");
        when(e.getCustomerName()).thenReturn("Clinica Norte");
        when(e.getDianStatus()).thenReturn(dianStatus);
        when(e.getCufe()).thenReturn("cufe-" + id);
        when(e.getCude()).thenReturn("cude-" + id);
        when(e.getLineExtensionAmount()).thenReturn(new BigDecimal("100000.00"));
        when(e.getTaxInclusiveAmount()).thenReturn(new BigDecimal("119000.00"));
        when(e.getPayableAmount()).thenReturn(new BigDecimal("119000.00"));
        when(e.getReteFuenteAmount()).thenReturn(BigDecimal.ZERO);
        when(e.getReteIvaAmount()).thenReturn(BigDecimal.ZERO);
        when(e.getReteIcaAmount()).thenReturn(BigDecimal.ZERO);
        when(e.getLines()).thenReturn(lines);
        when(e.getPayments()).thenReturn(payments);
        return e;
    }

    private static ElectronicDocumentLineJpaEntity lineaConEsquema(TaxScheme scheme,
            BigDecimal rate, BigDecimal base, BigDecimal tax) {
        ElectronicDocumentLineJpaEntity l = mock(ElectronicDocumentLineJpaEntity.class);
        when(l.getTaxScheme()).thenReturn(scheme);
        if (scheme != null) {
            when(l.getTaxRate()).thenReturn(rate);
            when(l.getLineExtensionAmount()).thenReturn(base);
            when(l.getTaxAmount()).thenReturn(tax);
        }
        return l;
    }

    private static ElectronicDocumentPaymentJpaEntity pagoCon(PaymentMeans medio,
            BigDecimal monto) {
        ElectronicDocumentPaymentJpaEntity p = mock(ElectronicDocumentPaymentJpaEntity.class);
        when(p.getPaymentMeans()).thenReturn(medio);
        when(p.getAmount()).thenReturn(monto);
        return p;
    }

    @Nested
    @DisplayName("Agrupamientos vacios")
    class AgrupamientosVacios {

        @Test
        @DisplayName("sin documentos en el repositorio la lista es vacia, no null")
        void sin_documentos_en_el_repositorio_la_lista_es_vacia() {
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of());

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("Rango de fechas")
    class RangoDeFechas {

        @Test
        @DisplayName("un documento con fecha anterior al inicio del rango no aparece")
        void un_documento_antes_del_rango_no_aparece() {
            ElectronicDocumentJpaEntity fueraDeRango = documentoConFecha(
                    LocalDate.of(2025, 12, 31));
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(fueraDeRango));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("un documento con fecha posterior al fin del rango no aparece")
        void un_documento_despues_del_rango_no_aparece() {
            ElectronicDocumentJpaEntity fueraDeRango = documentoConFecha(LocalDate.of(2026, 2, 1));
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(fueraDeRango));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("un documento sin fecha de emision nunca entra en el rango")
        void un_documento_sin_fecha_nunca_entra_en_el_rango() {
            ElectronicDocumentJpaEntity sinFecha = documentoConFecha(null);
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(sinFecha));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("un rango invertido (to antes que from) excluye cualquier documento")
        void un_rango_invertido_excluye_cualquier_documento() {
            // Ningun 'issueDate' puede ser a la vez >= HASTA y <= DESDE: el filtro
            // degrada en silencio a lista vacia, no lanza. Confirma a nivel de
            // adaptador el mismo hueco que documentan los tests del use case.
            ElectronicDocumentJpaEntity documento = documentoConFecha(LocalDate.of(2026, 1, 15));
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, HASTA,
                    DESDE, BRANCH_ID);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("sin 'from' el rango trae todo lo emitido hasta 'to'")
        void sin_from_trae_todo_hasta_to() {
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, LocalDate.of(2020, 1, 1),
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, null,
                    HASTA, BRANCH_ID);

            assertThat(resultado).extracting(SalesDocumentView::id).containsExactly(1L);
        }

        @Test
        @DisplayName("sin 'to' el rango trae todo lo emitido desde 'from'")
        void sin_to_trae_todo_desde_from() {
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, LocalDate.of(2030, 1, 1),
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    null, BRANCH_ID);

            assertThat(resultado).extracting(SalesDocumentView::id).containsExactly(1L);
        }

        @Test
        @DisplayName("los documentos se devuelven ordenados por fecha de emision ascendente")
        void los_documentos_se_ordenan_por_fecha_ascendente() {
            ElectronicDocumentJpaEntity tardio = documentoEnRango(2L, LocalDate.of(2026, 1, 20),
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, Set.of(), Set.of());
            ElectronicDocumentJpaEntity temprano = documentoEnRango(1L, LocalDate.of(2026, 1, 5),
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(tardio, temprano));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado).extracting(SalesDocumentView::id).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("el mismo documento repetido por el repositorio no se duplica en el resultado")
        void el_mismo_documento_repetido_no_se_duplica() {
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, DESDE,
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento, documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Mapeo de la vista")
    class MapeoDeLaVista {

        @Test
        @DisplayName("un documento sin tipo asignado mapea el tipo como null")
        void un_documento_sin_tipo_mapea_el_tipo_como_null() {
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, DESDE, null,
                    DianStatus.VALIDADO, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado.get(0).documentType()).isNull();
        }

        @Test
        @DisplayName("un documento sin estado dian mapea el estado como null")
        void un_documento_sin_estado_dian_mapea_el_estado_como_null() {
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, DESDE,
                    ElectronicDocumentType.FE_VENTA, null, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado.get(0).dianStatus()).isNull();
        }

        @Test
        @DisplayName("el tipo y el estado se mapean a su nombre cuando estan presentes")
        void el_tipo_y_el_estado_se_mapean_a_su_nombre() {
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, DESDE,
                    ElectronicDocumentType.NOTA_CREDITO, DianStatus.RECHAZADO, Set.of(), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado.get(0).documentType()).isEqualTo("NOTA_CREDITO");
            assertThat(resultado.get(0).dianStatus()).isEqualTo("RECHAZADO");
        }

        @Test
        @DisplayName("una linea sin esquema tributario no entra en el desglose fiscal")
        void una_linea_sin_esquema_tributario_no_entra_en_el_desglose() {
            ElectronicDocumentLineJpaEntity sinEsquema = lineaConEsquema(null, null, null, null);
            ElectronicDocumentLineJpaEntity conIva = lineaConEsquema(TaxScheme.IVA,
                    new BigDecimal("19.00"), new BigDecimal("100000.00"),
                    new BigDecimal("19000.00"));
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, DESDE,
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO,
                    Set.of(sinEsquema, conIva), Set.of());
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado.get(0).taxLines()).hasSize(1);
            assertThat(resultado.get(0).taxLines().get(0).taxScheme()).isEqualTo("IVA");
        }

        @Test
        @DisplayName("cada pago se mapea con su codigo dian")
        void cada_pago_se_mapea_con_su_codigo_dian() {
            ElectronicDocumentPaymentJpaEntity pago = pagoCon(PaymentMeans.TARJETA_CREDITO,
                    new BigDecimal("119000.00"));
            ElectronicDocumentJpaEntity documento = documentoEnRango(1L, DESDE,
                    ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, Set.of(), Set.of(pago));
            when(documentJpaRepository.findByCompanyIdAndOptionalBranch(COMPANY_ID, BRANCH_ID))
                    .thenReturn(List.of(documento));

            List<SalesDocumentView> resultado = port.findByCompanyAndDateRange(COMPANY_ID, DESDE,
                    HASTA, BRANCH_ID);

            assertThat(resultado.get(0).paymentLines()).hasSize(1);
            assertThat(resultado.get(0).paymentLines().get(0).paymentMeans())
                    .isEqualTo("TARJETA_CREDITO");
            assertThat(resultado.get(0).paymentLines().get(0).dianCode()).isEqualTo("49");
        }
    }
}
