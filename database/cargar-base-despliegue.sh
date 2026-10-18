#!/bin/sh
# ============================================================
# Carga database/init/*.sql en la base de despliegue (Neon), en el mismo
# orden que usa docker-compose. Solo para una base VACIA: los scripts crean
# tablas e insertan datos, no son repetibles.
#
#   DATABASE_URL='postgresql://usuario:clave@host/base?sslmode=require' \
#     sh database/cargar-base-despliegue.sh
#
# Usa psql de la imagen postgres:18.6, asi no hace falta instalarlo.
# ============================================================
set -e

if [ -z "$DATABASE_URL" ]; then
  echo "Falta DATABASE_URL (cadena de conexion directa de Neon)." >&2
  exit 1
fi

INIT_DIR="$(cd "$(dirname "$0")/init" && (pwd -W 2>/dev/null || pwd))"

MSYS_NO_PATHCONV=1 docker run --rm -e DATABASE_URL -v "$INIT_DIR:/init:ro" postgres:18.6 \
  sh -c 'for f in /init/*.sql; do echo "== $f"; psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -q -f "$f"; done'
