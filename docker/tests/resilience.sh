#!/usr/bin/env bash
# Ensayos de resiliencia del stack de observabilidad (OBS-027).
#
# Cubre los cinco escenarios del hallazgo:
#   1. Loki no disponible
#   2. Tempo no disponible
#   3. Collector reiniciado
#   4. Pico de errores / tormenta de telemetría
#   5. Red interrumpida
#
# La telemetría se inyecta directamente por OTLP en lugar de conducir el backend. Es deliberado: da
# marcadores únicos y conteos exactos, no necesita base de datos ni credenciales, y lo que el
# hallazgo quiere comprobar es el comportamiento del STACK, no de la aplicación.
#
# Cada escenario afirma que la telemetría emitida durante la caída **acaba llegando**. Que el
# collector no se muera no basta: el riesgo del hallazgo es la pérdida silenciosa.
#
# Uso:  bash docker/tests/resilience.sh [escenario...]
#       bash docker/tests/resilience.sh          # todos
#       bash docker/tests/resilience.sh loki collector
#
# Requisitos: stack levantado (docker compose up -d). No requiere el backend ni el override de
# depuración: Loki y Tempo se consultan por la red interna del stack (OBS-025).

set -uo pipefail

NETWORK="${RESILIENCE_NETWORK:-vetsoftware_default}"
# Loki y Tempo no publican puerto en el host (OBS-025): viven en la red interna del plano de
# telemetría. Sus APIs se consultan desde un contenedor curl conectado a esa red, no desde el host.
TELEMETRY_NETWORK="${RESILIENCE_TELEMETRY_NETWORK:-vetsoftware_telemetry}"
LOKI_URL="${LOKI_URL:-http://loki:3100}"
TEMPO_URL="${TEMPO_URL:-http://tempo:3200}"
# El receptor OTLP sí se publica, en loopback: el emisor esperado es el backend del host.
OTLP_URL="${OTLP_URL:-http://localhost:4318}"
CURL_IMAGE="curlimages/curl:8.11.1"

# Margen de entrega. Los logs pasan por `batch` (timeout 5s); las trazas además por `tail_sampling`
# (decision_wait 10s), de ahí el margen mayor. Ver OBS-015.
LOG_SETTLE_SECONDS="${LOG_SETTLE_SECONDS:-25}"
TRACE_SETTLE_SECONDS="${TRACE_SETTLE_SECONDS:-40}"

PASSED=0
FAILED=0
FAILED_NAMES=()

# ---------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------

log()  { printf '%s\n' "$*"; }
step() { printf '  → %s\n' "$*"; }

pass() {
    PASSED=$((PASSED + 1))
    printf '  \033[32mPASA\033[0m  %s\n\n' "$1"
}

fail() {
    FAILED=$((FAILED + 1))
    FAILED_NAMES+=("$1")
    printf '  \033[31mFALLA\033[0m %s\n         %s\n\n' "$1" "$2"
}

now_nanos() { echo "$(date +%s)000000000"; }

