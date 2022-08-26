package bzh.toolapp.apps.specifique.web;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.production.db.TempBomTree;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import bzh.toolapp.apps.specifique.service.etatstock.BillOfMaterialServiceSpecifique;

public class ProductController {
	public void openProductTree(ActionRequest request, ActionResponse response) throws AxelorException {

		TempBomTree bomTree = request.getContext().asType(TempBomTree.class);
		Product product = bomTree.getProduct();
		Company company = bomTree.getCompany();
		StockLocation stockLocation = bomTree.getStockLocation();

		TempBomTree tempBomTree = Beans.get(BillOfMaterialServiceSpecifique.class)
				.generateTree(product.getDefaultBillOfMaterial(), true, stockLocation.getId(), company.getId());

		response.setView(ActionView.define(I18n.get("Bill of materials")).model(TempBomTree.class.getName())
				.add("tree", "bom-tree-detail").context("_tempBomTreeId", tempBomTree.getId()).map());
	}
}
