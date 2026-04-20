
package itch.twp.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionDTO {
    private Integer id;
    private String calle;
    private String numero;
    private String colonia;
    private String localidad;
    
    }

