package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentAlreadyIssuedException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentAlreadyVoidedException;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.ExternalInvoiceReference;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxBreakdown;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El servicio que anula una cuenta de cobro, que hasta ahora no tenia ni una
 * prueba de comportamiento.
 *
 * <p>
 * <b>Lo que vale la clase entera es
 * {@link Anulacion#anular_devuelve_los_cargos_al_ciclo_siguiente}.</b> Anular
 * el documento y no liberar sus cargos los dejaba en {@code INVOICED} apuntando
 * a un documento {@code VOIDED}: el ciclo de facturacion siguiente no los
 * recoge —{@code findPendingByCompanyIdAndSubscription} filtra {@code PENDING}—
 * y no existe ninguna vigilancia que los detecte, porque
 * {@code subscription_charges} no tiene ni {@code enabled} ni un estado
 * «huerfano» que consultar. El resultado es dinero devengado que no se factura
 * jamas, sin una sola señal: ni excepcion, ni log, ni metrica. El servicio
 * incluso lo tenia escrito en un comentario —«deja huerfanos los cargos que
 * sello dentro si nadie los libera»— y no lo tapaba.
 *
 * <p>
 * Ese caso falla si alguien vuelve a dejar el cargo colgado: es una
 * verificacion de la llamada, no de un efecto observable, precisamente porque
 * el efecto que habria que observar es la <i>ausencia</i> de una fila huerfana
 * y eso no se ve desde aqui. El SQL que la ejecuta se prueba aparte.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VoidBillingDocumentService — anular libera lo que el documento sello")
class VoidBillingDocumentServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 900L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 1, 7, 0);
    private static final ServicePeriod AGOSTO = new ServicePeriod(LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31));

    @Mock
    private BillingDocumentRepository repository;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private SubscriptionBillingMetrics metrics;
    @Mock
    private SubscriptionBillingAuditPort audit;

    private VoidBillingDocumentService service;

    @BeforeEach
    void montar() {
        service = new VoidBillingDocumentService(repository, chargeRepository, metrics, audit);
    }

    private static SubscriptionCharge cuota() {
        return new SubscriptionCharge(500L, EMPRESA, 7L, null, ChargeType.RECURRING, "Plan CORE",
                AGOSTO, BigDecimal.ONE, new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null, null,
                null, AHORA);
    }

    private static SubscriptionBillingDocument documento(IssueStatus estado,
            ExternalInvoiceReference external) {
        TaxBreakdown breakdown = TaxBreakdown.of(List.of(cuota()), DocumentKind.INVOICE, EMPRESA,
                AHORA);
        return new SubscriptionBillingDocument(DOCUMENTO, "DC-000001", EMPRESA, 7L,
                DocumentKind.INVOICE, BillingReason.RECURRING_CYCLE, AGOSTO, estado, external, null,
                null, breakdown.subtotalAmount(), breakdown.taxAmount(), breakdown.totalAmount(),
                new BigDecimal("0.00"), breakdown.lineas(), AHORA, 0L);
    }

    private static SubscriptionBillingDocument borrador() {
        return documento(IssueStatus.DRAFT, null);
    }

    private void seEncuentra(SubscriptionBillingDocument document) {
        when(repository.findByIdAndCompanyId(DOCUMENTO, EMPRESA)).thenReturn(Optional.of(document));
    }

    @Nested
    @DisplayName("Anulacion")
    class Anulacion {

        @Test
        @DisplayName("anular devuelve los cargos del documento al ciclo siguiente: sin eso"
                + " quedan INVOICED contra un documento VOIDED y no se cobran nunca")
        void anular_devuelve_los_cargos_al_ciclo_siguiente() {
            seEncuentra(borrador());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
            when(chargeRepository.releaseFromVoidedDocument(DOCUMENTO, EMPRESA)).thenReturn(3);

            BillingDocumentDto dto = service
                    .execute(new VoidBillingDocumentCommand(DOCUMENTO, EMPRESA));

            verify(chargeRepository).releaseFromVoidedDocument(DOCUMENTO, EMPRESA);
            assertThat(dto.issueStatus()).isEqualTo(IssueStatus.VOIDED);
        }

        @Test
        @DisplayName("libera por el id del documento y la empresa del comando, no por una"
                + " lista de cargos que el llamador no conoce")
        void libera_por_documento_y_empresa() {
            seEncuentra(borrador());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
            when(chargeRepository.releaseFromVoidedDocument(DOCUMENTO, EMPRESA)).thenReturn(0);

            service.execute(new VoidBillingDocumentCommand(DOCUMENTO, EMPRESA));

            // La empresa no es defensa en profundidad aqui: el UPDATE no tiene lectura
            // previa que valide de quien son las filas, asi que su WHERE es toda la
            // seguridad. Un id de documento suelto liberaria cargos de otra clinica.
            verify(chargeRepository).releaseFromVoidedDocument(DOCUMENTO, EMPRESA);
        }

        @Test
        @DisplayName("un documento que no existe en esa empresa no libera ningun cargo")
        void un_documento_inexistente_no_libera_nada() {
            when(repository.findByIdAndCompanyId(DOCUMENTO, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new VoidBillingDocumentCommand(DOCUMENTO, EMPRESA)))
                    .isInstanceOf(SubscriptionBillingDocumentNotFoundException.class);

            verifyNoInteractions(chargeRepository);
        }

        @Test
        @DisplayName("un documento ya emitido fuera no se anula, y por tanto tampoco suelta"
                + " sus cargos: la factura existe en la DIAN")
        void un_documento_emitido_fuera_no_suelta_sus_cargos() {
            ExternalInvoiceReference external = new ExternalInvoiceReference("FE-4711", "CUFE123",
                    LocalDate.of(2026, 8, 20), "SIIGO", LocalDateTime.of(2026, 8, 20, 9, 0), 3L);
            seEncuentra(documento(IssueStatus.EXTERNAL_REGISTERED, external));

            assertThatThrownBy(
                    () -> service.execute(new VoidBillingDocumentCommand(DOCUMENTO, EMPRESA)))
                    .isInstanceOf(BillingDocumentAlreadyIssuedException.class);

            // El orden importa: primero decide el agregado si la anulacion es legal y
            // solo despues se sueltan los cargos. Al reves, un documento que rechaza la
            // anulacion se quedaria emitido y con sus cargos ya liberados —los mismos
            // que sustentan su importe— y el papel de la DIAN dejaria de cuadrar.
            verifyNoInteractions(chargeRepository);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("anular dos veces lo rechaza el agregado y no vuelve a tocar los cargos")
        void anular_dos_veces_no_vuelve_a_tocar_los_cargos() {
            seEncuentra(documento(IssueStatus.VOIDED, null));

            assertThatThrownBy(
                    () -> service.execute(new VoidBillingDocumentCommand(DOCUMENTO, EMPRESA)))
                    .isInstanceOf(BillingDocumentAlreadyVoidedException.class);

            verifyNoInteractions(chargeRepository);
        }
    }

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("la anulacion se cuenta y se audita con el estado ya anulado")
        void la_anulacion_se_cuenta_y_se_audita() {
            seEncuentra(borrador());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
            when(chargeRepository.releaseFromVoidedDocument(DOCUMENTO, EMPRESA)).thenReturn(1);

            service.execute(new VoidBillingDocumentCommand(DOCUMENTO, EMPRESA));

            verify(metrics).documentVoided(IssueStatus.VOIDED);
            // reason viaja nulo: VoidBillingDocumentCommand no lo pide. Queda escrito
            // aqui para que el dia que se añada al request se vea que este caso cambia.
            verify(audit).documentVoided(DOCUMENTO, "DC-000001", 7L, null);
        }
    }
}
