# ReactiveAerospike

 ReactiveAerospike is a pure functional and type safe Scalawrapper for the [Aerospike Java Client Library
](https://github.com/aerospike/aerospike-client-java).

 It makes use of the `async` client and return values are wrapped into Futures that can easily be mapped to your preferred Future implementation (though we also bundle standard Scala Futures as well as [Twitter Futures](https://github.com/twitter/util#futures))

#### Usage

A client can be easily instantiated by proving host and port for your running server

```scala
val client = new AerospikeClient("localhost", 3000)
```

ReactiveAerospike provides two levels of usage: Direct and ORM-like.

#### Direct

Direct API lets you use the usual basic `put`, `get`, `delete` commands to interact with Aerospike.
You will always need to provide your `key` for any operation.

An `AerospikeKey` usually requires a `namespace`, the name of the `set` and its `value`.

```scala
val key = AerospikeKey("my-namespace", "my-set",  42)
```

Implicit conversions are used to support transformations from your types to Aerospike values.

####### put
A `put` operation for a given `key` and a list of `bins` looks like this:
```scala
client.put(key, Seq(bin1, bin2, bin3))
```
####### get
A `get` operation for a given `key` requires you to specify a so-called `RecordReader`, a map that contains a way to convert values contained in your bins. So given:
```scala
    val bin1 = AerospikeBin("x", 1)
    val bin2 = AerospikeBin("y", 2)
    val bin3 = AerospikeBin("z", 3)
```
You will need to specify a `AerospikeRecordReader` like this:

```scala
    val recordReader = new AerospikeRecordReader(
    Map(("x" -> AerospikeIntConverter),
        ("y" -> AerospikeIntConverter),
        ("z" -> AerospikeIntConverter)))
```

A series of built-int implicit converters are defined in `AerospikeValue`.
Now you can `get`:

```scala
client.get(key, recordReader)
```

#### ORM-like
