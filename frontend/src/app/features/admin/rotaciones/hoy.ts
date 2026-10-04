/** Fecha de hoy en Bogotá como `yyyy-MM-dd`, el formato de las fechas de rotación. */
export function hoyBogota(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date());
}
