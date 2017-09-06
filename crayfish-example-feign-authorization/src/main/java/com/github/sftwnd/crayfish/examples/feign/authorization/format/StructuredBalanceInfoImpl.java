package com.github.sftwnd.crayfish.examples.feign.authorization.format;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

public class StructuredBalanceInfoImpl {

    private StructuredBalanceInfoImmutable balanceInfo = new StructuredBalanceInfoImmutable();

    public Long getCustomerId() {
        return balanceInfo.getCustomerId();
    }

    public void setCustomerId(Long customerId) {
        balanceInfo.customerId = customerId;
    }

    public BigDecimal getAvailableBalance() {
        return balanceInfo.getAvailableBalance();
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        balanceInfo.availableBalance = availableBalance;
    }

    public BigDecimal getReservedBalance() {
        return balanceInfo.getReservedBalance();
    }

    public void setReservedBalance(BigDecimal reservedBalance) {
        balanceInfo.reservedBalance = reservedBalance;
    }

    public BigDecimal getSpentBalance() {
        return balanceInfo.getSpentBalance();
    }

    public void setSpentBalance(BigDecimal spentBalance) {
        balanceInfo.spentBalance = spentBalance;
    }

    public Boolean getAccountEnabled() {
        return balanceInfo.getAccountEnabled();
    }

    public void setAccountEnabled(Boolean accountEnabled) {
        balanceInfo.accountEnabled = accountEnabled;
    }

    public BigDecimal getVirtualPayments() {
        return balanceInfo.getVirtualPayments();
    }

    public void setVirtualPayments(BigDecimal virtualPayments) {
        balanceInfo.virtualPayments = virtualPayments;
    }

    public BigDecimal getFinanceAccums() {
        return balanceInfo.getFinanceAccums();
    }

    public void setFinanceAccums(BigDecimal financeAccums) {
        balanceInfo.financeAccums = financeAccums;
    }

    public BigDecimal getSynchronizedBalance() {
        return balanceInfo.getSynchronizedBalance();
    }

    public void setSynchronizedBalance(BigDecimal synchronizedBalance) {
        balanceInfo.synchronizedBalance = synchronizedBalance;
    }

    public BigDecimal getPayments() {
        return balanceInfo.getPayments();
    }

    public void setPayments(BigDecimal payments) {
        balanceInfo.payments = payments;
    }

    public StructuredBalanceInfo.OnlineSystemBalanceType getBalanceType() {
        return balanceInfo.getBalanceType();
    }

    public void setBalanceType(StructuredBalanceInfo.OnlineSystemBalanceType balanceType) {
        balanceInfo.balanceType = balanceType;
    }

    public void setBalanceType(int balanceTypeId) {
        setBalanceType(StructuredBalanceInfo.OnlineSystemBalanceType.values()[balanceTypeId-1]);
    }

    public StructuredBalanceInfo immutable() {
        return this.balanceInfo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }



}
