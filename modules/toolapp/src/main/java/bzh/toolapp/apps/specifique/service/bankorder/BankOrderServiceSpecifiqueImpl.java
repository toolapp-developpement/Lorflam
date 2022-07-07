package bzh.toolapp.apps.specifique.service.bankorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import com.axelor.apps.account.db.repo.InvoicePaymentRepository;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentCancelService;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.apps.bankpayment.db.BankOrderFileFormat;
import com.axelor.apps.bankpayment.db.BankPaymentConfig;
import com.axelor.apps.bankpayment.db.repo.BankOrderFileFormatRepository;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.axelor.apps.bankpayment.ebics.service.EbicsService;
import com.axelor.apps.bankpayment.exception.IExceptionMessage;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderLineOriginService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderLineService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderMoveService;
import com.axelor.apps.bankpayment.service.bankorder.file.directdebit.BankOrderFile00800101Service;
import com.axelor.apps.bankpayment.service.bankorder.file.directdebit.BankOrderFile00800102Service;
import com.axelor.apps.bankpayment.service.bankorder.file.directdebit.BankOrderFile008Service;
import com.axelor.apps.bankpayment.service.bankorder.file.transfer.BankOrderFile00100102Service;
import com.axelor.apps.bankpayment.service.bankorder.file.transfer.BankOrderFile00100103Service;
import com.axelor.apps.bankpayment.service.bankorder.file.transfer.BankOrderFileAFB160ICTService;
import com.axelor.apps.bankpayment.service.bankorder.file.transfer.BankOrderFileAFB320XCTService;
import com.axelor.apps.bankpayment.service.config.BankPaymentConfigService;
import com.axelor.apps.base.db.Sequence;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.service.bankorder.BankOrderServiceHRImpl;
import com.axelor.apps.hr.service.expense.ExpenseService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.google.inject.Inject;

public class BankOrderServiceSpecifiqueImpl extends BankOrderServiceHRImpl {

	protected BankOrderRepository bankOrderRepo;
	protected InvoicePaymentRepository invoicePaymentRepo;
	protected BankOrderLineService bankOrderLineService;
	protected EbicsService ebicsService;
	protected InvoicePaymentCancelService invoicePaymentCancelService;
	protected BankPaymentConfigService bankPaymentConfigService;
	protected SequenceService sequenceService;
	protected BankOrderLineOriginService bankOrderLineOriginService;
	protected BankOrderMoveService bankOrderMoveService;
	protected AppBaseService appBaseService;
	protected BankPaymentConfigSpecifiqueService bankPaymentConfigSpecifiqueService;

	@Inject
	public BankOrderServiceSpecifiqueImpl(BankOrderRepository bankOrderRepo,
			InvoicePaymentRepository invoicePaymentRepo, BankOrderLineService bankOrderLineService,
			EbicsService ebicsService, InvoicePaymentCancelService invoicePaymentCancelService,
			BankPaymentConfigService bankPaymentConfigService, SequenceService sequenceService,
			BankOrderLineOriginService bankOrderLineOriginService, ExpenseService expenseService,
			BankOrderMoveService bankOrderMoveService, AppBaseService appBaseService,
			BankPaymentConfigSpecifiqueService bankPaymentConfigSpecifiqueService) {

		super(bankOrderRepo, invoicePaymentRepo, bankOrderLineService, ebicsService, invoicePaymentCancelService,
				bankPaymentConfigService, sequenceService, bankOrderLineOriginService, expenseService,
				bankOrderMoveService, appBaseService);

		this.bankOrderRepo = bankOrderRepo;
		this.invoicePaymentRepo = invoicePaymentRepo;
		this.bankOrderLineService = bankOrderLineService;
		this.ebicsService = ebicsService;
		this.invoicePaymentCancelService = invoicePaymentCancelService;
		this.bankPaymentConfigService = bankPaymentConfigService;
		this.sequenceService = sequenceService;
		this.bankOrderLineOriginService = bankOrderLineOriginService;
		this.bankOrderMoveService = bankOrderMoveService;
		this.appBaseService = appBaseService;
		this.bankPaymentConfigSpecifiqueService = bankPaymentConfigSpecifiqueService;
	}

