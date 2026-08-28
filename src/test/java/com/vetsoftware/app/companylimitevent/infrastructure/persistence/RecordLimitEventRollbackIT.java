package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.application.usecase.RecordLimitEventService;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R-LIMIT-18 contra el motor: <b>el portazo sobrevive a la vuelta atrás de la
 * operación que lo provocó</b>.
 *
 * <h2>Qué se probaba antes, y por qué no bastaba</h2>
 *
 * <p>
 * {@code RecordLimitEventServiceTest} leía la anotación por reflexión y
 * comprobaba que decía {@code REQUIRES_NEW}. Eso confirma la <em>intención</em>
 * escrita en el código fuente y nada más: la propagación la aplica el proxy de
 * Spring, así que el caso pasaba en verde con el proxy ausente, con el servicio
 * invocado por autollamada desde su propia clase —que esquiva el proxy sin
 * avisar y sin fallar— o con la gestión de transacciones apagada. En los tres
 * escenarios la fila que existe para demostrar el límite se iría con la
 * transacción que revierte, que es exactamente el caso que hay que demostrar.
 *
 * <p>
 * Aquí se provoca el rechazo de verdad: una transacción externa llama al caso
 * de uso <em>desde otro bean</em>, revierte, y <b>después</b> se lee la fila.
 *
 * <h2>Por qué esta rodaja no es transaccional</h2>
 *
 * <p>
 * {@code @DataJpaTest} envuelve cada caso en una transacción con vuelta atrás
 * al final, y dentro de ella no hay forma de observar lo que otra transacción
 * confirmó. {@code NOT_SUPPORTED} la quita, y las transacciones las abre este
 * test a mano con {@link TransactionTemplate}: la externa que revierte, y la de
 * lectura posterior.
 *
 * <p>
 * <b>Contrapartida, y su límite.</b> Sin vuelta atrás automática, lo que este
 * caso escribe se queda en el contenedor compartido. Las filas de
 * {@link SchemaSeed} son idénticas a las que siembra cualquier otra rodaja y su
 * inserción está guardada por clave primaria, así que dejarlas confirmadas es
 * un no-op para las demás; lo único propio de este caso —el hecho de cupo— se
 * borra en {@link #limpiarLoConfirmado()}, porque sí lo verían las rodajas que
 * cuentan filas de la bitácora.
 */
@Import({PersistenceSliceConfig.class, RecordLimitEventService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("RecordLimitEventService — el hecho que sobrevive a la vuelta atrás, contra MySQL"
        + " real")
class RecordLimitEventRollbackIT extends AbstractDataJpaTest {

    @Autowired
    private RecordLimitEventUseCase useCase;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private Long ejeAnimal;

    @BeforeEach
    void seed() {
        transactionTemplate.executeWithoutResult(status -> SchemaSeed.seed(entityManager));
        ejeAnimal = transactionTemplate
                .execute(status -> SchemaSeed.limitDimensionId(entityManager, "ANIMAL"));
        limpiarLoConfirmado();
    }

    @AfterEach
    void limpiarLoConfirmado() {
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createNativeQuery("DELETE FROM company_limit_events WHERE company_id = :companyId")
                .setParameter("companyId", SchemaSeed.COMPANY_ID).executeUpdate());
    }

    private RecordLimitEventCommand elPortazo() {
        return new RecordLimitEventCommand(SchemaSeed.COMPANY_ID, ejeAnimal,
                LimitEventType.LIMIT_BLOCKED, 100, 100, 1, LimitSource.CATALOG_DEFAULT, null,
                EventActor.employee(SchemaSeed.EMPLOYEE_ID), null, null);
    }

    private long hechosConfirmados() {
        return transactionTemplate.execute(status -> ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM company_limit_events WHERE company_id = :companyId")
                .setParameter("companyId", SchemaSeed.COMPANY_ID).getSingleResult()).longValue());
    }

    /**
     * El caso violador exacto: negar la creación de la mascota 101 revierte la
     * operación, y la fila que lo documenta tiene que quedarse. Sin
     * {@code REQUIRES_NEW} efectivo, el {@code INSERT} viaja dentro de la
     * transacción que se deshace y desaparece con ella.
     */
    @Test
    @DisplayName("R-LIMIT-18 · negar la creación de la mascota 101 deja su fila LIMIT_BLOCKED"
            + " confirmada aunque la operación que la provocó se deshaga entera")
    void el_portazo_sobrevive_a_la_vuelta_atras_de_la_operacion_externa() {
        assertThat(hechosConfirmados()).isZero();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            useCase.execute(elPortazo());
            throw new CupoAgotadoSimulado();
        })).isInstanceOf(CupoAgotadoSimulado.class);

        assertThat(hechosConfirmados())
                .as("filas de la bitácora que sobrevivieron a la vuelta atrás").isEqualTo(1);
    }

    /**
     * La otra mitad, y la que distingue «se confirmó sola» de «se confirmó porque
     * la externa también lo hizo»: con la transacción externa terminando bien, el
     * recuento es el mismo. Sin este caso, un servicio con la propagación por
     * defecto seguiría dando 1 en el camino feliz y el de arriba sería la única
     * red.
     */
    @Test
    @DisplayName("el hecho se escribe una sola vez, también cuando la operación externa termina"
            + " bien")
    void el_hecho_se_escribe_una_sola_vez_cuando_la_externa_confirma() {
        transactionTemplate.executeWithoutResult(status -> useCase.execute(elPortazo()));

        assertThat(hechosConfirmados()).isEqualTo(1);
    }

    /** El rechazo de cupo, tal como lo lanzaría el caso de uso que lo provoca. */
    private static final class CupoAgotadoSimulado extends RuntimeException {

        private static final long serialVersionUID = 1L;

        CupoAgotadoSimulado() {
            super("cupo de mascotas agotado: la operación se deshace");
        }
    }
}
