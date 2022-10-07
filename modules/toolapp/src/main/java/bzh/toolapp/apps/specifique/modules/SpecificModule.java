package bzh.toolapp.apps.specifique.modules;

import com.avr.apps.helpdesk.service.impl.PurchaseOrderCreateStockServiceImpl;
import com.avr.apps.helpdesk.service.impl.SaleOrderCreateStockMoveServiceImpl;
import com.axelor.app.AxelorModule;
import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;

import bzh.toolapp.apps.specifique.repository.StockMoveLineSpecificRepository;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import bzh.toolapp.apps.specifique.service.StockMoveLineSpecifiqueCreationService;
import bzh.toolapp.apps.specifique.service.impl.PurchaseOrderStockSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.SaleOrderStockSpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.SpecifiqueServiceImpl;
import bzh.toolapp.apps.specifique.service.impl.StockMoveLineSpecifiqueCreationServiceImpl;

public class SpecificModule extends AxelorModule {

	@Override
	protected void configure() {
		bind(StockMoveLineProductionRepository.class).to(StockMoveLineSpecificRepository.class);
		bind(StockMoveLineSpecifiqueCreationService.class).to(StockMoveLineSpecifiqueCreationServiceImpl.class);
		bind(SaleOrderCreateStockMoveServiceImpl.class).to(SaleOrderStockSpecifiqueServiceImpl.class);
		bind(PurchaseOrderCreateStockServiceImpl.class).to(PurchaseOrderStockSpecifiqueServiceImpl.class);
		bind(SpecifiqueService.class).to(SpecifiqueServiceImpl.class);
	}
}
