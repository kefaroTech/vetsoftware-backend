package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequenceNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBillingDocumentSequenceRepository — el consecutivo, sin carrera y sin huecos")
class JpaBillingDocumentSequenceRepositoryTest {

    @Mock
    private BillingDocumentSequenceJpaRepository jpaRepository;

    private JpaBillingDocumentSequenceRepository adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaBillingDocumentSequenceRepository(jpaRepository,
                new BillingDocumentSequenceJpaMapper());
    }

    @Nested
    @DisplayName("Sin carrera")
    class SinCarrera {

        @Test
        @DisplayName("bloquea la fila del prefijo y SOLO DESPUES incrementa:"
                + " leer y escribir en el orden contrario deja la carrera abierta")
        void bloquea_y_luego_incrementa() {
            when(jpaRepository.lockNextValue("DC")).thenReturn(Optional.of(1L));

            DocumentNumber number = adapter.nextNumber("DC");

            InOrder orden = inOrder(jpaRepository);
            orden.verify(jpaRepository).lockNextValue("DC");
            orden.verify(jpaRepository).advance("DC");
            assertThat(number.value()).isOne();
        }

        @Test
        @DisplayName("la lectura de bloqueo va por SQL nativo con FOR UPDATE:"
                + " es lo unico que serializa dos emisiones simultaneas")
        void la_lectura_es_for_update() throws Exception {
            Method lock = BillingDocumentSequenceJpaRepository.class.getMethod("lockNextValue",
                    String.class);
            Query query = lock.getAnnotation(Query.class);

            assertThat(query).isNotNull();
            assertThat(query.nativeQuery()).isTrue();
            assertThat(query.value()).contains("FOR UPDATE").contains("WHERE prefix = :prefix");
        }

        @Test
        @DisplayName("el incremento lo hace el motor sobre la fila bloqueada,"
                + " no un valor calculado en Java")
        void el_incremento_es_del_motor() throws Exception {
            Method advance = BillingDocumentSequenceJpaRepository.class.getMethod("advance",
                    String.class);

            assertThat(advance.getAnnotation(Modifying.class)).isNotNull();
            assertThat(advance.getAnnotation(Query.class).value())
                    .contains("next_value = next_value + 1");
        }

        @Test
        @DisplayName("el UPDATE no mueve ninguna version, porque la tabla no esta versionada"
                + " (E6_YA_PROTEGIDO): el bloqueo pesimista ya la protege")
        void el_update_no_toca_ninguna_version() throws Exception {
            Method advance = BillingDocumentSequenceJpaRepository.class.getMethod("advance",
                    String.class);

            assertThat(advance.getAnnotation(Query.class).value()).doesNotContain("version");
            assertThat(Arrays.stream(BillingDocumentSequenceJpaEntity.class.getDeclaredFields())
                    .map(f -> f.getName()).toList()).doesNotContain("version", "enabled");
        }
    }

    @Nested
    @DisplayName("Sin huecos")
    class SinHuecos {

        @Test
        @DisplayName("el adaptador NO abre transaccion propia: el incremento se une a la del"
                + " caso de uso, asi que un fallo posterior lo deshace con el documento")
        void se_une_a_la_transaccion_del_caso_de_uso() {
            assertThat(
                    JpaBillingDocumentSequenceRepository.class.getAnnotation(Transactional.class))
                    .isNull();
            assertThat(
                    Arrays.stream(JpaBillingDocumentSequenceRepository.class.getDeclaredMethods())
                            .filter(m -> m.getAnnotation(Transactional.class) != null).toList())
                    .isEmpty();
        }

        @Test
        @DisplayName("una serie que no existe no se crea sola: arrancaria en 1 sin que"
                + " nadie lo haya decidido, y no se incrementa nada")
        void la_serie_no_se_autocrea() {
            when(jpaRepository.lockNextValue("XX")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.nextNumber("XX"))
                    .isInstanceOf(BillingDocumentSequenceNotFoundException.class)
                    .hasMessageContaining("XX");

            verify(jpaRepository, never()).advance(anyString());
        }
    }

    @Nested
    @DisplayName("Formato del numero")
    class Formato {

        @Test
        @DisplayName("DC-000001, con relleno para que el orden por texto no ponga el 10"
                + " antes del 9")
        void formato_con_relleno() {
            assertThat(new DocumentNumber("DC", 1L).formatted()).isEqualTo("DC-000001");
            assertThat(new DocumentNumber("NC", 42L).formatted()).isEqualTo("NC-000042");
            assertThat(new DocumentNumber("ND", 1234567L).formatted()).isEqualTo("ND-1234567");
        }

        @Test
        @DisplayName("un consecutivo por debajo de 1 no existe")
        void nunca_por_debajo_de_uno() {
            assertThatThrownBy(() -> new DocumentNumber("DC", 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }
    }
}
