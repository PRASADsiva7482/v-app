# HttpCallerService - Query Parameters Support

## ✨ New Feature: Query Parameters

All HTTP methods now support query parameters! You can pass URL query parameters as a Map for cleaner, safer code.

## 📖 Usage Examples

### Before (Manual URL Building)
```java
// ❌ Old way - manual string concatenation, error-prone
String url = "https://api.example.com/users?page=1&size=10&sort=name";
httpCallerService.get(url, headers, UserResponse.class);

// ❌ Potential issues:
// - Forgot to URL encode values
// - Hard to maintain
// - Error-prone with special characters
```

### After (Using Params Map)
```java
// ✅ New way - clean, safe, automatic URL encoding
Map<String, String> params = Map.of(
    "page", "1",
    "size", "10",
    "sort", "name"
);
httpCallerService.get("https://api.example.com/users", params, headers, UserResponse.class);

// ✅ Benefits:
// - Automatic URL encoding
// - Type-safe
// - Easier to read and maintain
// - Handles special characters automatically
```

## 🚀 All Methods Support Params

### GET with Params
```java
// Basic GET with query parameters
Map<String, String> params = Map.of(
    "userId", "12345",
    "includeDetails", "true"
);

UserResponse user = httpCallerService.get(
    "https://api.example.com/user",
    params,              // Query parameters
    headers,             // HTTP headers
    UserResponse.class   // Response type
);

// Generates: https://api.example.com/user?userId=12345&includeDetails=true
```

### POST with Params
```java
// POST with query parameters (less common, but supported)
Map<String, String> params = Map.of("async", "true");

CreateUserRequest requestBody = new CreateUserRequest("John", "john@example.com");

UserResponse response = httpCallerService.post(
    "https://api.example.com/users",
    params,              // Query parameters
    requestBody,         // Request body
    headers,             // HTTP headers
    UserResponse.class   // Response type
);

// Generates: https://api.example.com/users?async=true
// Body: {"name":"John","email":"john@example.com"}
```

### PUT with Params
```java
Map<String, String> params = Map.of(
    "validate", "true",
    "notify", "false"
);

UpdateUserRequest updateBody = new UpdateUserRequest("Jane Doe");

UserResponse response = httpCallerService.put(
    "https://api.example.com/users/12345",
    params,              // Query parameters
    updateBody,          // Request body
    headers,             // HTTP headers
    UserResponse.class   // Response type
);

// Generates: https://api.example.com/users/12345?validate=true&notify=false
```

### DELETE with Params
```java
Map<String, String> params = Map.of(
    "cascade", "true",
    "reason", "user requested"
);

DeleteResponse response = httpCallerService.delete(
    "https://api.example.com/users/12345",
    params,              // Query parameters
    headers,             // HTTP headers
    DeleteResponse.class // Response type
);

// Generates: https://api.example.com/users/12345?cascade=true&reason=user+requested
// Note: Spaces automatically URL encoded
```

### PATCH with Params
```java
Map<String, String> params = Map.of("fields", "name,email");

Map<String, Object> patchBody = Map.of("name", "Updated Name");

UserResponse response = httpCallerService.patch(
    "https://api.example.com/users/12345",
    params,              // Query parameters
    patchBody,           // Request body (partial update)
    headers,             // HTTP headers
    UserResponse.class   // Response type
);
```

### GET Raw with Params
```java
Map<String, String> params = Map.of(
    "format", "json",
    "pretty", "true"
);

String rawResponse = httpCallerService.getRaw(
    "https://api.example.com/data",
    params,   // Query parameters
    headers   // HTTP headers
);

// Returns raw string response
```

### POST Raw with Params
```java
Map<String, String> params = Map.of("debug", "true");

String requestJson = "{\"key\":\"value\"}";

String rawResponse = httpCallerService.postRaw(
    "https://api.example.com/webhook",
    params,      // Query parameters
    requestJson, // Request body
    headers      // HTTP headers
);
```

## 🔧 Advanced Examples

### Dynamic Parameters
```java
// Build params dynamically
Map<String, String> params = new HashMap<>();
params.put("page", String.valueOf(currentPage));
params.put("size", String.valueOf(pageSize));

if (sortField != null) {
    params.put("sort", sortField);
}

if (filterActive) {
    params.put("status", "active");
}

List<User> users = httpCallerService.get(
    "https://api.example.com/users",
    params,
    headers,
    UserListResponse.class
);
```

### Search with Multiple Filters
```java
Map<String, String> searchParams = Map.of(
    "q", "john doe",              // Search query
    "city", "New York",           // Filter by city
    "age_min", "25",              // Min age
    "age_max", "40",              // Max age
    "sort", "relevance",          // Sort order
    "page", "1",                  // Page number
    "limit", "20"                 // Results per page
);

SearchResults results = httpCallerService.get(
    "https://api.example.com/search",
    searchParams,
    headers,
    SearchResults.class
);

// Generates: https://api.example.com/search?q=john+doe&city=New+York&age_min=25&...
```

