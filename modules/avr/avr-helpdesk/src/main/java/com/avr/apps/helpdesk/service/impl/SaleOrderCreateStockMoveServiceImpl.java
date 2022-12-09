package com.avr.apps.helpdesk.service.impl;

import com.avr.apps.helpdesk.service.StockMoveCreateService;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.service.*;
import com.axelor.apps.stock.service.config.StockConfigService;
import com.axelor.apps.supplychain.service.SaleOrderLineServiceSupplyChain;
import com.axelor.apps.supplychain.service.SaleOrderStockServiceImpl;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychain;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.apps.supplychain.service.config.SupplyChainConfigService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 12:09 @Update 25/04/2022
 */
public class SaleOrderCreateStockMoveServiceImpl extends SaleOrderStockServiceImpl {

  protected final StockMoveCreateService stockMoveCreateService;

  @Inject
  public SaleOrderCreateStockMoveServiceImpl(
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
      StockMoveCreateService stockMoveCreateService) {
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
        partnerStockSettingsService);
    this.stockMoveCreateService = stockMoveCreateService;
  }

  @Override
  public StockMove createStockMove(
      SaleOrder saleOrder, Company company, LocalDate estimatedDeliveryDate)
      throws AxelorException {
    StockLocation toStockLocation = saleOrder.getToStockLocation();
    if (toStockLocation == null) {
      toStockLocation =
          partnerStockSettingsService.getDefaultExternalStockLocation(
              saleOrder.getClientPartner(), company);
    }
    if (toStockLocation == null) {
      toStockLocation =
          stockConfigService.getCustomerVirtualStockLocation(
              stockConfigService.getStockConfig(company));
    }

    Partner partner = computePartnerToUseForStockMove(saleOrder);

    StockMove stockMove =
        stockMoveCreateService.createStockMove(
            null,
            saleOrder.getDeliveryAddress(),
            company,
            partner,
            saleOrder.getStockLocation(),
            toStockLocation,
            null,
            estimatedDeliveryDate,
            saleOrder.getDescription(),
            saleOrder.getShipmentMode(),
            saleOrder.getFreightCarrierMode(),
            saleOrder.getCarrierPartner(),
            saleOrder.getForwarderPartner(),
            saleOrder.getIncoterm(),
            StockMoveRepository.TYPE_OUTGOING,
            saleOrder.getYard());

    stockMove.setToAddressStr(saleOrder.getDeliveryAddressStr());
    stockMove.setOriginId(saleOrder.getId());
    stockMove.setOriginTypeSelect(StockMoveRepository.ORIGIN_SALE_ORDER);
    stockMove.setOrigin(saleOrder.getSaleOrderSeq());
    stockMove.setStockMoveLineList(new ArrayList<>());
    stockMove.setTradingName(saleOrder.getTradingName());
    stockMove.setSpecificPackage(saleOrder.getSpecificPackage());
    stockMove.setNote(saleOrder.getDeliveryComments());
    stockMove.setPickingOrderComments(saleOrder.getPickingOrderComments());
    stockMove.setGroupProductsOnPrintings(partner.getGroupProductsOnPrintings());
    stockMove.setInvoicedPartner(saleOrder.getInvoicedPartner());
    if (stockMove.getPartner() != null) {
      setDefaultAutoMailSettings(stockMove);
    }
    return stockMove;
  }
}
