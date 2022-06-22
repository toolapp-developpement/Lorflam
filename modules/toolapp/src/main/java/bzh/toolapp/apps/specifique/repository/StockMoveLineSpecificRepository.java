package bzh.toolapp.apps.specifique.repository;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMoveLine;
import com.google.inject.persist.Transactional;

public class StockMoveLineSpecificRepository extends StockMoveLineProductionRepository {
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Transactional
	@Override
	public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {

		logger.debug("<<<<<<< timmmyyyy >>>>>>>");

		// Recuperation du champ de lecran
		Long id = (Long) json.get("id");
		logger.debug(
				"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Num StockMove est {} >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>",
				id);

		String saleOrderSeq = new String();
		StockMoveLine sml = null;
		if (id != null) {
			sml = find(id);
			// if (sml.getCustSaleOrderSeq() == null) {

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

			sml.setCustSaleOrderSeq(saleOrderSeq);
			this.save(sml);

			logger.debug("L'origine est {}", saleOrderSeq);

			json.put("custSaleOrderSeq", saleOrderSeq);
			// }
		}

		return super.populate(json, context);
	}

	public static final int STATUS_PREPARED = 5;
}
