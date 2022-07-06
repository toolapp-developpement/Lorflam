package bzh.toolapp.apps.specifique.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceLineRepository;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.invoice.generator.InvoiceLineGenerator;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.supplychain.exception.IExceptionMessage;
import com.axelor.apps.supplychain.service.PurchaseOrderInvoiceService;
import com.axelor.apps.supplychain.service.SaleOrderInvoiceService;
import com.axelor.apps.supplychain.service.StockMoveInvoiceServiceImpl;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychain;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.apps.supplychain.service.config.SupplyChainConfigService;
import com.axelor.apps.supplychain.service.invoice.generator.InvoiceLineGeneratorSupplyChain;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;

public class StockMoveInvoiceSpecifiqueServiceImpl extends StockMoveInvoiceServiceImpl {
	private SaleOrderInvoiceService saleOrderInvoiceService;
	private PurchaseOrderInvoiceService purchaseOrderInvoiceService;
	private StockMoveLineServiceSupplychain stockMoveLineServiceSupplychain;
	private InvoiceRepository invoiceRepository;
	private SaleOrderRepository saleOrderRepo;
	private PurchaseOrderRepository purchaseOrderRepo;
	private StockMoveLineRepository stockMoveLineRepository;
	private InvoiceLineRepository invoiceLineRepository;
	private SupplyChainConfigService supplyChainConfigService;
	private AppSupplychainService appSupplychainService;

	public StockMoveInvoiceSpecifiqueServiceImpl(SaleOrderInvoiceService saleOrderInvoiceService,
			PurchaseOrderInvoiceService purchaseOrderInvoiceService,
			StockMoveLineServiceSupplychain stockMoveLineServiceSupplychain, InvoiceRepository invoiceRepository,
			SaleOrderRepository saleOrderRepo, PurchaseOrderRepository purchaseOrderRepo,
			StockMoveLineRepository stockMoveLineRepository, InvoiceLineRepository invoiceLineRepository,
			SupplyChainConfigService supplyChainConfigService, AppSupplychainService appSupplychainService) {
		super(saleOrderInvoiceService, purchaseOrderInvoiceService, stockMoveLineServiceSupplychain, invoiceRepository,
				saleOrderRepo, purchaseOrderRepo, stockMoveLineRepository, invoiceLineRepository,
				supplyChainConfigService, appSupplychainService);
		this.saleOrderInvoiceService = saleOrderInvoiceService;
		this.purchaseOrderInvoiceService = purchaseOrderInvoiceService;
		this.stockMoveLineServiceSupplychain = stockMoveLineServiceSupplychain;
		this.invoiceRepository = invoiceRepository;
		this.saleOrderRepo = saleOrderRepo;
		this.purchaseOrderRepo = purchaseOrderRepo;
		this.stockMoveLineRepository = stockMoveLineRepository;
		this.invoiceLineRepository = invoiceLineRepository;
		this.supplyChainConfigService = supplyChainConfigService;
		this.appSupplychainService = appSupplychainService;
	}

	@Override
	public InvoiceLine createInvoiceLine(Invoice invoice, StockMoveLine stockMoveLine, BigDecimal qty)
			throws AxelorException {

		Product product = stockMoveLine.getProduct();
		boolean isTitleLine = false;

		int sequence = InvoiceLineGenerator.DEFAULT_SEQUENCE;
		SaleOrderLine saleOrderLine = stockMoveLine.getSaleOrderLine();
		PurchaseOrderLine purchaseOrderLine = stockMoveLine.getPurchaseOrderLine();

		if (saleOrderLine != null) {
			sequence = saleOrderLine.getSequence();
		} else if (purchaseOrderLine != null) {
			if (purchaseOrderLine.getIsTitleLine()) {
				isTitleLine = true;
			}
			sequence = purchaseOrderLine.getSequence();
		}

		// do not create lines with no qties
		if ((qty == null || qty.signum() == 0 || stockMoveLine.getRealQty().signum() == 0) && !isTitleLine) {
			return null;
		}
		if (product == null && !isTitleLine) {
			throw new AxelorException(TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
					I18n.get(IExceptionMessage.STOCK_MOVE_INVOICE_1), stockMoveLine.getStockMove().getStockMoveSeq());
		}

		InvoiceLineGenerator invoiceLineGenerator = new InvoiceLineGeneratorSupplyChain(invoice, product,
				stockMoveLine.getProductName(), stockMoveLine.getDescription(), qty, stockMoveLine.getUnit(), sequence,
				false, stockMoveLine.getSaleOrderLine(), stockMoveLine.getPurchaseOrderLine(), stockMoveLine) {
			@Override
			public List<InvoiceLine> creates() throws AxelorException {

				InvoiceLine invoiceLine = this.createInvoiceLine();

				invoiceLine.setYard(stockMoveLine.getYard());

				List<InvoiceLine> invoiceLines = new ArrayList<>();
				invoiceLines.add(invoiceLine);

				return invoiceLines;
			}
		};

		List<InvoiceLine> invoiceLines = invoiceLineGenerator.creates();
		InvoiceLine invoiceLine = null;
		if (invoiceLines != null && !invoiceLines.isEmpty()) {
			invoiceLine = invoiceLines.get(0);
			if (!stockMoveLine.getIsMergedStockMoveLine()) {
				// not a consolidated line so we can set the reference.
				invoiceLine.setStockMoveLine(stockMoveLine);
			} else {
				// set the reference to a correct stock move line by following either the sale
				// order line or
				// purchase order line. We cannot have a consolidated line without purchase
				// order line or
				// sale order line reference
				StockMoveLine nonConsolidatedStockMoveLine = null;
				StockMove stockMove = stockMoveLine.getStockMove();
				if (saleOrderLine != null) {
					nonConsolidatedStockMoveLine = stockMoveLineRepository.all()
							.filter("self.saleOrderLine.id = :saleOrderLineId "
									+ "AND self.stockMove.id = :stockMoveId " + "AND self.id != :stockMoveLineId")
							.bind("saleOrderLineId", saleOrderLine.getId()).bind("stockMoveId", stockMove.getId())
							.bind("stockMoveLineId", stockMoveLine.getId()).order("id").fetchOne();
				} else if (purchaseOrderLine != null) {
					nonConsolidatedStockMoveLine = stockMoveLineRepository.all()
							.filter("self.purchaseOrderLine.id = :purchaseOrderLineId "
									+ "AND self.stockMove.id = :stockMoveId " + "AND self.id != :stockMoveLineId")
							.bind("purchaseOrderLineId", purchaseOrderLine.getId())
							.bind("stockMoveId", stockMove.getId()).bind("stockMoveLineId", stockMoveLine.getId())
							.order("id").fetchOne();
				}
				invoiceLine.setStockMoveLine(nonConsolidatedStockMoveLine);
				deleteConsolidatedStockMoveLine(stockMoveLine);
			}
		}
		return invoiceLine;
	}

}
