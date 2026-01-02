# Complete Guide to Creating Performance Tests

This comprehensive guide walks you through creating performance tests using the framework's code-first approach. Whether you're testing simple APIs or complex workflows, you'll find step-by-step instructions and examples here.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start - Basic Test](#quick-start---basic-test)
3. [Understanding the Test Structure](#understanding-the-test-structure)
4. [Step-by-Step Test Creation](#step-by-step-test-creation)
5. [Working with Controllers](#working-with-controllers)
6. [Adding Assertions](#adding-assertions)
7. [Multi-Thread Group Tests](#multi-thread-group-tests)
8. [Advanced Patterns](#advanced-patterns)
9. [Configuration](#configuration)
10. [Running Tests](#running-tests)
11. [Troubleshooting](#troubleshooting)
12. [Examples Library](#examples-library)

---

## Prerequisites

### Required Knowledge

- Basic Java programming
- Understanding of HTTP/REST APIs
- Familiarity with JUnit 5 testing

### Environment Setup

1. ✅ Java 17+ installed
2. ✅ Maven installed
3. ✅ IDE (IntelliJ IDEA or Eclipse) configured
4. ✅ Framework cloned and dependencies resolved (`mvn clean install`)

### Configuration File

Update `src/main/resources/config.properties` with your target environment:

```properties
# Target API Configuration
target.domain=api.example.com
target.path=/api/v1

# Global Headers (Applied to ALL requests)
global.header.Authorization=Bearer YOUR_TOKEN_HERE
global.header.Content-Type=application/json
global.header.Accept=application/json

# Default Thread Group Settings
thread.count=10
loop.count=5
ramp.up=2

# Assertion Defaults
assertion.expected.response.codes=200,201,202
assertion.max.duration.ms=3000

# Reporting
report.path=report/performance-tests/test-report.html
report.title=Performance Test Report
report.name=API Performance Tests
```

---

## Quick Start - Basic Test

Here's the simplest possible test to get you started:

```java
package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import com.perf.reporting.ExtentReportListener;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ExtentReportListener.class)
class MyFirstTest extends BasePerformanceTest {
    
    @Test
    void testGetUsers() {
        // 1. Start the test suite
        startSuite("My First Performance Test");
        
        // 2. Create a thread group (10 users, 2 sec ramp-up, 5 loops)
        TestContext ctx = createSuiteThreadGroup("API Users", 10, 2, 5);
        
        // 3. Create an HTTP GET request
        HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(
            "GET /users",           // Sampler name
            "gorest.co.in",        // Domain
            "/public/v2/users",    // Path
            "GET"                  // HTTP method
        );
        
        // 4. Add the sampler to the thread group
        HashTree samplerTree = ctx.getThreadGroupTree().add(sampler);
        
        // 5. Add assertions to validate the response
        addResponseCodeAssertion(samplerTree, "200");
        addDurationAssertion(samplerTree, 2000); // Max 2 seconds
        
        // 6. Run the test
        runSuite();
    }
}
```

**Expected Result:**

- ✅ Test executes 10 concurrent users
- ✅ Each user makes 5 requests
- ✅ Validates HTTP 200 response
- ✅ Validates response time < 2000ms
- ✅ HTML report generated at `report/performance-tests/test-report.html`

---

## Understanding the Test Structure

### Test Lifecycle

```
@Test Method
    │
    ├─► startSuite("Name")           // Initialize test plan
    │       └─► Creates TestPlan
    │       └─► Attaches global headers from config
    │
    ├─► createSuiteThreadGroup(...)  // Add user load profile
    │       └─► Creates ThreadGroup
    │       └─► Returns TestContext
    │
    ├─► Add Controllers (optional)   // Organize samplers
    │       └─► TransactionController
    │       └─► SimpleController
    │
    ├─► Add Samplers                 // Define requests
    │       └─► HTTPSamplerProxy
    │
    ├─► Add Assertions               // Validate responses
    │       └─► ResponseCodeAssertion
    │       └─► DurationAssertion
    │
    └─► runSuite()                   // Execute test plan
            └─► JMeter runs test
            └─► Results captured
            └─► Report generated
            └─► Assertions checked
            └─► Test passes or fails
```

### Key Classes to Import

```java
// Framework Base
import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;

// JMeter Components
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;

// Reporting
import com.perf.reporting.ExtentReportListener;

// JUnit
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
```

---

## Step-by-Step Test Creation

### Step 1: Create Test Class

Create a new Java class in `src/test/java/com/perf/tests/`:

```java
package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.reporting.ExtentReportListener;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ExtentReportListener.class)  // Required for ExtentReports integration
class ProductAPITest extends BasePerformanceTest {
    
    // Your test methods go here
}
```

**Important Notes:**

- ✅ **Always extend** `BasePerformanceTest`
- ✅ **Always add** `@ExtendWith(ExtentReportListener.class)` annotation
- ✅ Name your class descriptively (e.g., `UserAPITest`, `CheckoutFlowTest`)

---

### Step 2: Write Test Method

Add a `@Test` method to your class:

```java
@Test
void testCreateProduct() {
    // Test implementation here
}
```

**Test Naming Convention:**

- Use descriptive names: `testCreateProduct()`, `testUserRegistrationFlow()`
- Avoid generic names: `test1()`, `myTest()`

---

### Step 3: Initialize Test Suite

Every test must start with `startSuite()`:

```java
@Test
void testCreateProduct() {
    startSuite("Product Creation Load Test");
    
    // ... rest of test
}
```

**What happens during `startSuite()`:**

1. Creates a new JMeter TestPlan
2. Loads global headers from `config.properties`
3. Attaches headers to test plan (applied to all requests)
4. Initializes thread-local context

---

### Step 4: Create Thread Group

Define user load profile with `createSuiteThreadGroup()`:

```java
TestContext ctx = createSuiteThreadGroup(
    "Product Users",  // Thread group name (shown in reports)
    10,               // Number of threads (concurrent users)
    2,                // Ramp-up period in seconds
    5                 // Loop count (iterations per user)
);

HashTree threadTree = ctx.getThreadGroupTree();
```

**Parameters Explained:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| **name** | Descriptive name for this user group | "API Users", "Browser Users" |
| **threads** | Number of concurrent virtual users | 10, 50, 100 |
| **rampUp** | Time in seconds to start all threads | 2, 5, 10 |
| **loops** | Number of iterations each thread executes | 1, 5, 10 |

**Thread Calculation:**

- **Total Requests** = threads × loops × (number of samplers)
- **Example:** 10 threads × 5 loops × 3 samplers = 150 total requests

**Ramp-Up Example:**

```
threads = 10, rampUp = 10
│
├─ 0s:  Thread 1 starts
├─ 1s:  Thread 2 starts
├─ 2s:  Thread 3 starts
├─ ...
└─ 10s: Thread 10 starts
```

---

### Step 5: Add HTTP Samplers

Create HTTP requests using `TestPlanFactory.createHttpSampler()`:

```java
HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(
    "POST /products",              // Sampler name (shown in reports)
    "api.example.com",             // Domain (without protocol)
    "/api/v1/products",            // Path
    "POST"                         // HTTP Method (GET, POST, PUT, DELETE)
);

HashTree samplerTree = threadTree.add(sampler);
```

**Supported HTTP Methods:**

- `GET` - Retrieve data
- `POST` - Create new resource
- `PUT` - Update existing resource
- `DELETE` - Remove resource
- `PATCH` - Partial update

**Example for Different Methods:**

```java
// GET Request
HTTPSamplerProxy getUsers = TestPlanFactory.createHttpSampler(
    "GET /users", "api.example.com", "/api/users", "GET");

// POST Request
HTTPSamplerProxy createUser = TestPlanFactory.createHttpSampler(
    "POST /users", "api.example.com", "/api/users", "POST");

// PUT Request
HTTPSamplerProxy updateUser = TestPlanFactory.createHttpSampler(
    "PUT /users/123", "api.example.com", "/api/users/123", "PUT");

// DELETE Request
HTTPSamplerProxy deleteUser = TestPlanFactory.createHttpSampler(
    "DELETE /users/123", "api.example.com", "/api/users/123", "DELETE");
```

**Important:** The HashTree returned by `.add()` is used for attaching assertions.

---

### Step 6: Add Assertions

Validate responses by adding assertions to the sampler:

#### Response Code Assertion

Validates HTTP status codes:

```java
// Single expected code
addResponseCodeAssertion(samplerTree, "200");

// Multiple accepted codes
addResponseCodeAssertion(samplerTree, "200", "201", "202");
```

**Common HTTP Status Codes:**

- `200` - OK (successful GET)
- `201` - Created (successful POST)
- `204` - No Content (successful DELETE)
- `301` - Moved Permanently (redirect)
- `400` - Bad Request (client error)
- `401` - Unauthorized (authentication required)
- `404` - Not Found (resource doesn't exist)
- `500` - Internal Server Error (server error)

#### Duration Assertion

Validates response time (performance SLA):

```java
// Max response time: 2000ms
addDurationAssertion(samplerTree, 2000);

// Named duration assertion (shows in reports)
addDurationAssertion(samplerTree, "Product Creation SLA", 2000);
```

**When to Use:**

- ✅ Use for performance-critical endpoints
- ✅ Set realistic thresholds based on requirements
- ✅ Use named assertions for important SLAs

---

### Step 7: Execute Test

Run the test with `runSuite()`:

```java
runSuite();
```

**What happens during `runSuite()`:**

1. Test plan is validated
2. JMeter engine starts
3. All thread groups execute concurrently
4. Sample results are collected
5. ExtentReport is generated
6. Assertions are checked
7. If any assertion fails → JUnit test **FAILS**
8. If all assertions pass → JUnit test **PASSES**

---

## Working with Controllers

Controllers organize samplers into logical groups and provide transaction timing.

### Transaction Controller

Groups samplers and measures cumulative time:

```java
// Add transaction controller
HashTree txn = addTransactionController(
    threadTree,              // Parent tree
    "User Registration",     // Transaction name
    true                     // Generate parent sampler (recommended)
);

// Add samplers to transaction
HTTPSamplerProxy step1 = TestPlanFactory.createHttpSampler(
    "Check Username", "api.example.com", "/api/users/check", "GET");
HashTree step1Tree = txn.add(step1);
addResponseCodeAssertion(step1Tree, "200");

HTTPSamplerProxy step2 = TestPlanFactory.createHttpSampler(
    "Create User", "api.example.com", "/api/users", "POST");
HashTree step2Tree = txn.add(step2);
addResponseCodeAssertion(step2Tree, "201");
```

**When to Use TransactionController:**

- ✅ Group related steps (e.g., login flow, checkout process)
- ✅ Measure end-to-end timing of business transactions
- ✅ Generate meaningful transaction names in reports

**Transaction Timing:**

```
Transaction: User Registration
├─ Check Username: 100ms
└─ Create User: 200ms
Total Transaction Time: 300ms
```

---

### Simple Controller

Groups samplers for organization without timing:

```java
HashTree simpleCtrl = addSimpleController(threadTree, "Product Operations");

// Add multiple samplers
simpleCtrl.add(createSampler1);
simpleCtrl.add(createSampler2);
simpleCtrl.add(createSampler3);
```

**When to Use SimpleController:**

- ✅ Organize related samplers logically
- ✅ Group samplers without measuring cumulative time
- ✅ Better report structure

---

### Nested Controllers

Controllers can be nested for complex hierarchies:

```java
// Outer transaction
HashTree outerTxn = addTransactionController(threadTree, "E-Commerce Flow", true);

// Inner transaction 1
HashTree browseTxn = addTransactionController(outerTxn, "Browse Products", true);
browseTxn.add(listProductsSampler);
browseTxn.add(viewProductSampler);

// Inner transaction 2
HashTree checkoutTxn = addTransactionController(outerTxn, "Checkout", true);
checkoutTxn.add(addToCartSampler);
checkoutTxn.add(checkoutSampler);
```

**Hierarchy:**

```
E-Commerce Flow (Total: 800ms)
├─ Browse Products (300ms)
│   ├─ List Products (150ms)
│   └─ View Product (150ms)
└─ Checkout (500ms)
    ├─ Add to Cart (200ms)
    └─ Checkout (300ms)
```

---

## Adding Assertions

### Available Assertion Types

#### 1. Response Code Assertion

```java
addResponseCodeAssertion(samplerTree, "200");
addResponseCodeAssertion(samplerTree, "200", "201");
```

**Use Cases:**

- Verify successful responses
- Accept multiple valid codes
- Validate redirect codes

#### 2. Duration Assertion

```java
addDurationAssertion(samplerTree, 2000);
addDurationAssertion(samplerTree, "Login SLA", 1500);
```

**Use Cases:**

- Enforce performance SLAs
- Identify slow responses
- Performance regression testing

---

### Assertion Best Practices

✅ **DO:**

- Add assertions to every sampler
- Use meaningful names for duration assertions
- Set realistic thresholds based on requirements
- Test both success and edge cases

❌ **DON'T:**

- Skip assertions (tests without assertions don't validate correctness)
- Use overly strict thresholds that fail unnecessarily
- Ignore assertion failures in reports

---

## Multi-Thread Group Tests

Simulate mixed user loads with multiple thread groups:

```java
@Test
void testMixedWorkload() {
    startSuite("Mixed Load Test");
    
    // Thread Group 1: Browser Users
    TestContext browsers = createSuiteThreadGroup("Browser Users", 20, 5, 10);
    HashTree browserTree = browsers.getThreadGroupTree();
    // Add browser-specific samplers
    
    // Thread Group 2: API Users
    TestContext apiUsers = createSuiteThreadGroup("API Users", 50, 10, 20);
    HashTree apiTree = apiUsers.getThreadGroupTree();
    // Add API-specific samplers
    
    // Thread Group 3: Admin Users
    TestContext admins = createSuiteThreadGroup("Admin Users", 2, 1, 5);
    HashTree adminTree = admins.getThreadGroupTree();
    // Add admin-specific samplers
    
    runSuite(); // All thread groups execute concurrently
}
```

**Use Cases:**

- **Different user types** (customers, admins, guests)
- **Different load profiles** (peak hours vs. normal traffic)
- **Different workflows** (browsing vs. purchasing)

**Execution:**

```
Time →
0s─────────────────────────────────────────►
│
├─ Browser Users (20 threads, 5s ramp-up)
│  ████████████████████████
│
├─ API Users (50 threads, 10s ramp-up)
│  ████████████████████████████████████
│
└─ Admin Users (2 threads, 1s ramp-up)
   ██
```

---

## Advanced Patterns

### Pattern 1: Sequential Steps in Transaction

```java
HashTree txn = addTransactionController(threadTree, "User Journey", true);

// Step 1: Login
HTTPSamplerProxy login = TestPlanFactory.createHttpSampler(
    "POST /login", "api.example.com", "/api/auth/login", "POST");
HashTree loginTree = txn.add(login);
addResponseCodeAssertion(loginTree, "200");

// Step 2: Get Profile
HTTPSamplerProxy profile = TestPlanFactory.createHttpSampler(
    "GET /profile", "api.example.com", "/api/users/me", "GET");
HashTree profileTree = txn.add(profile);
addResponseCodeAssertion(profileTree, "200");

// Step 3: Update Settings
HTTPSamplerProxy update = TestPlanFactory.createHttpSampler(
    "PUT /settings", "api.example.com", "/api/users/settings", "PUT");
HashTree updateTree = txn.add(update);
addResponseCodeAssertion(updateTree, "200");
addDurationAssertion(updateTree, "Settings Update SLA", 1000);
```

---

### Pattern 2: Multiple Assertions on Same Sampler

```java
HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(...);
HashTree samplerTree = threadTree.add(sampler);

// Multiple validations
addResponseCodeAssertion(samplerTree, "200", "201");
addDurationAssertion(samplerTree, "Response Time", 2000);
// Can add more custom assertions if needed
```

---

### Pattern 3: Realistic User Simulation

```java
@Test
void testRealisticUserBehavior() {
    startSuite("Realistic E-Commerce Test");
    
    TestContext users = createSuiteThreadGroup("Shoppers", 100, 30, 1);
    HashTree userTree = users.getThreadGroupTree();
    
    // 1. Browse products (all users)
    HashTree browseTxn = addTransactionController(userTree, "Browse", true);
    browseTxn.add(TestPlanFactory.createHttpSampler(
        "Homepage", "shop.com", "/", "GET"));
    browseTxn.add(TestPlanFactory.createHttpSampler(
        "Category", "shop.com", "/category/electronics", "GET"));
    browseTxn.add(TestPlanFactory.createHttpSampler(
        "Product Details", "shop.com", "/product/laptop-123", "GET"));
    
    // 2. Add to cart (~50% of users - use simple controller)
    HashTree cartCtrl = addSimpleController(userTree, "Shopping Cart");
    HTTPSamplerProxy addCart = TestPlanFactory.createHttpSampler(
        "Add to Cart", "shop.com", "/api/cart", "POST");
    HashTree cartTree = cartCtrl.add(addCart);
    addResponseCodeAssertion(cartTree, "200", "201");
    
    // 3. Checkout (~20% of users - subset of cart users)
    HashTree checkoutTxn = addTransactionController(userTree, "Checkout", true);
    HTTPSamplerProxy checkout = TestPlanFactory.createHttpSampler(
        "Complete Order", "shop.com", "/api/checkout", "POST");
    HashTree checkoutTree = checkoutTxn.add(checkout);
    addResponseCodeAssertion(checkoutTree, "200");
    addDurationAssertion(checkoutTree, "Checkout SLA", 3000);
    
    runSuite();
}
```

---

## Configuration

### Global Headers

Headers configured in `config.properties` are automatically applied to ALL requests:

```properties
# Applied to every HTTP request
global.header.Authorization=Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
global.header.Content-Type=application/json
global.header.Accept=application/json
global.header.User-Agent=PerformanceTestFramework/1.0
```

**Benefits:**

- ✅ DRY (Don't Repeat Yourself)
- ✅ Centralized authentication
- ✅ Easy to update tokens
- ✅ Applied at TestPlan level (efficient)

**No Code Required:**

```java
startSuite("My Test");  // Headers automatically attached!
```

---

### Thread Group Defaults

Set default values in `config.properties`:

```properties
thread.count=10
loop.count=5
ramp.up=2
```

Use in code:

```java
// Uses config defaults
TestContext ctx = createSuiteThreadGroup("Users", 
    getDefaultThreadCount(), 
    getDefaultRampUp(), 
    getDefaultLoopCount());

// Or override
TestContext ctx = createSuiteThreadGroup("Users", 50, 10, 20);
```

---

### Assertion Defaults

```properties
assertion.expected.response.codes=200,201,202
assertion.max.duration.ms=3000
```

---

## Running Tests

### From IDE

1. Right-click on test class or method
2. Select "Run 'testMethodName()'"
3. View results in IDE test runner
4. Open HTML report: `report/performance-tests/test-report.html`

---

### From Maven (Command Line)

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProductAPITest

# Run specific test method
mvn test -Dtest=ProductAPITest#testCreateProduct

# Run with custom thread count
mvn test -Dthread.count=50 -Dloop.count=10
```

---

### Viewing Reports

After test execution:

1. **HTML Report:** `report/performance-tests/test-report.html`
   - Open in browser
   - View summary statistics
   - Check detailed execution log
   - Review assertion failures

2. **JTL File:** `logs/test_result.jtl`
   - Raw JMeter output
   - Can be opened in JMeter GUI for analysis

---

## Troubleshooting

### Common Issues

#### Issue 1: Test Passes But Assertions Failed

**Symptom:** Test shows as PASSED but report shows failures

**Cause:** Old version before assertion failure detection

**Solution:** Update to latest framework version (has `hasFailures()` check)

---

#### Issue 2: "Test Failed: null" in Report

**Symptom:** Error message shows as "null"

**Cause:** Old version before error message extraction

**Solution:** Update to latest framework version (extracts from sub-results and cause chain)

---

#### Issue 3: Headers Not Applied

**Symptom:** Requests missing Authorization header

**Cause:** Headers not configured or typo in property name

**Solution:**

1. Check `config.properties` has `global.header.` prefix
2. Verify token value is correct
3. Ensure `startSuite()` is called

---

#### Issue 4:: Sampler Name Not Showing in Report

**Symptom:** Report shows generic names

**Cause:** Sampler not named properly

**Solution:** Always provide descriptive names:

```java
createHttpSampler("GET /users", ...);  // ✅ Good
createHttpSampler("sampler1", ...);     // ❌ Bad
```

---

#### Issue 5: Assertion Not Triggering

**Symptom:** Assertion doesn't fail even when response is wrong

**Cause:** Assertion added to wrong tree

**Solution:** Add assertion to sampler tree:

```java
HashTree samplerTree = threadTree.add(sampler);  // Get tree first
addResponseCodeAssertion(samplerTree, "200");    // Use sampler tree
```

---

## Examples Library

### Example 1: Simple GET Request

```java
@ExtendWith(ExtentReportListener.class)
class SimpleGetTest extends BasePerformanceTest {
    
    @Test
    void testListUsers() {
        startSuite("List Users Test");
        
        TestContext ctx = createSuiteThreadGroup("Users", 10, 2, 5);
        
        HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(
            "GET /users", "gorest.co.in", "/public/v2/users", "GET");
        
        HashTree tree = ctx.getThreadGroupTree().add(sampler);
        addResponseCodeAssertion(tree, "200");
        addDurationAssertion(tree, 2000);
        
        runSuite();
    }
}
```

---

### Example 2: POST Request with Transaction

```java
@ExtendWith(ExtentReportListener.class)
class CreateUserTest extends BasePerformanceTest {
    
    @Test
    void testCreateUser() {
        startSuite("Create User Test");
        
        TestContext ctx = createSuiteThreadGroup("API Users", 20, 5, 10);
        HashTree threadTree = ctx.getThreadGroupTree();
        
        HashTree txn = addTransactionController(threadTree, "User Creation", true);
        
        HTTPSamplerProxy createUser = TestPlanFactory.createHttpSampler(
            "POST /users", "gorest.co.in", "/public/v2/users", "POST");
        
        HashTree samplerTree = txn.add(createUser);
        addResponseCodeAssertion(samplerTree, "201");
        addDurationAssertion(samplerTree, "User Creation SLA", 1500);
        
        runSuite();
    }
}
```

---

### Example 3: Complex Workflow

```java
@ExtendWith(ExtentReportListener.class)
class ECommerceWorkflowTest extends BasePerformanceTest {
    
    @Test
    void testCompleteShoppingJourney() {
        startSuite("E-Commerce Complete Journey");
        
        TestContext ctx = createSuiteThreadGroup("Shoppers", 50, 10, 1);
        HashTree threadTree = ctx.getThreadGroupTree();
        
        // Transaction 1: Browse Phase
        HashTree browseTxn = addTransactionController(threadTree, "Browse Products", true);
        
        HTTPSamplerProxy homepage = TestPlanFactory.createHttpSampler(
            "Homepage", "shop.example.com", "/", "GET");
        HashTree homepageTree = browseTxn.add(homepage);
        addResponseCodeAssertion(homepageTree, "200");
        
        HTTPSamplerProxy category = TestPlanFactory.createHttpSampler(
            "Category Page", "shop.example.com", "/category/electronics", "GET");
        HashTree categoryTree = browseTxn.add(category);
        addResponseCodeAssertion(categoryTree, "200");
        
        HTTPSamplerProxy product = TestPlanFactory.createHttpSampler(
            "Product Details", "shop.example.com", "/product/laptop-123", "GET");
        HashTree productTree = browseTxn.add(product);
        addResponseCodeAssertion(productTree, "200");
        addDurationAssertion(productTree, "Product Page Load", 1500);
        
        // Transaction 2: Cart Phase
        HashTree cartTxn = addTransactionController(threadTree, "Shopping Cart", true);
        
        HTTPSamplerProxy addCart = TestPlanFactory.createHttpSampler(
            "Add to Cart", "shop.example.com", "/api/cart", "POST");
        HashTree cartTree = cartTxn.add(addCart);
        addResponseCodeAssertion(cartTree, "200", "201");
        
        HTTPSamplerProxy viewCart = TestPlanFactory.createHttpSampler(
            "View Cart", "shop.example.com", "/api/cart", "GET");
        HashTree viewCartTree = cartTxn.add(viewCart);
        addResponseCodeAssertion(viewCartTree, "200");
        
        // Transaction 3: Checkout Phase
        HashTree checkoutTxn = addTransactionController(threadTree, "Checkout Process", true);
        
        HTTPSamplerProxy checkout = TestPlanFactory.createHttpSampler(
            "Submit Order", "shop.example.com", "/api/checkout", "POST");
        HashTree checkoutTree = checkoutTxn.add(checkout);
        addResponseCodeAssertion(checkoutTree, "200");
        addDurationAssertion(checkoutTree, "Checkout SLA", 3000);
        
        HTTPSamplerProxy confirm = TestPlanFactory.createHttpSampler(
            "Order Confirmation", "shop.example.com", "/api/orders/confirm", "POST");
        HashTree confirmTree = checkoutTxn.add(confirm);
        addResponseCodeAssertion(confirmTree, "200");
        
        runSuite();
    }
}
```

---

### Example 4: Multi-Thread Group Load Test

```java
@ExtendWith(ExtentReportListener.class)
class MultiUserTypeTest extends BasePerformanceTest {
    
    @Test
    void testMixedUserLoad() {
        startSuite("Mixed User Type Load Test");
        
        // Regular users: Browse and search
        TestContext regularUsers = createSuiteThreadGroup("Regular Users", 80, 20, 10);
        HashTree regularTree = regularUsers.getThreadGroupTree();
        HTTPSamplerProxy browse = TestPlanFactory.createHttpSampler(
            "Browse", "shop.com", "/browse", "GET");
        HashTree browseTree = regularTree.add(browse);
        addResponseCodeAssertion(browseTree, "200");
        
        // Premium users: Browse, cart, checkout
        TestContext premiumUsers = createSuiteThreadGroup("Premium Users", 15, 5, 5);
        HashTree premiumTree = premiumUsers.getThreadGroupTree();
        HashTree premiumTxn = addTransactionController(premiumTree, "Premium Journey", true);
        // Add premium user samplers...
        
        // Admin users: Management operations
        TestContext admins = createSuiteThreadGroup("Admins", 5, 2, 3);
        HashTree adminTree = admins.getThreadGroupTree();
        HTTPSamplerProxy adminOp = TestPlanFactory.createHttpSampler(
            "Admin Dashboard", "shop.com", "/admin/dashboard", "GET");
        HashTree adminOpTree = adminTree.add(adminOp);
        addResponseCodeAssertion(adminOpTree, "200");
        addDurationAssertion(adminOpTree, "Admin Response", 1000);
        
        runSuite();
    }
}
```

---

## Quick Reference

### Essential Method Signatures

```java
// Suite Management
void startSuite(String name)
void runSuite()

// Thread Group Creation
TestContext createSuiteThreadGroup(String name, int threads, int rampUp, int loops)

// Controllers
HashTree addTransactionController(HashTree parent, String name, boolean generateParent)
HashTree addSimpleController(HashTree parent, String name)

// Samplers
HTTPSamplerProxy createHttpSampler(String name, String domain, String path, String method)

// Assertions
void addResponseCodeAssertion(HashTree samplerTree, String... codes)
void addDurationAssertion(HashTree samplerTree, long maxMs)
void addDurationAssertion(HashTree samplerTree, String name, long maxMs)
```

---

## Checklist for Every Test

Before running your test, ensure:

- [ ] Class extends `BasePerformanceTest`
- [ ] Class annotated with `@ExtendWith(ExtentReportListener.class)`
- [ ] Test method annotated with `@Test`
- [ ] `startSuite(descriptiveName)` called first
- [ ] At least one thread group created
- [ ] At least one sampler added
- [ ] Assertions added to all samplers
- [ ] `runSuite()` called at end
- [ ] Global headers configured in `config.properties`
- [ ] Descriptive names used for all components

---

## Next Steps

1. **Read:** [Architecture Documentation](architecture.md)
2. **Configure:** Update `config.properties` with your API details
3. **Create:** Write your first test following examples above
4. **Run:** Execute test from IDE or Maven
5. **Review:** Check HTML report for results
6. **Iterate:** Refine based on findings

**Need Help?**

- Check [Troubleshooting](#troubleshooting) section
- Review [Examples Library](#examples-library)
- Examine existing tests in `src/test/java/com/perf/tests/`

---

**Happy Testing! 🚀**
