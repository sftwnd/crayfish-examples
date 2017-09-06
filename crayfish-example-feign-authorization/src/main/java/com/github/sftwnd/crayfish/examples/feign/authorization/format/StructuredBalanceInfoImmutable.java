package com.github.sftwnd.crayfish.examples.feign.authorization.format;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

public class StructuredBalanceInfoImmutable implements StructuredBalanceInfo {

    protected Long                                          customerId;
    protected BigDecimal                                    availableBalance;
    protected BigDecimal                                    reservedBalance;
    protected BigDecimal                                    spentBalance;
    protected Boolean                                       accountEnabled;
    protected BigDecimal                                    virtualPayments;
    protected BigDecimal                                    financeAccums;
    protected BigDecimal                                    synchronizedBalance;
    protected BigDecimal                                    payments;
    protected StructuredBalanceInfo.OnlineSystemBalanceType balanceType;

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public BigDecimal getSpentBalance() {
        return spentBalance;
    }

    public Boolean getAccountEnabled() {
        return accountEnabled;
    }

    public BigDecimal getVirtualPayments() {
        return virtualPayments;
    }

    public BigDecimal getFinanceAccums() {
        return financeAccums;
    }

    public BigDecimal getSynchronizedBalance() {
        return synchronizedBalance;
    }

    public BigDecimal getPayments() {
        return payments;
    }

    public StructuredBalanceInfo.OnlineSystemBalanceType getBalanceType() {
        return balanceType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

}
