# Refactoring Quick Reference Card

**📋 Quick lookup for developers - See REFACTORING_GUIDELINES.md for full details**

---

## ⚖️ File Size Targets

| Status | Lines | Action |
|--------|-------|--------|
| ✅ Good | < 200 | Keep it up! |
| ⚠️ Warning | 200-300 | Consider refactoring |
| ❌ Refactor | > 300 | Must refactor (extract classes/methods) |

**Exception:** Generated code (com.kuka.generated.*)

---

## 🎯 Method Size Targets

- **Target:** < 30 lines
- **Maximum:** 50 lines
- **Action:** Extract methods if longer

---

## 📦 Package Structure

```
hartu/
├── protocols/constants/     # Protocol definitions
├── robot/
│   ├── commands/           # Command data structures
│   ├── communication/      # Network layer
│   │   ├── server/        # Server components
│   │   └── client/        # Client components
│   ├── executor/          # Robot execution
│   │   ├── motion/       # Motion executors (NEW)
│   │   └── io/           # IO executors (NEW)
│   └── utils/            # Utilities
└── tests/                 # Test applications
```

---

## 🔤 Naming Conventions

### Classes
- **Format:** PascalCase
- **Suffixes:** Handler, Manager, Executor, Parser, Builder, Factory, Config, Constants

### Methods
- **Format:** camelCase
- **Prefixes:** create*, build*, execute*, parse*, validate*, is*, has*, get*, set*

### Variables
- **Format:** camelCase
- **Constants:** UPPER_SNAKE_CASE
- **Booleans:** isConnected, hasPermission (reads like a question)

---

## 🏗️ Core Principles

1. **Single Responsibility:** One class = one clear purpose
2. **Separation of Concerns:** Keep layers separate (protocol/domain/communication/application)
3. **No Magic Numbers:** Use named constants
4. **Descriptive Names:** Code should be self-documenting
5. **Fail Fast:** Validate early, throw exceptions for invalid state

---

## 🤖 KUKA API Patterns

### Creating Motions
```java
// PTP Joint Motion
PTP ptp = new PTP(jointPosition);
ptp.setJointVelocityRel(0.2);
ptp.setBlendingRel(0.05);

// Cartesian Motion
CartesianPTP cptp = new CartesianPTP(frame);
cptp.setJointVelocityRel(0.2);

// Linear Motion
LIN lin = new LIN(frame);
lin.setJointVelocityRel(0.2);
lin.setBlendingRel(0.05);

// Circular Motion
CIRC circ = new CIRC(auxFrame, destFrame);
circ.setJointVelocityRel(0.2);
```

### Executing Motions
```java
try {
    IMotionContainer container = robot.moveAsync(motion);
    container.await();
} catch (CommandInvalidException e) {
    // Invalid parameters
} catch (CancelledException e) {
    // User/system cancelled
} catch (ExternalStopException e) {
    // E-stop triggered
}
```

### Batching Motions
```java
MotionBatch batch = new MotionBatch(
    motion1, motion2, motion3
);
robot.moveAsync(batch).await();
```

---

## 📝 Logging

```java
// Use Logger singleton
Logger.getInstance().log("TAG", "message");
Logger.getInstance().warn("TAG", "warning message");
Logger.getInstance().error("TAG", "error message");
Logger.getInstance().error("TAG", "error with exception", exception);

// Common tags: COMM, PARSER, ROBOT_EXEC, MOTION, IO, QUEUE, SERVER
```

---

## 🔒 Thread Safety

```java
// Synchronization (Java 7)
private final Object lock = new Object();

public void safeMethod() {
    synchronized (lock) {
        // Thread-safe code
    }
}

// Volatile for flags
private volatile boolean isRunning = true;

// Use concurrent collections
ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();
BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
```

---

## ✅ Pre-Commit Checklist

Before committing code:

- [ ] No class > 400 lines (excluding generated)
- [ ] No method > 50 lines
- [ ] All magic numbers have named constants
- [ ] Proper error handling and logging
- [ ] Code formatted consistently
- [ ] Public methods have JavaDoc
- [ ] No compiler warnings
- [ ] Thread safety considered
- [ ] Follows naming conventions
- [ ] Single responsibility per class

---

## 🚨 Common Anti-Patterns to Avoid

❌ **God Class** - One class doing too much  
✅ **Solution:** Extract responsibilities

❌ **Long Parameter List** - Methods with > 4 parameters  
✅ **Solution:** Parameter object or builder pattern

❌ **Magic Numbers** - Literal values with business meaning  
✅ **Solution:** Named constants

❌ **Duplicated Code** - Same logic in multiple places  
✅ **Solution:** Extract to common method/utility

❌ **Generic Exceptions** - Catching `Exception`  
✅ **Solution:** Catch specific exceptions

---

## 🎓 Refactoring Techniques

### Extract Method
```java
// BEFORE: Long method
public void process() {
    // 20 lines of logic A
    // 15 lines of logic B
}

// AFTER: Extracted
public void process() {
    doLogicA();
    doLogicB();
}
```

### Extract Class
```java
// BEFORE: Too many responsibilities
public class BigClass {
    // Motion logic
    // IO logic
    // Logging logic
}

// AFTER: Separated
public class MotionHandler { }
public class IoHandler { }
```

### Replace Magic Number
```java
// BEFORE
if (pin == 10) { lockGimatic(); }

// AFTER
public static final int GIMATIC_LOCK_PIN = 10;
if (pin == GIMATIC_LOCK_PIN) { lockGimatic(); }
```

---

## 📚 Key Documents

- **REFACTORING_GUIDELINES.md** - Full coding standards (17KB)
- **REFACTORING_PLAN.md** - Detailed refactoring roadmap (24KB)
- **REFACTORING_QUICK_REFERENCE.md** - This document

---

## 🔧 Java 7 Constraints

**Cannot Use:**
- Lambda expressions
- Stream API
- Try-with-resources
- Diamond operator for generics
- String switch statements
- Method references

**Must Use:**
- Anonymous inner classes instead of lambdas
- Traditional loops instead of streams
- Manual resource cleanup in finally blocks
- Explicit generic types: `new ArrayList<String>()`
- If-else or lookup maps instead of switch on strings

---

## 🎯 When in Doubt

1. Is this class doing ONE thing?
2. Can I understand what this does in < 1 minute?
3. Would a new developer understand this?
4. Is this the simplest solution?
5. Have I followed the guidelines?

**If answer is "no" to any, refactor!**

---

## 📞 Getting Help

1. Check **REFACTORING_GUIDELINES.md** for detailed patterns
2. Check **REFACTORING_PLAN.md** for planned changes
3. Review existing code for examples
4. Ask team members
5. Document new patterns discovered

---

**Version 1.0 | Last Updated: 2025-11-12**
