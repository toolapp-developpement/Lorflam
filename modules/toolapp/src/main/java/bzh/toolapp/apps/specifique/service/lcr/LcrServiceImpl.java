package bzh.toolapp.apps.specifique.service.lcr;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.apps.bankpayment.db.BankOrderLine;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderCreateService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderLineService;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.db.Query;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.time.LocalDate;
import java.util.List;

public class LcrServiceImpl implements LcrService {

  protected BankOrderCreateService bankOrderCreateService;
  protected BankOrderLineService bankOrderLineService;
  protected BankOrderRepository bankOrderRepository;
  protected InvoiceRepository invoiceRepository;

  @Inject
  public LcrServiceImpl(
      BankOrderCreateService bankOrderCreateService,
      BankOrderLineService bankOrderLineService,
      BankOrderRepository bankOrderRepository,
      InvoiceRepository invoiceRepository) {
    this.bankOrderCreateService = bankOrderCreateService;
    this.bankOrderLineService = bankOrderLineService;
    this.bankOrderRepository = bankOrderRepository;
    this.invoiceRepository = invoiceRepository;
  }

  @Override
  public void generateLcrFile(LocalDate dueDate, Company company) throws AxelorException {
    String paymentModeCode = "ENC_VIR";
    String companyName = "Axelor";

    PaymentMode paymentMode =
        Query.of(PaymentMode.class).filter("self.code = ?1", paymentModeCode).fetchOne();
    Company senderCompany =
        Query.of(Company.class).filter("self.name = ?1", companyName).fetchOne();
    List<Invoice> invoiceList =
        invoiceRepository.all().filter("self.dueDate <= ?1", dueDate).fetch();
    LocalDate bankOrderDate = dueDate;
    BankDetails senderBankDetails = senderCompany.getDefaultBankDetails();
    Currency currency = senderCompany.getCurrency();

    int partnerType = BankOrderRepository.PARTNER_TYPE_CUSTOMER;
    String senderReference = "";
    String senderLabel = "";

    BankOrder bankOrder =
        bankOrderCreateService.createBankOrder(
            paymentMode,
            partnerType,
            bankOrderDate,
            senderCompany,
            senderBankDetails,
            currency,
            senderReference,
            senderLabel,
            BankOrderRepository.ORDER_TYPE_BILL_OF_EXCHANGE);

    for (Invoice invoice : invoiceList) {
      BankOrderLine bankOrderLine =
          bankOrderLineService.createBankOrderLine(
              paymentMode.getBankOrderFileFormat(),
              null,
              invoice.getPartner(),
              invoice.getBankDetails(),
              invoice.getAmountPaid(),
              invoice.getCurrency(),
              bankOrderDate,
              invoice.getId().toString(),
              "",
              invoice);
      bankOrder.addBankOrderLineListItem(bankOrderLine);
    }

    bankOrderRepository.save(bankOrder);
  }
}
