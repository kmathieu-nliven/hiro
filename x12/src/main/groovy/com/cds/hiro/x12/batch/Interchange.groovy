package com.cds.hiro.x12.batch

import com.cds.hiro.x12.structures.Message

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Created by rahul on 10/23/15.
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class Interchange {
  ISA isa
  GS gs
  List<Message> messages
  GE ge
  IEA iea

  List<List<List<List<String>>>> toTokens(int indent = -1) {
    def retval = []
    def indentOld = indent > -1 ? indent : -1
    def indentNew = indent > -1 ? (indent + 1) : -1


    if (isa) retval.add(isa.toTokens(indentOld))
    if (gs) retval.add(gs.toTokens(indentNew))
    messages.each {
      retval.addAll(it.toTokens(indentNew))
    }
    if (ge) retval.add(ge.toTokens(indentNew))
    if (iea) retval.add(iea.toTokens(indentNew))

    retval
  }

  static Interchange createInterchange(
      List<? extends Message> messages, LocalDateTime localDateTime = LocalDateTime.now(), String senderId = '9' * 11, String receiverId = '8' * 11,
      int groupControlNumber = 1234567890, int interchangeControlNumber = 23
  ) {
    def localDate = localDateTime.toLocalDate()
    def localTime = localDateTime.toLocalTime()
    new Interchange().
        withIsa(new ISA().
            withAuthorizationInfoQualifier_01(ISA.AuthorizationInfoQualifier.NoAuthorizationInformationPresent_00).
            withSecurityInfoQualifier_03(ISA.SecurityInfoQualifier.NoSecurityInformationPresent_00).
            withInterchangeIdQualifier_05(ISA.InterchangeIdQualifier.MutuallyDefined_ZZ).
            withInterchangeSenderId_06(senderId).
            withInterchangeIdQualifier_07(ISA.InterchangeIdQualifier.MutuallyDefined_ZZ).
            withInterchangeReceiverId_08(receiverId).
            withInterchangeDate_09(localDate).
            withInterchangeTime_10(localTime).
            withInterchangeControlVersionNumber_12(ISA.InterchangeControlVersionNumber.AscX12_00501).
            withInterchangeControlNumber_13(interchangeControlNumber).
            withAckRequested_14(ISA.AckRequested.NoAckRequested_0).
            withUsageIndicator_15(ISA.UsageIndicator.Test_T)
        ).
        withGs(new GS().
            withFunctionalIdentifierCode_01('HC').
            withApplicationSenderCode_02(senderId).
            withApplicationReceiverCode_03(receiverId).
            withDate_04(localDate).
            withTime_05(localTime).
            withGroupControlNumber_06(groupControlNumber)
        ).
        withMessages(messages).
        withGe(new GE().
            withNumberOfTSIncluded_01(messages.size()).
            withGroupControlNumber_02(groupControlNumber)
        ).
        withIea(new IEA().
            withNumberOfFunctionalGroups(1).
            withInterchangeControlNumber(interchangeControlNumber)
        )
  }

}
