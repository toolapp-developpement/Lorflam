package com.avr.apps.helpdesk.service.impl;

import com.avr.apps.helpdesk.service.StockMoveCreateService;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.ShippingCoefService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.service.PartnerStockSettingsService;
import com.axelor.apps.stock.service.StockMoveService;
import com.axelor.apps.supplychain.db.SupplyChainConfig;
import com.axelor.apps.supplychain.db.repo.SupplyChainConfigRepository;
import com.axelor.apps.supplychain.service.PurchaseOrderLineServiceSupplychainImpl;
import com.axelor.apps.supplychain.service.PurchaseOrderStockServiceImpl;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychain;
import com.axelor.apps.supplychain.service.config.SupplyChainConfigService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 12:16 @Update 25/04/2022
 */
public class PurchaseOrderCreateStockServiceImpl extends PurchaseOrderStockServiceImpl {

  protected final StockMoveCreateService stockMoveCreateService;

  @Inject
  public PurchaseOrderCreateStockServiceImpl(
      UnitConversionService unitConversionService,
      StockMoveLineRepository stockMoveLineRepository,
      PurchaseOrderLineServiceSupplychainImpl purchaseOrderLineServiceSupplychainImpl,
      AppBaseService appBaseService,
      ShippingCoefService shippingCoefService,
      StockMoveLineServiceSupplychain stockMoveLineServiceSupplychain,
      StockMoveService stockMoveService,
      PartnerStockSettingsService partnerStockSettingsService,
      StockMoveCreateService stockMoveCreateService) {
    super(
        unitConversionService,
        stockMoveLineRepository,
        purchaseOrderLineServiceSupplychainImpl,
        appBaseService,
        shippingCoefService,
        stockMoveLineServiceSupplychain,
        stockMoveService,
        partnerStockSettingsService);
    this.stockMoveCreateService = stockMoveCreateService;
  }

  @Override
  protected List<Long> createStockMove(
      PurchaseOrder purchaseOrder,
      LocalDate estimatedDeliveryDate,
      List<PurchaseOrderLine> purchaseOrderLineList)
      throws AxelorException {
    List<Long> stockMoveIdList = new ArrayList<>();

    Partner supplierPartner = purchaseOrder.getSupplierPartner();
    Company company = purchaseOrder.getCompany();

    Address address = Beans.get(PartnerService.class).getDeliveryAddress(supplierPartner);

    StockLocation startLocation = getStartStockLocation(purchaseOrder);

    StockMove stockMove =
        stockMoveCreateService.createStockMove(
            address,
            null,
            company,
            supplierPartner,
            startLocation,
            purchaseOrder.getStockLocation(),
            null,
            estimatedDeliveryDate,
            purchaseOrder.getNotes(),
            purchaseOrder.getShipmentMode(),
            purchaseOrder.getFreightCarrierMode(),
            null,
            null,
            null,
            StockMoveRepository.TYPE_INCOMING,
            purchaseOrder.getYard());

    StockMove qualityStockMove =
        stockMoveCreateService.createStockMove(
            address,
            null,
            company,
            supplierPartner,
            startLocation,
            appBaseService.getAppBase().getEnableTradingNamesManagement()
                ? purchaseOrder.getTradingName().getQualityControlDefaultStockLocation()
                : company.getStockConfig().getQualityControlDefaultStockLocation(),
            null,
            estimatedDeliveryDate,
            purchaseOrder.getNotes(),
            purchaseOrder.getShipmentMode(),
            purchaseOrder.getFreightCarrierMode(),
            null,
            null,
            null,
            StockMoveRepository.TYPE_INCOMING,
            purchaseOrder.getYard());

    stockMove.setOriginId(purchaseOrder.getId());
    stockMove.setOriginTypeSelect(StockMoveRepository.ORIGIN_PURCHASE_ORDER);
    stockMove.setOrigin(purchaseOrder.getPurchaseOrderSeq());
    stockMove.setTradingName(purchaseOrder.getTradingName());
    stockMove.setGroupProductsOnPrintings(purchaseOrder.getGroupProductsOnPrintings());

    qualityStockMove.setOriginId(purchaseOrder.getId());
    qualityStockMove.setOriginTypeSelect(StockMoveRepository.ORIGIN_PURCHASE_ORDER);
    qualityStockMove.setOrigin(purchaseOrder.getPurchaseOrderSeq());
    qualityStockMove.setTradingName(purchaseOrder.getTradingName());
    qualityStockMove.setGroupProductsOnPrintings(purchaseOrder.getGroupProductsOnPrintings());

    SupplyChainConfig supplychainConfig =
        Beans.get(SupplyChainConfigService.class).getSupplyChainConfig(purchaseOrder.getCompany());
    if (supplychainConfig.getDefaultEstimatedDateForPurchaseOrder()
            == SupplyChainConfigRepository.CURRENT_DATE
        && stockMove.getEstimatedDate() == null) {
      stockMove.setEstimatedDate(appBaseService.getTodayDate(company));
    } else if (supplychainConfig.getDefaultEstimatedDateForPurchaseOrder()
            == SupplyChainConfigRepository.CURRENT_DATE_PLUS_DAYS
        && stockMove.getEstimatedDate() == null) {
      stockMove.setEstimatedDate(
          appBaseService
              .getTodayDate(company)
              .plusDays(supplychainConfig.getNumberOfDaysForPurchaseOrder().longValue()));
    }

    for (PurchaseOrderLine purchaseOrderLine : purchaseOrderLineList) {
      BigDecimal qty =
          purchaseOrderLineServiceSupplychainImpl.computeUndeliveredQty(purchaseOrderLine);

      if (qty.signum() > 0 && !existActiveStockMoveForPurchaseOrderLine(purchaseOrderLine)) {
        this.createStockMoveLine(stockMove, qualityStockMove, purchaseOrderLine, qty);
      }
    }
    if (stockMove.getStockMoveLineList() != null && !stockMove.getStockMoveLineList().isEmpty()) {
      stockMoveService.plan(stockMove);
      stockMoveIdList.add(stockMove.getId());
    }
    if (qualityStockMove.getStockMoveLineList() != null
        && !qualityStockMove.getStockMoveLineList().isEmpty()) {
      stockMoveService.plan(qualityStockMove);
      stockMoveIdList.add(qualityStockMove.getId());
    }

    return stockMoveIdList;
  }
}
