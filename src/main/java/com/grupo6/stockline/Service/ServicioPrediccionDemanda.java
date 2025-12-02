package com.grupo6.stockline.Service;

import com.grupo6.stockline.Service.dto.EstadisticasDemandaDTO;

public interface ServicioPrediccionDemanda {

    EstadisticasDemandaDTO obtenerEstadisticas(Long idArticulo);

}
