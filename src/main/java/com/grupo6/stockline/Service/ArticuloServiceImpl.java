package com.grupo6.stockline.Service;

import com.grupo6.stockline.Entities.*;
import com.grupo6.stockline.Enum.EstadoOrdenCompra;
import com.grupo6.stockline.Enum.ModeloInventario;
import com.grupo6.stockline.Repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArticuloServiceImpl extends BaseServiceImpl<Articulo, Long> implements ArticuloService {

    @Autowired
    private ArticuloRepository articuloRepository;
    @Autowired
    private DetalleOrdenCompraRepository detalleOrdenCompraRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ArticuloProveedorRepository articuloProveedorRepository;
    @Autowired
    private DatosModeloInventarioRepository datosRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ArticuloServiceImpl(BaseRepository<Articulo, Long> baseRepository,
                               ArticuloRepository articuloRepository) {
        super(baseRepository);
        this.articuloRepository = articuloRepository;
    }

    // =========================================================
    // CRUD + REGLAS DE NEGOCIO
    // =========================================================

    @Override
    @Transactional
    public void save(Articulo articulo) throws Exception {
        try {
            articulo.setFechaAlta(LocalDateTime.now());
            articulo.setStockActual(0);
            articuloRepository.save(articulo);
        } catch (Exception e) {
            throw new Exception("Error al guardar el artículo: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void update(Long id, Articulo articulo) throws Exception {
        try {
            Articulo existente = articuloRepository.findById(id)
                    .orElseThrow(() -> new Exception("No se puede actualizar: Artículo no encontrado con ID: " + id));

            articulo.setId(id);
            articulo.setFechaAlta(existente.getFechaAlta());
            articulo.setStockActual(existente.getStockActual());

            articuloRepository.save(articulo);

            calcularModeloInventario(articulo.getId());
        } catch (Exception e) {
            throw new Exception("Error al actualizar artículo: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void bajaArticulo(Long id) throws Exception {
        try {
            Articulo articulo = articuloRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("No se puede dar de baja: Artículo no encontrado"));

            if (articulo.getFechaBaja() != null) {
                throw new IllegalStateException("El artículo ya se encuentra dado de baja.");
            }

            if (articulo.getStockActual() != null && articulo.getStockActual() > 0) {
                throw new IllegalStateException("No se puede dar de baja: todavía hay unidades en stock");
            }

            Long idArticulo = articulo.getId();
            List<DetalleOrdenCompra> detallesArticulos =
                    detalleOrdenCompraRepository.obtenerDetallesPorArticulo(idArticulo);

            boolean tieneOrdenesActivas = detallesArticulos.stream()
                    .anyMatch(dOC ->
                            dOC.getOrdenCompra().getEstadoOrdenCompra() == EstadoOrdenCompra.PENDIENTE
                                    || dOC.getOrdenCompra().getEstadoOrdenCompra() == EstadoOrdenCompra.ENVIADA);

            if (tieneOrdenesActivas) {
                throw new IllegalStateException("No se puede dar de baja: el artículo tiene órdenes de compra pendientes o enviadas.");
            }

            articuloRepository.darDeBajaPorId(idArticulo);

        } catch (Exception e) {
            throw new IllegalStateException("Error al dar de baja el artículo: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // LISTADOS ESPECIALES
    // =========================================================

    @Override
    @Transactional
    public List<Articulo> listarArticulosReponer() throws Exception {
        try {
            List<Articulo> listaArticulosAReponer = new ArrayList<>();
            List<Articulo> articuloList = articuloRepository.findAll();

            for (Articulo a : articuloList) {

                if (a.getFechaBaja() != null) {
                    continue;
                }

                List<DatosModeloInventario> datosInventario = a.getDatosModeloInventario();
                List<DetalleOrdenCompra> detallesArticulos = a.getDetalleOrdenCompra();

                boolean sinOrdenesActivas = detallesArticulos.stream().noneMatch(
                        dOC -> dOC.getOrdenCompra().getEstadoOrdenCompra() == EstadoOrdenCompra.PENDIENTE
                                || dOC.getOrdenCompra().getEstadoOrdenCompra() == EstadoOrdenCompra.ENVIADA
                );

                if (!sinOrdenesActivas) continue;

                for (DatosModeloInventario dmi : datosInventario) {
                    if (dmi.getFechaBaja() == null &&
                            dmi.getPuntoPedido() != null &&
                            a.getStockActual() != null &&
                            a.getStockActual() <= dmi.getPuntoPedido() &&
                            a.getModeloInventario() == ModeloInventario.LoteFijo) {

                        listaArticulosAReponer.add(a);
                        break;
                    }
                }
            }
            return listaArticulosAReponer;

        } catch (Exception e) {
            throw new IllegalStateException("Error al listar artículos a reponer: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public List<Articulo> listarArticulosFaltantes() throws Exception {
        try {
            List<Articulo> listaArticulosFaltantes = new ArrayList<>();
            List<Articulo> articuloList = articuloRepository.findAll();

            for (Articulo a : articuloList) {

                if (a.getFechaBaja() != null) continue;

                List<DatosModeloInventario> datosArticulo = a.getDatosModeloInventario();

                for (DatosModeloInventario dmi : datosArticulo) {
                    if (dmi.getFechaBaja() == null &&
                            dmi.getStockSeguridad() != null &&
                            a.getStockActual() != null &&
                            a.getStockActual() <= dmi.getStockSeguridad()) {
                        listaArticulosFaltantes.add(a);
                        break;
                    }
                }
            }

            return listaArticulosFaltantes;
        } catch (Exception e) {
            throw new IllegalStateException("Error al listar artículos faltantes: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // PROVEEDOR PREDETERMINADO + CGI
    // =========================================================

    @Override
    @Transactional
    public void asignarProveedorPredeterminado(Articulo articulo, Long idProveedor) throws Exception {
        try {
            Proveedor proveedor = proveedorRepository.findById(idProveedor)
                    .orElseThrow(() -> new IllegalArgumentException("No se pudo asignar el proveedor: no existe"));

            if (proveedor.getFechaBaja() != null) {
                throw new IllegalStateException("No se pudo asignar el proveedor: proveedor dado de baja");
            }

            articulo.setProveedorPredeterminado(proveedor);
            articuloRepository.save(articulo);

            calcularModeloInventario(articulo.getId());

        } catch (Exception e) {
            throw new IllegalStateException("Error al asignar proveedor predeterminado: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Double calcularCGI(Long id) throws Exception {
        try {
            Articulo articulo = articuloRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("No se pudo calcular el CGI: artículo no encontrado"));

            if (articulo.getProveedorPredeterminado() == null) {
                throw new IllegalStateException("No se ha asignado un proveedor predeterminado.");
            }

            ArticuloProveedor articuloProveedor =
                    articuloProveedorRepository.findByProveedorAndArticulo(
                            articulo.getProveedorPredeterminado().getId(), id);

            if (articuloProveedor == null) {
                throw new IllegalStateException("No se encontró configuración de Artículo-Proveedor para el predeterminado.");
            }

            DatosModeloInventario datos = articulo.getDatosModeloInventario()
                    .stream()
                    .filter(dmi -> dmi.getFechaBaja() == null)
                    .findFirst()
                    .orElse(null);

            if (datos == null) {
                throw new IllegalStateException("No se encontraron datos de modelo de inventario activos para este artículo.");
            }

            Integer demandaArticulo = articulo.getDemandaArticulo();
            Integer costoAlmacenamiento = articulo.getCostoAlmacenamiento();

            if (demandaArticulo == null || demandaArticulo <= 0) {
                throw new IllegalStateException("No se puede calcular el CGI: la demanda del artículo no es válida.");
            }
            if (costoAlmacenamiento == null || costoAlmacenamiento <= 0) {
                throw new IllegalStateException("No se puede calcular el CGI: el costo de almacenamiento no es válido.");
            }
            if (articuloProveedor.getCostoCompra() <= 0 || articuloProveedor.getCostoPedido() <= 0) {
                throw new IllegalStateException("No se puede calcular el CGI: costo de compra o costo de pedido inválidos.");
            }

            double costoArticulo = articuloProveedor.getCostoCompra();
            double costoPedido = articuloProveedor.getCostoPedido();

            Integer loteOptimo;

            if (articulo.getModeloInventario() == ModeloInventario.LoteFijo) {
                loteOptimo = datos.getLoteOptimo();
            } else if (articulo.getModeloInventario() == ModeloInventario.IntervaloFijo) {
                if (datos.getInventarioMaximo() == null) {
                    throw new IllegalStateException("Datos de inventario incompletos para modelo de intervalo fijo.");
                }
                loteOptimo = datos.getInventarioMaximo() - (articulo.getStockActual() != null ? articulo.getStockActual() : 0);
            } else {
                throw new IllegalStateException("Modelo de inventario no soportado para cálculo de CGI.");
            }

            if (loteOptimo == null || loteOptimo <= 0) {
                throw new IllegalStateException("No se puede calcular el CGI: lote óptimo no válido (<= 0).");
            }

            // CGI = D*C + Cp*D/Q + Ca*Q/2
            double cgi = demandaArticulo * costoArticulo
                    + (costoPedido * demandaArticulo) / loteOptimo
                    + (costoAlmacenamiento * loteOptimo) / 2.0;

            System.out.println("CGI calculado para artículo " + id + ": " + cgi);

            return cgi;

        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular el CGI: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // AJUSTE DE STOCK
    // =========================================================

    @Override
    @Transactional
    public void realizarAjuste(Long id, Integer cantAjuste) throws Exception {
        try {
            Articulo articulo = articuloRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("No se pudo realizar el ajuste: artículo no encontrado con ID: " + id));

            if (cantAjuste == null || cantAjuste <= 0) {
                throw new IllegalArgumentException("La cantidad a ajustar debe ser mayor a cero.");
            }

            if (articulo.getStockActual() == null) {
                articulo.setStockActual(0);
            }

            if (articulo.getStockActual() < cantAjuste) {
                throw new IllegalStateException("No se puede realizar el ajuste: el stock actual ("
                        + articulo.getStockActual() + ") es menor que la cantidad a reducir (" + cantAjuste + ").");
            }

            articulo.setStockActual(articulo.getStockActual() - cantAjuste);
            articuloRepository.save(articulo);

        } catch (Exception e) {
            throw new Exception("Error al realizar ajuste de stock: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // CÁLCULO DE MODELO (LOTE FIJO / INTERVALO FIJO)
    // =========================================================

    @Override
    @Transactional
    public void calcularModeloInventario(Long id) throws Exception {
        try {
            Articulo articulo = articuloRepository.findById(id).orElseThrow(() ->
                    new IllegalArgumentException("No se pudo calcular el modelo de inventario: artículo no encontrado"));

            if (articulo.getModeloInventario() == ModeloInventario.LoteFijo) {
                calcularModeloLoteFijo(articulo);
            } else if (articulo.getModeloInventario() == ModeloInventario.IntervaloFijo) {
                calcularModeloIntervaloFijo(articulo);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular el modelo de inventario: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void calcularModeloLoteFijo(Articulo articulo) throws Exception {
        try {
            if (articulo.getProveedorPredeterminado() == null) {
                throw new IllegalStateException("No se puede calcular modelo de lote fijo: el artículo no tiene proveedor predeterminado.");
            }

            if (articulo.getDemandaArticulo() == null || articulo.getDemandaArticulo() <= 0) {
                throw new IllegalStateException("Demanda del artículo no válida para modelo de lote fijo.");
            }

            if (articulo.getCostoAlmacenamiento() == null || articulo.getCostoAlmacenamiento() <= 0) {
                throw new IllegalStateException("Costo de almacenamiento no válido para modelo de lote fijo.");
            }

            ArticuloProveedor articuloProveedor = articuloProveedorRepository
                    .findByProveedorAndArticulo(articulo.getProveedorPredeterminado().getId(), articulo.getId());

            if (articuloProveedor == null) {
                throw new IllegalStateException("No se puede calcular modelo de lote fijo: no existe configuración Artículo-Proveedor.");
            }

            if (articuloProveedor.getCostoPedido() <= 0) {
                throw new IllegalStateException("Costo de pedido no válido para modelo de lote fijo.");
            }

            // Cierro datos anteriores
            for (DatosModeloInventario d : articulo.getDatosModeloInventario()) {
                if (d.getFechaBaja() == null) {
                    d.setFechaBaja(LocalDateTime.now());
                    datosRepository.save(d);
                }
            }

            DatosModeloInventario datosNuevo = new DatosModeloInventario();

            double demandaAnual = articulo.getDemandaArticulo();
            double demandaDiaria = demandaAnual / 360.0;

            double sigmaDiario = demandaDiaria * 0.20; // 20% de variabilidad
            double valorZ = 1.64; // 95% nivel de servicio

            int demoraEntrega = articuloProveedor.getDemoraEntrega() != null ? articuloProveedor.getDemoraEntrega() : 0;
            if (demoraEntrega < 0) demoraEntrega = 0;

            int loteOptimo = (int) Math.round(Math.sqrt(
                    (2.0 * demandaAnual * articuloProveedor.getCostoPedido()) / articulo.getCostoAlmacenamiento()
            ));

            if (loteOptimo <= 0) {
                throw new IllegalStateException("Resultado de lote óptimo no válido (<= 0). Verifique los datos del artículo.");
            }

            int stockSeguridad = (int) Math.ceil(valorZ * sigmaDiario * Math.sqrt(demoraEntrega));
            int puntoPedido = (int) Math.round(demandaDiaria * demoraEntrega) + stockSeguridad;

            datosNuevo.setLoteOptimo(loteOptimo);
            datosNuevo.setPuntoPedido(puntoPedido);
            datosNuevo.setStockSeguridad(stockSeguridad);
            datosNuevo.setArticulo(articulo);
            datosNuevo.setModeloInventario(articulo.getModeloInventario());
            datosNuevo.setFechaAlta(LocalDateTime.now());

            datosRepository.save(datosNuevo);
            articuloRepository.save(articulo);

        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular modelo lote fijo: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void calcularModeloIntervaloFijo(Articulo articulo) throws Exception {
        try {
            if (articulo.getProveedorPredeterminado() == null) {
                throw new IllegalStateException("No se puede calcular modelo de intervalo fijo: el artículo no tiene proveedor predeterminado.");
            }

            if (articulo.getDemandaArticulo() == null || articulo.getDemandaArticulo() <= 0) {
                throw new IllegalStateException("Demanda del artículo no válida para modelo de intervalo fijo.");
            }

            if (articulo.getCostoAlmacenamiento() == null || articulo.getCostoAlmacenamiento() <= 0) {
                throw new IllegalStateException("Costo de almacenamiento no válido para modelo de intervalo fijo.");
            }

            if (articulo.getTiempoRevision() == null || articulo.getTiempoRevision() <= 0) {
                throw new IllegalStateException("Tiempo de revisión no válido para modelo de intervalo fijo.");
            }

            ArticuloProveedor articuloProveedor = articuloProveedorRepository
                    .findByProveedorAndArticulo(articulo.getProveedorPredeterminado().getId(), articulo.getId());

            if (articuloProveedor == null) {
                throw new IllegalStateException("No se puede calcular modelo de intervalo fijo: no existe configuración Artículo-Proveedor.");
            }

            // Cierro datos anteriores
            for (DatosModeloInventario d : articulo.getDatosModeloInventario()) {
                if (d.getFechaBaja() == null) {
                    d.setFechaBaja(LocalDateTime.now());
                    datosRepository.save(d);
                }
            }

            DatosModeloInventario datosNuevo = new DatosModeloInventario();

            double demandaAnual = articulo.getDemandaArticulo();
            double demandaDiaria = demandaAnual / 360.0;
            double sigmaDiario = demandaDiaria * 0.20;
            double valorZ = 1.64;

            int demoraEntrega = articuloProveedor.getDemoraEntrega() != null ? articuloProveedor.getDemoraEntrega() : 0;
            int tiempoRevision = articulo.getTiempoRevision();

            int tiempoTotalRiesgo = demoraEntrega + tiempoRevision;
            if (tiempoTotalRiesgo <= 0) {
                throw new IllegalStateException("Tiempo total de riesgo (T + L) no válido para modelo de intervalo fijo.");
            }

            int stockSeguridad = (int) Math.ceil(valorZ * sigmaDiario * Math.sqrt(tiempoTotalRiesgo));
            int invMax = (int) Math.round(demandaDiaria * tiempoTotalRiesgo) + stockSeguridad;

            datosNuevo.setStockSeguridad(stockSeguridad);
            datosNuevo.setInventarioMaximo(invMax);
            datosNuevo.setArticulo(articulo);
            datosNuevo.setModeloInventario(articulo.getModeloInventario());
            datosNuevo.setFechaAlta(LocalDateTime.now());

            datosRepository.save(datosNuevo);
            articuloRepository.save(articulo);

        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular modelo intervalo fijo: " + e.getMessage(), e);
        }
    }
}
