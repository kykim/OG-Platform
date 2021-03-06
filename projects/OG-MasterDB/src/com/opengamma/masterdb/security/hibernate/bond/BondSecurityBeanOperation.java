/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.masterdb.security.hibernate.bond;

import static com.opengamma.masterdb.security.hibernate.Converters.*;

import com.opengamma.financial.security.FinancialSecurityVisitorAdapter;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.financial.security.bond.CorporateBondSecurity;
import com.opengamma.financial.security.bond.GovernmentBondSecurity;
import com.opengamma.financial.security.bond.MunicipalBondSecurity;
import com.opengamma.masterdb.security.hibernate.AbstractSecurityBeanOperation;
import com.opengamma.masterdb.security.hibernate.Converters;
import com.opengamma.masterdb.security.hibernate.HibernateSecurityMasterDao;
import com.opengamma.masterdb.security.hibernate.OperationContext;

/**
 * Operation for working on a bond security.
 */
public final class BondSecurityBeanOperation extends AbstractSecurityBeanOperation<BondSecurity, BondSecurityBean> {

  /**
   * Singleton.
   */
  public static final BondSecurityBeanOperation INSTANCE = new BondSecurityBeanOperation();

  private BondSecurityBeanOperation() {
    super("BOND", BondSecurity.class, BondSecurityBean.class);
  }

  @Override
  public BondSecurity createSecurity(final OperationContext context, final BondSecurityBean bean) {
    return bean.getBondType().accept(new FinancialSecurityVisitorAdapter<BondSecurity>() {

      @Override
      public BondSecurity visitCorporateBondSecurity(CorporateBondSecurity bond) {
        BondSecurity bondSecurity = new CorporateBondSecurity(bean.getIssuerName(), bean.getIssuerType().getName(), bean.getIssuerDomicile(), bean.getMarket().getName(),
          currencyBeanToCurrency(bean.getCurrency()), yieldConventionBeanToYieldConvention(bean.getYieldConvention()),
          expiryBeanToExpiry(bean.getLastTradeDate()), bean.getCouponType().getName(), bean.getCouponRate(),
          frequencyBeanToFrequency(bean.getCouponFrequency()), dayCountBeanToDayCount(bean.getDayCountConvention()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getInterestAccrualDate()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getSettlementDate()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getFirstCouponDate()),
          bean.getIssuancePrice(), bean.getTotalAmountIssued(), bean.getMinimumAmount(), bean.getMinimumIncrement(),
          bean.getParAmount(), bean.getRedemptionValue());
        bondSecurity.setBusinessDayConvention(businessDayConventionBeanToBusinessDayConvention(bean.getBusinessDayConvention()));
        bondSecurity.setAnnouncementDate(zonedDateTimeBeanToDateTimeWithZone(bean.getAnnouncementDate()));
        bondSecurity.setGuaranteeType(bean.getGuaranteeType() != null ? bean.getGuaranteeType().getName() : null);
        return bondSecurity;
      }

      @Override
      public BondSecurity visitGovernmentBondSecurity(GovernmentBondSecurity bond) {
        BondSecurity bondSecurity = new GovernmentBondSecurity(bean.getIssuerName(), bean.getIssuerType().getName(), bean.getIssuerDomicile(),
          bean.getMarket().getName(), currencyBeanToCurrency(bean.getCurrency()),
          yieldConventionBeanToYieldConvention(bean.getYieldConvention()), expiryBeanToExpiry(bean.getLastTradeDate()),
          bean.getCouponType().getName(), bean.getCouponRate(), frequencyBeanToFrequency(bean.getCouponFrequency()),
          dayCountBeanToDayCount(bean.getDayCountConvention()), zonedDateTimeBeanToDateTimeWithZone(bean.getInterestAccrualDate()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getSettlementDate()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getFirstCouponDate()), bean.getIssuancePrice(), bean.getTotalAmountIssued(),
          bean.getMinimumAmount(), bean.getMinimumIncrement(), bean.getParAmount(), bean.getRedemptionValue());
        bondSecurity.setBusinessDayConvention(businessDayConventionBeanToBusinessDayConvention(bean.getBusinessDayConvention()));
        bondSecurity.setAnnouncementDate(zonedDateTimeBeanToDateTimeWithZone(bean.getAnnouncementDate()));
        bondSecurity.setGuaranteeType(bean.getGuaranteeType() != null ? bean.getGuaranteeType().getName() : null);
        return bondSecurity;
      }

      @Override
      public BondSecurity visitMunicipalBondSecurity(MunicipalBondSecurity bond) {
        BondSecurity bondSecurity = new MunicipalBondSecurity(bean.getIssuerName(), bean.getIssuerType().getName(), bean.getIssuerDomicile(),
          bean.getMarket().getName(), currencyBeanToCurrency(bean.getCurrency()),
          yieldConventionBeanToYieldConvention(bean.getYieldConvention()), expiryBeanToExpiry(bean.getLastTradeDate()),
          bean.getCouponType().getName(), bean.getCouponRate(), frequencyBeanToFrequency(bean.getCouponFrequency()),
          dayCountBeanToDayCount(bean.getDayCountConvention()), zonedDateTimeBeanToDateTimeWithZone(bean.getInterestAccrualDate()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getSettlementDate()),
          zonedDateTimeBeanToDateTimeWithZone(bean.getFirstCouponDate()),
          bean.getIssuancePrice(), bean.getTotalAmountIssued(), bean.getMinimumAmount(), bean.getMinimumIncrement(),
          bean.getParAmount(), bean.getRedemptionValue());
        bondSecurity.setBusinessDayConvention(businessDayConventionBeanToBusinessDayConvention(bean.getBusinessDayConvention()));
        bondSecurity.setAnnouncementDate(zonedDateTimeBeanToDateTimeWithZone(bean.getAnnouncementDate()));
        bondSecurity.setGuaranteeType(bean.getGuaranteeType() != null ? bean.getGuaranteeType().getName() : null);
        return bondSecurity;
      }

    });
  }

