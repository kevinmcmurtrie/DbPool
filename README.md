# DbPool
Database pool that doesn't suck

Goals:
- No deadlocks
- Never stalls
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
- Crude testing
- Maven

TODO:
- JDBC Wrapper classes
- SQL error monitoring for instant ejection
- Formal unit tests
- Optimize service thread use
- Configuration beans for DI systems
- Logging
- Monitoring
- "Where are they?" dump
- Clean-up
