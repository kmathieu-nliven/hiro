package com.cds.hiro.x12.batch

import com.cds.hiro.x12.structures.Segment

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Created by rahul on 10/23/15.
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class ISA extends Segment {
  static enum AuthorizationInfoQualifier {
    NoAuthorizationInformationPresent_00('00'),
    AdditionalDataIdentification_03('03');

    String code

    AuthorizationInfoQualifier(String code) { this.code = code }

    @Override
    public String toString() { return this.code; }
  }

  static enum SecurityInfoQualifier {
    NoSecurityInformationPresent_00('00'), Password_01('01');
    String code

    SecurityInfoQualifier(String code) {
      this.code = code
    }

    @Override
    public String toString() { return this.code; }
  }

  static enum InterchangeIdQualifier {
    DunsAndBradstreet_01('01'),
    DunsPlusSuffix_14('14'),
    HealthIndustryNumber_20('20'),
    CarrierIdentificationNumber_27('27'),
    FiscalIntermediaryIdentificationNumber_28('28'),
    MedicareProviderAndSupplierIdentificationNumber_29('29'),
    USFederalTaxIdentificationNumber_33('33'),
    MutuallyDefined_ZZ('ZZ');

    String code

    InterchangeIdQualifier(String code) { this.code = code }

    @Override
    public String toString() { return this.code; }
  }

  enum InterchangeControlVersionNumber {
    AscX12_00501('00501');

    String code

    InterchangeControlVersionNumber(String code) { this.code = code }

    @Override
    public String toString() { return this.code; }
  }

  enum AckRequested {
    NoAckRequested_0('0'), AckRequested_1('1')

    String code

    AckRequested(String code) { this.code = code }

    @Override
    public String toString() { return this.code; }
  }

  enum UsageIndicator {
    Production_P('P'), Test_T('T')

    String code

    UsageIndicator(String code) { this.code = code }

    @Override
    public String toString() { return this.code; }
  }


  AuthorizationInfoQualifier authorizationInfoQualifier_01
  String authorizationInformation_02
  SecurityInfoQualifier securityInfoQualifier_03
  String securityInformation_04
  InterchangeIdQualifier interchangeIdQualifier_05
  String interchangeSenderId_06
  InterchangeIdQualifier interchangeIdQualifier_07
  String interchangeReceiverId_08
  LocalDate interchangeDate_09 = LocalDate.now()
  LocalTime interchangeTime_10 = LocalTime.now()
  String repetitionSeparator_11 = '^'
  InterchangeControlVersionNumber interchangeControlVersionNumber_12
  int interchangeControlNumber_13 = 0
  AckRequested ackRequested_14
  UsageIndicator usageIndicator_15
  String componentElementSeparator_16 = ':'

  @Override
  void parse(List<List<List<String>>> input) {

  }

  @Override
  List<List<List<String>>> toTokens(int indent) {
    def indentString = indent > -1 ? ('  ' * indent) : ''

    def retval = new ArrayList().with {
      add new ArrayList().with {
        add new ArrayList().with {
          add "${indentString}ISA".toString()
          it
        }
        it
      }
      it
    }

    retval.add([authorizationInfoQualifier_01].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([authorizationInformation_02].collect { rep -> [(rep ?: '').toString().padRight(10)] })
    retval.add([securityInfoQualifier_03].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([securityInformation_04].collect { rep -> [(rep ?: '').toString().padRight(10)] })
    retval.add([interchangeIdQualifier_05].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([interchangeSenderId_06].collect { rep -> [(rep ?: '').toString().padRight(15)]})
    retval.add([interchangeIdQualifier_07].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([interchangeReceiverId_08].collect { rep -> [(rep ?: '').toString().padRight(15)] })
    retval.add([interchangeDate_09].collect { rep -> rep ? [DateTimeFormatter.ofPattern('yyMMdd').format(rep)] : [] })
    retval.add([interchangeTime_10].collect { rep -> rep ? [DateTimeFormatter.ofPattern('HHmm').format(rep)] : [] })
    retval.add([repetitionSeparator_11].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([interchangeControlVersionNumber_12].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([interchangeControlNumber_13].collect { rep -> [(rep ?: '').toString().padLeft(9, '0')] })
    retval.add([ackRequested_14].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([usageIndicator_15].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([componentElementSeparator_16].collect { rep -> rep ? [rep.toString()] : [] })

    retval

  }
}
