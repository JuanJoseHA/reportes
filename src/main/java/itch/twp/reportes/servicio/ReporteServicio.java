package itch.twp.reportes.servicio;

public interface ReporteServicio {
    byte[] generarReporteColoniasPdf();
    byte[] generarReporteTiposIncidenciaPdf();
    byte[] generarReporteEstatusYPromedioPdf();
}