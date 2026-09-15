package co.edu.javeriana.husi.dto.response;
import java.util.List;
public record PaginaResponse<T>(List<T> contenido, long totalElementos, int totalPaginas, int pagina, int tamano) {}
