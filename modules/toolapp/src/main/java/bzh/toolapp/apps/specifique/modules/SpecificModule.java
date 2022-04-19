package bzh.toolapp.apps.specifique.modules;

import com.axelor.app.AxelorModule;
import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;

import bzh.toolapp.apps.specifique.repository.StockMoveLineSpecificRepository;

public class SpecificModule extends AxelorModule {

	@Override
	protected void configure() {
		bind(StockMoveLineProductionRepository.class).to(StockMoveLineSpecificRepository.class);
	}

}
