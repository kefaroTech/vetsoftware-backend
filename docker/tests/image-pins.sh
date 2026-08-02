#!/usr/bin/env bash
# Guarda de versiones de imagen del stack (OBS-024).
#
# Comprueba tres cosas, en orden de coste creciente:
#
#   1. Ningún servicio de docker-compose.yml usa un tag móvil (`latest`, un major/minor suelto como
#      `8.0` o `3`, un alias de canal como `lts-community`, o un tag ausente).
#   2. Compose y docker/image-versions.lock coinciden servicio por servicio, en los dos sentidos: un
#      servicio nuevo sin línea en el lock falla, y una línea del lock sin servicio también.
#   3. Con `--digests`: el digest que el registro sirve HOY para cada tag fijado es el del lock.
#      Requiere red. Es la única comprobación que detecta un tag re-empujado.
#
# El punto 1 sin el 3 sería una falsa tranquilidad: un tag de versión concreta es inmutable por
# convención, no por diseño del registro.
#
# Uso:  bash docker/tests/image-pins.sh              # puntos 1 y 2, sin red
#       bash docker/tests/image-pins.sh --digests    # los tres
#       bash docker/tests/image-pins.sh --update     # reescribe el lock desde compose + registro
#
# Salida: 0 si todo cuadra, 1 si algo falla. Apto para CI.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
LOCK_FILE="$ROOT_DIR/docker/image-versions.lock"

CHECK_DIGESTS=0
UPDATE_LOCK=0

for arg in "$@"; do
    case "$arg" in
        --digests) CHECK_DIGESTS=1 ;;
        --update)  UPDATE_LOCK=1; CHECK_DIGESTS=1 ;;
        -h|--help) sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)         printf 'Argumento no reconocido: %s\n' "$arg" >&2; exit 2 ;;
    esac
done

FAILED=0

fail() {
    FAILED=$((FAILED + 1))
    printf '  \033[31mFALLA\033[0m %s\n         %s\n' "$1" "$2"
}

pass() { printf '  \033[32mOK\033[0m    %s\n' "$1"; }

# ---------------------------------------------------------------------------
# Lectura de compose
# ---------------------------------------------------------------------------

# Extrae "<servicio> <imagen:tag>" de docker-compose.yml. Un servicio es una clave con exactamente
# dos espacios de indentación; su `image:` va con cuatro. Se usa awk y no `docker compose config`
# para que la guarda funcione sin daemon ni variables de entorno resueltas.
read_compose() {
    awk '
        /^  [a-z0-9_-]+:[[:space:]]*$/ { svc = $1; sub(/:$/, "", svc); next }
        /^    image:[[:space:]]/       { if (svc != "") { print svc, $2; svc = "" } }
    ' "$COMPOSE_FILE"
}

# Extrae "<servicio> <imagen:tag> <digest>" del lock, sin comentarios ni líneas vacías.
read_lock() {
    grep -v '^[[:space:]]*#' "$LOCK_FILE" | awk 'NF >= 3 { print $1, $2, $3 }'
}

# Digest de la lista de manifiestos que el registro sirve hoy para un tag.
#
# `docker manifest inspect -v` devuelve el descriptor de cada manifiesto de plataforma, no el de la
# lista. El digest de la lista es el que `docker image inspect` reporta en RepoDigests tras un pull,
# así que se resuelve con `buildx imagetools` (que sí lo expone) y se cae a pull+inspect si no está.
resolve_digest() {
    local ref=$1 digest=""

    digest=$(docker buildx imagetools inspect "$ref" --format '{{.Manifest.Digest}}' 2>/dev/null)
    if [[ "$digest" == sha256:* ]]; then
        printf '%s' "$digest"
        return 0
    fi

    docker pull -q "$ref" >/dev/null 2>&1 || return 1
    digest=$(docker image inspect "$ref" --format '{{index .RepoDigests 0}}' 2>/dev/null | sed 's/.*@//')
    [[ "$digest" == sha256:* ]] || return 1
    printf '%s' "$digest"
}

# ---------------------------------------------------------------------------
# 1. Tags móviles
# ---------------------------------------------------------------------------

# Un tag se considera fijado si contiene al menos dos puntos separando dígitos (`3.7.1`, `v0.32.1`,
# `9.9.8-community`, `1.37.0`). Eso rechaza `8.0` y `3` a propósito: en esos publicadores son tags
# móviles que reciben cada parche.
is_pinned_tag() {
    local tag=$1
    [[ "$tag" =~ ^v?[0-9]+\.[0-9]+\.[0-9]+ ]]
}

