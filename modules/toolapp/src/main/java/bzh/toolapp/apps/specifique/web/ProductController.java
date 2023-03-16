package bzh.toolapp.apps.specifique.web;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.production.db.TempBomTree;
import com.axelor.apps.stock.db.StockLocation;

// import bzh.toolapp.apps.specifique.service.etatstock.BillOfMaterialServiceSpecifique;

import com.axelor.apps.supplychain.db.MrpLine;
import com.axelor.apps.supplychain.service.ProjectedStockService;
import com.axelor.apps.toolapp.db.MrpLineCustom;
import com.axelor.db.mapper.Mapper;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import bzh.toolapp.apps.specifique.service.etatstock.BillOfMaterialServiceSpecifique;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductController {
  
  public void openProductTree(ActionRequest request, ActionResponse response)
      throws AxelorException {

    TempBomTree bomTree = request.getContext().asType(TempBomTree.class);
    Product product = bomTree.getProduct();
    Company company = bomTree.getCompany();
    StockLocation stockLocation = bomTree.getStockLocation();

    TempBomTree tempBomTree =
        Beans.get(BillOfMaterialServiceSpecifique.class) 
            .generateTree(
                product.getDefaultBillOfMaterial(), true, stockLocation.getId(), company.getId());

    response.setView(
        ActionView.define(I18n.get("Bill of materials"))
            .model(TempBomTree.class.getName())
            .add("tree", "bom-tree-detail")
            .context("_tempBomTreeId", tempBomTree.getId())
            .map());
  }

  public void showProjectedStock(ActionRequest request, ActionResponse response) {

    try {
      ProjectedStockService projectedStockService = Beans.get(ProjectedStockService.class);

      MrpLineCustom mrpLine = request.getContext().asType(MrpLineCustom.class);

      Map<String, Long> mapId = new HashMap<>();

      mapId.put("productId", mrpLine.getProduct().getId());
      mapId.put("companyId", mrpLine.getStockLocation().getCompany().getId());
      mapId.put("stockLocationId", mrpLine.getStockLocation().getId());

      final List<MrpLine> mrpLineList = new ArrayList<>();
      try {
        mrpLineList.addAll(
            projectedStockService.createProjectedStock(
                mapId.get("productId"), mapId.get("companyId"), mapId.get("stockLocationId")));
        response.setView(
            ActionView.define(I18n.get("Projected stock"))
                .model(MrpLine.class.getName())
                .add("form", "projected-stock-form-custome")
                .param("popup", "true")
                .param("popup-save", "false")
                .param("popup.maximized", "true")
                .context("_mrpLineList", mrpLineList)
                .map());
      } catch (Exception e) {
        TraceBackService.trace(response, e);
      } finally {
        projectedStockService.removeMrpAndMrpLine(mrpLineList);
      }
    } catch (Exception e) {
      //      TraceBackService.trace(response, e);
    }
  }

  public void showCustomeChartProjectedStock(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    List<Map<String, Object>> dataList = new ArrayList<>();

    @SuppressWarnings("unchecked")
    Collection<Map<String, Object>> contextMrpLineList =
        (Collection<Map<String, Object>>) context.get("_mrpLineListToProject");

    List<MrpLine> mrpLineList =
        contextMrpLineList.stream()
            .map(map -> Mapper.toBean(MrpLine.class, map))
            .collect(Collectors.toList());

    if (!mrpLineList.isEmpty()) {
      List<MrpLine> mrpLineLastList = new ArrayList<>();
      MrpLine lastMrpLine = mrpLineList.get(0);

      for (int i = 1; i < mrpLineList.size(); ++i) {
        MrpLine mrpLine = mrpLineList.get(i);
        if (mrpLine.getMaturityDate().isAfter(lastMrpLine.getMaturityDate())) {
          mrpLineLastList.add(lastMrpLine);
        }
        lastMrpLine = mrpLine;
      }
      mrpLineLastList.add(lastMrpLine);
      lastMrpLine = mrpLineList.get(0);
      LocalDate mrpDate = lastMrpLine.getMaturityDate();
      for (MrpLine mrpLine : mrpLineLastList) {
        mrpDate = addInterElementForProjectedStockChart(dataList, lastMrpLine, mrpDate, mrpLine);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("name", mrpLine.getMaturityDate());
        dataMap.put("cumulativeQty", mrpLine.getCumulativeQty());
        dataList.add(dataMap);
        lastMrpLine = mrpLine;
      }
    }
    response.setData(dataList);
  }

  private LocalDate addInterElementForProjectedStockChart(
      List<Map<String, Object>> dataList, MrpLine lastMrpLine, LocalDate mrpDate, MrpLine mrpLine) {
    while (mrpDate.isBefore(mrpLine.getMaturityDate())) {
      mrpDate = mrpDate.plusDays(1);
      Map<String, Object> dataMapDate = new HashMap<>();
      dataMapDate.put("name", mrpDate);
      dataMapDate.put("cumulativeQty", lastMrpLine.getCumulativeQty());
      dataList.add(dataMapDate);
    }
    return mrpDate;
  }
}
