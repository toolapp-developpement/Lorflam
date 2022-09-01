package bzh.toolapp.apps.specifique.service.impl;

import bzh.toolapp.apps.specifique.service.StockMoveLineSpecifiqueCreationService;
import com.avr.apps.helpdesk.service.StockMoveCreateService;
import com.avr.apps.helpdesk.service.impl.SaleOrderCreateStockMoveServiceImpl;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.service.PartnerStockSettingsService;
import com.axelor.apps.stock.service.StockMoveLineService;
import com.axelor.apps.stock.service.StockMoveService;
import com.axelor.apps.stock.service.config.StockConfigService;
import com.axelor.apps.supplychain.service.SaleOrderLineServiceSupplyChain;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychain;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.apps.supplychain.service.config.SupplyChainConfigService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SaleOrderStockSpecifiqueServiceImpl extends SaleOrderCreateStockMoveServiceImpl {

  protected StockMoveLineSpecifiqueCreationService stockMoveLineSpecifiqueCreationService;

  @Inject
  public SaleOrderStockSpecifiqueServiceImpl(
      StockMoveService stockMoveService,
      StockMoveLineService stockMoveLineService,
      StockConfigService stockConfigService,
      UnitConversionService unitConversionService,
      SaleOrderLineServiceSupplyChain saleOrderLineServiceSupplyChain,
      StockMoveLineServiceSupplychain stockMoveLineSupplychainService,
      StockMoveLineRepository stockMoveLineRepository,
      AppBaseService appBaseService,
      SaleOrderRepository saleOrderRepository,
      AppSupplychainService appSupplychainService,
      SupplyChainConfigService supplyChainConfigService,
      ProductCompanyService productCompanyService,
      PartnerStockSettingsService partnerStockSettingsService,
      StockMoveCreateService stockMoveCreateService,
      StockMoveLineSpecifiqueCreationService stockMoveLineSpecifiqueCreationService) {
    super(
        stockMoveService,
        stockMoveLineService,
        stockConfigService,
        unitConversionService,
        saleOrderLineServiceSupplyChain,
        stockMoveLineSupplychainService,
        stockMoveLineRepository,
        appBaseService,
        saleOrderRepository,
        appSupplychainService,
        supplyChainConfigService,
        productCompanyService,
        partnerStockSettingsService,
        stockMoveCreateService);

    this.stockMoveLineSpecifiqueCreationService = stockMoveLineSpecifiqueCreationService;
  }

  @Override
  public StockMoveLine createStockMoveLine(
      StockMove stockMove, SaleOrderLine saleOrderLine, BigDecimal qty) throws AxelorException {

    if (this.isStockMoveProduct(saleOrderLine)) {

      Unit unit = saleOrderLine.getProduct().getUnit();
      BigDecimal priceDiscounted = saleOrderLine.getPriceDiscounted();
      BigDecimal requestedReservedQty =
          saleOrderLine.getRequestedReservedQty().subtract(saleOrderLine.getDeliveredQty());

      BigDecimal companyUnitPriceUntaxed =
          (BigDecimal)
              productCompanyService.get(
                  saleOrderLine.getProduct(),
                  "costPrice",
                  saleOrderLine.getSaleOrder() != null
                      ? saleOrderLine.getSaleOrder().getCompany()
                      : null);
      if (unit != null && !unit.equals(saleOrderLine.getUnit())) {
        qty =
            unitConversionService.convert(
                saleOrderLine.getUnit(), unit, qty, qty.scale(), saleOrderLine.getProduct());
        priceDiscounted =
            unitConversionService.convert(
                unit,
                saleOrderLine.getUnit(),
                priceDiscounted,
                appBaseService.getNbDecimalDigitForUnitPrice(),
                saleOrderLine.getProduct());
        requestedReservedQty =
            unitConversionService.convert(
                saleOrderLine.getUnit(),
                unit,
                requestedReservedQty,
                requestedReservedQty.scale(),
                saleOrderLine.getProduct());
      }

      BigDecimal taxRate = BigDecimal.ZERO;
      TaxLine taxLine = saleOrderLine.getTaxLine();
      if (taxLine != null) {
        taxRate = taxLine.getValue();
      }
      if (saleOrderLine.getQty().signum() != 0) {
        companyUnitPriceUntaxed =
            saleOrderLine
                .getCompanyExTaxTotal()
                .divide(
                    saleOrderLine.getQty(),
                    Beans.get(AppBaseService.class).getNbDecimalDigitForUnitPrice(),
                    RoundingMode.HALF_UP);
      }

      StockMoveLine stockMoveLine =
          stockMoveLineSpecifiqueCreationService.createStockMoveLine(
              saleOrderLine.getProduct(),
              saleOrderLine.getProductName(),
              saleOrderLine.getDescription(),
              qty,
              requestedReservedQty,
              priceDiscounted,
              companyUnitPriceUntaxed,
              null,
              unit,
              stockMove,
              StockMoveLineService.TYPE_SALES,
              saleOrderLine.getSaleOrder().getInAti(),
              taxRate,
              saleOrderLine,
              null);

      if (saleOrderLine.getDeliveryState() == 0) {
        saleOrderLine.setDeliveryState(SaleOrderLineRepository.DELIVERY_STATE_NOT_DELIVERED);
      }

      return stockMoveLine;
    }
    return null;
  }
}
