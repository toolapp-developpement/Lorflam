package com.avr.apps.helpdesk.repository;

import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.supplychain.db.repo.StockMoveLineSupplychainRepository;
import java.util.Map;

public class StockMoveLineAvrHelpdeskRepository extends StockMoveLineSupplychainRepository {

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    // Recuperation du champ de l ecran
    Long id = (Long) json.get("id");
    String saleOrderSeq = new String();
    StockMoveLine sml = null;
    if (id != null) {
      sml = find(id);
      if (sml.getSaleOrderLine() != null && sml.getSaleOrderLine().getSaleOrder() != null) {
        saleOrderSeq = sml.getSaleOrderLine().getSaleOrder().getSaleOrderSeq();
      } else if (sml.getProducedManufOrder() != null
          && sml.getProducedManufOrder().getSaleOrderSet() != null) {
        if (!sml.getProducedManufOrder().getSaleOrderSet().isEmpty()) {
          SaleOrder so = sml.getProducedManufOrder().getSaleOrderSet().iterator().next();
          saleOrderSeq = so.getSaleOrderSeq();
        }
      } else if (sml.getConsumedManufOrder() != null
          && sml.getConsumedManufOrder().getSaleOrderSet() != null) {
        if (!sml.getConsumedManufOrder().getSaleOrderSet().isEmpty()) {
          SaleOrder so = sml.getConsumedManufOrder().getSaleOrderSet().iterator().next();
          saleOrderSeq = so.getSaleOrderSeq();
        }
      }
      json.put("custSaleOrderSeq", saleOrderSeq);
    }

    return super.populate(json, context);
  }
}