printf '\n=== Tags fijados en docker-compose.yml ===\n'

COMPOSE_ENTRIES=$(read_compose)

if [ -z "$COMPOSE_ENTRIES" ]; then
    fail "lectura de compose" "no se encontró ningún 'image:' en $COMPOSE_FILE"
fi

while read -r svc ref; do
    [ -n "$svc" ] || continue
    tag="${ref##*:}"

    if [ "$ref" = "$tag" ]; then
        fail "$svc" "la imagen '$ref' no lleva tag; Docker asumiría ':latest'"
    elif ! is_pinned_tag "$tag"; then
        fail "$svc" "tag móvil '$tag' en '$ref'; usa una versión concreta (x.y.z)"
    else
        pass "$svc → $ref"
    fi
done <<< "$COMPOSE_ENTRIES"

# ---------------------------------------------------------------------------
# 2. Compose ↔ lock
# ---------------------------------------------------------------------------

printf '\n=== Coherencia con docker/image-versions.lock ===\n'

if [ ! -f "$LOCK_FILE" ]; then
    fail "lock" "no existe $LOCK_FILE"
else
    LOCK_ENTRIES=$(read_lock)

    while read -r svc ref; do
        [ -n "$svc" ] || continue
        locked_ref=$(awk -v s="$svc" '$1 == s { print $2 }' <<< "$LOCK_ENTRIES")

        if [ -z "$locked_ref" ]; then
            fail "$svc" "está en compose pero no en el lock; añádelo con --update"
        elif [ "$locked_ref" != "$ref" ]; then
            fail "$svc" "compose usa '$ref' y el lock dice '$locked_ref'"
        else
            pass "$svc coincide con el lock"
        fi
    done <<< "$COMPOSE_ENTRIES"

    while read -r svc _ref _digest; do
        [ -n "$svc" ] || continue
        if ! awk -v s="$svc" '$1 == s { found = 1 } END { exit !found }' <<< "$COMPOSE_ENTRIES"; then
            fail "$svc" "está en el lock pero ya no existe en compose; quítalo del lock"
        fi
    done <<< "$LOCK_ENTRIES"
fi

# ---------------------------------------------------------------------------
# 3. Digests contra el registro
# ---------------------------------------------------------------------------

if [ "$CHECK_DIGESTS" = "1" ]; then
    printf '\n=== Digests servidos por el registro ===\n'

    UPDATED_LINES=()

    while read -r svc ref; do
        [ -n "$svc" ] || continue

        actual=$(resolve_digest "$ref")
        if [ -z "$actual" ]; then
            fail "$svc" "no se pudo resolver el digest de '$ref' (¿sin red o tag inexistente?)"
            continue
        fi

        expected=$(awk -v s="$svc" '$1 == s { print $3 }' <<< "${LOCK_ENTRIES:-}")
        UPDATED_LINES+=("$(printf '%-16s %-45s %s' "$svc" "$ref" "$actual")")

        if [ "$UPDATE_LOCK" = "1" ]; then
            pass "$svc → $actual"
        elif [ -z "$expected" ]; then
            fail "$svc" "sin digest en el lock para '$ref'"
        elif [ "$expected" != "$actual" ]; then
            fail "$svc" "el tag '$ref' cambió de digest:
           lock:     $expected
           registro: $actual
         Un tag de versión concreta no debería moverse. Investiga antes de regenerar el lock."
        else
            pass "$svc → digest intacto"
        fi
    done <<< "$COMPOSE_ENTRIES"

    if [ "$UPDATE_LOCK" = "1" ] && [ "$FAILED" -eq 0 ]; then
        # Conserva la cabecera de comentarios y reescribe solo las líneas de datos.
        tmp="${LOCK_FILE}.tmp"
        awk '/^[[:space:]]*#/ || NF == 0 { print; next } { exit }' "$LOCK_FILE" > "$tmp"
        printf '%s\n' "${UPDATED_LINES[@]}" >> "$tmp"
        mv "$tmp" "$LOCK_FILE"
        printf '\nLock regenerado: %s\n' "$LOCK_FILE"
    fi
fi

# ---------------------------------------------------------------------------

printf '\n'
if [ "$FAILED" -eq 0 ]; then
    printf '\033[32mTodas las imágenes están fijadas y coinciden con el lock.\033[0m\n'
    exit 0
fi

printf '\033[31m%d comprobación(es) fallida(s).\033[0m Ver docs/MATRIZ_VERSIONES_STACK.md\n' "$FAILED"
exit 1
