package bzh.toolapp.apps.specifique.service.lcr;

import java.time.LocalDate;

import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;

public interface LcrService {
	public void generateLcrFile(LocalDate dueDate, Company company) throws AxelorException;
}
