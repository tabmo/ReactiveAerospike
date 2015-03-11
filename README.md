# ReactiveAerospike

 ReactiveAerospike is a pure functional and type safe Scalawrapper for the [Aerospike Java Client Library
](https://github.com/aerospike/aerospike-client-java).

 It makes use of the `async` client and returns values are wrapped into custom Future types wich can easily be mapped to your preferred Future implementation (though we also bundle Scala standard Futures and [Twitter Futures](https://github.com/twitter/util#futures))

#### Usage

A client can be easily instantiated by proving host and port for your running server

```scala
val client = new AerospikeClient("localhost", 3000)
```

ReactiveAerospike provides two levels of usage: Direct and ORM-like.

#### Direct

Direct API lets you use basic `put`, `get`, `delete` commands to interact with Aerospike.
You will need to

Direct API expose

#### ORM-like
