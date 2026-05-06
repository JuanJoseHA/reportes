package itch.twp.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDetalleDTO {
    private Long id;
    private String nombre; 
    private String email;
    private String telefono;
    private String rol; 
    private Long departamentoId; 
    private Boolean activo; 
}