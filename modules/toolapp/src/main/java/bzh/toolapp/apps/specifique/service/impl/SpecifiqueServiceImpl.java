package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

import bzh.toolapp.apps.specifique.service.SpecifiqueService;

public class SpecifiqueServiceImpl implements SpecifiqueService {
	protected StockMoveRepository stockMoveRepository;

	@Inject
	public SpecifiqueServiceImpl(StockMoveRepository smr) {
		this.stockMoveRepository = smr;
	}

	@Override
	public void prepared(StockMove stockMove) throws AxelorException {
		stockMove.setStatusSelect(5);
		stockMoveRepository.save(stockMove);
	}

	@Override
	public void getCommandeClient(StockMove sm, StockMoveLine sml) throws AxelorException {

		String saleOrderSeq = new String();

		if (sml.getSaleOrderLine() != null && sml.getSaleOrderLine().getSaleOrder() != null) {
			saleOrderSeq = sml.getSaleOrderLine().getSaleOrder().getSaleOrderSeq();
		} else if (sml.getProducedManufOrder() != null && sml.getProducedManufOrder().getSaleOrderSet() != null) {
			if (!sml.getProducedManufOrder().getSaleOrderSet().isEmpty()) {
				SaleOrder so = sml.getProducedManufOrder().getSaleOrderSet().iterator().next();
				saleOrderSeq = so.getSaleOrderSeq();
			}
		} else if (sml.getConsumedManufOrder() != null && sml.getConsumedManufOrder().getSaleOrderSet() != null) {
			if (!sml.getConsumedManufOrder().getSaleOrderSet().isEmpty()) {
				SaleOrder so = sml.getConsumedManufOrder().getSaleOrderSet().iterator().next();
				saleOrderSeq = so.getSaleOrderSeq();
			}
		}

		if (saleOrderSeq != null) {
			sm.setCustSaleOrderSeq(saleOrderSeq);
			stockMoveRepository.save(sm);
		}

	}

}
