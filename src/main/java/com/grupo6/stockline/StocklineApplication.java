package com.grupo6.stockline;

import com.grupo6.stockline.Entities.*;
import com.grupo6.stockline.Enum.ModeloInventario;
import com.grupo6.stockline.Repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class StocklineApplication {

	public static void main(String[] args) {
		SpringApplication.run(StocklineApplication.class, args);
	}

	@Bean
	public CommandLineRunner cargarDatosDePruebaCompleto(ArticuloRepository articuloRepo,
														 ProveedorRepository proveedorRepo,
														 VentaRepository ventaRepo,
														 DetalleVentaRepository detalleVentaRepo,
														 ArticuloProveedorRepository articuloProveedorRepo,
														 DatosModeloInventarioRepository datosModeloRepo) {
		return args -> {

			if (ventaRepo.count() > 0) {
				return;
			}

			System.out.println("===== CARGANDO DATOS DE PRUEBA COMPLETOS =====");

			LocalDateTime ahora = LocalDateTime.now();

			// =========================================================
			// PROVEEDORES
			// =========================================================
			Proveedor proveedor1 = new Proveedor();
			proveedor1.setNombreProveedor("Ferretería Central");
			proveedor1.setMailProveedor("contacto@ferreteriacentral.com");
			proveedor1.setFechaAlta(ahora.minusDays(120));
			proveedorRepo.save(proveedor1);

			Proveedor proveedor2 = new Proveedor();
			proveedor2.setNombreProveedor("Mayorista Norte");
			proveedor2.setMailProveedor("ventas@mayoristanorte.com");
			proveedor2.setFechaAlta(ahora.minusDays(90));
			proveedorRepo.save(proveedor2);

			// =========================================================
			// ARTÍCULOS
			// =========================================================

			// Artículo 1: Martillo - tendencia creciente + pico fines de semana
			Articulo martillo = new Articulo();
			martillo.setNombreArticulo("Martillo de acero");
			martillo.setDescripcionArticulo("Martillo de acero forjado con mango de goma");
			martillo.setCostoAlmacenamiento(50);
			martillo.setDemandaArticulo(90);
			martillo.setStockActual(500);
			martillo.setPrecioVenta(5000.0);
			martillo.setProveedorPredeterminado(proveedor1);
			martillo.setModeloInventario(ModeloInventario.LoteFijo);
			martillo.setFechaAlta(ahora.minusDays(120));
			martillo.setFechaUltimaRevision(ahora.minusDays(30));
			martillo.setTiempoRevision(30);
			articuloRepo.save(martillo);

			// Artículo 2: Destornillador - demanda estable, leve efecto finde
			Articulo destornillador = new Articulo();
			destornillador.setNombreArticulo("Destornillador punta plana");
			destornillador.setDescripcionArticulo("Destornillador de acero cromo vanadio");
			destornillador.setCostoAlmacenamiento(30);
			destornillador.setDemandaArticulo(60);
			destornillador.setStockActual(400);
			destornillador.setPrecioVenta(2500.0);
			destornillador.setProveedorPredeterminado(proveedor2);
			destornillador.setModeloInventario(ModeloInventario.LoteFijo);
			destornillador.setFechaAlta(ahora.minusDays(120));
			destornillador.setFechaUltimaRevision(ahora.minusDays(30));
			destornillador.setTiempoRevision(30);
			articuloRepo.save(destornillador);

			// Artículo 3: Taladro - demanda muy estacional (sube mucho el finde)
			Articulo taladro = new Articulo();
			taladro.setNombreArticulo("Taladro percutor 750W");
			taladro.setDescripcionArticulo("Taladro percutor 750W con maletín y accesorios");
			taladro.setCostoAlmacenamiento(80);
			taladro.setDemandaArticulo(50);
			taladro.setStockActual(300);
			taladro.setPrecioVenta(9000.0);
			taladro.setProveedorPredeterminado(proveedor1);
			taladro.setModeloInventario(ModeloInventario.LoteFijo);
			taladro.setFechaAlta(ahora.minusDays(120));
			taladro.setFechaUltimaRevision(ahora.minusDays(30));
			taladro.setTiempoRevision(30);
			articuloRepo.save(taladro);

			// =========================================================
			// ARTICULO - PROVEEDOR (ArticuloProveedor)
			// =========================================================

			// Martillo comprado a ambos proveedores
			ArticuloProveedor ap1 = new ArticuloProveedor();
			ap1.setArticulo(martillo);
			ap1.setProveedor(proveedor1);
			ap1.setCostoCompra(4200.0);
			ap1.setCostoPedido(800.0);
			ap1.setDemoraEntrega(5);
			ap1.setFechaAlta(ahora.minusDays(60));
			articuloProveedorRepo.save(ap1);

			ArticuloProveedor ap2 = new ArticuloProveedor();
			ap2.setArticulo(martillo);
			ap2.setProveedor(proveedor2);
			ap2.setCostoCompra(4100.0);
			ap2.setCostoPedido(1200.0);
			ap2.setDemoraEntrega(8);
			ap2.setFechaAlta(ahora.minusDays(45));
			articuloProveedorRepo.save(ap2);

			// Destornillador solo proveedor2
			ArticuloProveedor ap3 = new ArticuloProveedor();
			ap3.setArticulo(destornillador);
			ap3.setProveedor(proveedor2);
			ap3.setCostoCompra(1800.0);
			ap3.setCostoPedido(600.0);
			ap3.setDemoraEntrega(4);
			ap3.setFechaAlta(ahora.minusDays(50));
			articuloProveedorRepo.save(ap3);

			// Taladro sólo proveedor1
			ArticuloProveedor ap4 = new ArticuloProveedor();
			ap4.setArticulo(taladro);
			ap4.setProveedor(proveedor1);
			ap4.setCostoCompra(7200.0);
			ap4.setCostoPedido(1500.0);
			ap4.setDemoraEntrega(7);
			ap4.setFechaAlta(ahora.minusDays(40));
			articuloProveedorRepo.save(ap4);

			// =========================================================
			// DATOS MODELO DE INVENTARIO
			// =========================================================

			DatosModeloInventario dmiMartillo = new DatosModeloInventario();
			dmiMartillo.setArticulo(martillo);
			dmiMartillo.setModeloInventario(ModeloInventario.LoteFijo);
			dmiMartillo.setInventarioMaximo(600);         // stock máximo deseado
			dmiMartillo.setLoteOptimo(150);               // lote ideal de compra
			dmiMartillo.setPuntoPedido(200);              // dispara OC automática
			dmiMartillo.setStockSeguridad(80);            // para cubrir picos y variación
			dmiMartillo.setFechaAlta(ahora.minusDays(90));
			datosModeloRepo.save(dmiMartillo);

			DatosModeloInventario dmiDest = new DatosModeloInventario();
			dmiDest.setArticulo(destornillador);
			dmiDest.setModeloInventario(ModeloInventario.LoteFijo);
			dmiDest.setInventarioMaximo(400);
			dmiDest.setLoteOptimo(100);
			dmiDest.setPuntoPedido(120);
			dmiDest.setStockSeguridad(50);
			dmiDest.setFechaAlta(ahora.minusDays(90));
			datosModeloRepo.save(dmiDest);

			DatosModeloInventario dmiTaladro = new DatosModeloInventario();
			dmiTaladro.setArticulo(taladro);
			dmiTaladro.setModeloInventario(ModeloInventario.LoteFijo);
			dmiTaladro.setInventarioMaximo(300);
			dmiTaladro.setLoteOptimo(70);
			dmiTaladro.setPuntoPedido(100);
			dmiTaladro.setStockSeguridad(40);
			dmiTaladro.setFechaAlta(ahora.minusDays(90));
			datosModeloRepo.save(dmiTaladro);

			// =========================================================
			// VENTAS DIARIAS DE LOS ÚLTIMOS 90 DÍAS
			// =========================================================

			java.util.Random random = new java.util.Random(42);
			LocalDate fechaInicio = LocalDate.now().minusDays(89);   // 90 días contando hoy
			LocalTime horaVenta = LocalTime.of(10, 0);

			for (int i = 0; i < 90; i++) {

				LocalDate fecha = fechaInicio.plusDays(i);
				LocalDateTime fechaVenta = LocalDateTime.of(fecha, horaVenta);
				DayOfWeek diaSemana = fecha.getDayOfWeek();

				Venta ventaDia = new Venta();
				ventaDia.setFechaAlta(fechaVenta);
				ventaDia.setTotalVenta(0.0);
				ventaRepo.save(ventaDia);

				double totalDia = 0.0;

				// -------------------------
				// Demanda MARTILLO
				// Tendencia creciente + finde
				// -------------------------
				int baseMartillo = 2 + (i / 15); // va subiendo cada ~15 días
				int extraFindeMartillo = (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) ? 3 : 0;
				int ruidoMartillo = random.nextInt(3) - 1; // -1, 0 o +1
				int demandaMartillo = Math.max(0, baseMartillo + extraFindeMartillo + ruidoMartillo);

				demandaMartillo = Math.min(demandaMartillo, martillo.getStockActual());
				if (demandaMartillo > 0) {
					DetalleVenta dvMartillo = new DetalleVenta();
					dvMartillo.setVenta(ventaDia);
					dvMartillo.setArticulo(martillo);
					dvMartillo.setCantidad(demandaMartillo);
					double subTotal = demandaMartillo * martillo.getPrecioVenta();
					dvMartillo.setSubTotal(subTotal);
					dvMartillo.setFechaAlta(fechaVenta);
					detalleVentaRepo.save(dvMartillo);

					totalDia += subTotal;
					martillo.setStockActual(martillo.getStockActual() - demandaMartillo);
				}

				// -------------------------
				// Demanda DESTORNILLADOR
				// Estable + leve finde
				// -------------------------
				int baseDest = 2;
				int extraFindeDest = (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) ? 1 : 0;
				int ruidoDest = random.nextInt(3) - 1;
				int demandaDest = Math.max(0, baseDest + extraFindeDest + ruidoDest);

				demandaDest = Math.min(demandaDest, destornillador.getStockActual());
				if (demandaDest > 0) {
					DetalleVenta dvDest = new DetalleVenta();
					dvDest.setVenta(ventaDia);
					dvDest.setArticulo(destornillador);
					dvDest.setCantidad(demandaDest);
					double subTotal = demandaDest * destornillador.getPrecioVenta();
					dvDest.setSubTotal(subTotal);
					dvDest.setFechaAlta(fechaVenta);
					detalleVentaRepo.save(dvDest);

					totalDia += subTotal;
					destornillador.setStockActual(destornillador.getStockActual() - demandaDest);
				}

				// -------------------------
				// Demanda TALADRO
				// Muy alta fines de semana, baja en semana
				// -------------------------
				int baseTaladro = 0;
				int extraFindeTaladro = (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) ? 5 : 0;
				int ruidoTaladro = random.nextInt(3) - 1;
				int demandaTaladro = Math.max(0, baseTaladro + extraFindeTaladro + ruidoTaladro);

				demandaTaladro = Math.min(demandaTaladro, taladro.getStockActual());
				if (demandaTaladro > 0) {
					DetalleVenta dvTaladro = new DetalleVenta();
					dvTaladro.setVenta(ventaDia);
					dvTaladro.setArticulo(taladro);
					dvTaladro.setCantidad(demandaTaladro);
					double subTotal = demandaTaladro * taladro.getPrecioVenta();
					dvTaladro.setSubTotal(subTotal);
					dvTaladro.setFechaAlta(fechaVenta);
					detalleVentaRepo.save(dvTaladro);

					totalDia += subTotal;
					taladro.setStockActual(taladro.getStockActual() - demandaTaladro);
				}

				ventaDia.setTotalVenta(totalDia);
				ventaRepo.save(ventaDia);
			}

			articuloRepo.save(martillo);
			articuloRepo.save(destornillador);
			articuloRepo.save(taladro);

			System.out.println("===== DATOS DE PRUEBA COMPLETOS CARGADOS =====");
		};
	}


}
