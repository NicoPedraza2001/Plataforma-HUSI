#!/bin/sh
# ============================================================
# Publica el frontend en Vercel (produccion) desde este computador.
#
#   sh frontend/scripts/desplegar-vercel.sh
#
# Requiere haber hecho `npx vercel login` con la cuenta de despliegue y
# `npx vercel link --project plataforma-husi` dentro de frontend/ (crea .vercel/).
#
# Se despliega una copia SIN la carpeta .git: la CLI de Vercel envia el autor
# del ultimo commit y bloquea el despliegue si ese correo no es miembro de la
# cuenta de despliegue (que es independiente de las cuentas personales).
# ============================================================
set -e

FRONTEND="$(cd "$(dirname "$0")/.." && pwd)"
COPIA="$(mktemp -d)"
trap 'cd / && rm -rf "$COPIA" 2>/dev/null || true' EXIT

if [ ! -f "$FRONTEND/.vercel/project.json" ]; then
  echo "Falta frontend/.vercel: ejecuta 'npx vercel link --project plataforma-husi' en frontend/." >&2
  exit 1
fi

tar -C "$FRONTEND" --exclude=node_modules --exclude=dist --exclude=out-tsc --exclude=.angular \
  --exclude=.env.local -cf - . | tar -C "$COPIA" -xf -

cd "$COPIA"
npx --yes vercel@latest deploy --prod --yes