### Pagination Example
```java
public List<User> fetchAllUsers() throws IOException {
    List<User> allUsers = new ArrayList<>();
    int page = 1;
    boolean hasMore = true;
    
    while (hasMore) {
        Map<String, String> params = Map.of(
            "page", String.valueOf(page),
            "size", "100"
        );
        
        PagedResponse<User> response = httpCallerService.get(
            "https://api.example.com/users",
            params,
            headers,
            new TypeReference<PagedResponse<User>>() {}
        );
        
        allUsers.addAll(response.getData());
        hasMore = response.hasNextPage();
        page++;
    }
    
    return allUsers;
}
```

### Special Characters Handling
```java
// Special characters are automatically URL encoded
Map<String, String> params = Map.of(
    "search", "hello world!",     // Spaces encoded
    "filter", "status:active",    // Colons encoded
    "email", "user@example.com",  // @ encoded
    "path", "/folder/file.txt"    // Slashes encoded
);

String response = httpCallerService.getRaw(
    "https://api.example.com/query",
    params,
    headers
);

// Generates: https://api.example.com/query?search=hello+world%21&filter=status%3Aactive&...
```

## 🔄 Backward Compatibility

### Old Methods Still Work!
```java
// ✅ Old way still works (no breaking changes)
String url = "https://api.example.com/users?page=1&size=10";
httpCallerService.get(url, headers, UserResponse.class);

// ✅ New way with params (recommended)
Map<String, String> params = Map.of("page", "1", "size", "10");
httpCallerService.get("https://api.example.com/users", params, headers, UserResponse.class);
```

### You Can Mix Both!
```java
// URL already has some params, add more via Map
String baseUrl = "https://api.example.com/users?active=true";

Map<String, String> additionalParams = Map.of(
    "page", "1",
    "sort", "name"
);

// Both params will be included
httpCallerService.get(baseUrl, additionalParams, headers, UserResponse.class);

// Generates: https://api.example.com/users?active=true&page=1&sort=name
```

## 📋 Method Signatures

### All Available Overloads

```java
// GET Methods
<T> T get(String url, Map<String, String> headers, Class<T> responseType)
<T> T get(String url, Map<String, String> params, Map<String, String> headers, Class<T> responseType)

// POST Methods  
<T> T post(String url, Object requestBody, Map<String, String> headers, Class<T> responseType)
<T> T post(String url, Map<String, String> params, Object requestBody, Map<String, String> headers, Class<T> responseType)

// PUT Methods
<T> T put(String url, Object requestBody, Map<String, String> headers, Class<T> responseType)
<T> T put(String url, Map<String, String> params, Object requestBody, Map<String, String> headers, Class<T> responseType)

// DELETE Methods
<T> T delete(String url, Map<String, String> headers, Class<T> responseType)
<T> T delete(String url, Map<String, String> params, Map<String, String> headers, Class<T> responseType)

// PATCH Methods
<T> T patch(String url, Object requestBody, Map<String, String> headers, Class<T> responseType)
<T> T patch(String url, Map<String, String> params, Object requestBody, Map<String, String> headers, Class<T> responseType)

// Raw Methods
String getRaw(String url, Map<String, String> headers)
String getRaw(String url, Map<String, String> params, Map<String, String> headers)

String postRaw(String url, Object requestBody, Map<String, String> headers)
String postRaw(String url, Map<String, String> params, Object requestBody, Map<String, String> headers)
```

## ✅ Benefits

1. **🔒 URL Encoding**: Automatic URL encoding of special characters
2. **📝 Readable**: Cleaner, more maintainable code
3. **🛡️ Type Safe**: Map-based, less error-prone
4. **🔄 Backward Compatible**: Old methods still work
5. **🧪 Testable**: Easier to mock and test
6. **📚 Self-Documenting**: Clear parameter names

## 🎯 Best Practices

### DO ✅
```java
// Use params Map for query parameters
Map<String, String> params = Map.of("page", "1", "size", "10");
httpCallerService.get(baseUrl, params, headers, ResponseClass.class);

// Use descriptive parameter names
Map<String, String> filters = Map.of(
    "startDate", "2024-01-01",
    "endDate", "2024-12-31",
    "status", "completed"
);
```

### DON'T ❌
```java
// Don't manually concatenate URLs (error-prone)
String url = baseUrl + "?page=" + page + "&size=" + size;

// Don't forget URL encoding (causes bugs)
String url = baseUrl + "?search=" + searchTerm; // Breaks with spaces/special chars
```

## 📊 Performance

- **URL Building**: O(n) where n is number of parameters
- **URL Encoding**: Built-in Java URLEncoder (fast)
- **Memory**: Minimal overhead (StringBuilder-based)
- **Thread Safety**: ✅ Yes (no shared mutable state)

## 🧪 Testing Example

```java
@Test
void testGetWithParams() throws IOException {
    Map<String, String> params = Map.of(
        "userId", "123",
        "includeDetails", "true"
    );
    
    Map<String, String> headers = Map.of(
        "Authorization", "Bearer token"
    );
    
    UserResponse response = httpCallerService.get(
        "https://api.example.com/user",
        params,
        headers,
        UserResponse.class
    );
    
    assertNotNull(response);
    assertEquals("123", response.getUserId());
}
```

## 📌 Summary

**All HTTP methods now support query parameters:**
- ✅ GET, POST, PUT, DELETE, PATCH
- ✅ getRaw, postRaw
- ✅ Automatic URL encoding
- ✅ Backward compatible
- ✅ Clean, maintainable code

**Use params Map for cleaner, safer API calls!** 🚀
