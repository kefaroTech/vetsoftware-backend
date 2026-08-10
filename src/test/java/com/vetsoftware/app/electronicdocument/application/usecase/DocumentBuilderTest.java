package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.bd;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.customer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.efectivo;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.issuer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.ivaLine;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.unaLineaGravada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort.SaleSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Nucleo de construccion del documento desde una cuenta CERRADA. Congela lo que
 * la cuenta ya tiene (emisor, adquiriente, lineas, pagos) y lo persiste
 * PENDIENTE y SIN numerar: numerar aqui quemaria un consecutivo que quiza nunca
 * se transmita, y los huecos en la secuencia fiscal los penaliza la DIAN.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentBuilder — construccion desde cuenta cerrada")
class DocumentBuilderTest {

    @Mock
    private SaleSnapshotQueryPort saleSnapshotQueryPort;
    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private PosTicketLimitValidator posTicketLimitValidator;
    @Mock
    private UvtQueryPort uvtQueryPort;
    @InjectMocks
    private DocumentBuilder builder;

    private static SaleSnapshot snapshot(boolean accountClosed, boolean withholdingAgent) {
        return new SaleSnapshot(9L, 100L, accountClosed, issuer(), customer(), unaLineaGravada(),
                efectivo("1190.00"), PaymentForm.CONTADO, withholdingAgent, bd("2.5"), bd("15"),
                bd("9.66"), 7L);
    }

