package com.grupo6.stockline.Repositories;

import com.grupo6.stockline.Entities.Venta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends BaseRepository<Venta, Long> {

    interface DemandaDiariaProyeccion {
        LocalDate getFechaAlta();
        Integer getCantidad();
    }

    @Query("""
        SELECT DATE(v.fechaAlta) AS fechaAlta,
               SUM(d.cantidad) AS cantidad
        FROM Venta v
        JOIN v.detalleVenta d
        WHERE d.articulo.id = :idArticulo
          AND v.fechaAlta >= :desde
        GROUP BY DATE(v.fechaAlta)
        ORDER BY DATE(v.fechaAlta)
    """)
    List<DemandaDiariaProyeccion> obtenerDemandaDiariaPorArticulo(
            @Param("idArticulo") Long idArticulo,
            @Param("desde") LocalDateTime desde
    );
}

