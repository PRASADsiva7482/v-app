package com.va.v.v_app.core.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.va.v.v_app.core.webConfig.ApplicationCacheConfiguration;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonUtilsImpl implements CommonUtils {

	@Autowired
	ApplicationCacheConfiguration configuration;

	public boolean validateFiled(String field) {
		boolean success = false;
		try {
			if (field != null && !field.equals("") && !field.equalsIgnoreCase("NULL")) {
				success = true;
			}
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return success;
	}

	public String checkAndAssignToNull(String field, String display) {
		try {
			if (!validateFiled(field)) {
				field = display;
			}
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return field;
	}

	public boolean validateToValue(String field, String field1) {
		boolean success = false;
		try {
			if (validateFiled(field) && validateFiled(field1) && field.equalsIgnoreCase(field1)) {
				success = true;
			}
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return success;
	}

	public int roundOfToNextVal(int count, int multiple) {
		return count / multiple
				+ (BigDecimal.valueOf((float) 23 / 10).divideAndRemainder(BigDecimal.ONE)[1].floatValue() > 0 ? 1 : 0);
	}

	@Override
	public String getTokenFromHeader() {
		final String requestTokenHeader = MDC.get(Constants.AUTHORIZATION_HEADER);
		if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
			return null;
		}
		return requestTokenHeader.substring(7);
	}

	public boolean checkIfListContains(String statusCode, String validStatusCodes) {
		try {
			List<Boolean> success = Arrays.asList(StringUtils.split(validStatusCodes, ",")).stream().map(key -> {
				return (key.equalsIgnoreCase(statusCode) ? true : false);
			}).collect(Collectors.toList());
			return success.contains(true);
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return false;
	}

	public boolean validateAndCheckContains(String currentVal, String configParams, String delimiter) {
		boolean success = false;
		try {
			if (!validateFiled(currentVal) || !validateFiled(configParams)) {
				success = false;
			} else {
				List<String> configArray = Arrays.asList(configParams.split(delimiter));
				log.info(" | Proces completed with status :- " + currentVal + " success code list :- "
						+ configArray.toString());
				if (configArray == null || configArray.size() == 0 || configArray.isEmpty()) {
					log.info(" | No currentVal " + currentVal + " Are configured for " + configParams);
					success = false;
				}
				if (configArray.contains(currentVal + "")) {
					success = true;
				}
			}
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return success;
	}

	@Override
	public String getServiceIdWithCountrycode(String serviceId) {
		try {

			String ccEnableReq = configuration.getMappings().get("systemServiceIdConfig").getDynamicDetails()
					.get("countryCodeRequired");
			if (validateToValue(ccEnableReq, "true")) {
				Assert.notNull(
						configuration.getMappings().get("systemServiceIdConfig").getDynamicDetails().get("countryCode"),
						" countryCode Not Defined in application-feature.yml File for Key :- countryCode");
				Assert.notNull(
						configuration.getMappings().get("systemServiceIdConfig").getDynamicDetails()
								.get("serviceIdLength"),
						" serviceIdLength Not Defined in application-feature.yml File for Key :- serviceIdLength");
				String countryCode = configuration.getMappings().get("systemServiceIdConfig").getDynamicDetails()
						.get("countryCode");
				int numberlength = Integer.parseInt(configuration.getMappings().get("systemServiceIdConfig")
						.getDynamicDetails().get("serviceIdLength"));
				if (serviceId != null) {
					String serviceIdLast = serviceId.substring(serviceId.length() - numberlength);
					serviceId = countryCode + "" + serviceIdLast;
				}
			}

		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return serviceId;
	}

	public boolean validateCreditLimitAndDepsoitAmountCheck(Double creditAmount, Double depositAmount, int status) {
		try {
			double multiplier = 1;
			// Assert.notNull(configuration.getMappings().get("approvalConfig").getDynamicDetails().get("multiplier"),
			// " FTP ID Not Defined in application-feature.yml File for Key :- ftpId");
			multiplier = Double.parseDouble(
					configuration.getMappings().get("approvalConfig").getDynamicDetails().get("multiplier"));
			if (status == 0) {
				log.info("Approval Status is Active");
				if ((creditAmount * multiplier) <= depositAmount) {
					log.info("Checking Deposit amount greater than Credit limit ");
					return true;
				}
			} else {
				log.info("Approval Status is InActive");
				return true;
			}
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return false;
	}

	@Override
	public String getContactNumberWithCountryCode(String telephone) {
		try {
			if (validateFiled(telephone)) {

				if (telephone.length() == 9) {
					String countryCode = configuration.getMappings().get("coreSystemConfig").getDynamicDetails()
							.get("telephoneCountryCode");
					return countryCode + telephone;
				} else if (telephone.length() == 10) {
					String countryCode = configuration.getMappings().get("coreSystemConfig").getDynamicDetails()
							.get("telephoneCountryCode");
					return countryCode + (telephone.substring(1, 10));
				}
			} else {
				return telephone;
			}
		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return telephone;

	}

	@Override
	public String getContactNoWithCountrycode(String contactNumber) {
		try {

			String ccEnableReq = configuration.getMappings().get("systemContactNumberConfig").getDynamicDetails()
					.get("countryCodeRequired");
			if (validateToValue(ccEnableReq, "true")) {
				Assert.notNull(
						configuration.getMappings().get("systemContactNumberConfig").getDynamicDetails()
								.get("countryCode"),
						" countryCode Not Defined in application-feature.yml File for Key :- countryCode");
				Assert.notNull(
						configuration.getMappings().get("systemContactNumberConfig").getDynamicDetails()
								.get("contactNumberLength"),
						" contactNumberLength Not Defined in application-feature.yml File for Key :- contactNumberLength");
				String countryCode = configuration.getMappings().get("systemContactNumberConfig").getDynamicDetails()
						.get("countryCode");
				int numberlength = Integer.parseInt(configuration.getMappings().get("systemContactNumberConfig")
						.getDynamicDetails().get("contactNumberLength"));
				if (contactNumber != null) {
					String contactNumberLast = contactNumber.substring(contactNumber.length() - numberlength);
					contactNumber = countryCode + "" + contactNumberLast;
				}
			}

		} catch (Exception e) {
			log.error("Exception Occured " + e.getMessage(), e);
		}
		return contactNumber;
	}

}
