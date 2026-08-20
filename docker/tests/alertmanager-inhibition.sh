#!/usr/bin/env bash
# Contrato de inhibicion del Alertmanager local (issue #88).
#
# POR QUE ESTE SCRIPT Y NO promtool:
#   `promtool test rules` evalua reglas de Prometheus. NO conoce el Alertmanager y por tanto NO
#   evalua `inhibit_rules`: una inhibicion mal escrita pasa todos los gates del repositorio en
#   verde. `amtool check-config` tampoco alcanza — valida la SINTAXIS, y la configuracion rota
#   que motivo este script era sintacticamente impecable.
#
#   La unica forma de comprobar una inhibicion es levantar un Alertmanager de verdad, empujarle
#   alertas por la API y leer que quedo suprimido. Eso hace este script, con la configuracion
#   REAL del repositorio (docker/alertmanager.yml), no con una copia de laboratorio.
#
# EL DEFECTO QUE VIGILA:
#   Alertmanager considera que una etiqueta de `equal` coincide cuando vale lo mismo en las dos
#   alertas — y "ausente en las dos" cuenta como coincidencia. Con `equal: [domain, instance]`,
#   las alertas agregadas (las que no llevan `instance` porque su expr parte de reglas de
#   grabacion ya agregadas) satisfacian el emparejamiento por el mero hecho de compartir
#   dominio, asi que un critical silenciaba warnings de OTRO problema del mismo dominio. El
#   sintoma es la ausencia de un correo: no hay error, no hay traza, no hay nada que revisar.
#   Por eso hace falta una prueba y no una lectura.
#
# Uso:  bash docker/tests/alertmanager-inhibition.sh
# Requisitos: Docker y curl. NO requiere el stack levantado — arranca su propio Alertmanager
#             efimero en un puerto de loopback y lo destruye al terminar.

set -uo pipefail

# Git Bash traduce las rutas absolutas de `-v` y de `docker exec`; sin esto el montaje falla en
# Windows y el script solo seria ejecutable en Linux.
export MSYS_NO_PATHCONV=1

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG="${ALERTMANAGER_CONFIG:-$REPO_ROOT/docker/alertmanager.yml}"
CONTAINER="vetsoftware_alertmanager_inhibition_test"
HOST_PORT="${ALERTMANAGER_TEST_PORT:-19093}"

# La imagen sale del lock, no de un literal: el comportamiento de `equal` es propiedad de una
# version concreta y probar con otra no probaria el stack local.
IMAGE="$(awk '$1 == "alertmanager" { print $2 "@" $3 }' "$REPO_ROOT/docker/image-versions.lock")"
[ -n "$IMAGE" ] || { echo "No hay linea 'alertmanager' en docker/image-versions.lock"; exit 1; }

# El pipeline de notificacion es quien marca una alerta como suprimida, y no corre hasta que el
# grupo se vacia. El `group_wait` del arbol real es de 15 s para el receptor por defecto (por el
# que salen los warning); 30 s da margen sin volver el script fragil.
SETTLE_SECONDS="${INHIBITION_SETTLE_SECONDS:-30}"

PASSED=0
FAILED=0
FAILED_NAMES=()

pass() { PASSED=$((PASSED + 1)); printf '  \033[32mPASA\033[0m  %s\n' "$1"; }
fail() {
    FAILED=$((FAILED + 1))
    FAILED_NAMES+=("$1")
    printf '  \033[31mFALLA\033[0m %s\n         %s\n' "$1" "$2"
}

