package bzh.toolapp.apps.specifique.web;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.exception.IExceptionMessage;
import com.axelor.apps.stock.service.StockMoveService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import bzh.toolapp.apps.specifique.repository.StockMoveLineSpecificRepository;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;

public class PlanificationController {

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public void prepared(ActionRequest request, ActionResponse response) throws AxelorException {

		StockMove sm = request.getContext().asType(StockMove.class);

		Boolean statusUpdated = false;

		this.logger.debug("Num StockMove est {}", sm.getId());

		if (sm.getId() != null) {
			statusUpdated = Beans.get(SpecifiqueService.class)
					.prepared(Beans.get(StockMoveRepository.class).find(sm.getId()));
		}
		if (statusUpdated) {
			response.setReload(true);
		}
	}

	public void realize(ActionRequest request, ActionResponse response) {

		try {
			StockMove stockMoveFromRequest = request.getContext().asType(StockMove.class);

			StockMove stockMove = Beans.get(StockMoveRepository.class).find(stockMoveFromRequest.getId());
			// we have to inject TraceBackService to use non static methods
			TraceBackService traceBackService = Beans.get(TraceBackService.class);
			long tracebackCount = traceBackService.countMessageTraceBack(stockMove);
			if (stockMove.getStatusSelect() == null
					|| stockMove.getStatusSelect() != StockMoveRepository.STATUS_PLANNED) {
				if (stockMove.getStatusSelect() != StockMoveLineSpecificRepository.STATUS_PREPARED) {
					throw new AxelorException(TraceBackRepository.CATEGORY_INCONSISTENCY,
							I18n.get(IExceptionMessage.STOCK_MOVE_REALIZATION_WRONG_STATUS));
				}
			}
			String newSeq = Beans.get(StockMoveService.class).realize(stockMove);

			response.setReload(true);

			if (newSeq != null) {
				if (stockMove.getTypeSelect() == StockMoveRepository.TYPE_INCOMING) {
					response.setFlash(
							String.format(I18n.get(IExceptionMessage.STOCK_MOVE_INCOMING_PARTIAL_GENERATED), newSeq));
				} else if (stockMove.getTypeSelect() == StockMoveRepository.TYPE_OUTGOING) {
					response.setFlash(
							String.format(I18n.get(IExceptionMessage.STOCK_MOVE_OUTGOING_PARTIAL_GENERATED), newSeq));
				} else {
					response.setFlash(String.format(I18n.get(IExceptionMessage.STOCK_MOVE_9), newSeq));
				}
			}
			if (traceBackService.countMessageTraceBack(stockMove) > tracebackCount) {
				traceBackService.findLastMessageTraceBack(stockMove)
						.ifPresent(traceback -> response.setNotify(String.format(
								I18n.get(com.axelor.apps.message.exception.IExceptionMessage.SEND_EMAIL_EXCEPTION),
								traceback.getMessage())));
			}
		} catch (

		Exception e) {
			TraceBackService.trace(response, e);
		}
	}

	@CallMethod
	public void selectOrCreateYard(String yardName) throws AxelorException {

		if (yardName != "") {
			Beans.get(SpecifiqueService.class).selectOrCreateYard(yardName);
		}
	}

}