  @Override
  public BondSecurityBean createBean(final OperationContext context, final HibernateSecurityMasterDao secMasterSession, final BondSecurity security) {
    Converters.validateYieldConvention(security.getYieldConvention().getConventionName());
    Converters.validateDayCount(security.getDayCount().getConventionName());
    Converters.validateFrequency(security.getCouponFrequency().getConventionName());

    final BondSecurityBean bond = new BondSecurityBean();
    bond.setBondType(BondType.identify(security));
    bond.setIssuerName(security.getIssuerName());
    bond.setIssuerType(secMasterSession.getOrCreateIssuerTypeBean(security.getIssuerType()));
    bond.setIssuerDomicile(security.getIssuerDomicile());
    bond.setMarket(secMasterSession.getOrCreateMarketBean(security.getMarket()));
    bond.setCurrency(secMasterSession.getOrCreateCurrencyBean(security.getCurrency().getCode()));
    bond.setYieldConvention(secMasterSession.getOrCreateYieldConventionBean(security.getYieldConvention().getConventionName()));
    bond.setGuaranteeType(security.getGuaranteeType() != null ? secMasterSession.getOrCreateGuaranteeTypeBean(security.getGuaranteeType()) : null);
    bond.setLastTradeDate(expiryToExpiryBean(security.getLastTradeDate()));
    bond.setCouponType(secMasterSession.getOrCreateCouponTypeBean(security.getCouponType()));
    bond.setCouponRate(security.getCouponRate());
    bond.setCouponFrequency(secMasterSession.getOrCreateFrequencyBean(security.getCouponFrequency().getConventionName()));
    bond.setDayCountConvention(secMasterSession.getOrCreateDayCountBean(security.getDayCount().getConventionName()));
    if (security.getBusinessDayConvention() != null) {
      Converters.validateBusinessDayConvention(security.getBusinessDayConvention().getConventionName());
      bond.setBusinessDayConvention(secMasterSession.getOrCreateBusinessDayConventionBean(security.getBusinessDayConvention().getConventionName()));
    }
    bond.setAnnouncementDate(security.getAnnouncementDate() != null ? dateTimeWithZoneToZonedDateTimeBean(security.getAnnouncementDate()) : null);
    bond.setInterestAccrualDate(dateTimeWithZoneToZonedDateTimeBean(security.getInterestAccrualDate()));
    bond.setSettlementDate(dateTimeWithZoneToZonedDateTimeBean(security.getSettlementDate()));
    bond.setFirstCouponDate(dateTimeWithZoneToZonedDateTimeBean(security.getFirstCouponDate()));
    bond.setIssuancePrice(security.getIssuancePrice());
    bond.setTotalAmountIssued(security.getTotalAmountIssued());
    bond.setMinimumAmount(security.getMinimumAmount());
    bond.setMinimumIncrement(security.getMinimumIncrement());
    bond.setParAmount(security.getParAmount());
    bond.setRedemptionValue(security.getRedemptionValue());
    return bond;
  }

}
