package co.edu.javeriana.husi.mapper;

import java.util.Map;

/**
 * Estado de un registro en un instante, tal como se muestra en el historial.
 *
 * @param nombre texto que identifica el registro para una persona (columna «Registro»)
 * @param datos campo legible → valor, en el orden en que se muestran
 */
public record FotografiaRegistro(String nombre, Map<String, Object> datos) {}
