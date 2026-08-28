package com.vetsoftware.app.smmlvvalue.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSmmlvValueRepository — el salario minimo y su estado contra MySQL real")
class SmmlvValuePersistenceIT extends AbstractDataJpaTest {

    private static final int ANO = 2030;
    private static final BigDecimal VALOR = new BigDecimal("2500000.00");
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2029, 12, 30, 10, 0, 0);
    private static final LocalDate SUSPENDIDO_EL = LocalDate.of(2030, 2, 12);
    private static final String AUTO = "Consejo de Estado, Seccion Segunda, auto del 12-02-2030";

    @Autowired
    private SmmlvValueJpaRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaSmmlvValueRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaSmmlvValueRepository(springDataRepository, new SmmlvValueJpaMapper());
    }

    private SmmlvValue guardarVigente() {
        SmmlvValue guardado = repository
                .save(SmmlvValue.create(ANO, VALOR, "Decreto de 2029", CREADO_EL));
        entityManager.flush();
        entityManager.clear();
        return guardado;
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("nace vigente y sin motivo de estado, como exige chk_smmlv_values_status")
        void nace_vigente_y_sin_motivo() {
            SmmlvValue guardado = guardarVigente();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getStatus()).isEqualTo(SmmlvStatus.IN_FORCE);
                assertThat(recuperado.getStatusReference()).isNull();
                assertThat(recuperado.getStatusChangedOn()).isNull();
                assertThat(recuperado.isInForce()).isTrue();
                assertThat(recuperado.getVersion()).isZero();
            });
        }
    }

    @Nested
    @DisplayName("La suspension judicial")
    class SuspensionJudicial {

        @Test
        @DisplayName("la suspension se anota sobre la fila que ya existia y mueve la version")
        void la_suspension_se_anota_sobre_la_fila_existente() {
            SmmlvValue guardado = guardarVigente();

            SmmlvValue cargado = repository.findById(guardado.getId()).orElseThrow();
            cargado.changeStatus(SmmlvStatus.SUSPENDED, AUTO, SUSPENDIDO_EL);
            repository.save(cargado);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByFiscalYear(ANO)).get().satisfies(suspendido -> {
                assertThat(suspendido.getStatus()).isEqualTo(SmmlvStatus.SUSPENDED);
                assertThat(suspendido.getStatusReference()).isEqualTo(AUTO);
                assertThat(suspendido.getStatusChangedOn()).isEqualTo(SUSPENDIDO_EL);
                // La cifra NO cambia: sigue aplicandose mientras el fondo se decide.
                assertThat(suspendido.getValueAmount()).isEqualByComparingTo(VALOR);
                assertThat(suspendido.isInForce()).isFalse();
                // La mutacion sobre fila viva es la razon de que esta tabla lleve
                // @Version, al contrario que sus tres hermanas del bloque.
                assertThat(suspendido.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("volver a vigente limpia el motivo y la fecha, o el CHECK rechazaria el UPDATE")
        void volver_a_vigente_limpia_el_motivo() {
            SmmlvValue guardado = guardarVigente();
            SmmlvValue cargado = repository.findById(guardado.getId()).orElseThrow();
            cargado.changeStatus(SmmlvStatus.SUSPENDED, AUTO, SUSPENDIDO_EL);
            repository.save(cargado);
            entityManager.flush();
            entityManager.clear();

            SmmlvValue suspendido = repository.findById(guardado.getId()).orElseThrow();
            suspendido.changeStatus(SmmlvStatus.IN_FORCE, null, null);
            repository.save(suspendido);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(vigente -> {
                assertThat(vigente.getStatus()).isEqualTo(SmmlvStatus.IN_FORCE);
                assertThat(vigente.getStatusReference()).isNull();
                assertThat(vigente.getStatusChangedOn()).isNull();
            });
        }
    }
}
