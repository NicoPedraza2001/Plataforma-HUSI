// Sin caracteres ambiguos (0/O, 1/l/I) para que el administrador pueda dictarla sin errores.
const MAYUSCULAS = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
const MINUSCULAS = 'abcdefghijkmnopqrstuvwxyz';
const DIGITOS = '23456789';
const SIMBOLOS = '!@#$%*-_?';
const TODOS = MAYUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;

/** Índice aleatorio uniforme en [0, max) con `crypto.getRandomValues` (sin sesgo de módulo). */
function indiceAleatorio(max: number): number {
  const limite = Math.floor(0x1_0000_0000 / max) * max;
  const buffer = new Uint32Array(1);
  do crypto.getRandomValues(buffer);
  while (buffer[0] >= limite);
  return buffer[0] % max;
}

function caracter(conjunto: string): string {
  return conjunto[indiceAleatorio(conjunto.length)];
}

/**
 * Contraseña sugerida para un usuario nuevo: ASCII (1 byte por carácter, cumple los 8 caracteres
 * y 72 bytes del backend) con al menos una mayúscula, una minúscula, un dígito y un símbolo.
 */
export function generarContrasena(longitud = 12): string {
  const caracteres = [
    caracter(MAYUSCULAS),
    caracter(MINUSCULAS),
    caracter(DIGITOS),
    caracter(SIMBOLOS),
  ];
  while (caracteres.length < longitud) caracteres.push(caracter(TODOS));
  for (let i = caracteres.length - 1; i > 0; i--) {
    const j = indiceAleatorio(i + 1);
    [caracteres[i], caracteres[j]] = [caracteres[j], caracteres[i]];
  }
  return caracteres.join('');
}
