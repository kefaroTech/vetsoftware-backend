package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionAmendmentDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionStatusChangeDto;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las ocho consultas del slice. Son finas a propósito —proyectan lo que
 * devuelve su puerto— y lo que aquí importa no es que deleguen, sino <b>qué
 * puerto eligen y con qué argumentos</b>: todas menos las dos de plataforma
 * reciben {@code companyId} y tienen que pasarlo, porque es lo único que impide
 * que un empleado lea el expediente de otra clínica.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consultas de subscription")
class SubscriptionQueryServicesTest {

    private static final Long CONTRATO = 55L;
    private static final Long EMPRESA = SubscriptionMother.EMPRESA;

    @Nested
    @DisplayName("El contrato")
    class ElContrato {

        @Mock
        private SubscriptionRepository repository;
        @InjectMocks
        private FindSubscriptionService findService;
        @InjectMocks
        private FindCurrentSubscriptionService findCurrentService;
        @InjectMocks
        private ListSubscriptionsByCompanyService listByCompanyService;
        @InjectMocks
        private ListAllSubscriptionsService listAllService;

        @Test
        @DisplayName("buscar por id acota por empresa y proyecta el contrato completo")
        void buscar_por_id_acota_por_empresa() {
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(SubscriptionMother.contratoVigente()));

            SubscriptionDto dto = findService.findById(CONTRATO, EMPRESA);

            assertThat(dto.subscriptionNumber()).isEqualTo("SUS-2026-00184");
            assertThat(dto.companyId()).isEqualTo(EMPRESA);
            assertThat(dto.current()).isTrue();
            verify(repository).findByIdAndCompanyId(CONTRATO, EMPRESA);
        }

