package itch.twp.reportes.servicio;

import java.util.List;

import itch.twp.reportes.dto.IncidenciaDTO;

public interface ReporteServicio {
	byte[] generarReporteColoniasPdf(String inicio, String fin);
    byte[] generarReporteTiposIncidenciaPdf(String inicio, String fin);
    byte[] generarReporteEstatusYPromedioPdf(String inicio, String fin);
    byte[] generarReporteDepartamentosPdf(String inicio, String fin);
    byte[] generarReportePersonalPdf(String inicio, String fin);
    byte[] generarReporteUsuariosPdf(String inicio, String fin);
    byte[] generarReporteClimaPdf(String inicio, String fin);
    byte[] generarPdfPrueba();
 // En ReporteServicio.java
    byte[] generarReportePorPeriodoPdf(String inicio, String fin);
    byte[] generarReporteDetalladoIncidenciaPdf(Integer id);
    List<IncidenciaDTO> obtenerIncidencias(String inicio, String fin); // Cambiado a String
}

