package bzh.toolapp.apps.specifique.service.impl;

import bzh.toolapp.apps.specifique.exception.IExceptionSpecifiqueMessage;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import com.avr.apps.helpdesk.db.Yard;
import com.avr.apps.helpdesk.db.repo.YardRepository;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecifiqueServiceImpl implements SpecifiqueService {

  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected StockMoveRepository stockMoveRepository;
  protected final SpecifiqueService specifiqueService;
  protected YardRepository yardRepository;

  @Inject
  public SpecifiqueServiceImpl(StockMoveRepository smr, SpecifiqueService sp, YardRepository yr) {
    this.stockMoveRepository = smr;
    this.specifiqueService = sp;
    this.yardRepository = yr;
  }

  @Transactional
  @Override
  public Boolean prepared(StockMove stockMove) throws AxelorException {
    stockMove.setStatusSelect(5);
    stockMoveRepository.save(stockMove);
    Boolean retour = true;
    return retour;
  }

  @Transactional
  @Override
  public void selectOrCreateYard(String yardName) throws AxelorException {
    // Recherche de l'objet par son nom
    Yard yard = yardRepository.findByName(yardName);

    if (yard == null) {
      // S'il n'existe pas alors on le cree
      yard = new Yard(yardName);
      yard.setFullName(yardName);
      yard.setYardReference(yardName);
      yardRepository.save(yard);
    }
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public Boolean enableEditPurchaseOrder(PurchaseOrder purchaseOrder) throws AxelorException {
    if (purchaseOrder.getStatusSelect() == PurchaseOrderRepository.STATUS_FINISHED) {
      throw new AxelorException(
          purchaseOrder,
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(IExceptionSpecifiqueMessage.PURCHASE_ORDER_VALIDATED));
    }

    purchaseOrder.setOrderBeingEdited(true);
    return false;
  }

  @Override
  public Boolean validateChangesPurchaseOrder(PurchaseOrder purchaseOrder) throws AxelorException {
    purchaseOrder.setOrderBeingEdited(false);
    
    return null;
  }
}
