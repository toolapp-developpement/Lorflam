package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.service.StockLocationLineService;
import com.axelor.apps.supplychain.service.StockLocationServiceSupplychainImpl;
import com.axelor.db.JPA;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import javax.persistence.Query;

public class StockLocationServiceSupplychainSpecifiqueImpl
    extends StockLocationServiceSupplychainImpl {

  @Inject
  public StockLocationServiceSupplychainSpecifiqueImpl(
      StockLocationRepository stockLocationRepo,
      StockLocationLineService stockLocationLineService,
      ProductRepository productRepo) {
    super(stockLocationRepo, stockLocationLineService, productRepo);
  }

  @Override
  public BigDecimal getStockLocationValue(StockLocation stockLocation) {

    Query query =
        JPA.em()
            .createQuery(
                "SELECT SUM( self.currentQty * CASE WHEN (location.company.stockConfig.stockValuationTypeSelect = 1) THEN "
                    + "(self.avgPrice)  WHEN (location.company.stockConfig.stockValuationTypeSelect = 2) THEN "
                    + "CASE WHEN (self.product.costTypeSelect = 3) THEN (self.avgPrice) ELSE (self.product.costPrice) END "
                    + "WHEN (location.company.stockConfig.stockValuationTypeSelect = 3) THEN (self.product.salePrice) "
                    + "WHEN (location.company.stockConfig.stockValuationTypeSelect = 4) THEN (self.product.purchasePrice) "
                    + "ELSE (self.avgPrice) END ) AS value "
                    + "FROM StockLocationLine AS self "
                    + "LEFT JOIN StockLocation AS location "
                    + "ON location.id= self.stockLocation "
                    //+ "WHERE self.stockLocation.id =:id "
                    // MA1-I50 - Karl - begin
                    + "WHERE self.stockLocation.id IN (:ids)"
                // MA1-I50 - Karl - end
                );
   // query.setParameter("id", stockLocation.getId()); 
    // MA1-I50 - Karl - begin
    query.setParameter("ids", getContentStockLocationIds(stockLocation));
    // MA1-I50 - Karl - end
    List<?> result = query.getResultList();
    return (result.get(0) == null || ((BigDecimal) result.get(0)).signum() == 0)
        ? BigDecimal.ZERO
        : ((BigDecimal) result.get(0)).setScale(2, BigDecimal.ROUND_HALF_UP);
  }
}
