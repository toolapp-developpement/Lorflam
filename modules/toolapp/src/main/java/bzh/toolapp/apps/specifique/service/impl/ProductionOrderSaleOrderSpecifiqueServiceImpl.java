package bzh.toolapp.apps.specifique.service.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.businessproduction.service.ProductionOrderSaleOrderServiceBusinessImpl;
import com.axelor.apps.businessproduction.service.ProductionOrderServiceBusinessImpl;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.productionorder.ProductionOrderService;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;

public class ProductionOrderSaleOrderSpecifiqueServiceImpl extends ProductionOrderSaleOrderServiceBusinessImpl {

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected ProductionOrderServiceBusinessImpl productionOrderServiceBusinessImpl;

	@Inject
	public ProductionOrderSaleOrderSpecifiqueServiceImpl(UnitConversionService unitConversionService,
			ProductionOrderService productionOrderService, ProductionOrderRepository productionOrderRepo,
			ProductionOrderServiceBusinessImpl productionOrderServiceBusinessImpl,
			AppProductionService appProductionService) {
		super(unitConversionService, productionOrderService, productionOrderRepo, productionOrderServiceBusinessImpl,
				appProductionService);

		this.productionOrderServiceBusinessImpl = productionOrderServiceBusinessImpl;
	}

	@Override
	public ProductionOrder generateManufOrders(ProductionOrder productionOrder, SaleOrderLine saleOrderLine)
			throws AxelorException {

		Product product = saleOrderLine.getProduct();

		if (saleOrderLine.getSaleSupplySelect() == ProductRepository.SALE_SUPPLY_PRODUCE && product != null
				&& product.getProductTypeSelect().equals(ProductRepository.PRODUCT_TYPE_STORABLE)) {

			BillOfMaterial billOfMaterial = saleOrderLine.getBillOfMaterial();

			if (billOfMaterial == null) {
				billOfMaterial = product.getDefaultBillOfMaterial();
			}

			if (billOfMaterial == null && product.getParentProduct() != null) {
				billOfMaterial = product.getParentProduct().getDefaultBillOfMaterial();
			}

			if (billOfMaterial == null) {
				throw new AxelorException(saleOrderLine, TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
						I18n.get(IExceptionMessage.PRODUCTION_ORDER_SALES_ORDER_NO_BOM), product.getName(),
						product.getCode());
			}

			if (billOfMaterial.getProdProcess() == null) {
				return null;
			}

			Unit unit = saleOrderLine.getProduct().getUnit();
			BigDecimal qty = saleOrderLine.getQty();
			if (unit != null && !unit.equals(saleOrderLine.getUnit())) {
				qty = unitConversionService.convert(saleOrderLine.getUnit(), unit, qty, qty.scale(),
						saleOrderLine.getProduct());
			}

			/*
			 * Modification de la date de debut de planification de l'OF par la date
			 * d'expedition de l'entete de commande de vente - 3jours
			 */
			System.out.println("Timmmy");
			LocalDateTime startedDate = LocalDateTime.now();
			if (saleOrderLine.getSaleOrder().getDeliveryDate() != null) {
				LocalDate startDate = saleOrderLine.getSaleOrder().getDeliveryDate();
				startedDate = startDate.minusDays(3).atStartOfDay();
			}

			return generateManufOrders(productionOrder, billOfMaterial, qty, startedDate, saleOrderLine.getSaleOrder());
		}

		return null;
	}

}
