package bzh.toolapp.apps.specifique.modules;

import bzh.toolapp.apps.specifique.repository.StockMoveLineSpecificRepository;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import bzh.toolapp.apps.specifique.service.StockMoveLineSpecifiqueCreationService;
import bzh.toolapp.apps.specifique.service.impl.*;
import bzh.toolapp.apps.specifique.service.impl.ProductionOrderSaleOrderSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.PurchaseOrderStockSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.SaleOrderStockSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.SpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.StockMoveLineSpecifiqueCreationServiceImpl;
import com.avr.apps.helpdesk.service.impl.PurchaseOrderCreateStockServiceImpl;
import com.avr.apps.helpdesk.service.impl.PurchaseOrderCreateSupplychainServiceImpl;
import com.avr.apps.helpdesk.service.impl.SaleOrderCreateStockMoveServiceImpl;
import com.axelor.app.AxelorModule;
import com.axelor.apps.businessproduction.service.ProductionOrderSaleOrderServiceBusinessImpl;
import com.axelor.apps.marketing.service.TemplateMessageServiceMarketingImpl;
import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;
import com.axelor.apps.production.service.PurchaseOrderServiceProductionImpl;
import com.axelor.apps.stock.service.InventoryService;
import com.axelor.apps.supplychain.service.SaleOrderServiceSupplychainImpl;

public class SpecificModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(StockMoveLineProductionRepository.class).to(StockMoveLineSpecificRepository.class);
    bind(StockMoveLineSpecifiqueCreationService.class)
        .to(StockMoveLineSpecifiqueCreationServiceImpl.class);
    bind(SaleOrderCreateStockMoveServiceImpl.class).to(SaleOrderStockSpecifiqueServiceImpl.class);
    bind(PurchaseOrderCreateStockServiceImpl.class)
        .to(PurchaseOrderStockSpecifiqueServiceImpl.class);
    bind(SpecifiqueService.class).to(SpecifiqueServiceImpl.class);
    bind(ProductionOrderSaleOrderServiceBusinessImpl.class)
        .to(ProductionOrderSaleOrderSpecifiqueServiceImpl.class);
    bind(PurchaseOrderCreateSupplychainServiceImpl.class)
        .to(PurchaseOrderCreateSpecifiqueServiceImpl.class);
    bind(PurchaseOrderServiceProductionImpl.class)
        .to(PurchaseOrderServiceSupplychainSpecifiqueImpl.class);
    bind(SaleOrderServiceSupplychainImpl.class).to(SaleOrderServiceSpecifiqueImpl.class);
    bind(InventoryService.class).to(InventoryServiceSpecifiqueImpl.class);
    bind(TemplateMessageServiceMarketingImpl.class)
        .to(TemplateMessageSpecifiqueServiceBaseImpl.class);
  }
}
