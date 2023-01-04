package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.BirtTemplate;
import com.axelor.apps.marketing.service.TemplateMessageServiceMarketingImpl;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.message.service.TemplateContextService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.text.Templates;
import com.axelor.tool.template.TemplateMaker;
import com.google.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TemplateMessageSpecifiqueServiceBaseImpl extends TemplateMessageServiceMarketingImpl {

  @Inject
  public TemplateMessageSpecifiqueServiceBaseImpl(
      MessageService messageService, TemplateContextService templateContextService) {
    super(messageService, templateContextService);
  }

  @Override
  public MetaFile createMetaFileUsingBirtTemplate(
      TemplateMaker maker,
      BirtTemplate birtTemplate,
      Templates templates,
      Map<String, Object> templatesContext)
      throws AxelorException, IOException {

    SaleOrder saleOrder;
    String saleOrderNumber = "";

    if (templatesContext.containsKey("SaleOrder")) {
      saleOrder = (SaleOrder) templatesContext.get("SaleOrder");
      saleOrderNumber = saleOrder.getSaleOrderSeq();
    }
    String fileName;
    if (saleOrderNumber.isEmpty()) {
      fileName =
          birtTemplate.getName()
              + "-"
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    } else {
      fileName = birtTemplate.getName() + "-" + saleOrderNumber;
    }

    File file =
        generateBirtTemplate(
            maker,
            templates,
            templatesContext,
            fileName,
            birtTemplate.getTemplateLink(),
            birtTemplate.getFormat(),
            birtTemplate.getBirtTemplateParameterList());

    try (InputStream is = new FileInputStream(file)) {
      return Beans.get(MetaFiles.class).upload(is, fileName + "." + birtTemplate.getFormat());
    }
  }
}
