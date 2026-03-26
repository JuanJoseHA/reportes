package itch.twp.reportes.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import itch.twp.reportes.entity.IncidenciaLectura;
import itch.twp.reportes.dto.ReporteItemDto;
import java.util.List;

public interface ReporteRepositorio extends JpaRepository<IncidenciaLectura, Long> {

    // 1. Incidencias Globales por Colonia
    @Query(value = "SELECT c.nombre AS nombre, COUNT(i.id) AS cantidad " +
                   "FROM incidencias i JOIN ubicaciones u ON i.ubicacion_id = u.id " +
                   "JOIN colonias c ON u.colonia_id = c.id " +
                   "GROUP BY c.nombre ORDER BY cantidad DESC", nativeQuery = true)
    List<ReporteItemDto> obtenerIncidenciasPorColonia();

    // 2. Tipos de Incidencias más frecuentes
    @Query(value = "SELECT t.nombre AS nombre, COUNT(i.id) AS cantidad " +
                   "FROM incidencias i JOIN tipos_incidencias t ON i.tipo_id = t.id " +
                   "GROUP BY t.nombre ORDER BY cantidad DESC", nativeQuery = true)
    List<ReporteItemDto> obtenerTopTiposIncidencias();

    // 3. Carga de Trabajo por Departamento
    @Query(value = "SELECT d.nombre AS nombre, COUNT(i.id) AS cantidad " +
                   "FROM incidencias i JOIN departamentos d ON i.departamento_id = d.id " +
                   "GROUP BY d.nombre ORDER BY cantidad DESC", nativeQuery = true)
    List<ReporteItemDto> obtenerCargaPorDepartamento();

    // 4. Estatus actual (Transparencia)
    @Query(value = "SELECT e.nombre AS nombre, COUNT(i.id) AS cantidad " +
                   "FROM incidencias i JOIN estados_incidencias e ON i.estado_id = e.id " +
                   "GROUP BY e.nombre ORDER BY cantidad DESC", nativeQuery = true)
    List<ReporteItemDto> obtenerEstatusGlobal();

    // 5. Tiempo Promedio de Resolución (El que ya teníamos)
    @Query(value = "SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, fecha_reporte, fecha_resolucion)), 0) " +
                   "FROM incidencias WHERE fecha_resolucion IS NOT NULL", nativeQuery = true)
    Double obtenerTiempoPromedioResolucion();
}