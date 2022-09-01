package bzh.toolapp.apps.specifique.exception;

public interface IExceptionSpecifiqueMessage {
  static final String BANK_ORDER_WRONG_ENDORSED_DETAIL_RECORD = /* $$( */
      "Anomaly has been detected during file generation for the endorsed detail record of the bank order line %s" /*
                                                                                                                   * )
                                                                                                                   */;
  static final String BANK_ORDER_WRONG_ADDITIONAL_DETAIL_RECORD = /* $$( */
      "Anomaly has been detected during file generation for the additional detail record of the bank order line %s" /*
                                                                                                                     * )
                                                                                                                     */;
  static final String BANK_ORDER_RECEIVER_BANK_DETAILS_MISSING_PARTNER_ADDRESS = /* $$( */
      "Please fill the address in %s's partner details." /* ) */;
  static final String BANK_ORDER_RECEIVER_BANK_DETAILS_MISSING_PARTNER_ZIP = /* $$( */
      "Please fill the zip in %s's partner details." /* ) */;
  static final String BANK_ORDER_RECEIVER_BANK_DETAILS_MISSING_PARTNER_CITY = /* $$( */
      "Please fill the city in %s's partner details." /* ) */;
  static final String ACCOUNT_CONFIG_SEQUENCE_12 = /* $$( */
      "%s : Please, configure a sequence for the Bill of exchange and the company %s" /* ) */;
}
