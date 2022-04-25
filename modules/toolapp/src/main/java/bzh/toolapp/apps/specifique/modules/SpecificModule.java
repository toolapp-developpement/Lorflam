package bzh.toolapp.apps.specifique.modules;

import com.axelor.app.AxelorModule;
import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;

import bzh.toolapp.apps.specifique.repository.StockMoveLineSpecificRepository;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import bzh.toolapp.apps.specifique.service.impl.SpecifiqueServiceImpl;

public class SpecificModule extends AxelorModule {

	@Override
	protected void configure() {
		bind(StockMoveLineProductionRepository.class).to(StockMoveLineSpecificRepository.class);
		bind(SpecifiqueService.class).to(SpecifiqueServiceImpl.class);
	}

}
