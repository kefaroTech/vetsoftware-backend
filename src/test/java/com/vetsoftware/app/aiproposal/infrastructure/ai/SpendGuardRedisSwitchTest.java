package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * {@code ValkeyDailySpendGuard} necesita una {@code StatefulRedisConnection}
 * real para arrancar, así que su rama activa no se ejercita con un contexto de
 * Spring aquí. Lo que sí es barato y se prueba: que las dos expresiones
 * condicionales son complementarias, para que nunca falten los dos guardianes
 * ni sobren los dos a la vez.
 */
class SpendGuardRedisSwitchTest {

    @Test
    @DisplayName("ValkeyDailySpendGuard exige Redis activado y spend-guard=valkey")
    void valkey_exige_redis_activado_y_spend_guard_valkey() {
        String expresion = ValkeyDailySpendGuard.class.getAnnotation(ConditionalOnExpression.class)
                .value();

        assertThat(expresion).contains("vetsoftware.redis.enabled")
                .contains("vetsoftware.ai.proposal.spend-guard").contains("'valkey'")
                .contains(" and ");
    }

    @Test
    @DisplayName("InProcessDailySpendGuard se activa con spend-guard=in-process o Redis apagado")
    void en_memoria_se_activa_con_spend_guard_in_process_o_redis_apagado() {
        String expresion = InProcessDailySpendGuard.class
                .getAnnotation(ConditionalOnExpression.class).value();

        assertThat(expresion).contains("vetsoftware.redis.enabled").contains("'in-process'")
                .contains(" or ");
    }
}
