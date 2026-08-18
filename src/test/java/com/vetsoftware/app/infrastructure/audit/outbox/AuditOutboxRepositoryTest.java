package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.chain.AuditChainHash;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditOutboxRepository")
class AuditOutboxRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final String HASH = AuditChainHash.payloadHash("seed");

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuditOutboxRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AuditOutboxRepository(jdbcTemplate);
    }

    @Nested
    @DisplayName("claim")
    class Claim {

        @Test
        @DisplayName("no reclama nada cuando no hay filas elegibles")
        void no_reclama_nada_cuando_no_hay_filas_elegibles() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(Timestamp.from(NOW)),
                    eq(Timestamp.from(NOW)), eq(100))).thenReturn(List.of());

            List<AuditOutboxRecord> claimed = repository.claim(100, NOW, Duration.ofMinutes(2));

            assertThat(claimed).isEmpty();
            verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
        }

        @Test
        @DisplayName("reclama las filas elegibles y las pasa a PROCESSING")
        void reclama_las_filas_elegibles() {
            AuditOutboxRecord record = new AuditOutboxRecord(1L, "event-1", "{}", 1, 5L, HASH, HASH,
                    HASH);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(Timestamp.from(NOW)),
                    eq(Timestamp.from(NOW)), eq(100))).thenReturn(List.of(record));

            List<AuditOutboxRecord> claimed = repository.claim(100, NOW, Duration.ofMinutes(2));

            assertThat(claimed).containsExactly(record);
            ArgumentCaptor<List<AuditOutboxRecord>> batchCaptor = ArgumentCaptor
                    .forClass(List.class);
            verify(jdbcTemplate).batchUpdate(org.mockito.ArgumentMatchers.contains("PROCESSING"),
                    batchCaptor.capture(), eq(1), any(ParameterizedPreparedStatementSetter.class));
            assertThat(batchCaptor.getValue()).containsExactly(record);
        }

        @Test
        @DisplayName("el RowMapper de claim reconstruye el AuditOutboxRecord fila a fila")
        void el_row_mapper_de_claim_reconstruye_el_registro() throws SQLException {
            ArgumentCaptor<RowMapper<AuditOutboxRecord>> mapperCaptor = ArgumentCaptor
                    .forClass(RowMapper.class);
            when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), eq(Timestamp.from(NOW)),
                    eq(Timestamp.from(NOW)), eq(100))).thenReturn(List.of());
            repository.claim(100, NOW, Duration.ofMinutes(2));

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("id")).thenReturn(1L);
            when(resultSet.getString("event_id")).thenReturn("event-1");
            when(resultSet.getString("payload")).thenReturn("{}");
            when(resultSet.getInt("attempts")).thenReturn(0);
            when(resultSet.getLong("chain_sequence")).thenReturn(5L);
            when(resultSet.getString("previous_hash")).thenReturn(HASH);
            when(resultSet.getString("payload_hash")).thenReturn(HASH);
            when(resultSet.getString("chain_hash")).thenReturn(HASH);

            AuditOutboxRecord mapped = mapperCaptor.getValue().mapRow(resultSet, 0);

            assertThat(mapped)
                    .isEqualTo(new AuditOutboxRecord(1L, "event-1", "{}", 1, 5L, HASH, HASH, HASH));
        }

        @Test
        @DisplayName("el batchUpdate de claim fija locked_until y el id de cada fila reclamada")
        void el_batch_update_de_claim_fija_locked_until_y_el_id() throws SQLException {
            AuditOutboxRecord record = new AuditOutboxRecord(9L, "event-9", "{}", 1, 5L, HASH, HASH,
                    HASH);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(Timestamp.from(NOW)),
                    eq(Timestamp.from(NOW)), eq(100))).thenReturn(List.of(record));
            ArgumentCaptor<ParameterizedPreparedStatementSetter<AuditOutboxRecord>> setterCaptor = ArgumentCaptor
                    .forClass(ParameterizedPreparedStatementSetter.class);

            repository.claim(100, NOW, Duration.ofMinutes(2));

            verify(jdbcTemplate).batchUpdate(anyString(), anyList(), eq(1), setterCaptor.capture());
            PreparedStatement statement = mock(PreparedStatement.class);
            setterCaptor.getValue().setValues(statement, record);

            verify(statement).setTimestamp(eq(1), any(Timestamp.class));
            verify(statement).setLong(2, 9L);
        }
    }

    @Nested
    @DisplayName("markPublished")
    class MarkPublished {

        @Test
        @DisplayName("no toca la base de datos si la lista de ids esta vacia")
        void no_toca_la_base_de_datos_si_no_hay_ids() {
            repository.markPublished(List.of(), NOW);

            verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
        }

        @Test
        @DisplayName("marca como PUBLISHED las filas reclamadas")
        void marca_como_published_las_filas_reclamadas() {
            repository.markPublished(List.of(1L, 2L), NOW);

            ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
            verify(jdbcTemplate).batchUpdate(org.mockito.ArgumentMatchers.contains("PUBLISHED"),
                    idsCaptor.capture(), eq(2), any(ParameterizedPreparedStatementSetter.class));
            assertThat(idsCaptor.getValue()).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("el batchUpdate de markPublished fija published_at, next_attempt_at y el id")
        void el_batch_update_de_mark_published_fija_los_campos() throws SQLException {
            ArgumentCaptor<ParameterizedPreparedStatementSetter<Long>> setterCaptor = ArgumentCaptor
                    .forClass(ParameterizedPreparedStatementSetter.class);

            repository.markPublished(List.of(5L), NOW);

            verify(jdbcTemplate).batchUpdate(anyString(), anyList(), eq(1), setterCaptor.capture());
            PreparedStatement statement = mock(PreparedStatement.class);
            setterCaptor.getValue().setValues(statement, 5L);

            verify(statement, times(2)).setTimestamp(anyInt(), eq(Timestamp.from(NOW)));
            verify(statement).setLong(3, 5L);
        }
    }

    @Nested
    @DisplayName("markFailed")
    class MarkFailed {

        @Test
        @DisplayName("registra el error tal cual cuando cabe en el limite")
        void registra_el_error_tal_cual() {
            repository.markFailed(3L, NOW, "boom");

            verify(jdbcTemplate).update(anyString(), eq(Timestamp.from(NOW)), eq("boom"), eq(3L));
        }

        @Test
        @DisplayName("trunca el mensaje de error a mil caracteres")
        void trunca_el_mensaje_de_error_a_mil_caracteres() {
            String longError = "e".repeat(1500);

            repository.markFailed(3L, NOW, longError);

            ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).update(anyString(), eq(Timestamp.from(NOW)), errorCaptor.capture(),
                    eq(3L));
            assertThat(errorCaptor.getValue()).hasSize(1000);
        }

        @Test
        @DisplayName("un error en blanco se registra como unknown_error")
        void un_error_en_blanco_se_registra_como_unknown_error() {
            repository.markFailed(3L, NOW, "   ");

            verify(jdbcTemplate).update(anyString(), eq(Timestamp.from(NOW)), eq("unknown_error"),
                    eq(3L));
        }

        @Test
        @DisplayName("un error nulo se registra como unknown_error")
        void un_error_nulo_se_registra_como_unknown_error() {
            repository.markFailed(3L, NOW, null);

            verify(jdbcTemplate).update(anyString(), eq(Timestamp.from(NOW)), eq("unknown_error"),
                    eq(3L));
        }
    }

    @Nested
    @DisplayName("deletePublishedBefore")
    class DeletePublishedBefore {

        @Test
        @DisplayName("depura publicados anteriores al corte, sin rebasar el checkpoint")
        void depura_publicados_anteriores_al_corte() {
            when(jdbcTemplate.update(anyString(), eq(Timestamp.from(NOW)), eq(500))).thenReturn(37);

            int deleted = repository.deletePublishedBefore(NOW, 500);

            assertThat(deleted).isEqualTo(37);
        }
    }

    @Nested
    @DisplayName("contadores")
    class Contadores {

        @Test
        @DisplayName("pendingCount devuelve cero cuando la consulta no arroja resultado")
        void pending_count_devuelve_cero_sin_resultado() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            assertThat(repository.pendingCount()).isZero();
        }

        @Test
        @DisplayName("pendingCount devuelve el conteo de pendientes, en proceso y fallidos")
        void pending_count_devuelve_el_conteo() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(4L);

            assertThat(repository.pendingCount()).isEqualTo(4L);
        }

        @Test
        @DisplayName("failedCount devuelve cero cuando la consulta no arroja resultado")
        void failed_count_devuelve_cero_sin_resultado() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            assertThat(repository.failedCount()).isZero();
        }

        @Test
        @DisplayName("failedCount devuelve el conteo de eventos fallidos")
        void failed_count_devuelve_el_conteo() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(2L);

            assertThat(repository.failedCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("oldestPendingAgeSeconds devuelve cero cuando la consulta no arroja resultado")
        void oldest_pending_age_devuelve_cero_sin_resultado() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            assertThat(repository.oldestPendingAgeSeconds()).isZero();
        }

        @Test
        @DisplayName("oldestPendingAgeSeconds devuelve la edad del evento no publicado mas antiguo")
        void oldest_pending_age_devuelve_la_edad() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(120L);

            assertThat(repository.oldestPendingAgeSeconds()).isEqualTo(120.0);
        }
    }
}
