package bzh.toolapp.apps.specifique.service.impl;

import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import com.avr.apps.helpdesk.db.Yard;
import com.avr.apps.helpdesk.db.repo.YardRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.exception.AxelorException;
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
}
