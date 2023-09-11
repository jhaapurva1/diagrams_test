# CPS and MongoDB

## Status

Accepted

## Context

>CPS is a write heavy system with more than 30K writes per second while the read throughput is less than 2K reads per second. Which means having two separate db for read and writes was not required and was leading to unnecessary cost.
>Since the ES sync was done via a background job, The data we showed to suppliers was not real-time. (Although for some APIs we redirected reads to HBase to make it real time but this couldn’t be done for all APIs as HBase doesn’t support aggregation queries)


## Decision

>Moving to a single db for both reads and writes. We chose MongoDB as it is a document based db.

## Consequences

>Total Savings ~ 8500 USD