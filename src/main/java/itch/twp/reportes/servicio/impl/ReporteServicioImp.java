package itch.twp.reportes.servicio.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import itch.twp.reportes.client.IncidenciaClient;
import itch.twp.reportes.dto.IncidenciaDTO;
import itch.twp.reportes.dto.ReporteItemDto;
import itch.twp.reportes.servicio.ReporteServicio;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteServicioImp implements ReporteServicio {

    private final IncidenciaClient incidenciaClient;

    private static final float HEADER_HEIGHT = 90f;

    private final Font tituloFont    = new Font(Font.HELVETICA, 18, Font.BOLD);
    private final Font subtituloFont = new Font(Font.HELVETICA, 14, Font.BOLD);
    private final Font textoFont     = new Font(Font.HELVETICA, 12, Font.NORMAL);
    private final Font cursivaFont   = new Font(Font.HELVETICA, 12, Font.ITALIC);
    private final Font boldFont      = new Font(Font.HELVETICA, 12, Font.BOLD);

    // ==========================================
    // 0. PAGE EVENT — encabezado en cada página
    // ==========================================

    private class HeaderPageEvent extends PdfPageEventHelper {

        private final ReporteServicioImp srv;

        HeaderPageEvent(ReporteServicioImp srv) {
            this.srv = srv;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                float pageWidth  = document.getPageSize().getWidth();
                float pageHeight = document.getPageSize().getHeight();
                float margin     = 36f;

                PdfPTable header = new PdfPTable(3);
                header.setTotalWidth(pageWidth - margin * 2);
                header.setWidths(new float[]{1.2f, 3f, 1.2f});

                header.addCell(srv.createImageCell("images/LogoChilpancingo.png", Element.ALIGN_LEFT));

                PdfPCell centroCell = new PdfPCell();
                centroCell.setBorder(Rectangle.NO_BORDER);
                centroCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                centroCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                Paragraph pCentro = new Paragraph();
                pCentro.add(new Chunk("ESTADO DE GUERRERO\n", new Font(Font.HELVETICA, 14, Font.BOLD)));
                pCentro.add(new Chunk("Ayuntamiento de Chilpancingo", new Font(Font.HELVETICA, 10, Font.NORMAL)));
                pCentro.setAlignment(Element.ALIGN_CENTER);
                centroCell.addElement(pCentro);
                header.addCell(centroCell);

                header.addCell(srv.createImageCell("images/LogoRenace.png", Element.ALIGN_RIGHT));

                PdfContentByte canvas = writer.getDirectContent();
                header.writeSelectedRows(0, -1, margin, pageHeight - margin, canvas);

                canvas.setLineWidth(0.5f);
                canvas.setRGBColorStroke(150, 150, 150);
                float lineY = pageHeight - margin - HEADER_HEIGHT + 6f;
                canvas.moveTo(margin, lineY);
                canvas.lineTo(pageWidth - margin, lineY);
                canvas.stroke();

                Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new java.awt.Color(120, 120, 120));
                Phrase pageNum = new Phrase("Página " + writer.getPageNumber(), footerFont);
                ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                        pageNum, pageWidth - margin, margin - 10f, 0);

            } catch (Exception e) {
                System.err.println("Error al dibujar encabezado: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 1. INICIO DE DOCUMENTO
    // ==========================================

    private Document iniciarDocumento(ByteArrayOutputStream baos, String tituloReporte,
                                      String inicio, String fin) throws Exception {
        float margenSuperior = HEADER_HEIGHT + 20f;
        Document document = new Document(PageSize.A4, 36, 36, margenSuperior, 36);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new HeaderPageEvent(this));
        document.open();

        Paragraph pTitulo = new Paragraph(tituloReporte, tituloFont);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitulo);

        if (inicio != null && !inicio.isEmpty()) {
            String periodo = (fin != null && !fin.isEmpty() && !inicio.equals(fin))
                    ? "Periodo: " + inicio + " al " + fin
                    : "Fecha específica: " + inicio;
            Paragraph pPeriodo = new Paragraph(periodo, boldFont);
            pPeriodo.setAlignment(Element.ALIGN_CENTER);
            document.add(pPeriodo);
        }

        Paragraph pFecha = new Paragraph(
                "Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                cursivaFont);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        document.add(pFecha);

        document.add(new Paragraph("\n"));
        return document;
    }

    // ==========================================
    // 2. REPORTES
    // ==========================================

    @Override
    public byte[] generarReporteColoniasPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Análisis de Situación por Colonia", inicio, fin);

            Map<String, List<IncidenciaDTO>> porColonia = todas.stream()
                    .collect(Collectors.groupingBy(i -> {
                        if (i.getColonia() != null) return i.getColonia();
                        if (i.getLocalidad() != null) return i.getLocalidad();
                        return "Ubicación no especificada";
                    }));

            String coloniaDestacada = porColonia.entrySet().stream()
                    .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                    .map(Map.Entry::getKey).orElse("ninguna");

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "El presente informe concentra el análisis geográfico de los reportes ciudadanos " +
                    "recibidos por el Ayuntamiento de Chilpancingo%s. " +
                    "Durante el periodo evaluado se contabilizó un total de %d incidencias distribuidas " +
                    "en %d colonias o localidades del municipio. " +
                    "La zona con mayor número de reportes es '%s', lo que indica una mayor demanda " +
                    "de atención en esa área. " +
                    "Este análisis permite al Ayuntamiento priorizar la asignación de recursos y personal " +
                    "operativo hacia las colonias con mayor actividad ciudadana, contribuyendo a una gestión " +
                    "territorial más eficiente y equitativa.",
                    describePeriodo(inicio, fin), todas.size(), porColonia.size(), coloniaDestacada));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DETALLE POR COLONIA\n", subtituloFont));
            insertarGrafico(document, "colonias.png");
            document.add(new Paragraph("\n"));

            if (todas.isEmpty()) {
                document.add(new Paragraph("No hay incidencias registradas para el análisis.", textoFont));
            } else {
                porColonia.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                        .forEach(entry -> {
                            try {
                                Map<String, Long> estados = entry.getValue().stream()
                                        .collect(Collectors.groupingBy(
                                                i -> formatearTexto(i.getNombreEstadoActual(), "Sin estado"),
                                                Collectors.counting()));
                                String estadosTexto = estados.entrySet().stream()
                                        .map(e -> e.getKey() + ": " + e.getValue())
                                        .collect(Collectors.joining(" | "));
                                agregarSeccionDatos(document,
                                        "Colonia / Zona: " + entry.getKey(),
                                        new String[][]{
                                                {"Total de reportes", String.valueOf(entry.getValue().size())},
                                                {"Distribución por estatus", estadosTexto}
                                        });
                            } catch (DocumentException e) { e.printStackTrace(); }
                        });
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de colonias", e);
        }
    }

    @Override
    public byte[] generarReporteTiposIncidenciaPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);

        // Agrupar ya con texto formateado
        Map<String, List<IncidenciaDTO>> porTipo = todas.stream()
                .collect(Collectors.groupingBy(i ->
                        formatearTexto(i.getNombreTipo(), "Tipo general")));

        String tipoMasFrecuente = porTipo.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(Map.Entry::getKey).orElse("ninguno");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Clasificación de Reportes Ciudadanos", inicio, fin);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "Este documento presenta la clasificación temática de los %d reportes ciudadanos " +
                    "registrados en el sistema%s. " +
                    "Los reportes han sido agrupados en %d categorías de incidencia, lo que permite " +
                    "identificar las problemáticas más recurrentes en el municipio. " +
                    "La categoría con mayor incidencia es '%s', concentrando %d casos, " +
                    "lo que sugiere la necesidad de atención prioritaria en esta área operativa. " +
                    "La correcta clasificación de los reportes facilita la canalización eficiente " +
                    "hacia los departamentos responsables y optimiza los tiempos de respuesta institucional.",
                    todas.size(), describePeriodo(inicio, fin), porTipo.size(),
                    tipoMasFrecuente, porTipo.getOrDefault(tipoMasFrecuente, Collections.emptyList()).size()));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DETALLE POR TIPO DE INCIDENCIA\n", subtituloFont));
            insertarGrafico(document, "tipos.png");
            document.add(new Paragraph("\n"));

            porTipo.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .forEach(entry -> {
                        try {
                            double pct = (entry.getValue().size() * 100.0) / (todas.isEmpty() ? 1 : todas.size());
                            agregarSeccionDatos(document,
                                    "Tipo: " + entry.getKey(),
                                    new String[][]{
                                            {"Número de reportes", String.valueOf(entry.getValue().size())},
                                            {"Participación del total", String.format("%.1f%%", pct)}
                                    });
                        } catch (DocumentException e) { e.printStackTrace(); }
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de tipos", e);
        }
    }

    @Override
    public byte[] generarReporteEstatusYPromedioPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        int total = todas.size();

        // Agrupar ya con texto formateado
        Map<String, List<IncidenciaDTO>> porEstado = todas.stream()
                .collect(Collectors.groupingBy(i ->
                        formatearTexto(i.getNombreEstadoActual(), "Estado desconocido")));

        String estadoMayor = porEstado.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(Map.Entry::getKey).orElse("ninguno");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Eficiencia y Estatus", inicio, fin);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "El presente informe tiene como objetivo evaluar el estado actual del seguimiento " +
                    "de atención ciudadana en el Ayuntamiento de Chilpancingo%s. " +
                    "Se han procesado un total de %d incidencias, distribuidas en %d estados de seguimiento " +
                    "distintos dentro del flujo operativo del sistema. " +
                    "El estatus con mayor número de casos actualmente es '%s', con %d reportes en esa fase. " +
                    "Este indicador es fundamental para medir la eficiencia institucional y detectar " +
                    "posibles cuellos de botella en la atención, permitiendo tomar decisiones operativas " +
                    "oportunas para mejorar los tiempos de resolución.",
                    describePeriodo(inicio, fin), total, porEstado.size(),
                    estadoMayor, porEstado.getOrDefault(estadoMayor, Collections.emptyList()).size()));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DETALLE POR ESTATUS DE ATENCIÓN\n", subtituloFont));
            insertarGrafico(document, "estatus.png");
            document.add(new Paragraph("\n"));

            porEstado.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .forEach(entry -> {
                        try {
                            double pct = (entry.getValue().size() * 100.0) / (total == 0 ? 1 : total);
                            agregarSeccionDatos(document,
                                    "Estatus: " + entry.getKey(),
                                    new String[][]{
                                            {"Reportes en este estatus", String.valueOf(entry.getValue().size())},
                                            {"Porcentaje del total", String.format("%.1f%%", pct)}
                                    });
                        } catch (DocumentException e) { e.printStackTrace(); }
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de estatus", e);
        }
    }

    @Override
    public byte[] generarReporteDepartamentosPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        Map<String, List<IncidenciaDTO>> porDepto = todas.stream()
                .filter(i -> i.getDepartamentoId() != null)
                .collect(Collectors.groupingBy(i ->
                        i.getNombreDepartamento() != null
                                ? i.getNombreDepartamento()
                                : "Departamento #" + i.getDepartamentoId()));

        String deptoMayor = porDepto.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(Map.Entry::getKey).orElse("ninguno");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Distribución por Departamento", inicio, fin);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "El presente informe describe la distribución de la carga operativa entre los " +
                    "departamentos del Ayuntamiento de Chilpancingo%s. " +
                    "Un total de %d incidencias han sido canalizadas a %d departamentos distintos " +
                    "para su resolución y seguimiento. " +
                    "El departamento con mayor número de asignaciones es '%s', con %d casos bajo su responsabilidad. " +
                    "Este análisis de carga de trabajo es clave para garantizar una distribución equitativa " +
                    "de responsabilidades institucionales y detectar áreas que requieran apoyo adicional " +
                    "en términos de personal o recursos.",
                    describePeriodo(inicio, fin),
                    todas.stream().filter(i -> i.getDepartamentoId() != null).count(),
                    porDepto.size(), deptoMayor,
                    porDepto.getOrDefault(deptoMayor, Collections.emptyList()).size()));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DETALLE POR DEPARTAMENTO\n", subtituloFont));
            document.add(new Paragraph("\n"));

            porDepto.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .forEach(entry -> {
                        try {
                            long sinAsignar = entry.getValue().stream()
                                    .filter(i -> i.getPersonalId() == null).count();
                            agregarSeccionDatos(document,
                                    "Departamento: " + entry.getKey(),
                                    new String[][]{
                                            {"Incidencias asignadas", String.valueOf(entry.getValue().size())},
                                            {"Sin personal asignado", String.valueOf(sinAsignar)}
                                    });
                        } catch (DocumentException e) { e.printStackTrace(); }
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de departamentos", e);
        }
    }

    @Override
    public byte[] generarReportePersonalPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        Map<String, List<IncidenciaDTO>> porPersonal = todas.stream()
                .filter(i -> i.getPersonalId() != null)
                .collect(Collectors.groupingBy(i ->
                        i.getNombrePersonal() != null
                                ? i.getNombrePersonal()
                                : "Personal #" + i.getPersonalId()));

        String mejorPersonal = porPersonal.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(Map.Entry::getKey).orElse("ninguno");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Desempeño del Personal Operativo", inicio, fin);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "El presente documento describe el registro de participación del personal operativo " +
                    "del Ayuntamiento de Chilpancingo en la atención de reportes ciudadanos%s. " +
                    "Un total de %d servidores públicos se encuentran activos en el sistema, " +
                    "gestionando en conjunto %d incidencias. " +
                    "El servidor público con mayor número de intervenciones es %s, " +
                    "con %d reportes bajo su gestión. " +
                    "Este informe permite evaluar la participación individual del personal y apoya " +
                    "la toma de decisiones en materia de distribución de carga de trabajo, " +
                    "reconocimiento al desempeño y detección de necesidades de capacitación.",
                    describePeriodo(inicio, fin), porPersonal.size(),
                    todas.stream().filter(i -> i.getPersonalId() != null).count(),
                    mejorPersonal,
                    porPersonal.getOrDefault(mejorPersonal, Collections.emptyList()).size()));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DETALLE POR SERVIDOR PÚBLICO\n", subtituloFont));
            document.add(new Paragraph("\n"));

            porPersonal.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .forEach(entry -> {
                        try {
                            Map<String, Long> estados = entry.getValue().stream()
                                    .collect(Collectors.groupingBy(
                                            i -> formatearTexto(i.getNombreEstadoActual(), "Sin estado"),
                                            Collectors.counting()));
                            String estadosTexto = estados.entrySet().stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(" | "));
                            agregarSeccionDatos(document,
                                    "Servidor Público: " + entry.getKey(),
                                    new String[][]{
                                            {"Reportes gestionados", String.valueOf(entry.getValue().size())},
                                            {"Distribución por estatus", estadosTexto}
                                    });
                        } catch (DocumentException e) { e.printStackTrace(); }
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de personal", e);
        }
    }

    @Override
    public byte[] generarReporteUsuariosPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        Map<String, List<IncidenciaDTO>> porUsuario = todas.stream()
                .filter(i -> i.getUsuarioId() != null)
                .collect(Collectors.groupingBy(i ->
                        i.getNombreUsuario() != null ? i.getNombreUsuario() : "Ciudadano Anónimo"));

        String ciudadanoDestacado = porUsuario.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(Map.Entry::getKey).orElse("ninguno");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Participación Ciudadana", inicio, fin);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "Este informe presenta las métricas de participación ciudadana en la plataforma " +
                    "de reporte del Ayuntamiento de Chilpancingo%s. " +
                    "Durante el periodo analizado, %d ciudadanos realizaron reportes de manera activa, " +
                    "generando un total de %d incidencias registradas. " +
                    "El ciudadano con mayor número de aportaciones es %s, con %d reportes enviados, " +
                    "lo que refleja un alto nivel de compromiso cívico. " +
                    "El análisis de la participación ciudadana es esencial para evaluar el alcance de la " +
                    "plataforma, identificar ciudadanos activos y diseñar estrategias de comunicación " +
                    "que fortalezcan la relación entre la comunidad y el gobierno municipal.",
                    describePeriodo(inicio, fin), porUsuario.size(),
                    todas.stream().filter(i -> i.getUsuarioId() != null).count(),
                    ciudadanoDestacado,
                    porUsuario.getOrDefault(ciudadanoDestacado, Collections.emptyList()).size()));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("RANKING DE PARTICIPACIÓN CIUDADANA\n", subtituloFont));
            document.add(new Paragraph("\n"));

            int[] posicion = {1};
            porUsuario.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .forEach(entry -> {
                        try {
                            agregarSeccionDatos(document,
                                    "#" + posicion[0]++ + " — " + entry.getKey(),
                                    new String[][]{
                                            {"Reportes enviados", String.valueOf(entry.getValue().size())}
                                    });
                        } catch (DocumentException e) { e.printStackTrace(); }
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de usuarios", e);
        }
    }

    @Override
    public byte[] generarReporteClimaPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        long conAlerta = todas.stream().filter(i -> Boolean.TRUE.equals(i.getClimaAlerta())).count();
        long sinAlerta = todas.size() - conAlerta;
        double pctAlerta = todas.isEmpty() ? 0 : (conAlerta * 100.0) / todas.size();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Impacto de Condiciones Climáticas", inicio, fin);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document, String.format(
                    "El presente informe analiza la correlación entre los reportes ciudadanos recibidos " +
                    "y las condiciones meteorológicas registradas en el municipio de Chilpancingo%s. " +
                    "De un total de %d incidencias analizadas, %d de ellas (%.1f%%) se presentaron " +
                    "durante periodos con alertas climáticas activas, mientras que los %d reportes " +
                    "restantes ocurrieron en condiciones meteorológicas normales. " +
                    "Este análisis permite al Ayuntamiento anticipar aumentos en la demanda de atención " +
                    "ciudadana durante eventos climáticos adversos, fortalecer los protocolos de " +
                    "respuesta preventiva y mejorar la coordinación con las autoridades de protección civil.",
                    describePeriodo(inicio, fin), todas.size(), conAlerta, pctAlerta, sinAlerta));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DETALLE DE IMPACTO CLIMÁTICO\n", subtituloFont));
            document.add(new Paragraph("\n"));

            agregarSeccionDatos(document, "Reportes durante Alerta Climática",
                    new String[][]{
                            {"Total de reportes", String.valueOf(conAlerta)},
                            {"Porcentaje del total", String.format("%.1f%%", pctAlerta)},
                            {"Observación", "Mayor demanda de atención y recursos durante emergencias"}
                    });
            agregarSeccionDatos(document, "Reportes en Condiciones Normales",
                    new String[][]{
                            {"Total de reportes", String.valueOf(sinAlerta)},
                            {"Porcentaje del total", String.format("%.1f%%", 100.0 - pctAlerta)},
                            {"Observación", "Operación estándar del sistema de reportes"}
                    });
            agregarSeccionDatos(document, "Resumen General del Periodo",
                    new String[][]{
                            {"Total de incidencias analizadas", String.valueOf(todas.size())},
                            {"Relación alerta vs. normal", String.format("%.1f%% / %.1f%%", pctAlerta, 100.0 - pctAlerta)}
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de clima", e);
        }
    }

    @Override
    public byte[] generarReportePorPeriodoPdf(String inicio, String fin) {
        try {
            List<IncidenciaDTO> filtradas = incidenciaClient.buscarPorFecha(inicio, fin);

            Map<String, Long> porEstado = filtradas.stream()
                    .collect(Collectors.groupingBy(
                            i -> formatearTexto(i.getNombreEstadoActual(), "Sin estado"),
                            Collectors.counting()));
            Map<String, Long> porColonia = filtradas.stream()
                    .collect(Collectors.groupingBy(
                            i -> i.getColonia() != null ? i.getColonia() : "Sin colonia",
                            Collectors.counting()));
            String coloniaTop = porColonia.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("ninguna");

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Document document = iniciarDocumento(baos, "Reporte General de Incidencias", inicio, fin);

                document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));

                if (filtradas.isEmpty()) {
                    agregarParrafoJustificado(document,
                            "No se encontraron incidencias registradas en el periodo comprendido entre " +
                            inicio + " y " + fin + ". El sistema no cuenta con datos que reportar para este rango de fechas.");
                } else {
                    agregarParrafoJustificado(document, String.format(
                            "El presente reporte consolida las incidencias ciudadanas registradas en el " +
                            "sistema del Ayuntamiento de Chilpancingo durante el periodo del %s al %s. " +
                            "Se contabilizaron un total de %d reportes, provenientes de diversas colonias " +
                            "del municipio. La colonia con mayor número de reportes fue '%s'. " +
                            "En cuanto al seguimiento institucional, los reportes se distribuyen en los " +
                            "siguientes estados: %s. " +
                            "Este reporte sirve como herramienta de rendición de cuentas y seguimiento " +
                            "de la gestión municipal ante la ciudadanía.",
                            inicio, fin, filtradas.size(), coloniaTop,
                            porEstado.entrySet().stream()
                                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                                    .collect(Collectors.joining(", "))));
                }

                document.add(new Paragraph("\n"));
                document.add(new Paragraph("LISTADO DETALLADO DE INCIDENCIAS\n", subtituloFont));
                document.add(new Paragraph("\n"));

                for (IncidenciaDTO i : filtradas) {
                    agregarSeccionDatos(document,
                            "Reporte #" + i.getId() + " — " + (i.getTitulo() != null ? i.getTitulo() : "Sin título"),
                            new String[][]{
                                    {"Colonia / Zona", i.getColonia() != null ? i.getColonia() : "No especificada"},
                                    {"Estatus actual", formatearTexto(i.getNombreEstadoActual(), "Sin estado")},
                                    {"Departamento responsable", i.getNombreDepartamento() != null ? i.getNombreDepartamento() : "No asignado"}
                            });
                }

                document.close();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte por periodo", e);
        }
    }

    @Override
    public byte[] generarPdfPrueba() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Documento de Prueba y Calibración", null, null);

            document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
            agregarParrafoJustificado(document,
                    "Este es un documento de calibración generado para verificar que el diseño " +
                    "y el layout institucional del Ayuntamiento de Chilpancingo funcionen correctamente " +
                    "en el servidor. Los componentes de encabezado, tipografía, imágenes institucionales " +
                    "y estructura de secciones han sido validados satisfactoriamente.");

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("DATOS DE CALIBRACIÓN\n", subtituloFont));
            document.add(new Paragraph("\n"));

            agregarSeccionDatos(document, "Estado del Sistema",
                    new String[][]{
                            {"Encabezado institucional", "Operativo"},
                            {"Logos (Chilpancingo / Renace)", "Cargados correctamente"},
                            {"Tipografía", "Helvetica — Normal / Bold / Italic"},
                            {"Secciones de datos", "Renderizando correctamente"}
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de prueba", e);
        }
    }

    @Override
    public byte[] generarReporteDetalladoIncidenciaPdf(Integer id) {
        try {
            IncidenciaDTO i = incidenciaClient.obtenerPorId(id);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Document document = iniciarDocumento(baos, "Informe Ejecutivo de Incidencia #" + id, null, null);

                document.add(new Paragraph("RESUMEN EJECUTIVO\n", subtituloFont));
                agregarParrafoJustificado(document, String.format(
                        "El presente documento técnico describe la situación de la incidencia titulada '%s', " +
                        "identificada bajo el tipo '%s'. Este reporte fue ingresado formalmente al sistema " +
                        "el día %s por el ciudadano %s. " +
                        "A la fecha de generación de este informe, el caso se mantiene bajo el estatus de '%s'.\n\n" +
                        "Respecto a la ubicación geográfica del incidente, se reportó en la calle %s, perteneciente " +
                        "a la colonia %s en la localidad de %s. Para su debida resolución, el caso ha sido " +
                        "canalizado al departamento de %s, contando con la asignación del servidor público %s " +
                        "para su seguimiento operativo.",
                        i.getTitulo(),
                        formatearTexto(i.getNombreTipo(), "Sin tipo"),
                        i.getFechaReporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        i.getNombreUsuario(),
                        formatearTexto(i.getNombreEstadoActual(), "Sin estado"),
                        i.getCalle(), i.getColonia(), i.getLocalidad(),
                        i.getNombreDepartamento(), i.getNombrePersonal()));

                if (Boolean.TRUE.equals(i.getClimaAlerta())) {
                    document.add(new Paragraph("\nNota Climática:", boldFont));
                    document.add(new Paragraph(
                            "Durante el registro se detectó una alerta climática activa. " +
                            "Observaciones: " + i.getObservacionesClima(), textoFont));
                }

                document.add(new Paragraph("\n"));
                document.add(new Paragraph("ANEXO TÉCNICO DE DATOS\n", subtituloFont));
                document.add(new Paragraph("\n"));

                agregarSeccionDatos(document, "Información General",
                        new String[][]{
                                {"Título", i.getTitulo()},
                                {"Tipo de incidencia", formatearTexto(i.getNombreTipo(), "Sin tipo")},
                                {"Estado actual", formatearTexto(i.getNombreEstadoActual(), "Sin estado")},
                                {"Fecha de reporte", i.getFechaReporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}
                        });
                agregarSeccionDatos(document, "Ubicación del Incidente",
                        new String[][]{
                                {"Calle", i.getCalle()},
                                {"Colonia", i.getColonia()},
                                {"Localidad", i.getLocalidad()}
                        });
                agregarSeccionDatos(document, "Responsables y Seguimiento",
                        new String[][]{
                                {"Departamento", i.getNombreDepartamento()},
                                {"Personal asignado", i.getNombrePersonal()},
                                {"Ciudadano que reportó", i.getNombreUsuario()}
                        });

                document.close();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte detallado narrativo", e);
        }
    }

    // ==========================================
    // 3. UTILIDADES Y MÉTODOS AUXILIARES
    // ==========================================

    /**
     * Convierte textos con guión bajo a formato oración legible.
     * EN_PROCESO → "En proceso" | PENDIENTE_REVISION → "Pendiente revisión"
     * Si el valor es null o vacío devuelve el fallback indicado.
     */
    private String formatearTexto(String valor, String fallback) {
        if (valor == null || valor.isBlank()) return fallback;
        return valor.substring(0, 1).toUpperCase()
                + valor.substring(1).toLowerCase().replace('_', ' ');
    }

    private void agregarParrafoJustificado(Document document, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto, textoFont);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setLeading(18f);
        document.add(p);
    }

    private void agregarSeccionDatos(Document document, String titulo, String[][] campos)
            throws DocumentException {
        Paragraph pTitulo = new Paragraph(titulo, boldFont);
        pTitulo.setSpacingBefore(10f);
        document.add(pTitulo);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(95);
        tabla.setWidths(new float[]{2f, 4f});
        tabla.setSpacingBefore(4f);
        tabla.setSpacingAfter(10f);

        for (String[] par : campos) {
            PdfPCell celdaCampo = new PdfPCell(new Phrase(par[0], boldFont));
            celdaCampo.setBackgroundColor(new java.awt.Color(235, 235, 235));
            celdaCampo.setPadding(6);
            celdaCampo.setBorderColor(new java.awt.Color(200, 200, 200));
            tabla.addCell(celdaCampo);

            PdfPCell celdaValor = new PdfPCell(new Phrase(par[1] != null ? par[1] : "—", textoFont));
            celdaValor.setBackgroundColor(java.awt.Color.WHITE);
            celdaValor.setPadding(6);
            celdaValor.setBorderColor(new java.awt.Color(200, 200, 200));
            tabla.addCell(celdaValor);
        }

        document.add(tabla);
    }

    private String describePeriodo(String inicio, String fin) {
        if (inicio == null || inicio.isEmpty()) return "";
        if (fin != null && !fin.isEmpty() && !inicio.equals(fin))
            return " durante el periodo del " + inicio + " al " + fin;
        return " correspondiente al " + inicio;
    }

    PdfPCell createImageCell(String path, int alignment) {
        try {
            InputStream is = getLogoStream(path);
            if (is != null) {
                Image img = Image.getInstance(is.readAllBytes());
                img.scaleToFit(70, 70);
                PdfPCell cell = new PdfPCell(img);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(alignment);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                return cell;
            }
        } catch (Exception e) { e.printStackTrace(); }
        PdfPCell empty = new PdfPCell();
        empty.setBorder(Rectangle.NO_BORDER);
        return empty;
    }

    private InputStream getLogoStream(String filename) {
        String[] paths = {
            filename, "static/" + filename, "static/images/" + filename,
            "META-INF/resources/" + filename, "META-INF/resources/static/" + filename
        };
        for (String path : paths) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) return is;
        }
        return null;
    }

    protected void insertarImagen(Document document, String imagePath, float width) {
        try {
            ClassPathResource resource = new ClassPathResource(imagePath);
            if (resource.exists()) {
                Image image = Image.getInstance(resource.getURL());
                image.scaleToFit(width, 200);
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
            }
        } catch (IOException e) {
            System.err.println("No se encontró la imagen: " + imagePath);
        } catch (Exception e) {
            System.err.println("Error al insertar imagen: " + e.getMessage());
        }
    }

    protected void insertarGrafico(Document document, String nombreGrafico) {
        insertarImagen(document, "static/images/graficos/" + nombreGrafico, 400);
    }

    @Override
    public List<IncidenciaDTO> obtenerIncidencias(String inicio, String fin) {
        try {
            if (inicio != null && !inicio.isEmpty() && fin != null && !fin.isEmpty()) {
                return incidenciaClient.buscarPorFecha(inicio, fin);
            } else if (inicio != null && !inicio.isEmpty()) {
                return incidenciaClient.buscarPorFecha(inicio, inicio);
            }
            return incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            System.err.println("Error de comunicación con el MS de Incidencias: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void imprimirLista(Document document, String titulo, List<ReporteItemDto> datos) throws Exception {
        document.add(new Paragraph(titulo, subtituloFont));
        for (ReporteItemDto item : datos) {
            document.add(new Paragraph("  " + item.getNombre() + ": " + item.getCantidad(), textoFont));
        }
    }
}