package bzh.toolapp.apps.specifique.service.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.service.PriceListService;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.ShippingCoefService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.tax.AccountManagementService;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.TrackingNumber;
import com.axelor.apps.stock.db.TrackingNumberConfiguration;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.TrackingNumberRepository;
import com.axelor.apps.stock.service.StockLocationLineService;
import com.axelor.apps.stock.service.StockMoveToolService;
import com.axelor.apps.stock.service.TrackingNumberService;
import com.axelor.apps.stock.service.WeightedAveragePriceService;
import com.axelor.apps.stock.service.app.AppStockService;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychainImpl;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

import bzh.toolapp.apps.specifique.service.StockMoveLineSpecifiqueCreationService;

@RequestScoped
public class StockMoveLineSpecifiqueCreationServiceImpl extends StockMoveLineServiceSupplychainImpl
		implements StockMoveLineSpecifiqueCreationService {

	@Inject
	public StockMoveLineSpecifiqueCreationServiceImpl(TrackingNumberService trackingNumberService,
			AppBaseService appBaseService, AppStockService appStockService, StockMoveToolService stockMoveToolService,
			StockMoveLineRepository stockMoveLineRepository, StockLocationLineService stockLocationLineService,
			UnitConversionService unitConversionService, WeightedAveragePriceService weightedAveragePriceService,
			TrackingNumberRepository trackingNumberRepo, ShippingCoefService shippingCoefService,
			AccountManagementService accountManagementService, PriceListService priceListService,
			ProductCompanyService productCompanyService) {

		super(trackingNumberService, appBaseService, appStockService, stockMoveToolService, stockMoveLineRepository,
				stockLocationLineService, unitConversionService, weightedAveragePriceService, trackingNumberRepo,
				shippingCoefService, accountManagementService, priceListService, productCompanyService);

	}

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public StockMoveLine createStockMoveLine(Product product, String productName, String description,
			BigDecimal quantity, BigDecimal requestedReservedQty, BigDecimal unitPrice,
			BigDecimal companyUnitPriceUntaxed, BigDecimal companyPurchasePrice, Unit unit, StockMove stockMove,
			int type, boolean taxed, BigDecimal taxRate, SaleOrderLine saleOrderLine,
			PurchaseOrderLine purchaseOrderLine) throws AxelorException {
		String yard = "";

		if (saleOrderLine != null) {
			yard = saleOrderLine.getYard2();
		}

		if (purchaseOrderLine != null) {
			yard = purchaseOrderLine.getYard();
		}

		if (product != null) {

			StockMoveLine stockMoveLine = generateStockMoveLineSpecifiqueConvertingUnitPrice(product, productName,
					description, quantity, unitPrice, companyUnitPriceUntaxed, companyPurchasePrice, unit, stockMove,
					taxed, taxRate, yard);
			stockMoveLine.setRequestedReservedQty(requestedReservedQty);
			stockMoveLine.setIsQtyRequested(requestedReservedQty != null && requestedReservedQty.signum() > 0);
			stockMoveLine.setSaleOrderLine(saleOrderLine);
			stockMoveLine.setPurchaseOrderLine(purchaseOrderLine);
			TrackingNumberConfiguration trackingNumberConfiguration = product.getTrackingNumberConfiguration();

			return assignOrGenerateTrackingNumber(stockMoveLine, stockMove, product, trackingNumberConfiguration, type);
		} else {
			return this.createStockMoveLine(product, productName, description, quantity, BigDecimal.ZERO,
					BigDecimal.ZERO, companyUnitPriceUntaxed, BigDecimal.ZERO, unit, stockMove, null, yard);
		}
	}

	@Override
	public StockMoveLine createStockMoveLine(Product product, String productName, String description,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal companyUnitPriceUntaxed, Unit unit,
			StockMove stockMove, int type, boolean taxed, BigDecimal taxRate, String yard) throws AxelorException {

		if (product != null) {

			StockMoveLine stockMoveLine = generateStockMoveLineSpecifiqueConvertingUnitPrice(product, productName,
					description, quantity, unitPrice, companyUnitPriceUntaxed, BigDecimal.ZERO, unit, stockMove, taxed,
					taxRate, yard);
			TrackingNumberConfiguration trackingNumberConfiguration = product.getTrackingNumberConfiguration();

			return assignOrGenerateTrackingNumber(stockMoveLine, stockMove, product, trackingNumberConfiguration, type);
		} else {
			return this.createStockMoveLine(product, productName, description, quantity, BigDecimal.ZERO,
					BigDecimal.ZERO, companyUnitPriceUntaxed, BigDecimal.ZERO, unit, stockMove, null, yard);
		}
	}

	@Override
	public StockMoveLine createStockMoveLine(Product product, String productName, String description,
			BigDecimal quantity, BigDecimal unitPriceUntaxed, BigDecimal unitPriceTaxed,
			BigDecimal companyUnitPriceUntaxed, BigDecimal companyPurchasePrice, Unit unit, StockMove stockMove,
			TrackingNumber trackingNumber, String yard) throws AxelorException {

		StockMoveLine stockMoveLine = new StockMoveLine();
		stockMoveLine.setProduct(product);
		stockMoveLine.setProductName(productName);
		stockMoveLine.setDescription(description);
		stockMoveLine.setQty(quantity);
		stockMoveLine.setRealQty(quantity);
		stockMoveLine.setUnitPriceUntaxed(unitPriceUntaxed);
		stockMoveLine.setUnitPriceTaxed(unitPriceTaxed);
		stockMoveLine.setUnit(unit);
		stockMoveLine.setTrackingNumber(trackingNumber);
		stockMoveLine.setCompanyUnitPriceUntaxed(companyUnitPriceUntaxed);
		stockMoveLine.setCompanyPurchasePrice(companyPurchasePrice);
		stockMoveLine.setYard(yard);

		if (stockMove != null) {
			stockMove.addStockMoveLineListItem(stockMoveLine);
			stockMoveLine.setNetMass(this.computeNetMass(stockMove, stockMoveLine, stockMove.getCompany()));
			stockMoveLine.setSequence(stockMove.getStockMoveLineList().size());
		} else {
			stockMoveLine.setNetMass(this.computeNetMass(stockMove, stockMoveLine, null));
		}

		stockMoveLine.setTotalNetMass(
				stockMoveLine.getRealQty().multiply(stockMoveLine.getNetMass()).setScale(2, RoundingMode.HALF_UP));

		if (product != null) {
			stockMoveLine.setCountryOfOrigin(product.getCountryOfOrigin());
			stockMoveLine.setProductTypeSelect(product.getProductTypeSelect());
		}

		return stockMoveLine;
	}

	protected StockMoveLine generateStockMoveLineSpecifiqueConvertingUnitPrice(Product product, String productName,
			String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal companyUnitPriceUntaxed,
			BigDecimal companyPurchasePrice, Unit unit, StockMove stockMove, boolean taxed, BigDecimal taxRate,
			String yard) throws AxelorException {
		BigDecimal unitPriceUntaxed;
		BigDecimal unitPriceTaxed;
		if (taxed) {
			unitPriceTaxed = unitPrice.setScale(appBaseService.getNbDecimalDigitForUnitPrice(), RoundingMode.HALF_UP);
			unitPriceUntaxed = unitPrice.divide(taxRate.add(BigDecimal.ONE),
					appBaseService.getNbDecimalDigitForUnitPrice(), RoundingMode.HALF_UP);
		} else {
			unitPriceUntaxed = unitPrice.setScale(appBaseService.getNbDecimalDigitForUnitPrice(), RoundingMode.HALF_UP);
			unitPriceTaxed = unitPrice.multiply(taxRate.add(BigDecimal.ONE))
					.setScale(appBaseService.getNbDecimalDigitForUnitPrice(), RoundingMode.HALF_UP);
		}
		return this.createStockMoveLine(product, productName, description, quantity, unitPriceUntaxed, unitPriceTaxed,
				companyUnitPriceUntaxed, companyPurchasePrice, unit, stockMove, null, yard);
	}

	@Override
	public StockMoveLine splitStockMoveLine(StockMoveLine stockMoveLine, BigDecimal qty, TrackingNumber trackingNumber)
			throws AxelorException {

		StockMoveLine newStockMoveLine = this.createStockMoveLine(stockMoveLine.getProduct(),
				stockMoveLine.getProductName(), stockMoveLine.getDescription(), qty,
				stockMoveLine.getUnitPriceUntaxed(), stockMoveLine.getUnitPriceTaxed(),
				stockMoveLine.getCompanyUnitPriceUntaxed(), stockMoveLine.getCompanyPurchasePrice(),
				stockMoveLine.getUnit(), stockMoveLine.getStockMove(), trackingNumber, stockMoveLine.getYard());

		stockMoveLine.setQty(stockMoveLine.getQty().subtract(qty));
		stockMoveLine.setRealQty(stockMoveLine.getRealQty().subtract(qty));

		return newStockMoveLine;
	}
}
