package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CompanyAlreadyHasActiveSubscriptionException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSubscriptionRepository - una empresa, un contrato vivo")
class JpaSubscriptionRepositoryTest {

    private static final Long EMPRESA = 42L;

    @Mock
    private SubscriptionJpaRepository jpaRepository;
    @Mock
    private CompanyJpaRepository companyJpaRepository;

    private JpaSubscriptionRepository repository;

    private JpaSubscriptionRepository repository() {
        if (repository == null) {
            repository = new JpaSubscriptionRepository(jpaRepository,
                    new SubscriptionJpaMapper(
                            Clock.fixed(Instant.parse("2026-01-01T10:15:30Z"), ZoneOffset.UTC)),
                    companyJpaRepository);
        }
        return repository;
    }

    private static Subscription nuevoContrato() {
        return Subscription.create("SUS-2026-00185", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), null, null, 0, true);
    }

    private static DataIntegrityViolationException duplicado(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry '42' for key 'subscriptions." + constraint + "'"));
    }

    @Nested
    @DisplayName("El conflicto lo decide el indice unico, no una comprobacion previa")
    class Conflicto {

        @Test
        @DisplayName("la violacion de uq_subscriptions_active_company sale como conflicto legible")
        void traduceLaViolacionDeUnique() {
            when(companyJpaRepository.getReferenceById(EMPRESA))
                    .thenReturn(mock(CompanyJpaEntity.class));
            when(jpaRepository.saveAndFlush(any()))
                    .thenThrow(duplicado("uq_subscriptions_active_company"));

            assertThatThrownBy(() -> repository().save(nuevoContrato()))
                    .isInstanceOf(CompanyAlreadyHasActiveSubscriptionException.class)
                    .hasMessageContaining(EMPRESA.toString());
        }

        @Test
        @DisplayName("otra violacion de integridad se propaga sin disfrazarse")
        void otraViolacionSePropaga() {
            when(companyJpaRepository.getReferenceById(EMPRESA))
                    .thenReturn(mock(CompanyJpaEntity.class));
            when(jpaRepository.saveAndFlush(any())).thenThrow(duplicado("uq_subscriptions_number"));

            assertThatThrownBy(() -> repository().save(nuevoContrato()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CompanyAlreadyHasActiveSubscriptionException.class);
        }

        @Test
        @DisplayName("usa saveAndFlush: sin flush el conflicto saltaria en el commit y seria un 500")
        void usaSaveAndFlush() {
            when(companyJpaRepository.getReferenceById(EMPRESA))
                    .thenReturn(mock(CompanyJpaEntity.class));
            when(jpaRepository.saveAndFlush(any()))
                    .thenThrow(duplicado("uq_subscriptions_active_company"));

            assertThatThrownBy(() -> repository().save(nuevoContrato()))
                    .isInstanceOf(CompanyAlreadyHasActiveSubscriptionException.class);

            verify(jpaRepository).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("El criterio de vigente")
    class Vigente {

        @Test
        @DisplayName("el contrato vigente se busca por los cuatro estados, no solo por ACTIVE")
        void buscaPorLosCuatroEstados() {
            when(jpaRepository.findFirstByCompany_IdAndStatusIn(eq(EMPRESA), any()))
                    .thenReturn(Optional.empty());

            assertThat(repository().findCurrentByCompanyId(EMPRESA)).isEmpty();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<SubscriptionStatus>> captor = ArgumentCaptor
                    .forClass(Collection.class);
            verify(jpaRepository).findFirstByCompany_IdAndStatusIn(anyLong(), captor.capture());
            // PAST_DUE y READ_ONLY tienen que estar: si se buscara solo ACTIVE, un moroso
            // apareceria como empresa sin contrato y se quedaria sin permisos.
            assertThat(captor.getValue()).containsExactlyInAnyOrder(SubscriptionStatus.TRIALING,
                    SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE,
                    SubscriptionStatus.READ_ONLY);
        }
    }
}
