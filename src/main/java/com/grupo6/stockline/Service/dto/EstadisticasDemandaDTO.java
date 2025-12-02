package com.grupo6.stockline.Service.dto;

public record EstadisticasDemandaDTO(
        double demandaPromedioDiaria,
        double desviacionEstandar,
        int diasConVentas,
        int diasCoberturaStock,
        double pronosticoDiario,
        double pronostico30Dias
) {
}
