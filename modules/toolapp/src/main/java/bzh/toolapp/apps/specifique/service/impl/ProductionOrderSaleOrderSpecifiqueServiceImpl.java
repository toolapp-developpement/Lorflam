package bzh.toolapp.apps.specifique.service.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.productionorder.ProductionOrderSaleOrderServiceImpl;
import com.axelor.apps.production.service.productionorder.ProductionOrderService;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;

public class ProductionOrderSaleOrderSpecifiqueServiceImpl extends ProductionOrderSaleOrderServiceImpl {

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected UnitConversionService unitConversionService;
	protected ProductionOrderService productionOrderService;
	protected ProductionOrderRepository productionOrderRepo;
	protected AppProductionService appProductionService;

	@Inject
	public ProductionOrderSaleOrderSpecifiqueServiceImpl(UnitConversionService unitConversionService,
			ProductionOrderService productionOrderService, ProductionOrderRepository productionOrderRepo,
			AppProductionService appProductionService) {
		super(unitConversionService, productionOrderService, productionOrderRepo, appProductionService);

		this.unitConversionService = unitConversionService;
		this.productionOrderService = productionOrderService;
		this.productionOrderRepo = productionOrderRepo;
		this.appProductionService = appProductionService;
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

			LocalDateTime startDate = saleOrderLine.getSaleOrder().getDeliveryDate().minusDays(3).atStartOfDay();

			return generateManufOrders(productionOrder, billOfMaterial, qty, startDate, saleOrderLine.getSaleOrder());
		}

		return null;
	}

}
