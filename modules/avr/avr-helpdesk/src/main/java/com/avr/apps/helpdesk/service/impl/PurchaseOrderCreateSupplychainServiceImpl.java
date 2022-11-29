package com.avr.apps.helpdesk.service.impl;

import com.avr.apps.helpdesk.db.Yard;
import com.avr.apps.helpdesk.service.PurchaseOrderCreationService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.base.db.*;
import com.axelor.apps.base.service.TradingNameService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.purchase.service.PurchaseOrderLineService;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.supplychain.service.BudgetSupplychainService;
import com.axelor.apps.supplychain.service.PurchaseOrderServiceSupplychainImpl;
import com.axelor.apps.supplychain.service.PurchaseOrderStockService;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 11:35 @Update 25/04/2022
 */
public class PurchaseOrderCreateSupplychainServiceImpl extends PurchaseOrderServiceSupplychainImpl
    implements PurchaseOrderCreationService {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public PurchaseOrderCreateSupplychainServiceImpl(
      AppSupplychainService appSupplychainService,
      AccountConfigService accountConfigService,
      AppAccountService appAccountService,
      AppBaseService appBaseService,
      PurchaseOrderStockService purchaseOrderStockService,
      BudgetSupplychainService budgetSupplychainService,
      PurchaseOrderLineRepository purchaseOrderLineRepository,
      PurchaseOrderLineService purchaseOrderLineService) {
    super(
        appSupplychainService,
        accountConfigService,
        appAccountService,
        appBaseService,
        purchaseOrderStockService,
        budgetSupplychainService,
        purchaseOrderLineRepository,
        purchaseOrderLineService);
  }

  @Override
  public PurchaseOrder createPurchaseOrder(
      User buyerUser,
      Company company,
      Partner contactPartner,
      Currency currency,
      LocalDate deliveryDate,
      String internalReference,
      String externalReference,
      StockLocation stockLocation,
      LocalDate orderDate,
      PriceList priceList,
      Partner supplierPartner,
      TradingName tradingName,
      Yard yard)
      throws AxelorException {
    logger.debug(
        "Création d'une commande fournisseur : Société = {},  Reference externe = {}, Fournisseur = {}",
        company.getName(),
        externalReference,
        supplierPartner.getFullName());

    PurchaseOrder purchaseOrder =
        createPurchaseOrder(
            buyerUser,
            company,
            contactPartner,
            currency,
            deliveryDate,
            internalReference,
            externalReference,
            orderDate,
            priceList,
            supplierPartner,
            tradingName,
            yard);

    purchaseOrder.setStockLocation(stockLocation);

    purchaseOrder.setPaymentMode(supplierPartner.getOutPaymentMode());
    purchaseOrder.setPaymentCondition(supplierPartner.getPaymentCondition());

    if (purchaseOrder.getPaymentMode() == null) {
      purchaseOrder.setPaymentMode(
          this.accountConfigService.getAccountConfig(company).getOutPaymentMode());
    }

    if (purchaseOrder.getPaymentCondition() == null) {
      purchaseOrder.setPaymentCondition(
          this.accountConfigService.getAccountConfig(company).getDefPaymentCondition());
    }

    purchaseOrder.setTradingName(tradingName);

    return purchaseOrder;
  }

  @Override
  public PurchaseOrder createPurchaseOrder(
      User buyerUser,
      Company company,
      Partner contactPartner,
      Currency currency,
      LocalDate deliveryDate,
      String internalReference,
      String externalReference,
      LocalDate orderDate,
      PriceList priceList,
      Partner supplierPartner,
      TradingName tradingName,
      Yard yard)
      throws AxelorException {
    logger.debug(
        "Création d'une commande fournisseur : Société = {},  Reference externe = {}, Fournisseur = {}",
        new Object[] {company.getName(), externalReference, supplierPartner.getFullName()});

    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setBuyerUser(buyerUser);
    purchaseOrder.setCompany(company);
    purchaseOrder.setContactPartner(contactPartner);
    purchaseOrder.setCurrency(currency);
    purchaseOrder.setDeliveryDate(deliveryDate);
    purchaseOrder.setInternalReference(internalReference);
    purchaseOrder.setExternalReference(externalReference);
    purchaseOrder.setOrderDate(orderDate);
    purchaseOrder.setPriceList(priceList);
    purchaseOrder.setTradingName(tradingName);
    purchaseOrder.setPurchaseOrderLineList(new ArrayList<>());

    purchaseOrder.setPrintingSettings(
        Beans.get(TradingNameService.class).getDefaultPrintingSettings(null, company));

    purchaseOrder.setPurchaseOrderSeq(this.getSequence(company));
    purchaseOrder.setStatusSelect(PurchaseOrderRepository.STATUS_DRAFT);
    purchaseOrder.setSupplierPartner(supplierPartner);

    purchaseOrder.setYard(yard);

    return purchaseOrder;
  }
}