    private void ventaCerrada(boolean withholdingAgent) {
        when(saleSnapshotQueryPort.findByOpenAccount(100L, 9L))
                .thenReturn(Optional.of(snapshot(true, withholdingAgent)));
        when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(ElectronicDocumentMother.UVT));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ElectronicDocument guardado() {
        ArgumentCaptor<ElectronicDocument> captor = ArgumentCaptor
                .forClass(ElectronicDocument.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste el documento con los datos congelados de la cuenta")
        void persiste_el_documento_con_los_datos_de_la_cuenta() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            ElectronicDocument documento = guardado();
            assertThat(documento.getCompanyId()).isEqualTo(9L);
            assertThat(documento.getOpenAccountId()).isEqualTo(100L);
            assertThat(documento.getBranchId()).isEqualTo(7L);
            assertThat(documento.getDocumentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(documento.getIssuer()).isEqualTo(issuer());
            assertThat(documento.getCustomer()).isEqualTo(customer());
            assertThat(documento.getLines()).hasSize(1);
            assertThat(documento.getPayments()).hasSize(1);
        }

        @Test
        @DisplayName("el documento nace PENDIENTE y sin numeracion fiscal: no quema consecutivo")
        void el_documento_nace_pendiente_y_sin_numerar() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            ElectronicDocument documento = guardado();
            assertThat(documento.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(documento.getPrefix()).isNull();
            assertThat(documento.getConsecutive()).isNull();
            assertThat(documento.getResolutionNumber()).isNull();
            assertThat(documento.getCufe()).isNull();
            assertThat(documento.getCude()).isNull();
        }

        @Test
        @DisplayName("la emision al cerrar la cuenta no tiene actor fiscal ni clave de "
                + "idempotencia")
        void la_emision_al_cerrar_no_tiene_actor_ni_clave() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            ElectronicDocument documento = guardado();
            assertThat(documento.getIssuedByEmployeeId()).isNull();
            assertThat(documento.getClientRequestId()).isNull();
        }

        @Test
        @DisplayName("devuelve el documento que respondio el repositorio, con su id")
        void devuelve_el_documento_que_respondio_el_repositorio() {
            when(saleSnapshotQueryPort.findByOpenAccount(100L, 9L))
                    .thenReturn(Optional.of(snapshot(true, false)));
            when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(ElectronicDocumentMother.UVT));
            when(repository.save(any()))
                    .thenReturn(ElectronicDocumentMother.facturaPendienteConId(55L));

            assertThat(builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false).getId())
                    .isEqualTo(55L);
        }

        @ParameterizedTest
        @EnumSource(value = ElectronicDocumentType.class, names = {"FE_VENTA", "DOC_EQUIV_POS"})
        @DisplayName("construye el tipo de documento que pidio el caller")
        void construye_el_tipo_que_pidio_el_caller(ElectronicDocumentType type) {
            ventaCerrada(false);

            builder.build(100L, type, 9L, false);

            assertThat(guardado().getDocumentType()).isEqualTo(type);
        }

        @Test
        @DisplayName("valida el tope de 5 UVT del tiquete POS antes de persistir")
        void valida_el_tope_del_tiquete_pos() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.DOC_EQUIV_POS, 9L, false);

            verify(posTicketLimitValidator).validate(any(ElectronicDocument.class));
        }
    }

    @Nested
    @DisplayName("consumidor final")
    class ConsumidorFinal {

        @Test
        @DisplayName("sustituye al adquiriente de la cuenta por la identidad generica DIAN")
        void sustituye_al_adquiriente_por_la_identidad_generica() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.DOC_EQUIV_POS, 9L, true);

            assertThat(guardado().getCustomer()).isEqualTo(CustomerSnapshot.finalConsumer());
        }

        @Test
        @DisplayName("sin consumidor final conserva al adquiriente real de la cuenta")
        void sin_consumidor_final_conserva_al_adquiriente_real() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            assertThat(guardado().getCustomer().documentId()).isEqualTo("1020304050");
        }
    }

    @Nested
    @DisplayName("retenciones del adquiriente")
    class Retenciones {

        /**
         * Venta por encima de la cuantia minima de 4 UVT: base 200.000, IVA 19 % =
         * 38.000, total 238.000. La factura canonica (base 1.000) no sirve aqui: se
         * queda muy por debajo del umbral y la reteFuente/reteIVA salen en cero por
         * diseno.
         */
        private void ventaGrandeCerrada(boolean withholdingAgent) {
            when(saleSnapshotQueryPort.findByOpenAccount(100L, 9L))
                    .thenReturn(Optional.of(new SaleSnapshot(9L, 100L, true, issuer(), customer(),
                            List.of(ivaLine(1, "200000.00", "19", "38000.00")),
                            efectivo("238000.00"), PaymentForm.CONTADO, withholdingAgent, bd("2.5"),
                            bd("15"), bd("9.66"), 7L)));
            when(uvtQueryPort.currentUvt()).thenReturn(Optional.of(ElectronicDocumentMother.UVT));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("un adquiriente agente retenedor genera las tres retenciones congeladas")
        void un_agente_retenedor_genera_las_tres_retenciones() {
            ventaGrandeCerrada(true);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            ElectronicDocument documento = guardado();
            assertThat(documento.getReteFuenteAmount()).isEqualByComparingTo("5000.00");
            assertThat(documento.getReteIvaAmount()).isEqualByComparingTo("5700.00");
            assertThat(documento.getReteIcaAmount()).isEqualByComparingTo("1932.00");
        }

        @Test
        @DisplayName("bajo la cuantia minima de 4 UVT solo se practica la reteICA")
        void bajo_la_cuantia_minima_solo_se_practica_la_reteica() {
            ventaCerrada(true);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            ElectronicDocument documento = guardado();
            assertThat(documento.getReteFuenteAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(documento.getReteIvaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(documento.getReteIcaAmount()).isEqualByComparingTo("9.66");
        }

        @Test
        @DisplayName("un adquiriente que no es agente retenedor no genera retenciones")
        void un_no_retenedor_no_genera_retenciones() {
            ventaCerrada(false);

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            ElectronicDocument documento = guardado();
            assertThat(documento.getReteFuenteAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(documento.getReteIvaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(documento.getReteIcaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("sin UVT configurado la retencion se practica igual: no se bloquea la venta")
        void sin_uvt_la_retencion_se_practica_igual() {
            when(saleSnapshotQueryPort.findByOpenAccount(100L, 9L))
                    .thenReturn(Optional.of(snapshot(true, true)));
            when(uvtQueryPort.currentUvt()).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false);

            assertThat(guardado().getReteFuenteAmount()).isEqualByComparingTo("25.00");
        }
    }

    @Nested
    @DisplayName("caminos de error")
    class CaminosDeError {

        @Test
        @DisplayName("una cuenta inexistente no llega a construir ni a guardar nada")
        void una_cuenta_inexistente_no_guarda_nada() {
            when(saleSnapshotQueryPort.findByOpenAccount(404L, 9L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> builder.build(404L, ElectronicDocumentType.FE_VENTA, 9L, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Open account not found: 404");

            verifyNoInteractions(repository, uvtQueryPort, posTicketLimitValidator);
        }

        @Test
        @DisplayName("una cuenta de otra empresa se ve como inexistente y no escribe nada")
        void una_cuenta_de_otra_empresa_se_ve_como_inexistente() {
            when(saleSnapshotQueryPort.findByOpenAccount(100L, 77L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> builder.build(100L, ElectronicDocumentType.FE_VENTA, 77L, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Open account not found: 100");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una cuenta aun abierta no se puede facturar y no escribe nada")
        void una_cuenta_abierta_no_se_puede_facturar() {
            when(saleSnapshotQueryPort.findByOpenAccount(100L, 9L))
                    .thenReturn(Optional.of(snapshot(false, false)));

            assertThatThrownBy(
                    () -> builder.build(100L, ElectronicDocumentType.FE_VENTA, 9L, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("La cuenta debe estar cerrada (CLOSE)");

            verifyNoInteractions(repository, uvtQueryPort, posTicketLimitValidator);
        }
    }
}
