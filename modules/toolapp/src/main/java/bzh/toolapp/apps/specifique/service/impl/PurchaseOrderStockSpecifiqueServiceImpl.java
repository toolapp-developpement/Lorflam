package bzh.toolapp.apps.specifique.service.impl;

import bzh.toolapp.apps.specifique.service.StockMoveLineSpecifiqueCreationService;
import com.avr.apps.helpdesk.service.StockMoveCreateService;
import com.avr.apps.helpdesk.service.impl.PurchaseOrderCreateStockServiceImpl;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.service.ShippingCoefService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.service.PartnerStockSettingsService;
import com.axelor.apps.stock.service.StockMoveLineService;
import com.axelor.apps.stock.service.StockMoveService;
import com.axelor.apps.supplychain.service.PurchaseOrderLineServiceSupplychainImpl;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychain;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PurchaseOrderStockSpecifiqueServiceImpl extends PurchaseOrderCreateStockServiceImpl {

  protected StockMoveLineSpecifiqueCreationService stockMoveLineSpecifiqueCreationService;

  @Inject
  public PurchaseOrderStockSpecifiqueServiceImpl(
      UnitConversionService unitConversionService,
      StockMoveLineRepository stockMoveLineRepository,
      PurchaseOrderLineServiceSupplychainImpl purchaseOrderLineServiceSupplychainImpl,
      AppBaseService appBaseService,
      ShippingCoefService shippingCoefService,
      StockMoveLineServiceSupplychain stockMoveLineServiceSupplychain,
      StockMoveService stockMoveService,
      PartnerStockSettingsService partnerStockSettingsService,
      StockMoveCreateService stockMoveCreateService,
      StockMoveLineSpecifiqueCreationService stockMoveLineSpecifiqueCreationService) {
    super(
        unitConversionService,
        stockMoveLineRepository,
        purchaseOrderLineServiceSupplychainImpl,
        appBaseService,
        shippingCoefService,
        stockMoveLineServiceSupplychain,
        stockMoveService,
        partnerStockSettingsService,
        stockMoveCreateService);
    this.stockMoveLineSpecifiqueCreationService = stockMoveLineSpecifiqueCreationService;
  }

  @Override
  protected StockMoveLine createProductStockMoveLine(
      PurchaseOrderLine purchaseOrderLine, BigDecimal qty, StockMove stockMove)
      throws AxelorException {

    PurchaseOrder purchaseOrder = purchaseOrderLine.getPurchaseOrder();
    Product product = purchaseOrderLine.getProduct();
    Unit unit = product.getUnit();
    BigDecimal priceDiscounted = purchaseOrderLine.getPriceDiscounted();
    BigDecimal companyUnitPriceUntaxed = purchaseOrderLine.getCompanyExTaxTotal();

    if (purchaseOrderLine.getQty().compareTo(BigDecimal.ZERO) != 0) {
      companyUnitPriceUntaxed =
          purchaseOrderLine
              .getCompanyExTaxTotal()
              .divide(
                  purchaseOrderLine.getQty(),
                  appBaseService.getNbDecimalDigitForUnitPrice(),
                  RoundingMode.HALF_UP);
    }

    if (unit != null && !unit.equals(purchaseOrderLine.getUnit())) {
      qty =
          unitConversionService.convert(
              purchaseOrderLine.getUnit(), unit, qty, qty.scale(), product);

      priceDiscounted =
          unitConversionService.convert(
              unit,
              purchaseOrderLine.getUnit(),
              priceDiscounted,
              appBaseService.getNbDecimalDigitForUnitPrice(),
              product);

      companyUnitPriceUntaxed =
          unitConversionService.convert(
              unit,
              purchaseOrderLine.getUnit(),
              companyUnitPriceUntaxed,
              appBaseService.getNbDecimalDigitForUnitPrice(),
              product);
    }

    BigDecimal shippingCoef =
        shippingCoefService.getShippingCoef(
            product, purchaseOrder.getSupplierPartner(), purchaseOrder.getCompany(), qty);
    BigDecimal companyPurchasePrice = priceDiscounted;
    priceDiscounted = priceDiscounted.multiply(shippingCoef);
    companyUnitPriceUntaxed = companyUnitPriceUntaxed.multiply(shippingCoef);

    BigDecimal taxRate = BigDecimal.ZERO;
    TaxLine taxLine = purchaseOrderLine.getTaxLine();
    if (taxLine != null) {
      taxRate = taxLine.getValue();
    }
    if (purchaseOrderLine.getReceiptState() == 0) {
      purchaseOrderLine.setReceiptState(PurchaseOrderLineRepository.RECEIPT_STATE_NOT_RECEIVED);
    }

    return stockMoveLineSpecifiqueCreationService.createStockMoveLine(
        product,
        purchaseOrderLine.getProductName(),
        purchaseOrderLine.getDescription(),
        qty,
        BigDecimal.ZERO,
        priceDiscounted,
        companyUnitPriceUntaxed,
        companyPurchasePrice,
        unit,
        stockMove,
        StockMoveLineService.TYPE_PURCHASES,
        purchaseOrder.getInAti(),
        taxRate,
        null,
        purchaseOrderLine);
  }

  @Override
  protected StockMoveLine createTitleStockMoveLine(
      PurchaseOrderLine purchaseOrderLine, StockMove stockMove) throws AxelorException {

    return stockMoveLineSpecifiqueCreationService.createStockMoveLine(
        purchaseOrderLine.getProduct(),
        purchaseOrderLine.getProductName(),
        purchaseOrderLine.getDescription(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        stockMove,
        2,
        purchaseOrderLine.getPurchaseOrder().getInAti(),
        null,
        null,
        purchaseOrderLine);
  }
}
