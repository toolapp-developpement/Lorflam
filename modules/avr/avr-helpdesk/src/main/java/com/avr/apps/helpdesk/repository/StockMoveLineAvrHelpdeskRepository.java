package com.avr.apps.helpdesk.repository;

import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.supplychain.db.repo.StockMoveLineSupplychainRepository;

import java.util.Iterator;
import java.util.Map;

import org.apache.jasper.tagplugins.jstl.core.ForEach;

public class StockMoveLineAvrHelpdeskRepository extends StockMoveLineSupplychainRepository {

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    // R�cup�ration du champ de l'�cran
    		Long id = (Long)json.get(id);
    		String saleOrderSeq = new String();
    		if(id != null) {
    			StockMoveLine sml = find(id);
       			if (sml.getSaleOrderLine() != null && sml.getSaleOrderLine().getSaleOrder() != null){
    				saleOrderSeq = sml.getSaleOrderLine().getSaleOrder().getSaleOrderSeq();
    			}else if (sml.getProducedManufOrder() != null && sml.getProducedManufOrder().getSaleOrderSet() != null) {
    				if (!sml.getProducedManufOrder().getSaleOrderSet().isEmpty()) {
    					SaleOrder so = sml.getProducedManufOrder().getSaleOrderSet().iterator().next(); 
    					saleOrderSeq = so.getSaleOrderSeq();
    				}
    			}else if (sml.getConsumedManufOrder() != null && sml.getConsumedManufOrder().getSaleOrderSet() != null) {
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
