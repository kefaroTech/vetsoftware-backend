package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CompanyAlreadyHasActiveSubscriptionException;
import com.vetsoftware.app.subscription.domain.QuoteAlreadyConvertedException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DC-2 contra <b>MySQL real</b>: las dos cosas que un test de dobles no puede
 * demostrar.
 *
 * <p>
 * <b>1. Que el orden cierre-antes-que-apertura sea obligatorio.</b>
 * {@code ReplaceSubscriptionFromQuoteService} cierra el contrato vigente y abre
 * el nuevo en la misma transaccion, y ese orden no es una preferencia: el
 * segundo bloque de abajo demuestra que invertirlo choca contra
 * {@code uq_subscriptions_active_company}. Un {@code InOrder} de Mockito
 * comprueba que el codigo llama en ese orden; solo el motor comprueba que
 * <em>tenga</em> que hacerlo. Y demuestra de paso lo que hace que funcione: que
 * {@code JpaSubscriptionRepository.save} hace {@code saveAndFlush}, asi que el
 * {@code UPDATE} del cierre llega a la base antes del {@code INSERT} del alta
 * en vez de quedarse en la cola de acciones de Hibernate —que ejecuta los
 * {@code INSERT} primero y haria saltar el unique con las dos operaciones
 * correctas—.
 *
 * <p>
 * <b>2. Que el driver reporte {@code uq_subscriptions_quote} con ese
 * nombre.</b> La traduccion de {@code JpaSubscriptionRepository} compara contra
 * un literal; si el nombre que llega en el mensaje del driver no fuera ese, la
 * carrera de la doble conversion saldria como un 500 y nadie se enteraria hasta
 * produccion. Es la misma clase de comprobacion que ya hace
 * {@code SubscriptionPersistenceIT} con el unique de contrato vigente.
 *
 * <p>
 * <b>Lo que este fichero sigue sin cubrir, y conviene decirlo:</b> la reversion
 * de la transaccion <em>del caso de uso completo</em> —aceptacion incluida—
 * necesita un {@code @SpringBootTest} con los servicios de aplicacion dentro,
 * no una rodaja de persistencia. Lo que aqui se prueba es el invariante del
 * motor del que esa reversion depende.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Sustitucion de contrato por cotizacion aceptada — contra MySQL real")
class SubscriptionReplacementPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN_PERIODO = LocalDate.of(2026, 3, 31);

    @Autowired
    private JpaSubscriptionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    private static Subscription contrato(SubscriptionStatus status, Long companyId, Long quoteId,
            String numero) {
        return new Subscription(null, numero, companyId, quoteId, SchemaSeed.PRICE_LIST_ID,
                BillingCycle.MONTHLY, status, INICIO,
                status == SubscriptionStatus.TRIALING ? LocalDate.of(2026, 3, 10) : null, INICIO,
                FIN_PERIODO, FIN_PERIODO, null, 5, null, true, null, null, null, true);
    }

    @Nested
    @DisplayName("Una cotizacion, un contrato")
    class UnaCotizacionUnContrato {

        @Test
        @DisplayName("dos contratos desde la misma cotizacion salen como conflicto, no como 500")
        void dosContratosDesdeLaMismaCotizacionEsConflicto() {
            // Los dos van CANCELLED para aislar el invariante que se quiere probar: un
            // estado terminal deja active_marker en NULL, asi que lo unico que puede
            // rechazar el segundo INSERT es uq_subscriptions_quote y no el unique de
            // contrato vigente. Si el test los creara vigentes, pasaria en verde por la
            // restriccion equivocada.
            repository.save(contrato(SubscriptionStatus.CANCELLED, SchemaSeed.COMPANY_ID,
                    SchemaSeed.QUOTE_ID, "SUS-TEST-000910"));

            assertThatThrownBy(() -> repository.save(contrato(SubscriptionStatus.CANCELLED,
                    SchemaSeed.COMPANY_ID, SchemaSeed.QUOTE_ID, "SUS-TEST-000911")))
                    .isInstanceOf(QuoteAlreadyConvertedException.class)
                    .hasMessageContaining(SchemaSeed.QUOTE_ID.toString());
        }

        @Test
        @DisplayName("firmar la cotizacion de OTRA empresa lo para la clave foranea, no el unique")
        void laCotizacionDeOtraEmpresaLaParaLaClaveForanea() {
            // Reparto de trabajo entre las dos restricciones, escrito como test para que
            // nadie lo deduzca mal en la direccion cara.
            //
            // fk_subscriptions_quote apunta a quotes(company_id, id) — COMPUESTA — asi
            // que colgar de un contrato la cotizacion de otra empresa no llega siquiera
            // a rozar uq_subscriptions_quote: no existe esa pareja en `quotes`. Lo que
            // uq_subscriptions_quote cierra es el caso que la FK deja pasar: la MISMA
            // empresa firmando dos veces su MISMA cotizacion, que es la carrera de dos
            // aceptaciones simultaneas.
            //
            // Sale como violacion de integridad cruda y no traducida, y esta bien que
            // asi sea: no es un conflicto de negocio que un cliente pueda provocar por
            // la via normal —el snapshot de la cotizacion ya va acotado por empresa—
            // sino un dato imposible.
            assertThatThrownBy(() -> repository.save(contrato(SubscriptionStatus.CANCELLED,
                    SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.QUOTE_ID, "SUS-TEST-000913")))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(QuoteAlreadyConvertedException.class);
        }

        @Test
        @DisplayName("los contratos SIN cotizacion conviven: el unique no ve los nulos")
        void losContratosSinCotizacionConviven() {
            // Es la premisa entera del diseno del indice: todo contrato inicial nace con
            // quote_id NULL —una empresa por cada alta— y MySQL admite tantos NULL como
            // haga falta en un unique. Si esto fuera falso, uq_subscriptions_quote
            // impediria registrar la segunda empresa de la plataforma.
            assertThatCode(() -> {
                repository.save(contrato(SubscriptionStatus.CANCELLED, SchemaSeed.COMPANY_ID, null,
                        "SUS-TEST-000914"));
                repository.save(contrato(SubscriptionStatus.CANCELLED, SchemaSeed.COMPANY_ID, null,
                        "SUS-TEST-000915"));
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Cerrar antes de abrir, en la misma transaccion")
    class CerrarAntesDeAbrir {

        @Test
        @DisplayName("cerrar el vigente y abrir el nuevo en la misma transaccion funciona")
        void cerrarYAbrirEnLaMismaTransaccion() {
            Subscription vigente = repository.findCurrentByCompanyId(SchemaSeed.COMPANY_ID)
                    .orElseThrow();
            vigente.changeStatus(SubscriptionStatus.CANCELLED,
                    SubscriptionStatusChangeReason.REPLACED_BY_NEW_CONTRACT.code(),
                    "quote-acceptance", LocalDateTime.now());
            repository.save(vigente);

            Subscription nuevo = repository.save(contrato(SubscriptionStatus.ACTIVE,
                    SchemaSeed.COMPANY_ID, SchemaSeed.QUOTE_ID, "SUS-TEST-000916"));

            entityManager.flush();
            entityManager.clear();

            assertThat(nuevo.getId()).isNotNull();
            assertThat(repository.findCurrentByCompanyId(SchemaSeed.COMPANY_ID)).get()
                    .extracting(Subscription::getId).isEqualTo(nuevo.getId());
        }

        @Test
        @DisplayName("abrir sin haber cerrado antes choca: por eso el orden no es opcional")
        void abrirSinCerrarChoca() {
            // El gemelo negativo del de arriba, y la razon de que exista este fichero.
            // Si algun dia alguien reordena ReplaceSubscriptionFromQuoteService para
            // «crear primero y limpiar despues», esto es lo que se encontraria en
            // produccion — con la diferencia de que aqui sale en el build.
            assertThatThrownBy(() -> repository.save(contrato(SubscriptionStatus.ACTIVE,
                    SchemaSeed.COMPANY_ID, SchemaSeed.QUOTE_ID, "SUS-TEST-000917")))
                    .isInstanceOf(CompanyAlreadyHasActiveSubscriptionException.class);
        }

        @Test
        @DisplayName("el contrato cerrado sigue existiendo: sustituir no borra historia")
        void elContratoCerradoSigueExistiendo() {
            Subscription vigente = repository.findCurrentByCompanyId(SchemaSeed.COMPANY_ID)
                    .orElseThrow();
            Long cerradoId = vigente.getId();
            vigente.changeStatus(SubscriptionStatus.CANCELLED,
                    SubscriptionStatusChangeReason.REPLACED_BY_NEW_CONTRACT.code(),
                    "quote-acceptance", LocalDateTime.now());
            repository.save(vigente);
            repository.save(contrato(SubscriptionStatus.ACTIVE, SchemaSeed.COMPANY_ID,
                    SchemaSeed.QUOTE_ID, "SUS-TEST-000918"));

            entityManager.flush();
            entityManager.clear();

            // R12 en la dimension del contrato: la fila anterior sigue ahi, con su
            // numero y su expediente. Una factura discutida seis meses despues se
            // explica con ella.
            assertThat(repository.findByIdAndCompanyId(cerradoId, SchemaSeed.COMPANY_ID)).get()
                    .extracting(Subscription::getStatus).isEqualTo(SubscriptionStatus.CANCELLED);
        }
    }
}
