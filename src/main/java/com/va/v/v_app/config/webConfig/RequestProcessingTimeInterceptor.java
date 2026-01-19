package com.va.v.v_app.config.webConfig;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.va.v.v_app.config.utils.CommonUtils;
import com.va.v.v_app.config.utils.Constants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class RequestProcessingTimeInterceptor implements HandlerInterceptor {

	@Autowired
	ApplicationLogService loggingService;

	@Autowired
	CommonUtils utils;

	@Autowired
	ApplicationCacheConfiguration configuration;

	@Value("${feature.mappings.coreSystemConfig.dynamicDetails.skipEntityIdUrls}")
	List<String> skipEntityIdUrls;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String invokeUrl = request.getRequestURI().toString();
		try {
			String txnId = ServiceCore.getSequencerInstance().getSequenceId(null);
			Long startTime = System.currentTimeMillis();
			request.setAttribute("appTag",
					(request.getHeader(Constants.appLogName) != null
							&& !request.getHeader(Constants.appLogName).equals(""))
									? request.getHeader(Constants.appLogName)
									: "APP");

			request.setAttribute("startTime", startTime);
			request.setAttribute("txnId",
					(request.getHeader(Constants.txnIdHeader) != null
							&& !request.getHeader(Constants.txnIdHeader).equals(""))
									? request.getHeader(Constants.txnIdHeader)
									: txnId);
			request.setAttribute("control_transaction_id", txnId);

			if (utils.validateFiled(invokeUrl)) {
				invokeUrl = invokeUrl.substring(invokeUrl.lastIndexOf("/"), invokeUrl.length());
			}

			MDC.put(Constants.AUTHORIZATION_HEADER, request.getHeader(Constants.AUTHORIZATION_HEADER));
			// MDC.put(Constants.SOURCE, request.getHeader(Constants.SOURCE));

			MDC.put(Constants.SOURCE,
					request.getHeader(Constants.SOURCE) != null && !request.getHeader(Constants.SOURCE).equals("")
							? request.getHeader(Constants.SOURCE)
							: Constants.defaultSource);

			MDC.put(Constants.txnIdHeader,
					(request.getHeader(Constants.txnIdHeader) != null
							&& !request.getHeader(Constants.txnIdHeader).equals(""))
									? request.getHeader(Constants.txnIdHeader)
									: txnId);

			MDC.put(Constants.appLogName,
					(request.getHeader(Constants.appLogName) != null
							&& !request.getHeader(Constants.appLogName).equals(""))
									? request.getHeader(Constants.appLogName)
									: "APP");

			MDC.put(Constants.sessionIdHeader, request.getRequestedSessionId());
			MDC.put(Constants.userNameHeader,
					request.getHeader(Constants.userNameHeader) != null
							&& !request.getHeader(Constants.userNameHeader).equals("")
									? request.getHeader(Constants.userNameHeader)
									: "systemAdmin");
			MDC.put(Constants.userFullNameHeader,
					request.getHeader(Constants.userFullNameHeader) != null
							&& !request.getHeader(Constants.userFullNameHeader).equals("")
									? request.getHeader(Constants.userFullNameHeader)
									: "systemAdmin");
			MDC.put("isProxyNumberSearch",
					request.getHeader("isProxyNumberSearch") != null ? request.getHeader("isProxyNumberSearch")
							: "false");

			MDC.put(Constants.userIdHeader,
					request.getHeader(Constants.userIdHeader) != null
							&& !request.getHeader(Constants.userIdHeader).equals("")
									? request.getHeader(Constants.userIdHeader)
									: "1");
			MDC.put(Constants.teamIdHeader,
					request.getHeader(Constants.teamIdHeader) != null
							&& !request.getHeader(Constants.teamIdHeader).equals("")
									? request.getHeader(Constants.teamIdHeader)
									: "1");
			if (configuration.getMappings().get("coreSystemConfig") != null && utils.validateToValue(
					configuration.getMappings().get("coreSystemConfig").getDynamicDetails().get("multitenancy"),
					"true")) {
				if (request.getHeader(Constants.entityIdHeader) != null
						&& !request.getHeader(Constants.entityIdHeader).equals("")) {
					MDC.put(Constants.entityIdHeader,
							request.getHeader(Constants.entityIdHeader) != null
									&& !request.getHeader(Constants.entityIdHeader).equals("")
											? request.getHeader(Constants.entityIdHeader)
											: Constants.defaultMvnoId);
				} else {
					if (checkskipEntityIdURLs(request.getRequestURI().toString())) {
						MDC.put(Constants.entityIdHeader,
								request.getHeader(Constants.entityIdHeader) != null
										&& !request.getHeader(Constants.entityIdHeader).equals("")
												? request.getHeader(Constants.entityIdHeader)
												: Constants.defaultMvnoId);
					} else {
						throw new CommonException(HttpConstants.CUSTOM_FIELD_VALIDATION, "Entity ID is required");
					}
				}
			} else {
				MDC.put(Constants.entityIdHeader,
						request.getHeader(Constants.entityIdHeader) != null
								&& !request.getHeader(Constants.entityIdHeader).equals("")
										? request.getHeader(Constants.entityIdHeader)
										: Constants.defaultMvnoId);
			}

			MDC.put(Constants.languageIdHeader,
					request.getHeader(Constants.languageIdHeader) != null
							&& !request.getHeader(Constants.languageIdHeader).equals("")
									? request.getHeader(Constants.languageIdHeader)
									: Constants.defaultLanguageId);

			MDC.put(Constants.functionIdHeader, request.getHeader(Constants.functionIdHeader));

			MDC.put(Constants.authTypeHeader, request.getHeader(Constants.authTypeHeader));
			MDC.put(Constants.biometricIdHeader, request.getHeader(Constants.biometricIdHeader));

			MDC.put(Constants.appLogName, Constants.appLogValue);

			if (loggingService != null) {
				loggingService.logRequest(request, null);
			}
			if (!invokeUrl.contains(".")) {
				log.warn("Request URL :- {}  IP :- {}", invokeUrl, request.getRemoteAddr());
			}

			// Mapping data source based on entity id
			if (MDC.get(Constants.entityIdHeader) != null) {
				if (configuration.getMappings().get("coreSystemConfig") != null && utils.validateToValue(
						configuration.getMappings().get("coreSystemConfig").getDynamicDetails().get("multitenancy"),
						"true")) {
					// setting crm data source
					if (configuration.getMappings().get("crmEntityIdDataSourceMapping") != null) {
						if (utils.validateFiled(configuration.getMappings().get("crmEntityIdDataSourceMapping")
								.getDynamicDetails().get("entityid-" + MDC.get(Constants.entityIdHeader)))) {
							MDC.put(Constants.crm_db_instance,
									configuration.getMappings().get("crmEntityIdDataSourceMapping").getDynamicDetails()
											.get("entityid-" + MDC.get(Constants.entityIdHeader)));
							log.info("entity-Id {} , crm-db-instance {}", MDC.get(Constants.entityIdHeader),
									MDC.get(Constants.crm_db_instance));
						} else {
							// not having configured entity id
							throw new CommonException(HttpConstants.CUSTOM_FIELD_VALIDATION,
									"Entity ID is not found for CRM DB");
						}
					}

				} else {
					MDC.put(Constants.crm_db_instance, Constants.initial_db_name);

				}
			}

		} catch (CommonException e) {
			throw new CommonException(HttpConstants.CUSTOM_FIELD_VALIDATION, e.getMessage());
		} catch (Throwable e) {
			log.error("Throwable Occured " + e.getMessage(), e);
		}

		// return super.preHandle(request, response, handler);
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	public boolean checkskipEntityIdURLs(String url) {
		boolean result = false;
		for (String skipUrl : skipEntityIdUrls) {
			if (url.contains(skipUrl)) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		String invokeUrl = request.getRequestURI().toString();
		try {
			long startTime = (Long) request.getAttribute("startTime");
			long timeTaken = (System.currentTimeMillis() - startTime);

			if (utils.validateFiled(invokeUrl)) {
				invokeUrl = invokeUrl.substring(invokeUrl.lastIndexOf("/"), invokeUrl.length());
			}

			if (timeTaken > 400) {

				MDC.put(Constants.appLogName, Constants.mtrLogValue);

				log.warn(
						"------------------------------------------------------------------------------------------------");
				log.warn("Request URL :- {} Time Taken For MVC Call :- {} IP :- {}", invokeUrl, timeTaken,
						request.getRemoteAddr());

				log.warn(
						"------------------------------------------------------------------------------------------------");

				MDC.put(Constants.appLogName, Constants.appLogValue);
			} else {
				if (!invokeUrl.contains(".")) {
					MDC.put(Constants.appLogName, Constants.mtrLogValue);
					log.info("Request URL :- {} Time Taken For MVC Call :- {} IP :- {}", invokeUrl, timeTaken,
							request.getRemoteAddr());
					MDC.put(Constants.appLogName, Constants.appLogValue);
				}
			}
		} catch (Throwable e) {
			log.error("Throwable Occured " + e.getMessage(), e);
		} finally {
			MDC.clear();
		}
	}
}
