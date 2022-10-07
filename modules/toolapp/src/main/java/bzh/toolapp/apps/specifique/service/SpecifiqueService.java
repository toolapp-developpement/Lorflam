package bzh.toolapp.apps.specifique.service;

import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.exception.AxelorException;

public interface SpecifiqueService {

	public Boolean prepared(StockMove stockMove) throws AxelorException;

	public void getCommandeClient(StockMove sm, StockMoveLine sml) throws AxelorException;

	public void selectOrCreateYard(String yardName) throws AxelorException;
}