# Hex aleatorio de N caracteres, para traceId/spanId y marcadores únicos.
rand_hex() {
    local chars=$1 out=""
    while [ ${#out} -lt "$chars" ]; do
        out="${out}$(od -An -tx1 -N16 /dev/urandom | tr -d ' \n')"
    done
    echo "${out:0:$chars}"
}

# Envía `count` registros de log con el marcador dado. Devuelve el código HTTP del collector.
#
# Cada registro lleva un timestamp en nanosegundos ÚNICO y el identificador de lote en el cuerpo.
# Es imprescindible, no cosmético: Loki descarta entradas con el mismo par (timestamp, contenido)
# dentro de un stream. Con timestamps de granularidad de segundo y cuerpos repetidos entre lotes, la
# deduplicación de Loki se confunde con pérdida de telemetría y el ensayo del pico falla sin motivo.
emit_logs() {
    local marker=$1 count=$2 service=$3 run_id=${4:-0}
    local records="" ts
    ts=$(now_nanos)
    for i in $(seq 1 "$count"); do
        [ -n "$records" ] && records="${records},"
        records="${records}{\"timeUnixNano\":\"$((ts + run_id * 100000 + i))\",\"severityNumber\":9,\"severityText\":\"INFO\",\"body\":{\"stringValue\":\"${marker} b=${run_id} n=${i}\"}}"
    done
    curl -s -o /dev/null -w '%{http_code}' --max-time 20 -X POST "${OTLP_URL}/v1/logs" \
        -H 'Content-Type: application/json' \
        -d "{\"resourceLogs\":[{\"resource\":{\"attributes\":[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"${service}\"}}]},\"scopeLogs\":[{\"logRecords\":[${records}]}]}]}"
}

# Envía una traza de un span. Imprime el traceId.
emit_trace() {
    local name=$1 service=$2
    local trace_id span_id start end
    trace_id=$(rand_hex 32)
    span_id=$(rand_hex 16)
    start=$(now_nanos)
    end=$((start + 1000000))
    curl -s -o /dev/null --max-time 20 -X POST "${OTLP_URL}/v1/traces" \
        -H 'Content-Type: application/json' \
        -d "{\"resourceSpans\":[{\"resource\":{\"attributes\":[{\"key\":\"service.name\",\"value\":{\"stringValue\":\"${service}\"}}]},\"scopeSpans\":[{\"spans\":[{\"traceId\":\"${trace_id}\",\"spanId\":\"${span_id}\",\"name\":\"${name}\",\"kind\":2,\"startTimeUnixNano\":\"${start}\",\"endTimeUnixNano\":\"${end}\"}]}]}]}"
    echo "$trace_id"
}

# curl dentro de la red interna de telemetría. Loki y Tempo no tienen puerto en el host, así que sus
# APIs solo son alcanzables desde un contenedor conectado a esa red.
telemetry_curl() {
    docker run --rm --network "$TELEMETRY_NETWORK" "$CURL_IMAGE" "$@" 2>/dev/null
}

# Cuenta líneas en Loki que contengan el marcador.
count_in_loki() {
    local marker=$1 service=$2 start end
    start=$(( $(date +%s) - 900 ))000000000
    end=$(now_nanos)
    telemetry_curl -sG --max-time 20 \
        --data-urlencode "query={service_name=\"${service}\"} |= \`${marker}\`" \
        --data-urlencode "start=${start}" \
        --data-urlencode "end=${end}" \
        --data-urlencode "limit=5000" \
        "${LOKI_URL}/loki/api/v1/query_range" \
        | grep -o '\[\"[0-9]\{19\}\"' | wc -l | tr -d ' '
}

trace_present() {
    local trace_id=$1 code
    code=$(telemetry_curl -s -o /dev/null -w '%{http_code}' --max-time 20 "${TEMPO_URL}/api/traces/${trace_id}")
    [ "$code" = "200" ]
}

collector_metric() {
    local name=$1
    docker run --rm --network "$NETWORK" "$CURL_IMAGE" -s --max-time 10 \
        http://otel-collector:8888/metrics 2>/dev/null \
        | awk -v m="$name" '$0 ~ "^"m"[ {]" { for (i=NF; i>0; i--) if ($i ~ /^[0-9.e+-]+$/) { s += $i; break } } END { printf "%.0f", s+0 }'
}

wait_until() {
    local description=$1 attempts=$2 sleep_s=$3; shift 3
    local i
    for ((i = 1; i <= attempts; i++)); do
        if "$@"; then return 0; fi
        sleep "$sleep_s"
    done
    step "agotado el tiempo esperando: ${description}"
    return 1
}

loki_ready()  { telemetry_curl -s -o /dev/null --max-time 5 "${LOKI_URL}/ready"; }
tempo_ready() { telemetry_curl -s -o /dev/null --max-time 5 "${TEMPO_URL}/ready"; }
collector_up() { [ "$(docker inspect -f '{{.State.Running}}' vetsoftware_otel_collector 2>/dev/null)" = "true" ]; }

# Redes a las que está conectado un contenedor. El escenario de corte de red desconecta TODAS: si
# quedara una, el collector seguiría teniendo ruta y el ensayo no probaría nada. Con el override de
# depuración, Loki está en dos redes.
container_networks() {
    docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$1" 2>/dev/null
}

# Reconecta un contenedor a una red CONSERVANDO su alias de servicio.
#
# `docker network connect` sin `--alias` registra solo el nombre del contenedor
# (`vetsoftware_loki`), no el alias que Compose le puso al crearlo (`loki`). El contenedor queda
# vivo y alcanzable por IP, pero `http://loki:3100` deja de resolver: ni el Collector ni los ensayos
# lo encuentran. Es un fallo silencioso y confuso —el servicio está sano y parece caído—, así que el
# alias es obligatorio en cualquier reconexión.
reconnect_with_alias() {
    local network=$1 container=$2 alias=$3
    docker network connect --alias "$alias" "$network" "$container" >/dev/null 2>&1
}

# Deja el stack como estaba, pase lo que pase.
restore_stack() {
    docker start vetsoftware_loki  >/dev/null 2>&1
    docker start vetsoftware_tempo >/dev/null 2>&1
    reconnect_with_alias "$TELEMETRY_NETWORK" vetsoftware_loki  loki
    reconnect_with_alias "$TELEMETRY_NETWORK" vetsoftware_tempo tempo
    docker start vetsoftware_otel_collector >/dev/null 2>&1
}
trap restore_stack EXIT

# ---------------------------------------------------------------------------
# Escenario 1 — Loki no disponible
# ---------------------------------------------------------------------------
scenario_loki() {
    local name="Loki no disponible: los logs emitidos durante la caída llegan después"
    local marker svc="resilience-loki" count=50
    marker="LOKI-DOWN-$(rand_hex 8)"
    log "[1/5] ${name}"

    step "deteniendo Loki"
    docker stop vetsoftware_loki >/dev/null || { fail "$name" "no se pudo detener Loki"; return; }

    step "emitiendo ${count} logs con Loki caído"
    local code
    code=$(emit_logs "$marker" "$count" "$svc")
    if [ "$code" != "200" ]; then
        fail "$name" "el collector rechazó la telemetría con Loki caído (HTTP ${code}); debería aceptarla y encolarla"
        return
    fi

    step "reactivando Loki"
    docker start vetsoftware_loki >/dev/null
    wait_until "Loki listo" 30 2 loki_ready || { fail "$name" "Loki no volvió a estar listo"; return; }

    step "esperando el drenaje de la cola (${LOG_SETTLE_SECONDS}s)"
    sleep "$LOG_SETTLE_SECONDS"

    local found
    found=$(count_in_loki "$marker" "$svc")
    if [ "$found" -ge "$count" ]; then
        pass "$name (${found}/${count} entregados)"
    else
        # Un segundo intento: el backoff de reintento puede tardar más que el margen.
        step "solo ${found}/${count}; esperando un ciclo de reintento más"
        sleep 30
        found=$(count_in_loki "$marker" "$svc")
        if [ "$found" -ge "$count" ]; then
            pass "$name (${found}/${count} entregados tras reintento)"
        else
            fail "$name" "se perdieron $((count - found)) de ${count} registros"
        fi
    fi
}

# ---------------------------------------------------------------------------
# Escenario 2 — Tempo no disponible
# ---------------------------------------------------------------------------
scenario_tempo() {
    local name="Tempo no disponible: la traza emitida durante la caída llega después"
    local svc="resilience-tempo" trace_id
    log "[2/5] ${name}"

    step "deteniendo Tempo"
    docker stop vetsoftware_tempo >/dev/null || { fail "$name" "no se pudo detener Tempo"; return; }

    step "emitiendo una traza con Tempo caído"
    trace_id=$(emit_trace "resilience-span" "$svc")

    step "reactivando Tempo"
    docker start vetsoftware_tempo >/dev/null
    wait_until "Tempo listo" 30 2 tempo_ready || { fail "$name" "Tempo no volvió a estar listo"; return; }

    step "esperando el drenaje de la cola (${TRACE_SETTLE_SECONDS}s)"
    sleep "$TRACE_SETTLE_SECONDS"

    if wait_until "traza ${trace_id} consultable" 10 6 trace_present "$trace_id"; then
        pass "$name (traza ${trace_id:0:12}… recuperada)"
    else
        fail "$name" "la traza ${trace_id} nunca llegó a Tempo"
    fi
}

# ---------------------------------------------------------------------------
# Escenario 3 — Collector reiniciado
#   Es el ensayo de la cola persistente: con Loki caído la telemetría queda encolada, y el reinicio
#   del collector no debe perderla. Sin `file_storage` este escenario falla.
# ---------------------------------------------------------------------------
scenario_collector() {
    local name="Collector reiniciado: la cola en disco sobrevive al reinicio"
    local marker svc="resilience-restart" count=50
    marker="RESTART-$(rand_hex 8)"
    log "[3/5] ${name}"

    step "deteniendo Loki para forzar el encolado"
    docker stop vetsoftware_loki >/dev/null

    step "emitiendo ${count} logs que quedarán en cola"
    local code
    code=$(emit_logs "$marker" "$count" "$svc")
    [ "$code" = "200" ] || { fail "$name" "el collector rechazó la telemetría (HTTP ${code})"; return; }

    step "dando tiempo a que la cola se escriba en disco"
    sleep 8

    step "reiniciando el collector"
    docker restart vetsoftware_otel_collector >/dev/null
    wait_until "collector en marcha" 30 2 collector_up || { fail "$name" "el collector no volvió a arrancar"; return; }
    sleep 5

    step "reactivando Loki"
    docker start vetsoftware_loki >/dev/null
    wait_until "Loki listo" 30 2 loki_ready || { fail "$name" "Loki no volvió a estar listo"; return; }

    step "esperando el drenaje de la cola (${LOG_SETTLE_SECONDS}s)"
    sleep "$LOG_SETTLE_SECONDS"

    local found
    found=$(count_in_loki "$marker" "$svc")
    if [ "$found" -ge "$count" ]; then
        pass "$name (${found}/${count} sobrevivieron al reinicio)"
    else
        step "solo ${found}/${count}; esperando un ciclo de reintento más"
        sleep 30
        found=$(count_in_loki "$marker" "$svc")
        if [ "$found" -ge "$count" ]; then
            pass "$name (${found}/${count} sobrevivieron al reinicio)"
        else
            fail "$name" "se perdieron $((count - found)) de ${count} registros al reiniciar; ¿la cola es persistente?"
        fi
    fi
}

# ---------------------------------------------------------------------------
# Escenario 4 — Pico de telemetría
#   No exige entrega total: exige que cualquier pérdida quede CONTABILIZADA. Un descarte medido es
#   operable; uno silencioso es el riesgo que describe el hallazgo.
# ---------------------------------------------------------------------------
scenario_burst() {
    local name="Pico de telemetría: el collector sobrevive y toda pérdida queda contabilizada"
    local marker svc="resilience-burst" batches=20 per_batch=200
    local total=$((batches * per_batch))
    marker="BURST-$(rand_hex 8)"
    log "[4/5] ${name}"

    local accepted_before refused_before enqueue_failed_before sent_before
    accepted_before=$(collector_metric otelcol_receiver_accepted_log_records)
    refused_before=$(collector_metric otelcol_receiver_refused_log_records)
    enqueue_failed_before=$(collector_metric otelcol_exporter_enqueue_failed_log_records)
    sent_before=$(collector_metric otelcol_exporter_sent_log_records)

    step "emitiendo ${total} registros en ${batches} lotes lo más rápido posible"
    local rejected=0 i code
    for ((i = 1; i <= batches; i++)); do
        # El índice del lote entra en el cuerpo y en el timestamp para que ningún registro sea
        # duplicado exacto de otro; si no, Loki deduplica y parece pérdida.
        code=$(emit_logs "$marker" "$per_batch" "$svc" "$i")
        [ "$code" = "200" ] || rejected=$((rejected + 1))
    done

    if ! collector_up; then
        fail "$name" "el collector se cayó durante el pico"
        return
    fi

    step "el collector sigue en marcha; comprobando la contabilidad"
    sleep "$LOG_SETTLE_SECONDS"

    local accepted_after refused_after enqueue_failed_after sent_after
    local accepted refused enqueue_failed sent
    accepted_after=$(collector_metric otelcol_receiver_accepted_log_records)
    refused_after=$(collector_metric otelcol_receiver_refused_log_records)
    enqueue_failed_after=$(collector_metric otelcol_exporter_enqueue_failed_log_records)
    sent_after=$(collector_metric otelcol_exporter_sent_log_records)
    accepted=$((accepted_after - accepted_before))
    refused=$((refused_after - refused_before))
    enqueue_failed=$((enqueue_failed_after - enqueue_failed_before))
    sent=$((sent_after - sent_before))

    step "aceptados=${accepted} entregados=${sent} rechazados=${refused} fallo_encolado=${enqueue_failed} lotes_rechazados_http=${rejected}"

    local found
    found=$(count_in_loki "$marker" "$svc")
    step "presentes en Loki: ${found}/${total}"

    # El fallo se juzga sobre lo que hizo el COLLECTOR, no sobre lo que Loki decidió conservar.
    # Distinguir ambas cosas evita el diagnóstico equivocado: si el collector entregó todo y en Loki
    # aparecen menos, el problema está en el destino (deduplicación, rate limiting), no en pérdida
    # silenciosa del pipeline.
    local unaccounted=$((accepted - sent - enqueue_failed))

    if [ "$unaccounted" -gt 0 ]; then
        fail "$name" "${unaccounted} registros aceptados no se entregaron ni quedaron contabilizados: pérdida silenciosa"
        return
    fi

    if [ "$found" -ge "$total" ]; then
        pass "$name (entrega completa: ${found}/${total} visibles en Loki)"
    elif [ "$sent" -ge "$accepted" ] && [ "$enqueue_failed" -eq 0 ]; then
        pass "$name (el collector entregó los ${sent} aceptados; Loki conserva ${found}, el resto lo descartó el destino)"
    else
        pass "$name (pérdida contabilizada: ${refused} rechazados, ${enqueue_failed} fallo de encolado)"
    fi
}

# ---------------------------------------------------------------------------
# Escenario 5 — Red interrumpida
#   Distinto de "servicio caído": aquí Loki está vivo pero inalcanzable, así que las conexiones se
#   quedan colgadas en lugar de ser rechazadas de inmediato.
# ---------------------------------------------------------------------------
scenario_network() {
    local name="Red interrumpida: la telemetría emitida sin ruta llega al restablecerse"
    local marker svc="resilience-network" count=50
    marker="NETCUT-$(rand_hex 8)"
    log "[5/5] ${name}"

    local nets n
    nets="$(container_networks vetsoftware_loki)"
    [ -n "$nets" ] || { fail "$name" "Loki no está conectado a ninguna red"; return; }

    step "desconectando Loki de sus redes (${nets})"
    for n in $nets; do
        docker network disconnect "$n" vetsoftware_loki >/dev/null 2>&1 \
            || { fail "$name" "no se pudo desconectar Loki de ${n}"; return; }
    done

    step "emitiendo ${count} logs sin ruta hacia Loki"
    local code
    code=$(emit_logs "$marker" "$count" "$svc")
    [ "$code" = "200" ] || { fail "$name" "el collector rechazó la telemetría (HTTP ${code})"; return; }

    step "reconectando la red (con alias de servicio)"
    for n in $nets; do
        reconnect_with_alias "$n" vetsoftware_loki loki
    done
    wait_until "Loki alcanzable" 30 2 loki_ready || { fail "$name" "Loki no volvió a estar alcanzable"; return; }

    step "esperando el drenaje de la cola"
    sleep "$LOG_SETTLE_SECONDS"

    local found
    found=$(count_in_loki "$marker" "$svc")
    if [ "$found" -ge "$count" ]; then
        pass "$name (${found}/${count} entregados)"
    else
        step "solo ${found}/${count}; esperando un ciclo de reintento más"
        sleep 40
        found=$(count_in_loki "$marker" "$svc")
        if [ "$found" -ge "$count" ]; then
            pass "$name (${found}/${count} entregados tras reintento)"
        else
            fail "$name" "se perdieron $((count - found)) de ${count} registros tras el corte de red"
        fi
    fi
}

# ---------------------------------------------------------------------------
# Comprobaciones previas
# ---------------------------------------------------------------------------
preflight() {
    local ok=0
    collector_up || { log "ERROR: el collector no está en marcha. Ejecute docker compose up -d"; ok=1; }
    loki_ready   || { log "ERROR: Loki no responde en ${LOKI_URL}/ready"; ok=1; }
    tempo_ready  || { log "ERROR: Tempo no responde en ${TEMPO_URL}/ready"; ok=1; }
    docker network inspect "$NETWORK" >/dev/null 2>&1 \
        || { log "ERROR: no existe la red ${NETWORK}"; ok=1; }
    docker network inspect "$TELEMETRY_NETWORK" >/dev/null 2>&1 \
        || { log "ERROR: no existe la red ${TELEMETRY_NETWORK}"; ok=1; }
    return $ok
}

# ---------------------------------------------------------------------------
main() {
    log "Ensayos de resiliencia del stack de observabilidad (OBS-027)"
    log "==========================================================="
    log ""

    preflight || exit 2

    local requested=("$@")
    [ ${#requested[@]} -eq 0 ] && requested=(loki tempo collector burst network)

    local s
    for s in "${requested[@]}"; do
        case "$s" in
            loki)      scenario_loki ;;
            tempo)     scenario_tempo ;;
            collector) scenario_collector ;;
            burst)     scenario_burst ;;
            network)   scenario_network ;;
            *)         log "escenario desconocido: ${s}"; exit 2 ;;
        esac
    done

    log "==========================================================="
    log "Resultado: ${PASSED} pasan, ${FAILED} fallan"
    if [ "$FAILED" -gt 0 ]; then
        for s in "${FAILED_NAMES[@]}"; do log "  FALLA: ${s}"; done
        exit 1
    fi
}

main "$@"
