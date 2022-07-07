package bzh.toolapp.apps.specifique.service.bankorder;

import com.axelor.apps.bankpayment.db.BankPaymentConfig;
import com.axelor.apps.bankpayment.service.config.BankPaymentConfigService;
import com.axelor.apps.base.db.Sequence;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;

import bzh.toolapp.apps.specifique.exception.IExceptionSpecifiqueMessage;

public class BankPaymentConfigSpecifiqueService extends BankPaymentConfigService {
	public Sequence getBillOfExchangeSequence(BankPaymentConfig bankPaymentConfig) throws AxelorException {
		if (bankPaymentConfig.getBillOfExchangeSequence() == null) {
			throw new AxelorException(bankPaymentConfig, TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
					I18n.get(IExceptionSpecifiqueMessage.ACCOUNT_CONFIG_SEQUENCE_12),
					I18n.get(com.axelor.apps.base.exceptions.IExceptionMessage.EXCEPTION),
					bankPaymentConfig.getCompany().getName());
		}
		return bankPaymentConfig.getBillOfExchangeSequence();
	}
}
