# IngeniumDB

Distributed file storage which is support queries and fast streaming engine.

Powerfull Opensource DB (https://en.wiktionary.org/wiki/ingenium - special thanks René Descartes for this word)

## Main functionality:

### - Release 0.0.1-SNAPSHOT:
- distributed mode for storing large files across a cluster with connectors: S3, Azure Blob, Bare Metal
- query language for gettimg fast access to the files (their metadata). Users can form metadata for their side
- fast reading in streaming mode 

### - Release 0.0.2-SNAPSHOT:
- heterogeneous
- smart caching system for files which is currently "processing"
- configurable storage redundancy
- fault tolerance 
- transactions
- smart snapshots for files
- fast data streaming
- defragmentation for files for fast data streaming

## PoC architecture:
![diagram](pictures/architecture.png "architecture")

