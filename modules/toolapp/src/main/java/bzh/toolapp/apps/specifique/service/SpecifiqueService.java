package bzh.toolapp.apps.specifique.service;

import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.exception.AxelorException;

public interface SpecifiqueService {

  public Boolean prepared(StockMove stockMove) throws AxelorException;

  public void selectOrCreateYard(String yardName) throws AxelorException;

  public Boolean enableEditPurchaseOrder(PurchaseOrder purchaseOrder) throws AxelorException;

  public void validateChangesPurchaseOrder(PurchaseOrder purchaseOrder) throws AxelorException;
}
