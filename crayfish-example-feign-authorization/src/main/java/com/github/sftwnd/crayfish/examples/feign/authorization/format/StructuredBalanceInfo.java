package com.github.sftwnd.crayfish.examples.feign.authorization.format;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

@JsonDeserialize(using = StructuredBalanceInfoJsonDeserializer.class)
public interface StructuredBalanceInfo {

    enum OnlineSystemBalanceType {

         EXCLUDE_VIRTPAYM_EXCLUDE_FINAC
        ,INCLUDE_VIRTPAYM_EXCLUDE_FINAC
        ,INCLUDE_VIRTPAYM_INCLUDE_FINAC
        ,EXCLUDE_VIRTPAYM_INCLUDE_FINAC

    }

    Long getCustomerId();

    BigDecimal getAvailableBalance();

    BigDecimal getReservedBalance();

    BigDecimal getSpentBalance();

    Boolean getAccountEnabled();

    BigDecimal getVirtualPayments();

    BigDecimal getFinanceAccums();

    BigDecimal getSynchronizedBalance();

    BigDecimal getPayments();

    OnlineSystemBalanceType getBalanceType();

}