	@Override
	public File generateFile(BankOrder bankOrder)
			throws JAXBException, IOException, AxelorException, DatatypeConfigurationException {

		if (bankOrder.getBankOrderLineList() == null || bankOrder.getBankOrderLineList().isEmpty()) {
			return null;
		}

		bankOrder.setFileGenerationDateTime(LocalDateTime.now());

		BankOrderFileFormat bankOrderFileFormat = bankOrder.getBankOrderFileFormat();

		File file = null;

		switch (bankOrderFileFormat.getOrderFileFormatSelect()) {
		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_001_001_02_SCT:
			file = new BankOrderFile00100102Service(bankOrder).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_001_001_03_SCT:
			file = new BankOrderFile00100103Service(bankOrder).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_XXX_CFONB320_XCT:
			file = new BankOrderFileAFB320XCTService(bankOrder).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_XXX_CFONB160_ICT:
			file = new BankOrderFileAFB160ICTService(bankOrder).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_XXX_CFONB160_DCO:
			file = new BankOrderFileAFB160DCOService(bankOrder).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_008_001_01_SDD:
			file = new BankOrderFile00800101Service(bankOrder, BankOrderFile008Service.SEPA_TYPE_CORE).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_008_001_01_SBB:
			file = new BankOrderFile00800101Service(bankOrder, BankOrderFile008Service.SEPA_TYPE_SBB).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_008_001_02_SDD:
			file = new BankOrderFile00800102Service(bankOrder, BankOrderFile008Service.SEPA_TYPE_CORE).generateFile();
			break;

		case BankOrderFileFormatRepository.FILE_FORMAT_PAIN_008_001_02_SBB:
			file = new BankOrderFile00800102Service(bankOrder, BankOrderFile008Service.SEPA_TYPE_SBB).generateFile();
			break;

		default:
			throw new AxelorException(bankOrder, TraceBackRepository.CATEGORY_INCONSISTENCY,
					I18n.get(IExceptionMessage.BANK_ORDER_FILE_UNKNOWN_FORMAT));
		}

		if (file == null) {
			throw new AxelorException(bankOrder, TraceBackRepository.CATEGORY_INCONSISTENCY,
					I18n.get(IExceptionMessage.BANK_ORDER_ISSUE_DURING_FILE_GENERATION), bankOrder.getBankOrderSeq());
		}

		MetaFiles metaFiles = Beans.get(MetaFiles.class);

		try (InputStream is = new FileInputStream(file)) {
			metaFiles.attach(is, file.getName(), bankOrder);
			bankOrder.setGeneratedMetaFile(metaFiles.upload(file));
		}

		return file;
	}

	@Override
	protected Sequence getSequence(BankOrder bankOrder) throws AxelorException {
		BankPaymentConfig bankPaymentConfig = Beans.get(BankPaymentConfigService.class)
				.getBankPaymentConfig(bankOrder.getSenderCompany());

		switch (bankOrder.getOrderTypeSelect()) {
		case BankOrderRepository.ORDER_TYPE_SEPA_DIRECT_DEBIT:
			return bankPaymentConfigService.getSepaDirectDebitSequence(bankPaymentConfig);

		case BankOrderRepository.ORDER_TYPE_SEPA_CREDIT_TRANSFER:
			return bankPaymentConfigService.getSepaCreditTransSequence(bankPaymentConfig);

		case BankOrderRepository.ORDER_TYPE_INTERNATIONAL_DIRECT_DEBIT:
			return bankPaymentConfigService.getIntDirectDebitSequence(bankPaymentConfig);

		case BankOrderRepository.ORDER_TYPE_INTERNATIONAL_CREDIT_TRANSFER:
			return bankPaymentConfigService.getIntCreditTransSequence(bankPaymentConfig);

		case BankOrderRepository.ORDER_TYPE_NATIONAL_TREASURY_TRANSFER:
			return bankPaymentConfigService.getNatTreasuryTransSequence(bankPaymentConfig);

		case BankOrderRepository.ORDER_TYPE_INTERNATIONAL_TREASURY_TRANSFER:
			return bankPaymentConfigService.getIntTreasuryTransSequence(bankPaymentConfig);

		case BankOrderRepository.ORDER_TYPE_BILL_OF_EXCHANGE:
			return bankPaymentConfigSpecifiqueService.getBillOfExchangeSequence(bankPaymentConfig);
		default:
			return bankPaymentConfigService.getOtherBankOrderSequence(bankPaymentConfig);
		}
	}
}
