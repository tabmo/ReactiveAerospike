### REACTIVE AEROSPIKE
 
 This is a pure Scala wrapper of the official "aerospike java client".
 
 It makes use of the async client and returns are wrapped into custom Future types wich can easly be mapped to your preferred Future implementation (bundled with Scala Standard Futures but if you prefer twitter ones just copy and past the code at the end of Future.scala file in your project and explicitly pass the Factory to the AerospikeClient).
 
 Under the "model" package you can see a proposal for an handy "ORM-like" pattern 

#### Install Instructions

Because the wrapper make use of query on secondary indexes that are not implemented in the async client at the last (3.0.32) version of the dirver yuo must:

```bash
git clone https://github.com/andreaTP/aerospike-client-java.git
cd aerospike-client-java/
./build_all
```

after that just clone and use Reactive Aerospike!