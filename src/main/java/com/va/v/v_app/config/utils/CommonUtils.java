package com.va.v.v_app.config.utils;

import org.springframework.stereotype.Component;

@Component
public interface CommonUtils {

	boolean validateFiled(String field);

	String checkAndAssignToNull(String field, String display);

	boolean validateToValue(String field, String field1);

	int roundOfToNextVal(int count, int multiple);

	String getTokenFromHeader();

	boolean validateAndCheckContains(String currentVal, String configParams, String delimiter);

	String getServiceIdWithCountrycode(String serviceId);

	boolean validateCreditLimitAndDepsoitAmountCheck(Double creditAmount, Double depositAmount, int status);

	String getContactNumberWithCountryCode(String telephone);

	String getContactNoWithCountrycode(String contactNumber);
}
