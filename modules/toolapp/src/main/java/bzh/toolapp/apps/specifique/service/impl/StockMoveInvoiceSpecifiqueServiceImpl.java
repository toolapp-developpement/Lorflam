package bzh.toolapp.apps.specifique.service.impl;

import java.math.BigDecimal;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceLineRepository;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.businessproject.service.ProjectStockMoveInvoiceServiceImpl;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.supplychain.service.PurchaseOrderInvoiceService;
import com.axelor.apps.supplychain.service.SaleOrderInvoiceService;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychain;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.apps.supplychain.service.config.SupplyChainConfigService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

public class StockMoveInvoiceSpecifiqueServiceImpl extends ProjectStockMoveInvoiceServiceImpl {
	@Inject
	public StockMoveInvoiceSpecifiqueServiceImpl(SaleOrderInvoiceService saleOrderInvoiceService,
			PurchaseOrderInvoiceService purchaseOrderInvoiceService,
			StockMoveLineServiceSupplychain stockMoveLineServiceSupplychain, InvoiceRepository invoiceRepository,
			SaleOrderRepository saleOrderRepo, PurchaseOrderRepository purchaseOrderRepo,
			StockMoveLineRepository stockMoveLineRepository, InvoiceLineRepository invoiceLineRepository,
			SupplyChainConfigService supplyChainConfigService, AppSupplychainService appSupplychainService) {
		super(saleOrderInvoiceService, purchaseOrderInvoiceService, stockMoveLineServiceSupplychain, invoiceRepository,
				saleOrderRepo, purchaseOrderRepo, stockMoveLineRepository, invoiceLineRepository,
				supplyChainConfigService, appSupplychainService);
	}

	@Override
	public InvoiceLine createInvoiceLine(Invoice invoice, StockMoveLine stockMoveLine, BigDecimal qty)
			throws AxelorException {

		InvoiceLine invoiceLine = super.createInvoiceLine(invoice, stockMoveLine, qty);

		invoiceLine.setYard(stockMoveLine.getYard());

		return invoiceLine;
	}
}
