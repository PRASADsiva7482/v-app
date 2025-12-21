package com.va.v.v_app.config.utils;

public interface Constants {

	final String jwt_secret = "javainuse";

	Integer SUCCESS = 0;
	Integer GENERAL_FAILURE = 1;
	Integer UNKNOWN_FAILURE = 101;

	Integer NOT_FOUND = 404;
	Integer USER_BLOCKED = 101;
	Integer USER_FORCE_BLOCKED = 102;
	Integer USER_FIRST_TIME_BLOCKED = 103;
	Integer USER_TERMINATED = 104;
	Integer USER_PASSWORD_EXPIRED = 105;
	Integer USER_EXPIRED = 106;

	final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
	final String DATE_FORMAT_NO_TIME = "dd-MM-yyyy";
	final String REQUEST_DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

	// newly Added
	final String REQUEST_DATE_FORMAT2 = "MM/dd/yyyy";
	final String yyyyHMMdd = "yyyy-MM-dd";
	final String OM_REQUEST_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	final String mmddyyyy = "MM/dd/yyyy";
	final String mmddyyyyHHmmss = "MM/dd/yyyy HH:mm:ss";
	final String billingDateFormat2 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	final String billingDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	final String ngDateFormat = "yyyy-MM-dd HH:mm:ss";
	final String ESB_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	String defaultSize = "10";
	String defaultDownloadSize = "1000";
	String defaultMaxSize = "900";
	String defaultSorting = "desc";
	String defaultMvnoId = "200";
	String defaultSource = "CRM";
	String defaultLanguageId = "1";

	String systemFailedErrorMessage = "Unable to process request Inernal Server Error";
	String unauthorizedErrorMessage = "UnAuthorized";

	final String cdrSeparator = "|";
	final String cdrDefaultValue = "NA";

	final String userIdHeader = "userId";
	final String teamIdHeader = "teamId";
	final String sessionIdHeader = "sessionId";
	final String txnIdHeader = "txnId";
	final String userNameHeader = "userName";
	final String userFullNameHeader = "userFullName";
	final String entityIdHeader = "entityId";
	final String dynamicSearchHeader = "filter";
	final String languageIdHeader = "languageId";
	final String restrictedAccessHeader = "isSpecialCustomer";
	final String uniqueIdForModifyDelete = "id";
	final String incomingPacketHeader = "request";
	final String apiHeader = "X-api-key";
	final String apiRateLimitHeader = "apiRateLimit";
	final String apiRateLimitPerSecondsHeader = "apiRateLimitPerSeconds";
	final String fileNameHeader = "fileName";
	final String channelId = "1";
	final String resultCountHeader = "X-Result-Count";
	final String totalCountHeader = "X-Total-Count";
	final String bsChannelIdHeader = "channel-id";
	final String bsEntityIdHeader = "entity-id";
	final String bsReqIdHeader = "request-id";
	final String bsTxnIdHeader = "transaction-id";
	final String bsSessionIdHeader = "session-id";
	final String functionIdHeader = "functionIds";
	final String appLogName = "appTag";
	final String appLogValue = "APP";
	final String mtrLogValue = "MTR";
	final String userEmail = "emailID";
	final String groupNames = "Groupnames";

	final String authTypeHeader = "authType";
	final String biometricIdHeader = "biometricId";

	public static final String SOURCE = "source";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String AUTHORIZATION_HEADER_RESPONSE = "xe-authorization";
	public static final String DEFAULT_INCLUDE_PATTERN = "/api/.*";
	public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;
	public static final String Com_userEmail = "useremail";
	public static final String Com_X_Trace_Id = "X-Trace-Id";
	public static final String Com_username = "username";

	final String ACTIVE_STATUS = "0";
	final String IN_ACTIVE_STATUS = "1";

	final String ADDRESS_TYPE_PERMANENT = "0";
	final String ADDRESS_TYPE_RESIDENTIAL = "1";
	final String ADDRESS_TYPE_BILLING = "2";

	final String SUBSCRIBER_LEVEL_PROFILE = "0";

	final int assetCount = 5;

	final String BILL_INVOICE_STATUS = "0";
	final String DEFAULT_STATUS_CODE = "0";
	public static final String INTER_MICRO_SERVICE_COMM = "X-UnAuth-Request";

	public static final String crm_db_instance = "crm-db-instance";
	public static final String bill_db_instance = "bill-db-instance";
	public static final String initial_db_name = "initial";
}
