package bzh.toolapp.apps.specifique.web;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import bzh.toolapp.apps.specifique.service.SpecifiqueService;

public class PlanificationController {

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public void prepared(ActionRequest request, ActionResponse response) throws AxelorException {

		StockMove sm = request.getContext().asType(StockMove.class);

		this.logger.debug("Num StockMove est {}", sm.getId());

		if (sm.getId() != null) {
			Beans.get(SpecifiqueService.class).prepared(Beans.get(StockMoveRepository.class).find(sm.getId()));
		}
		response.setReload(true);
	}

	public void getCustCommandeClient(ActionRequest request, ActionResponse response) throws AxelorException {
		StockMove sm = request.getContext().asType(StockMove.class);
		StockMoveLine sml = request.getContext().asType(StockMoveLine.class);

		if (sm.getId() != null) {
			Beans.get(SpecifiqueService.class).getCommandeClient(sm, sml);
		}

		response.setReload(true);
	}
}
