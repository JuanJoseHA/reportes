package itch.twp.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaDTO {
    private Integer id;
    private String titulo;
    private String descripcion;
    private Integer tipoId;
    private LocalDateTime fechaReporte;

    private Integer usuarioId;
    private Integer ubicacionId;
    private Integer departamentoId;
    private Integer personalId;

    private Boolean climaAlerta;
    private String observacionesClima;

    private String nombreEstadoActual;
    private String calle;
    private String colonia;
    private String localidad;

    private String nombrePersonal;
    private Boolean personalDisponible;
    private String nombreDepartamento;
    private String descripcionDepartamento;

    private String nombreTipo;
    private String nombreUsuario;
}

