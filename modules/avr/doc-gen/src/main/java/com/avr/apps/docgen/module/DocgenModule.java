/**
 * *********************************** AVR SOLUTIONS * ***********************************
 *
 * @author David
 * @date 11/03/2021
 * @time 17:06
 * @version 1.0
 */
package com.avr.apps.docgen.module;

import com.avr.apps.docgen.service.*;
import com.avr.apps.docgen.service.interfaces.*;
import com.axelor.app.AxelorModule;
import com.axelor.apps.businessproduction.service.SaleOrderWorkflowServiceBusinessProductionImpl;
import com.axelor.apps.businessproject.service.SaleOrderInvoiceProjectServiceImpl;
import com.axelor.apps.cash.management.service.InvoiceServiceManagementImpl;
import com.axelor.apps.production.service.StockMoveProductionServiceImpl;
import com.axelor.apps.supplychain.service.PurchaseOrderStockServiceImpl;
import com.axelor.apps.supplychain.service.SaleOrderStockServiceImpl;

public class DocgenModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(SaleOrderWorkflowServiceBusinessProductionImpl.class)
        .to(SaleOrderWorkflowDocgenServiceImpl.class);
    bind(MetaFieldOverrideSerivce.class).to(MetaFieldOverrideSerivceImpl.class);
    bind(AppDocgenService.class).to(AppDocgenServiceImpl.class);
    bind(TypeDataService.class).to(TypeDataServiceImpl.class);
    bind(InvoiceServiceManagementImpl.class).to(InvoiceAvrDocgenServiceImpl.class);
    bind(EmailAvrBaseService.class).to(EmailAvrBaseServiceImpl.class);
    bind(SaleOrderAvrDocgenService.class).to(SaleOrderAvrDocgenServiceImpl.class);
    bind(PurchaseOrderAvrDocgenService.class).to(PurchaseOrderAvrDocgenServiceImpl.class);
    bind(SaleOrderStockServiceImpl.class).to(SaleOrderStockAvrDocgenServiceImpl.class);
    bind(PurchaseOrderStockServiceImpl.class)
        .to(PurchaseOrderStockAvrDocgenProductionServiceImpl.class);
    bind(StockMoveProductionServiceImpl.class).to(StockMoveAvrDocgenServiceImpl.class);
    bind(SaleOrderInvoiceProjectServiceImpl.class).to(SaleOrderInvoiceAvrDocgenServiceImpl.class);
    bind(DocgenTraitmentDataService.class).to(DocgenTraitmentDataServiceImpl.class);
  }
}
