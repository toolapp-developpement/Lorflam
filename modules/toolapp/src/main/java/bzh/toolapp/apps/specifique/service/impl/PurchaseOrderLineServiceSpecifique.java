package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.businessproject.service.PurchaseOrderLineServiceProjectImpl;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.exception.AxelorException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PurchaseOrderLineServiceSpecifique extends PurchaseOrderLineServiceProjectImpl {
  private Map<Long, Integer> sequenceMap = new HashMap<Long, Integer>();

  @Override
  public PurchaseOrderLine createPurchaseOrderLine(
      PurchaseOrder purchaseOrder,
      Product product,
      String productName,
      String description,
      BigDecimal qty,
      Unit unit)
      throws AxelorException {
    PurchaseOrderLine purchaseOrderLine =
        super.createPurchaseOrderLine(purchaseOrder, product, productName, description, qty, unit);
    Integer sequence = sequenceMap.get(purchaseOrder.getId());
    if (sequence == null) {
      sequence = 1;
    }
    purchaseOrderLine.setSequence(sequence);

    sequenceMap.put(purchaseOrder.getId(), sequence + 1);
    purchaseOrderLine.setDesiredDelivDate(null);
    purchaseOrderLine.setEstimatedDelivDate(null);
    return purchaseOrderLine;
  }
}
