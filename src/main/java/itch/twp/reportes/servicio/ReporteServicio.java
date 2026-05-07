package itch.twp.reportes.servicio;

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
}

