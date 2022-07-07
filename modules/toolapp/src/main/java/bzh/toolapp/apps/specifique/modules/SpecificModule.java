package bzh.toolapp.apps.specifique.modules;

import com.avr.apps.helpdesk.service.impl.PurchaseOrderCreateStockServiceImpl;
import com.avr.apps.helpdesk.service.impl.SaleOrderCreateStockMoveServiceImpl;
import com.axelor.app.AxelorModule;
import com.axelor.apps.bankpayment.service.config.BankPaymentConfigService;
import com.axelor.apps.businessproduction.service.ProductionOrderSaleOrderServiceBusinessImpl;
import com.axelor.apps.businessproject.service.ProjectStockMoveInvoiceServiceImpl;
import com.axelor.apps.hr.service.bankorder.BankOrderServiceHRImpl;
import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;

import bzh.toolapp.apps.specifique.repository.StockMoveLineSpecificRepository;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import bzh.toolapp.apps.specifique.service.StockMoveLineSpecifiqueCreationService;
import bzh.toolapp.apps.specifique.service.bankorder.BankOrderServiceSpecifiqueImpl;
import bzh.toolapp.apps.specifique.service.bankorder.BankPaymentConfigSpecifiqueService;
import bzh.toolapp.apps.specifique.service.impl.ProductionOrderSaleOrderSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.PurchaseOrderStockSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.SaleOrderStockSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.SpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.StockMoveInvoiceSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.StockMoveLineSpecifiqueCreationServiceImpl;

public class SpecificModule extends AxelorModule {

	@Override
	protected void configure() {
		bind(StockMoveLineProductionRepository.class).to(StockMoveLineSpecificRepository.class);
		bind(StockMoveLineSpecifiqueCreationService.class).to(StockMoveLineSpecifiqueCreationServiceImpl.class);
		bind(SaleOrderCreateStockMoveServiceImpl.class).to(SaleOrderStockSpecifiqueServiceImpl.class);
		bind(PurchaseOrderCreateStockServiceImpl.class).to(PurchaseOrderStockSpecifiqueServiceImpl.class);
		bind(SpecifiqueService.class).to(SpecifiqueServiceImpl.class);
		bind(ProductionOrderSaleOrderServiceBusinessImpl.class).to(ProductionOrderSaleOrderSpecifiqueServiceImpl.class);
		bind(ProjectStockMoveInvoiceServiceImpl.class).to(StockMoveInvoiceSpecifiqueServiceImpl.class);
		bind(BankOrderServiceHRImpl.class).to(BankOrderServiceSpecifiqueImpl.class);
		bind(BankPaymentConfigService.class).to(BankPaymentConfigSpecifiqueService.class);
	}
}
