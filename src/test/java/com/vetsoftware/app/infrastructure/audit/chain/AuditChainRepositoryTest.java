package com.vetsoftware.app.infrastructure.audit.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditChainRepository")
class AuditChainRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final String HASH_A = AuditChainHash.payloadHash("a");
    private static final String HASH_B = AuditChainHash.payloadHash("b");

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuditChainRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AuditChainRepository(jdbcTemplate);
    }

    @Nested
    @DisplayName("sequencePending")
    class SequencePending {

        @Test
        @DisplayName("no hace nada si no hay filas pendientes de secuenciar")
        void no_hace_nada_si_no_hay_filas_pendientes() {
            when(jdbcTemplate.queryForObject(contains("FOR UPDATE"), any(RowMapper.class)))
                    .thenReturn(new AuditChainRepository.Head(5L, HASH_A, 5L));
            when(jdbcTemplate.query(contains("chain_sequence IS NULL"), any(RowMapper.class),
                    eq(50))).thenReturn(List.of());

            int count = repository.sequencePending(50, NOW);

            assertThat(count).isZero();
            verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
            verify(jdbcTemplate, never()).update(contains("audit_chain_head"), any(), any(), any());
        }

        @Test
        @DisplayName("asigna posicion y eslabon a las filas pendientes, en orden de insercion")
        void asigna_posicion_y_eslabon_a_las_filas_pendientes() throws SQLException {
            when(jdbcTemplate.queryForObject(contains("FOR UPDATE"), any(RowMapper.class)))
                    .thenReturn(new AuditChainRepository.Head(5L, HASH_A, 5L));

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("id")).thenReturn(99L);
            when(resultSet.getString("payload_hash")).thenReturn(HASH_B);

            when(jdbcTemplate.query(contains("chain_sequence IS NULL"), any(RowMapper.class),
                    eq(50))).thenAnswer(invocation -> {
                        RowMapper<?> mapper = invocation.getArgument(1);
                        return List.of(mapper.mapRow(resultSet, 0));
                    });

            int count = repository.sequencePending(50, NOW);
            String expectedChainHash = AuditChainHash.chainHash(HASH_A, 6L, HASH_B);

            assertThat(count).isEqualTo(1);
            verify(jdbcTemplate).batchUpdate(contains("UPDATE audit_event_outbox"), anyList());
            verify(jdbcTemplate).update(contains("UPDATE audit_chain_head"), eq(6L),
                    eq(expectedChainHash), eq(Timestamp.from(NOW)));
        }

        @Test
        @DisplayName("el RowMapper de lockHead (privado) reconstruye el estado de la cabeza bloqueada")
        void el_row_mapper_de_lock_head_reconstruye_el_estado() throws SQLException {
            ArgumentCaptor<RowMapper<AuditChainRepository.Head>> mapperCaptor = ArgumentCaptor
                    .forClass(RowMapper.class);
            when(jdbcTemplate.queryForObject(contains("FOR UPDATE"), mapperCaptor.capture()))
                    .thenReturn(new AuditChainRepository.Head(5L, HASH_A, 5L));
            when(jdbcTemplate.query(contains("chain_sequence IS NULL"), any(RowMapper.class),
                    eq(50))).thenReturn(List.of());

            repository.sequencePending(50, NOW);

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("last_sequence")).thenReturn(30L);
            when(resultSet.getString("last_chain_hash")).thenReturn(HASH_B);
            when(resultSet.getLong("last_checkpoint_sequence")).thenReturn(28L);

            AuditChainRepository.Head mapped = mapperCaptor.getValue().mapRow(resultSet, 0);

            assertThat(mapped).isEqualTo(new AuditChainRepository.Head(30L, HASH_B, 28L));
        }
    }

    @Nested
    @DisplayName("head")
    class HeadQuery {

        @Test
        @DisplayName("devuelve el estado actual de la cabeza sin bloquear")
        void devuelve_el_estado_actual_de_la_cabeza() {
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                    .thenReturn(new AuditChainRepository.Head(10L, HASH_A, 8L));

            AuditChainRepository.Head head = repository.head();

            assertThat(head.lastSequence()).isEqualTo(10L);
            assertThat(head.lastChainHash()).isEqualTo(HASH_A);
            assertThat(head.lastCheckpointSequence()).isEqualTo(8L);
        }

        @Test
        @DisplayName("el RowMapper de head reconstruye el estado sin bloquear")
        void el_row_mapper_de_head_reconstruye_el_estado() throws SQLException {
            ArgumentCaptor<RowMapper<AuditChainRepository.Head>> mapperCaptor = ArgumentCaptor
                    .forClass(RowMapper.class);
            when(jdbcTemplate.queryForObject(anyString(), mapperCaptor.capture()))
                    .thenReturn(new AuditChainRepository.Head(1L, HASH_A, 1L));
            repository.head();

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("last_sequence")).thenReturn(20L);
            when(resultSet.getString("last_chain_hash")).thenReturn(HASH_B);
            when(resultSet.getLong("last_checkpoint_sequence")).thenReturn(18L);

            AuditChainRepository.Head mapped = mapperCaptor.getValue().mapRow(resultSet, 0);

            assertThat(mapped).isEqualTo(new AuditChainRepository.Head(20L, HASH_B, 18L));
        }
    }

    @Nested
    @DisplayName("markCheckpoint")
    class MarkCheckpoint {

        @Test
        @DisplayName("registra la secuencia anclada con el instante dado")
        void registra_la_secuencia_anclada() {
            repository.markCheckpoint(42L, NOW);

            verify(jdbcTemplate).update(contains("last_checkpoint_sequence"), eq(42L),
                    eq(Timestamp.from(NOW)), eq(42L));
        }
    }

    @Nested
    @DisplayName("linksAfter")
    class LinksAfter {

        @Test
        @DisplayName("devuelve los eslabones a partir de la posicion dada, en orden")
        void devuelve_los_eslabones_a_partir_de_la_posicion_dada() {
            AuditChainRepository.Link link = new AuditChainRepository.Link(11L, "{}", HASH_A,
                    HASH_B, HASH_A);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10L), eq(5)))
                    .thenReturn(List.of(link));

            List<AuditChainRepository.Link> links = repository.linksAfter(10L, 5);

            assertThat(links).containsExactly(link);
        }

        @Test
        @DisplayName("el RowMapper de linksAfter reconstruye cada eslabon")
        void el_row_mapper_de_links_after_reconstruye_cada_eslabon() throws SQLException {
            ArgumentCaptor<RowMapper<AuditChainRepository.Link>> mapperCaptor = ArgumentCaptor
                    .forClass(RowMapper.class);
            when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), eq(10L), eq(5)))
                    .thenReturn(List.of());
            repository.linksAfter(10L, 5);

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("chain_sequence")).thenReturn(11L);
            when(resultSet.getString("payload")).thenReturn("{}");
            when(resultSet.getString("payload_hash")).thenReturn(HASH_A);
            when(resultSet.getString("previous_hash")).thenReturn(HASH_B);
            when(resultSet.getString("chain_hash")).thenReturn(HASH_A);

            AuditChainRepository.Link mapped = mapperCaptor.getValue().mapRow(resultSet, 0);

            assertThat(mapped)
                    .isEqualTo(new AuditChainRepository.Link(11L, "{}", HASH_A, HASH_B, HASH_A));
        }
    }

    @Nested
    @DisplayName("minRetainedSequence")
    class MinRetainedSequence {

        @Test
        @DisplayName("devuelve cero cuando no hay filas secuenciadas todavia")
        void devuelve_cero_cuando_no_hay_filas_secuenciadas() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            assertThat(repository.minRetainedSequence()).isZero();
        }

        @Test
        @DisplayName("devuelve la posicion minima retenida en la tabla")
        void devuelve_la_posicion_minima_retenida() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(15L);

            assertThat(repository.minRetainedSequence()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("unsequencedCount")
    class UnsequencedCount {

        @Test
        @DisplayName("devuelve cero cuando la consulta no arroja resultado")
        void devuelve_cero_cuando_la_consulta_no_arroja_resultado() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

            assertThat(repository.unsequencedCount()).isZero();
        }

        @Test
        @DisplayName("devuelve cuantos eventos siguen sin posicion en la cadena")
        void devuelve_cuantos_eventos_siguen_sin_posicion() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(3L);

            assertThat(repository.unsequencedCount()).isEqualTo(3L);
        }
    }
}
