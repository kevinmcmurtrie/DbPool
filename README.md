# DbPool

Not under active development.  Message me if you'd like the code modernized.
------------

Database pool that doesn't suck

Goals:
- Fully working with trivial configuration
- Never stalls or deadlocks
- DB shielded from app load spikes
- App shielded from DB connector overloads
- Graceful sizing adjustments
- Detects, logs, and recovers from leaks
- Extremely low overhead
- Incredibly fast even with high concurrency


Done:
- Initial tuning
- 500 thread performance tests
- Leak detection
- Initial stats
- "Where are they?" dump
- Maven
- JDBC Wrapper classes
- SQL error monitoring for instant ejection
- Optimize service thread use
- Logging
- Configuration beans for DI systems
- DropWizard & JDBI integration test
- Formal unit tests

TODO:
- Monitoring tests
