package itch.twp.reportes.dto;

// Spring JPA inyectará los resultados de la consulta SQL aquí automáticamente
public interface BachesPorColoniaDto {
    String getNombreColonia();
    Integer getTotalIncidencias();
}