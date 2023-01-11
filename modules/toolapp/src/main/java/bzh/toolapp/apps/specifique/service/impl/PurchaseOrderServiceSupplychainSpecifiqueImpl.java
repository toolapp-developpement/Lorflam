package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.base.db.*;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.service.PurchaseOrderServiceProductionImpl;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.service.PurchaseOrderLineService;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.supplychain.service.BudgetSupplychainService;
import com.axelor.apps.supplychain.service.PurchaseOrderStockService;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.axelor.apps.purchase.db.repo.PurchaseConfigRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.time.LocalDate;

public class PurchaseOrderServiceSupplychainSpecifiqueImpl
    extends PurchaseOrderServiceProductionImpl {
  @Inject
  public PurchaseOrderServiceSupplychainSpecifiqueImpl(
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
      TradingName tradingName)
      throws AxelorException {
    PurchaseOrder purchaseOrder =
        super.createPurchaseOrder(
            buyerUser,
            company,
            contactPartner,
            currency,
            deliveryDate,
            internalReference,
            externalReference,
            stockLocation,
            orderDate,
            priceList,
            supplierPartner,
            tradingName);
    purchaseOrder.setOrderBeingEdited(true);
    //begin MA1-I53 karl
    PurchaseConfigRepository purchaseConfigRepo = Beans.get(PurchaseConfigRepository.class);
    Boolean displayPriceOnQuotationRequest = 
        purchaseConfigRepo
        .all()
        .filter("self.company = ?",purchaseOrder.getCompany())
        .fetchOne().getDisplayPriceOnQuotationRequest();
    purchaseOrder.setDisplayPriceOnQuotationRequest(displayPriceOnQuotationRequest);
    // end MA1-I53
    return purchaseOrder;
  }
}
