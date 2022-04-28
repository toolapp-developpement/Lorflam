package bzh.toolapp.apps.specifique.service;

import java.math.BigDecimal;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.TrackingNumber;
import com.axelor.exception.AxelorException;

public interface StockMoveLineSpecifiqueCreationService {

	public StockMoveLine createStockMoveLine(Product product, String productName, String description,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal companyUnitPriceUntaxed, Unit unit,
			StockMove stockMove, int type, boolean taxed, BigDecimal taxRate, String yard) throws AxelorException;

	public StockMoveLine createStockMoveLine(Product product, String productName, String description,
			BigDecimal quantity, BigDecimal unitPriceUntaxed, BigDecimal unitPriceTaxed,
			BigDecimal companyUnitPriceUntaxed, BigDecimal companyPurchasePrice, Unit unit, StockMove stockMove,
			TrackingNumber trackingNumber, String yard) throws AxelorException;

	public StockMoveLine createStockMoveLine(Product product, String productName, String description,
			BigDecimal quantity, BigDecimal requestedReservedQty, BigDecimal unitPrice,
			BigDecimal companyUnitPriceUntaxed, BigDecimal purchasePrice, Unit unit, StockMove stockMove, int type,
			boolean taxed, BigDecimal taxRate, SaleOrderLine saleOrderLine, PurchaseOrderLine purchaseOrderLine)
			throws AxelorException;

}
