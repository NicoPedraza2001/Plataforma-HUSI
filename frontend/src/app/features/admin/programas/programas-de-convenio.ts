/**
 * Regla de negocio de los programas (espejo de backend `service/impl/ProgramasDeConvenio`):
 * solo la Pontificia Universidad Javeriana registra programas académicos con nombre propio.
 * Las demás universidades reciben residentes por «Convenio Nacional» (universidades de
 * Colombia) o «Convenio Internacional» (universidades del exterior), y el convenio siempre
 * debe corresponder al país de la universidad.
 */
export const UNIVERSIDAD_PROPIA = 'Pontificia Universidad Javeriana';
export const PAIS_NACIONAL = 'Colombia';
export const CONVENIO_NACIONAL = 'Convenio Nacional';
export const CONVENIO_INTERNACIONAL = 'Convenio Internacional';
export const PROGRAMAS_DE_CONVENIO = [CONVENIO_NACIONAL, CONVENIO_INTERNACIONAL];

/**
 * Nombre que ve el usuario para cada convenio. Es solo de presentación: lo que se guarda en
 * `programa.nombre_programa` y las comparaciones del código siguen usando el nombre de arriba.
 */
const ETIQUETAS_CONVENIO: Readonly<Record<string, string>> = {
  [CONVENIO_NACIONAL]: 'Movilidad Nacional',
  [CONVENIO_INTERNACIONAL]: 'Observador Internacional',
};

export function etiquetaConvenio(convenio: string): string {
  return ETIQUETAS_CONVENIO[convenio] ?? convenio;
}

/** Igualdad que aplica el backend: ignora mayúsculas y espacios extremos. */
function claveDeComparacion(texto: string | null | undefined): string {
  return (texto ?? '').trim().toLowerCase();
}

export function esUniversidadPropia(nombreUniversidad: string): boolean {
  return claveDeComparacion(nombreUniversidad) === claveDeComparacion(UNIVERSIDAD_PROPIA);
}

/** Nombre del convenio tal como se guarda, o null si el texto no es ninguno de los dos. */
export function buscarConvenio(nombrePrograma: string): string | null {
  return (
    PROGRAMAS_DE_CONVENIO.find(
      (convenio) => claveDeComparacion(convenio) === claveDeComparacion(nombrePrograma),
    ) ?? null
  );
}

/** Universidad de Colombia, sin importar mayúsculas ni espacios ("Colombia", "colombia"). */
export function esPaisNacional(pais: string | null | undefined): boolean {
  return claveDeComparacion(pais) === claveDeComparacion(PAIS_NACIONAL);
}

/** Convenio que corresponde a una universidad externa según su país. */
export function convenioSegunPais(pais: string | null | undefined): string {
  return esPaisNacional(pais) ? CONVENIO_NACIONAL : CONVENIO_INTERNACIONAL;
}

/**
 * Medida de seguridad ante errores de digitación (igual que el backend): una universidad de
 * Colombia solo admite «Convenio Nacional» y una del exterior solo «Convenio Internacional».
 * Devuelve el mensaje de rechazo, o null si el convenio corresponde al país. Sin país conocido
 * no se puede juzgar y la decisión queda en el backend.
 */
export function buscarErrorDeConvenioSegunPais(
  convenio: string,
  universidad: { nombre: string; pais?: string | null },
): string | null {
  const convenioEscrito = buscarConvenio(convenio);
  const pais = universidad.pais?.trim();
  if (!convenioEscrito || !pais || convenioEscrito === convenioSegunPais(pais)) return null;
  return esPaisNacional(pais)
    ? `La universidad «${universidad.nombre}» es nacional (${pais}), por ende su convenio no puede ser internacional. Usa «${etiquetaConvenio(CONVENIO_NACIONAL)}».`
    : `La universidad «${universidad.nombre}» es internacional (${pais}), por ende su convenio no puede ser nacional. Usa «${etiquetaConvenio(CONVENIO_INTERNACIONAL)}».`;
}