        @Test
        @DisplayName("un contrato de otra empresa no existe para quien pregunta")
        void un_contrato_de_otra_empresa_no_existe() {
            // El repositorio devuelve vacio porque la consulta ya filtro por empresa, y
            // el servicio lo convierte en un 404: un 403 confirmaria que ese id existe.
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findService.findById(CONTRATO, EMPRESA))
                    .isInstanceOf(SubscriptionNotFoundException.class).hasMessageContaining("55");
        }

        @Test
        @DisplayName("el contrato vigente sale del criterio de CURRENT, no de status = ACTIVE")
        void el_contrato_vigente_sale_del_criterio_de_current() {
            when(repository.findCurrentByCompanyId(EMPRESA)).thenReturn(
                    Optional.of(SubscriptionMother.contratoEn(SubscriptionStatus.PAST_DUE)));

            SubscriptionDto dto = findCurrentService.findCurrent(EMPRESA);

            // PAST_DUE es vigente: debe, pero sigue trabajando. Si el criterio se
            // hubiera escrito como status = ACTIVE, este cliente se quedaria sin
            // contrato vigente y sin permisos, y nadie lo notaria hasta que reclamara.
            assertThat(dto.current()).isTrue();
            assertThat(dto.status()).isEqualTo(SubscriptionStatus.PAST_DUE);
        }

        @Test
        @DisplayName("una empresa sin contrato vigente da 404")
        void empresa_sin_contrato_vigente() {
            when(repository.findCurrentByCompanyId(EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findCurrentService.findCurrent(EMPRESA))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("el listado del tenant pasa la empresa y conserva los metadatos")
        void el_listado_del_tenant_pasa_la_empresa() {
            when(repository.findAllByCompanyId(EMPRESA, 1, 5)).thenReturn(
                    PageResult.of(List.of(SubscriptionMother.contratoVigente()), 1, 5, 11L));

            PageResult<SubscriptionDto> pagina = listByCompanyService.listByCompany(EMPRESA, 1, 5);

            // Los totales son los de la consulta, no los del contenido paginado:
            // recalcularlos aqui es como se acaba reportando «1 de 1» sobre once.
            assertThat(pagina.totalElements()).isEqualTo(11L);
            assertThat(pagina.totalPages()).isEqualTo(3);
            assertThat(pagina.content()).singleElement().extracting(SubscriptionDto::companyId)
                    .isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("la vista de plataforma no filtra por empresa a proposito")
        void la_vista_de_plataforma_no_filtra() {
            when(repository.findAll(0, 20)).thenReturn(PageResult.empty(0, 20));

            assertThat(listAllService.listAll(0, 20).content()).isEmpty();
            verify(repository).findAll(0, 20);
        }
    }

    @Nested
    @DisplayName("Las lineas")
    class LasLineas {

        @Mock
        private SubscriptionItemRepository repository;
        @InjectMocks
        private ListSubscriptionItemsService listItemsService;
        @InjectMocks
        private FindOverlappingSubscriptionItemsService findOverlapsService;

        @Test
        @DisplayName("sin fecha devuelve el expediente completo, con las cerradas incluidas")
        void sin_fecha_devuelve_el_expediente_completo() {
            when(repository.findAllBySubscriptionIdAndCompanyId(CONTRATO, EMPRESA, 0, 20))
                    .thenReturn(PageResult.of(List.of(SubscriptionMother
                            .lineaEntre(SubscriptionMother.ENERO_1, SubscriptionMother.JUNIO_30)),
                            0, 20, 1L));

            PageResult<SubscriptionItemDto> pagina = listItemsService.listAll(CONTRATO, EMPRESA,
                    null, 0, 20);

            assertThat(pagina.content()).singleElement()
                    .extracting(SubscriptionItemDto::effectiveTo)
                    .isEqualTo(SubscriptionMother.JUNIO_30);
            verify(repository).findAllBySubscriptionIdAndCompanyId(CONTRATO, EMPRESA, 0, 20);
        }

        @Test
        @DisplayName("con fecha pregunta que tenia la clinica ese dia, no el expediente entero")
        void con_fecha_pregunta_por_ese_dia() {
            // Son dos consultas distintas y elegir la equivocada no da error: da una
            // respuesta plausible y falsa. Con onDate el listado tiene que ir a
            // findCurrentOn, que es donde vive el criterio de vigente escrito en SQL.
            LocalDate dia = LocalDate.of(2026, 3, 15);
            when(repository.findCurrentOn(CONTRATO, EMPRESA, dia, 0, 20)).thenReturn(
                    PageResult.of(List.of(SubscriptionMother.lineaAbierta()), 0, 20, 1L));

            PageResult<SubscriptionItemDto> pagina = listItemsService.listAll(CONTRATO, EMPRESA,
                    dia, 0, 20);

            assertThat(pagina.content()).singleElement()
                    .extracting(SubscriptionItemDto::billableQuantity).isEqualTo(3);
            verify(repository).findCurrentOn(CONTRATO, EMPRESA, dia, 0, 20);
        }

        @Test
        @DisplayName("la vigilancia devuelve lista vacia cuando la plataforma esta sana")
        void la_vigilancia_devuelve_vacio_si_esta_sana() {
            when(repository.findAllOverlaps()).thenReturn(List.of());

            assertThat(findOverlapsService.findAllOverlaps()).isEmpty();
        }

        @Test
        @DisplayName("la vigilancia devuelve cada par que se pisa tal cual lo da el puerto")
        void la_vigilancia_devuelve_los_pares() {
            when(repository.findAllOverlaps()).thenReturn(List.of(SubscriptionMother.solapeDto()));

            assertThat(findOverlapsService.findAllOverlaps()).singleElement().satisfies(solape -> {
                assertThat(solape.firstItemId()).isEqualTo(1L);
                assertThat(solape.secondItemId()).isEqualTo(2L);
                assertThat(solape.itemCode()).isEqualTo("EXTRA_USER");
            });
        }
    }

    @Nested
    @DisplayName("El expediente documental")
    class ExpedienteDocumental {

        @Mock
        private SubscriptionAmendmentRepository amendmentRepository;
        @Mock
        private SubscriptionStatusHistoryRepository historyRepository;
        @InjectMocks
        private ListSubscriptionAmendmentsService listAmendmentsService;
        @InjectMocks
        private ListSubscriptionStatusHistoryService listHistoryService;

        @Test
        @DisplayName("los otrosies se listan acotados por contrato y por empresa")
        void los_otrosies_se_listan_acotados() {
            when(amendmentRepository.findAllBySubscriptionIdAndCompanyId(CONTRATO, EMPRESA, 0, 20))
                    .thenReturn(PageResult
                            .of(List.of(SubscriptionMother.otrosiFirmadoPorEmpleado()), 0, 20, 1L));

            PageResult<SubscriptionAmendmentDto> pagina = listAmendmentsService.listAll(CONTRATO,
                    EMPRESA, 0, 20);

            assertThat(pagina.content()).singleElement().satisfies(dto -> {
                assertThat(dto.requestedByEmployeeId()).isEqualTo(SubscriptionMother.EMPLEADO);
                assertThat(dto.requestedBySystemUserId()).isNull();
                assertThat(dto.amendmentNumber()).isEqualTo("AMD-2026-00001");
            });
        }

        @Test
        @DisplayName("la bitacora se lista acotada por contrato y por empresa")
        void la_bitacora_se_lista_acotada() {
            when(historyRepository.findAllBySubscriptionIdAndCompanyId(CONTRATO, EMPRESA, 0, 20))
                    .thenReturn(PageResult.of(List.of(SubscriptionMother.transicion()), 0, 20, 1L));

            PageResult<SubscriptionStatusChangeDto> pagina = listHistoryService.listAll(CONTRATO,
                    EMPRESA, 0, 20);

            assertThat(pagina.content()).singleElement().satisfies(dto -> {
                assertThat(dto.fromStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                assertThat(dto.toStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
                assertThat(dto.actor()).isEqualTo("cobranza");
            });
        }

        @Test
        @DisplayName("un contrato sin movimientos devuelve paginas vacias, no null")
        void un_contrato_sin_movimientos() {
            when(amendmentRepository.findAllBySubscriptionIdAndCompanyId(CONTRATO, EMPRESA, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));
            when(historyRepository.findAllBySubscriptionIdAndCompanyId(CONTRATO, EMPRESA, 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            assertThat(listAmendmentsService.listAll(CONTRATO, EMPRESA, 0, 20).content()).isEmpty();
            assertThat(listHistoryService.listAll(CONTRATO, EMPRESA, 0, 20).content()).isEmpty();
        }
    }
}