cleanup() { docker rm -f "$CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT

echo "Alertmanager: $IMAGE"
echo "Configuracion: $CONFIG"
cleanup

docker run -d --name "$CONTAINER" \
    -p "127.0.0.1:${HOST_PORT}:9093" \
    -v "${CONFIG}:/etc/alertmanager/alertmanager.yml:ro" \
    --entrypoint /bin/alertmanager \
    "$IMAGE" \
    --config.file=/etc/alertmanager/alertmanager.yml \
    --storage.path=/alertmanager \
    --enable-feature=utf8-strict-mode >/dev/null \
    || { echo "No se pudo arrancar el contenedor"; exit 1; }

printf 'Esperando a que el Alertmanager este listo'
for _ in $(seq 1 30); do
    if curl -fsS "http://127.0.0.1:${HOST_PORT}/-/ready" >/dev/null 2>&1; then
        printf ' listo\n'
        break
    fi
    printf '.'
    sleep 1
done
if ! curl -fsS "http://127.0.0.1:${HOST_PORT}/-/ready" >/dev/null 2>&1; then
    printf '\n'
    docker logs "$CONTAINER" 2>&1 | tail -20
    echo "El Alertmanager no llego a estar listo"
    exit 1
fi

STARTS_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
ENDS_AT="$(date -u -d '+1 hour' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+1H +%Y-%m-%dT%H:%M:%SZ)"

# ---------------------------------------------------------------------------
# Muestra de entrada.
#
# Cinco de las ocho alertas son AGREGADAS —sin `instance`— porque ese es justamente el caso en
# que `equal` mentia. Las tres de VetSoftwarePruebaEscalonSeveridad son sinteticas a proposito:
# hoy ningun alertname del repositorio se emite con dos severidades, y son las que comprueban
# que la regla generica sigue sirviendo para lo que se escribio.
# ---------------------------------------------------------------------------
read -r -d '' PAYLOAD <<JSON
[
  {"labels":{"alertname":"VetSoftwareSloFastBurn","severity":"critical","domain":"slo","slo":"animales-disponibilidad","sli":"availability"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwareSloSlowBurn","severity":"warning","domain":"slo","slo":"animales-disponibilidad","sli":"availability"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwareSloSlowBurn","severity":"warning","domain":"slo","slo":"facturacion-latencia","sli":"latency"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwareLokiDiscardingLogs","severity":"critical","domain":"observability"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwareOtelQueueNearCapacity","severity":"warning","domain":"observability"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwarePruebaEscalonSeveridad","severity":"critical","domain":"runtime","instance":"backend:8080"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwarePruebaEscalonSeveridad","severity":"warning","domain":"runtime","instance":"backend:8080"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"},
  {"labels":{"alertname":"VetSoftwarePruebaEscalonSeveridad","severity":"warning","domain":"runtime","instance":"backend:9090"},
   "startsAt":"$STARTS_AT","endsAt":"$ENDS_AT"}
]
JSON

curl -fsS -X POST -H 'Content-Type: application/json' \
    --data "$PAYLOAD" "http://127.0.0.1:${HOST_PORT}/api/v2/alerts" >/dev/null \
    || { echo "No se pudieron empujar las alertas"; exit 1; }

printf 'Alertas empujadas. Esperando %ss al pipeline de notificacion...\n' "$SETTLE_SECONDS"
sleep "$SETTLE_SECONDS"

# La consulta se hace con el amtool de la propia imagen: no exige jq en el host y filtra por
# matchers en vez de recortar JSON con grep, que es fragil.
#
# Las tres banderas son obligatorias y es el detalle que hace o rompe esta prueba: amtool
# documenta que "si no se da ninguna de estas banderas, solo se devuelven las alertas activas".
# Son filtros de INCLUSION, no de seleccion: pedir solo --inhibited deja fuera las activas y una
# alerta que si notifica se leeria como inexistente. Con las tres, la columna State del listado
# ("active" o "suppressed") es el veredicto directo.
consulta() {
    docker exec "$CONTAINER" amtool alert query --alertmanager.url=http://127.0.0.1:9093 \
        --active --inhibited --silenced "$@" 2>/dev/null | tail -n +2 | awk 'NF { print $NF }'
}

comprobar() {
    local nombre="$1" esperado="$2"; shift 2
    local salida real
    salida="$(consulta "$@")"

    if [ -z "$salida" ]; then
        real="ausente"
    elif [ "$(printf '%s\n' "$salida" | grep -c .)" -ne 1 ]; then
        # Mas de una fila significa que los matchers no discriminan lo que se cree que
        # discriminan, y el veredicto seria el de una alerta cualquiera del conjunto.
        real="ambigua($(printf '%s' "$salida" | tr '\n' ',' ))"
    else
        real="$salida"
    fi

    if [ "$real" = "$esperado" ]; then
        pass "$nombre"
    else
        fail "$nombre" "estado '$real', se esperaba '$esperado' (matchers: $*)"
    fi
}

echo
echo "Contrato de inhibicion:"

# 1. La cascada legitima de burn rate sigue viva (regla especifica, equal: [slo, sli]).
comprobar "el burn rate lento del MISMO SLO queda inhibido por el rapido" suppressed \
    'alertname="VetSoftwareSloSlowBurn"' 'slo="animales-disponibilidad"'

# 2. La regresion de #88: el mismo warning de OTRO SLO ya no se silencia.
comprobar "el burn rate lento de OTRO SLO sigue notificando" active \
    'alertname="VetSoftwareSloSlowBurn"' 'slo="facturacion-latencia"'

# 3. Dos sintomas distintos del dominio observability no se tapan entre si.
comprobar "un critical de observability no silencia otro sintoma del mismo dominio" active \
    'alertname="VetSoftwareOtelQueueNearCapacity"'

# 4. La regla generica sigue haciendo su trabajo cuando SI aplica: mismo alertname, misma
#    instancia, dos escalones de severidad. El matcher lleva `severity` porque sin el la
#    consulta devuelve tambien el critical de origen y el veredicto seria el suyo.
comprobar "el escalon warning del MISMO alertname e instancia queda inhibido" suppressed \
    'alertname="VetSoftwarePruebaEscalonSeveridad"' 'instance="backend:8080"' 'severity="warning"'

# 5. ...y no se pasa de largo: el mismo alertname en otra instancia sigue notificando.
comprobar "el mismo alertname en OTRA instancia sigue notificando" active \
    'alertname="VetSoftwarePruebaEscalonSeveridad"' 'instance="backend:9090"' 'severity="warning"'

echo
printf 'Resultado: %s pasan, %s fallan\n' "$PASSED" "$FAILED"
if [ "$FAILED" -gt 0 ]; then
    printf 'Fallaron: %s\n' "${FAILED_NAMES[*]}"
    exit 1
fi
