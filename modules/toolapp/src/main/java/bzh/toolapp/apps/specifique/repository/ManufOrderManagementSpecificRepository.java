package bzh.toolapp.apps.specifique.repository;

import com.axelor.apps.businessproduction.db.repo.ManufOrderBusinessProductionManagementRepository;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import java.math.BigDecimal;
import java.util.Map;

public class ManufOrderManagementSpecificRepository
    extends ManufOrderBusinessProductionManagementRepository {

  // MA1-I43 - Karl - begin
  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    Long manufOrderId = (Long) json.get("id");
    ManufOrder manufOrder = find(manufOrderId);
    json.put("qtyToProduce", getQtyToProduce(manufOrder));
    json.put("qtyProduced", getQtyProduced(manufOrder));
    return super.populate(json, context);
  }
  // get qty to produce
  private BigDecimal getQtyToProduce(ManufOrder manufOrder) {
    BigDecimal qtyToProduce = BigDecimal.ZERO;
    if (manufOrder.getProducedStockMoveLineList() != null) {
      for (StockMoveLine stockMoveLine : manufOrder.getProducedStockMoveLineList()) {
        if (stockMoveLine.getStockMove().getStatusSelect() != StockMoveRepository.STATUS_REALIZED && stockMoveLine.getRealQty()!=null) {
          qtyToProduce = qtyToProduce.add(stockMoveLine.getRealQty());
        }
      }
    }
    return qtyToProduce;
  }

  // get qty produced
  private BigDecimal getQtyProduced(ManufOrder manufOrder) {
    BigDecimal qtyProduced = BigDecimal.ZERO;
    if (manufOrder.getProducedStockMoveLineList() != null) {
      for (StockMoveLine stockMoveLine : manufOrder.getProducedStockMoveLineList()) {
        if (stockMoveLine.getStockMove().getStatusSelect() == StockMoveRepository.STATUS_REALIZED && stockMoveLine.getRealQty()!=null) {
          qtyProduced = qtyProduced.add(stockMoveLine.getRealQty());
        }
      }
    }
    return qtyProduced;
  }
  // MA1-I43 - Karl - end
}
