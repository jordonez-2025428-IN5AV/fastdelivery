#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
PASSWORD="${TEST_PASSWORD:-123456}"
command -v curl >/dev/null || { echo '[FAIL] Instala curl'; exit 1; }
command -v python3 >/dev/null || { echo '[FAIL] Instala Python 3 para leer JSON'; exit 1; }
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
extract() { python3 -c 'import json,sys; v=json.load(open(sys.argv[1])); print(v[sys.argv[2]])' "$WORK/response.json" "$1"; }
request() {
 local label="$1" expected="$2" method="$3" route="$4" token="${5:-}" body="${6:-}"
 local args=(-sS --connect-timeout 5 --max-time 20 -o "$WORK/response.json" -w '%{http_code}' -X "$method")
 if [[ -n "$token" ]]; then args+=(-H "Authorization: Bearer $token"); fi
 if [[ -n "$body" ]]; then args+=(-H 'Content-Type: application/json' --data "$body"); fi
 local code
 code=$(curl "${args[@]}" "$BASE_URL$route") || { echo "[FAIL] $label: no se pudo conectar"; exit 1; }
 if [[ "$code" == "$expected" ]]; then echo "[PASS] $label ($code)"; else echo "[FAIL] $label: esperado $expected, recibido $code"; cat "$WORK/response.json"; exit 1; fi
}
# Crea datos propios por ejecución para no consumir el stock del catálogo inicial.
request 'Login ADMIN' 200 POST /api/v1/auth/login '' "{\"email\":\"admin@fastdelivery.com\",\"password\":\"$PASSWORD\"}"
ADMIN=$(extract token)
request 'Login CLIENTE' 200 POST /api/v1/auth/login '' "{\"email\":\"cliente@fastdelivery.com\",\"password\":\"$PASSWORD\"}"
CLIENTE=$(extract token)
request 'Login REPARTIDOR' 200 POST /api/v1/auth/login '' "{\"email\":\"repartidor@fastdelivery.com\",\"password\":\"$PASSWORD\"}"
REPARTIDOR=$(extract token)
request 'Consulta sin JWT' 401 GET /api/v1/comercios
request 'Consultar comercios' 200 GET /api/v1/comercios "$CLIENTE"
request 'Crear comercio' 201 POST /api/v1/comercios "$ADMIN" "{\"nombre\":\"Prueba curl $(date +%s)\",\"categoria\":\"RESTAURANTE\",\"direccion\":\"Guatemala\",\"abierto\":true}"
COMERCIO=$(extract id)
request 'Crear producto' 201 POST "/api/v1/comercios/$COMERCIO/productos" "$ADMIN" '{"nombre":"Producto curl","precio":30.00,"stock":10,"disponible":true}'
PRODUCTO=$(extract id)
request 'Consultar productos' 200 GET "/api/v1/comercios/$COMERCIO/productos" "$CLIENTE"
request 'Crear pedido' 201 POST /api/v1/pedidos "$CLIENTE" "{\"items\":[{\"productoId\":$PRODUCTO,\"cantidad\":2}]}"
PEDIDO=$(extract id)
python3 - "$WORK/response.json" <<'PY'
import json,sys
from decimal import Decimal
v=json.load(open(sys.argv[1]),parse_float=Decimal)
assert v['costoEnvio']==Decimal('20.00') and v['montoTotal']==Decimal('80.00')
assert v['detalles'][0]['subtotal']==Decimal('60.00')
print('[PASS] Subtotal Q60, envío Q20 y total Q80')
PY
request 'Mis pedidos' 200 GET /api/v1/pedidos/mis-pedidos "$CLIENTE"
request 'Pedidos disponibles' 200 GET /api/v1/pedidos/disponibles "$REPARTIDOR"
request 'Rechazar salto a ENTREGADO' 409 PATCH "/api/v1/pedidos/$PEDIDO/estado" "$ADMIN" '{"estado":"ENTREGADO"}'
request 'Preparar y asignar al repartidor autenticado' 200 PATCH "/api/v1/pedidos/$PEDIDO/estado" "$REPARTIDOR" '{"estado":"EN_PREPARACION"}'
request 'Rechazar cancelación en preparación' 409 PATCH "/api/v1/pedidos/$PEDIDO/cancelar" "$CLIENTE"
request 'Iniciar entrega' 200 PATCH "/api/v1/pedidos/$PEDIDO/estado" "$REPARTIDOR" '{"estado":"EN_CAMINO"}'
request 'Entregar' 200 PATCH "/api/v1/pedidos/$PEDIDO/estado" "$REPARTIDOR" '{"estado":"ENTREGADO"}'
request 'Crear pedido para cancelar' 201 POST /api/v1/pedidos "$CLIENTE" "{\"items\":[{\"productoId\":$PRODUCTO,\"cantidad\":1}]}"
CANCELAR=$(extract id)
request 'Cancelar pendiente' 200 PATCH "/api/v1/pedidos/$CANCELAR/cancelar" "$CLIENTE"
request 'No cancelar dos veces' 409 PATCH "/api/v1/pedidos/$CANCELAR/cancelar" "$CLIENTE"
request 'Consultar stock final' 200 GET "/api/v1/comercios/$COMERCIO/productos" "$CLIENTE"
python3 - "$WORK/response.json" <<'PY'
import json,sys
assert json.load(open(sys.argv[1]))[0]['stock']==8
print('[PASS] Stock final 8: descuento y restauración correctos')
PY
echo '[PASS] Todas las pruebas curl completadas'
