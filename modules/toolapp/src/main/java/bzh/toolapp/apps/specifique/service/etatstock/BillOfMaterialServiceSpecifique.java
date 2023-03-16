package bzh.toolapp.apps.specifique.service.etatstock;

import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.TempBomTree;
import com.google.inject.persist.Transactional;

public interface BillOfMaterialServiceSpecifique {
  @Transactional(rollbackOn = {Exception.class})
  public TempBomTree generateTree(
      BillOfMaterial billOfMaterial,
      boolean useProductDefaultBom,
      Long stockLocationId,
      Long companyId);
}