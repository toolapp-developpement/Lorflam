package bzh.toolapp.apps.specifique.service.lcr;

import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;
import java.time.LocalDate;

public interface LcrService {
  public void generateLcrFile(LocalDate dueDate, Company company) throws AxelorException;
}
