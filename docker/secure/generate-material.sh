#!/usr/bin/env bash
# Material de TLS y autenticación para docker-compose.secure.yml (OBS-025).
#
# Genera en docker/secure/material/:
#   ca.crt / ca.key            autoridad propia del laboratorio
#   collector.crt / .key       certificado de servidor del receptor OTLP
#   grafana.crt / .key         certificado de servidor de Grafana
#   otlp.htpasswd              credencial de los emisores OTLP (bcrypt)
#   otlp.authorization         cabecera lista para OTEL_EXPORTER_OTLP_HEADERS
#
# ESTO ES MATERIAL DE LABORATORIO. Una CA propia sirve para probar el camino TLS y para un ambiente
# compartido interno, no para uno expuesto a terceros: allí el certificado debe venir de una CA que
# los clientes ya confíen, y la credencial del gestor de secretos de la plataforma.
#
# Uso:  bash docker/secure/generate-material.sh [usuario]
#       OTLP_PASSWORD=... bash docker/secure/generate-material.sh backend
#
# Requiere openssl (viene con Git Bash) y Docker (para generar el bcrypt con la imagen de httpd).

set -euo pipefail

# Git Bash convierte cualquier argumento que empiece por `/` en una ruta de Windows, así que
# `-subj "/CN=..."` llegaría a openssl como `C:/Program Files/Git/CN=...`. Estas dos variables
# desactivan la conversión; en Linux y macOS no tienen efecto.
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL='*'

USERNAME="${1:-backend}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/material"
DAYS="${CERT_DAYS:-365}"

# SANs: los nombres por los que se alcanza cada servicio. `localhost`/`127.0.0.1` para el backend en
# el host, el nombre del servicio para los contenedores, y `${OBS_EXTERNAL_HOST}` para el nombre DNS
# real cuando el stack es compartido.
EXTERNAL_HOST="${OBS_EXTERNAL_HOST:-}"

mkdir -p "$DIR"
cd "$DIR"

if [ -z "${OTLP_PASSWORD:-}" ]; then
    OTLP_PASSWORD="$(openssl rand -base64 24 | tr -d '\n=/+' | cut -c1-24)"
    GENERATED_PASSWORD=1
fi

san_for() {
    local service=$1
    local san="DNS:localhost,DNS:${service},DNS:vetsoftware_${service//-/_},IP:127.0.0.1"
    [ -n "$EXTERNAL_HOST" ] && san="${san},DNS:${EXTERNAL_HOST}"
    echo "$san"
}

echo "→ CA del laboratorio"
openssl req -x509 -newkey rsa:2048 -sha256 -days $((DAYS * 2)) -nodes \
    -keyout ca.key -out ca.crt \
    -subj "/CN=VetSoftware Observability Lab CA" 2>/dev/null

issue_cert() {
    local name=$1 cn=$2 service=$3 san
    san="$(san_for "$service")"
    echo "→ certificado de ${name} (${san})"
    openssl req -newkey rsa:2048 -sha256 -nodes \
        -keyout "${name}.key" -out "${name}.csr" \
        -subj "/CN=${cn}" 2>/dev/null
    printf 'subjectAltName=%s\nextendedKeyUsage=serverAuth\n' "$san" > "${name}.ext"
    openssl x509 -req -in "${name}.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
        -days "$DAYS" -sha256 -out "${name}.crt" \
        -extfile "${name}.ext" 2>/dev/null
    rm -f "${name}.csr" "${name}.ext"
}

issue_cert collector "vetsoftware-otel-collector" otel-collector
issue_cert grafana "vetsoftware-grafana" grafana

echo "→ htpasswd del receptor OTLP (bcrypt)"
# El bcrypt se genera con la imagen de httpd para no depender de que el host tenga `htpasswd`.
docker run --rm httpd:2.4-alpine htpasswd -nbB "$USERNAME" "$OTLP_PASSWORD" > otlp.htpasswd

printf 'Authorization=Basic%%20%s\n' "$(printf '%s:%s' "$USERNAME" "$OTLP_PASSWORD" | openssl base64 -A)" \
    > otlp.authorization

chmod 600 ca.key collector.key grafana.key otlp.htpasswd otlp.authorization 2>/dev/null || true

echo
echo "Material en docker/secure/material/ (ignorado por git)."
echo
echo "El emisor OTLP necesita, además del endpoint https:"
echo "  OTEL_EXPORTER_OTLP_HEADERS=$(cat otlp.authorization)"
echo "  y confiar en ca.crt (OTEL_EXPORTER_OTLP_CERTIFICATE=<ruta>/ca.crt)"
if [ "${GENERATED_PASSWORD:-0}" = "1" ]; then
    echo
    echo "Contraseña generada para '${USERNAME}': ${OTLP_PASSWORD}"
    echo "No queda guardada en claro en ningún archivo salvo otlp.authorization; anótela ahora."
fi
