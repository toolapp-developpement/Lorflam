package bzh.toolapp.apps.specifique.repository;

import com.axelor.apps.production.db.repo.StockMoveProductionRepository;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.inject.Beans;

public class StockMoveSpecificRepository extends StockMoveProductionRepository {

  @Override
  public StockMove save(StockMove entity) {
    SaleOrderRepository saleOrderRepo = Beans.get(SaleOrderRepository.class);
    String tmp = entity.getOriginTypeSelect();
    if (entity.getOriginTypeSelect() != null
        && StockMoveRepository.ORIGIN_SALE_ORDER.equals(entity.getOriginTypeSelect())) {
      SaleOrder saleOrder = saleOrderRepo.find(entity.getOriginId());
      if (saleOrder.getTicket() != null) 
      {        
        entity.setTicket(saleOrder.getTicket());
      }
    }

    return super.save(entity);
  }
}
