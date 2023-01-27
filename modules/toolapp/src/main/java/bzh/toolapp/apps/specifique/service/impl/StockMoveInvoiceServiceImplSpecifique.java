package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceLineRepository;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.generator.line.InvoiceLineManagement;
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
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;

public class StockMoveInvoiceServiceImplSpecifique extends ProjectStockMoveInvoiceServiceImpl {

  @Inject
  public StockMoveInvoiceServiceImplSpecifique(
      SaleOrderInvoiceService saleOrderInvoiceService,
      PurchaseOrderInvoiceService purchaseOrderInvoiceService,
      StockMoveLineServiceSupplychain stockMoveLineServiceSupplychain,
      InvoiceRepository invoiceRepository,
      SaleOrderRepository saleOrderRepo,
      PurchaseOrderRepository purchaseOrderRepo,
      StockMoveLineRepository stockMoveLineRepository,
      InvoiceLineRepository invoiceLineRepository,
      SupplyChainConfigService supplyChainConfigService,
      AppSupplychainService appSupplychainService) {
    super(
        saleOrderInvoiceService,
        purchaseOrderInvoiceService,
        stockMoveLineServiceSupplychain,
        invoiceRepository,
        saleOrderRepo,
        purchaseOrderRepo,
        stockMoveLineRepository,
        invoiceLineRepository,
        supplyChainConfigService,
        appSupplychainService);
  }

  @Override
  public InvoiceLine createInvoiceLine(Invoice invoice, StockMoveLine stockMoveLine, BigDecimal qty)
      throws AxelorException {
    InvoiceLine invoiceLine = super.createInvoiceLine(invoice, stockMoveLine, qty);

    if(stockMoveLine.getStockMove().getOriginId() != 0)
        return invoiceLine;

    if (stockMoveLine.getDiscountAmount() != null)
      invoiceLine.setDiscountAmount(stockMoveLine.getDiscountAmount());
    if (stockMoveLine.getDiscountTypeSelect() != null)
      invoiceLine.setDiscountTypeSelect(stockMoveLine.getDiscountTypeSelect());
    if (stockMoveLine.getPriceDiscounted() != null)
      invoiceLine.setPriceDiscounted(stockMoveLine.getPriceDiscounted());

    InvoiceLineService invoiceLineService = Beans.get(InvoiceLineService.class);
    invoiceLine.setPriceDiscounted(invoiceLineService.computeDiscount(invoiceLine, false));

    BigDecimal taxRate = BigDecimal.ZERO;
    if (invoiceLine.getTaxLine() != null) {
      taxRate = invoiceLine.getTaxLine().getValue();
    }

    BigDecimal exTaxTotal =
        InvoiceLineManagement.computeAmount(invoiceLine.getQty(), invoiceLine.getPriceDiscounted());
    BigDecimal inTaxTotal = exTaxTotal.add(exTaxTotal.multiply(taxRate));
    invoiceLine.setExTaxTotal(exTaxTotal);
    invoiceLine.setInTaxTotal(inTaxTotal);
    return invoiceLine;
  }
}
