package com.grupo6.stockline.Service;

import com.grupo6.stockline.Entities.Articulo;
import com.grupo6.stockline.Repositories.ArticuloRepository;
import com.grupo6.stockline.Repositories.VentaRepository;
import com.grupo6.stockline.Service.dto.EstadisticasDemandaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioPrediccionDemandaImpl implements ServicioPrediccionDemanda {

    private final VentaRepository ventaRepository;
    private final ArticuloRepository articuloRepository;

    @Override
    public EstadisticasDemandaDTO obtenerEstadisticas(Long idArticulo) {

        Articulo articulo = articuloRepository.findById(idArticulo)
                .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado"));

        LocalDateTime desde = LocalDate.now().minusDays(30).atStartOfDay();

        List<VentaRepository.DemandaDiariaProyeccion> registros =
                ventaRepository.obtenerDemandaDiariaPorArticulo(idArticulo, desde);

        if (registros.isEmpty()) {
            // sin datos: todo en 0, cobertura "infinita"
            return new EstadisticasDemandaDTO(
                    0, 0, 0, Integer.MAX_VALUE, 0, 0
            );
        }

        int n = registros.size();
        double suma = registros.stream()
                .mapToInt(VentaRepository.DemandaDiariaProyeccion::getCantidad)
                .sum();

        double promedio = suma / n;

        double varianza = registros.stream()
                .mapToDouble(r -> Math.pow(r.getCantidad() - promedio, 2))
                .sum() / n;

        double desviacion = Math.sqrt(varianza);

        int stockActual = articulo.getStockActual() != null ? articulo.getStockActual() : 0;

        int diasCobertura = promedio > 0
                ? (int) Math.floor(stockActual / promedio)
                : Integer.MAX_VALUE;

        double alpha = 0.3;
        double pronostico = promedio;

        for (VentaRepository.DemandaDiariaProyeccion r : registros) {
            pronostico = alpha * r.getCantidad() + (1 - alpha) * pronostico;
        }

        double pronostico30Dias = pronostico * 30;

        return new EstadisticasDemandaDTO(
                promedio,
                desviacion,
                n,
                diasCobertura,
                pronostico,
                pronostico30Dias
        );
    }
}
