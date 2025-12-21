# Log4j2 Configuration Added

## Changes Made

### 1. Added log4j2.xml Configuration
- **Location**: `src/main/resources/log4j2.xml`
- **Features**:
  - Multiple log appenders (Console, File, Error, Audit, SQL)
  - Async logging for better performance
  - PII masking for sensitive data (passwords, tokens, secrets)
  - Rolling file policies with compression
  - Automatic log cleanup (30 days retention)
  - Colored console output
  - MDC context support (txnId, userId, sessionId, entityId)

### 2. Log Files Created

All logs are stored in `./logs/` directory:

1. **v-app.log** - Main application log (all levels)
2. **v-app-error.log** - Error and Fatal logs only
3. **v-app-audit.log** - Audit trail (authentication, user actions)
4. **v-app-sql.log** - Database queries and SQL statements

Archived logs are compressed and stored in `./logs/archive/`

### 3. Log Levels Configured

- **com.va.v.v_app**: DEBUG (your application code)
- **Keycloak**: INFO (authentication logs)
- **Spring Framework**: INFO
- **Spring Web**: DEBUG
- **Spring Security**: DEBUG
- **Hibernate SQL**: DEBUG (queries logged to sql log)
- **Hibernate Type**: TRACE (parameter binding)
- **HikariCP**: INFO (connection pool)

### 4. MDC Context Fields

The following fields are automatically captured from MDCFilter:

- `txnId` - Transaction ID (auto-generated UUID)
- `userId` - User ID from header
- `userName` - Username from header
- `sessionId` - Session ID from header
- `entityId` - Entity/Tenant ID from header

### 5. PII Masking

Sensitive data is automatically masked in logs:
- Passwords: `password=secret123` → `password=*****`
- Tokens: `token=abc123xyz` → `token=*****`
- Secrets: `secret=mysecret` → `secret=*****`
- API Keys: `apiKey=key123` → `apiKey=*****`

### 6. Log Pattern Format

```
[V-APP] [2025-12-16T17:15:00.123+0530] [http-nio-2000-exec-1] [com.va.v.v_app.service.CheckLoginServiceImpl] [125] [uuid-txn-id] [user-123] [session-456] [entity-789] [INFO] : User logged in successfully
```

Components:
- Application name
- Timestamp (ISO 8601)
- Thread name
- Class name (shortened)
- Line number
- Transaction ID
- User ID
- Session ID
- Entity ID
- Log level
- Message

### 7. Performance Features

- **Async Loggers**: All loggers are async for non-blocking I/O
- **RandomAccessFile**: Faster file I/O than standard FileAppender
- **Compression**: Old logs are gzipped to save space
- **Auto-cleanup**: Logs older than 30 days are deleted automatically

### 8. Log Rotation

- **Time-based**: Hourly rotation for main log
- **Size-based**: 100MB max size per file
- **Max files**: 30 archived files retained
- **Compression**: Automatic gzip compression

## Usage Examples

### In Your Code

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MyService {
    
    public void doSomething() {
        log.info("Processing request");
        log.debug("Debug details: {}", someVariable);
        log.error("Error occurred", exception);
    }
}
```

### MDC Context

MDC fields are automatically populated by MDCFilter. You can also add custom fields:

```java
import org.slf4j.MDC;

MDC.put("customField", "customValue");
log.info("This log will include customField");
MDC.remove("customField");
```

## Log File Locations

```
./logs/
├── v-app.log                    # Current main log
├── v-app-error.log              # Current error log
├── v-app-audit.log              # Current audit log
├── v-app-sql.log                # Current SQL log
└── archive/
    ├── v-app-2025-12-16-10-0.log.gz
    ├── v-app-2025-12-16-11-0.log.gz
    ├── v-app-error-2025-12-16-0.log.gz
    └── ...
```

## Viewing Logs

### Real-time monitoring
```bash
# Main log
tail -f ./logs/v-app.log

# Error log
tail -f ./logs/v-app-error.log

# SQL queries
tail -f ./logs/v-app-sql.log
```

### Search logs
```bash
# Find all ERROR logs
grep "ERROR" ./logs/v-app.log

# Find logs for specific transaction
grep "uuid-txn-id" ./logs/v-app.log

# Find logs for specific user
grep "user-123" ./logs/v-app.log
```

### View compressed archives
```bash
zcat ./logs/archive/v-app-2025-12-16-10-0.log.gz | less
```

## Configuration Customization

To customize logging, edit `src/main/resources/log4j2.xml`:

### Change log level
```xml
<AsyncLogger name="com.va.v.v_app" level="info">
```

### Change log path
```xml
<Property name="log-path">/var/log/v-app</Property>
```

### Change retention period
```xml
<IfLastModified age="90d"/>  <!-- Keep for 90 days -->
```

### Change file size limit
```xml
<SizeBasedTriggeringPolicy size="200MB"/>
```

## Benefits

1. **Performance**: Async logging doesn't block application threads
2. **Debugging**: Detailed logs with context (txnId, userId, etc.)
3. **Security**: PII masking protects sensitive data
4. **Maintenance**: Automatic rotation and cleanup
5. **Troubleshooting**: Separate error and SQL logs for focused analysis
6. **Audit**: Dedicated audit log for compliance
7. **Production-ready**: Optimized for high-throughput applications

## Notes

- Log4j2 is already configured and ready to use
- No code changes needed - just use `@Slf4j` annotation
- MDC context is automatically populated by MDCFilter
- Logs are created in `./logs/` relative to application directory
- In production, update `log-path` property to absolute path
- Monitor disk space if application generates high log volume

## Comparison with Reference Project

The configuration is based on the reference project but improved:

✅ **Kept**: PII masking, MDC context, async logging, rolling policies  
✅ **Improved**: Cleaner patterns, better organization, modern Log4j2 features  
✅ **Added**: Separate error log, audit log, SQL log, auto-cleanup  
✅ **Removed**: CDR-specific loggers (team_management, discount_profile)  
✅ **Simplified**: Removed overly complex regex patterns, kept essential masking  

The configuration is production-ready and optimized for your V2 application!
