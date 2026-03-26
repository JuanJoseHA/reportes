package itch.twp.reportes.servicio.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import itch.twp.reportes.client.IncidenciaClient;
import itch.twp.reportes.dto.IncidenciaDTO;
import itch.twp.reportes.dto.ReporteItemDto;
import itch.twp.reportes.servicio.ReporteServicio;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor 
public class ReporteServicioImp implements ReporteServicio {

    private IncidenciaClient incidenciaClient;

    private final Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
    private final Font subtituloFont = new Font(Font.HELVETICA, 14, Font.BOLD);
    private final Font textoFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
    private final Font cursivaFont = new Font(Font.HELVETICA, 12, Font.ITALIC);

    @Override
    public byte[] generarReporteColoniasPdf() {
        List<IncidenciaDTO> todas = incidenciaClient.listarParaEstadisticas();

        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getColonia() != null && !i.getColonia().trim().isEmpty())
            .collect(Collectors.groupingBy(IncidenciaDTO::getColonia, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Análisis Geográfico de Incidencias");

            document.add(new Paragraph("El presente documento detalla la distribución geográfica de los reportes ciudadanos.", textoFont));
            document.add(new Paragraph("\n"));

            if (!datos.isEmpty()) {
                ReporteItemDto top1 = datos.get(0);
                document.add(new Paragraph("Hallazgo Principal: Se observa que la colonia '" + top1.getNombre() + 
                        "' concentra la mayor cantidad de reportes.", cursivaFont));
                document.add(new Paragraph("\n"));
            }

            imprimirLista(document, "Desglose por Colonia:", datos);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de colonias", e);
        }
    }

    @Override
    public byte[] generarReporteTiposIncidenciaPdf() {
        List<IncidenciaDTO> todas = incidenciaClient.listarParaEstadisticas();

        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getTipoId() != null)
            .collect(Collectors.groupingBy(i -> "Categoría ID: " + i.getTipoId(), Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Problemas Urbanos Frecuentes");

            document.add(new Paragraph("Este informe expone las categorías de incidencias más recurrentes reportadas.", textoFont));
            document.add(new Paragraph("\n"));

            imprimirLista(document, "Categorías Reportadas:", datos);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de tipos", e);
        }
    }

    @Override
    public byte[] generarReporteEstatusYPromedioPdf() {
        List<IncidenciaDTO> todas = incidenciaClient.listarParaEstadisticas();

        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getNombreEstadoActual() != null)
            .collect(Collectors.groupingBy(IncidenciaDTO::getNombreEstadoActual, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Métricas de Eficiencia y Transparencia");

            document.add(new Paragraph("Este reporte refleja el nivel de atención frente a las demandas ciudadanas.", textoFont));
            document.add(new Paragraph("\n"));

            imprimirLista(document, "Estado Actual de los Reportes (Volumen):", datos);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de estatus", e);
        }
    }

    private Document iniciarDocumento(ByteArrayOutputStream baos, String tituloReporte) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();
        document.add(new Paragraph(tituloReporte, tituloFont));
        document.add(new Paragraph("Sistema de Incidencias", new Font(Font.HELVETICA, 12, Font.BOLDITALIC)));
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textoFont));
        document.add(new Paragraph("-----------------------------------------------------------------------------------------"));
        document.add(new Paragraph("\n"));
        return document;
    }

    private void imprimirLista(Document document, String titulo, List<ReporteItemDto> datos) throws Exception {
        document.add(new Paragraph(titulo, subtituloFont));
        if (datos.isEmpty()) {
            document.add(new Paragraph("   Sin datos registrados.", textoFont));
        } else {
            for (ReporteItemDto item : datos) {
                document.add(new Paragraph("   • " + item.getNombre() + " : " + item.getCantidad() + " incidencias", textoFont));
            }
        }
    }
}