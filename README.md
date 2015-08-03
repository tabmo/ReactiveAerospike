# ReactiveAerospike

 ReactiveAerospike is a pure functional and type safe Scalawrapper for the [Aerospike Java Client Library
](https://github.com/aerospike/aerospike-client-java).

 It makes use of the `async` client and return values are wrapped into scala Futures.

## Installation

Add in your build.sbt

```
resolvers += "Tabmo Bintray" at "https://dl.bintray.com/tabmo/maven"

libraryDependencies += "io.tabmo" %% "reactive-aerospike" % "0.0.3"
```

## Usage

Usually you just need these imports:

```scala
import io.tabmo.aerospike.client._
import io.tabmo.aerospike.data._

//for built-in readers and converters
import io.tabmo.aerospike.data.AerospikeValue._

```

A client can be easily instantiated by proving host and port for your running server

```scala

import com.aerospike.client.async.AsyncClientPolicy
import io.tabmo.aerospike.client.ReactiveAerospikeClient

// Connect to only one instance
val client = ReactiveAerospikeClient("192.168.59.103", 3000)

// Connect ton many instances
val client = ReactiveAerospikeClient(new AsyncClientPolicy(), Seq("server1:3000", "server2:3000", "server3:3000"))
```

Don't forget to close the connection when your app shutdown.

```
client.asyncClient.close()
```

Direct API lets you use the usual basic `put`, `get`, `delete` commands to interact with Aerospike.
You will always need to provide your `key` for any operation.

An `AerospikeKey` usually requires a `namespace`, the name of the `set` (Optional) and its `value`.

```scala
val key = AerospikeKey("test", "my-set",  42)
```

**IMPORTANT note about Aerospike Keys**: Internally Aerospikes only cares about its [keys](https://github.com/aerospike/aerospike-client-java/blob/master/client/src/com/aerospike/client/Key.java) digests. By deafult the key value provided by the user is discarded. You're going to have to specifically define a `WritePolicy` with `sendKey = true` if you want Aerospike to store your key. 
You can go on using the key you have defined and pass it through your functions, but the original value would not be available, if you need it.

Also note that implicit conversions are used to support transformations from your types to Aerospike values.

If we now define some `bins`:

```scala
val bin1 = AerospikeBin("x", 1) //an Aerospike bin named x containing 1
val bin2 = AerospikeBin("y", 2)
val bin3 = AerospikeBin("z", 3)
```

we can then go on and persist them.

#### put
A `put` operation for a given `key` and a list of `bins` looks like this:
```scala
client.put(key, Seq(bin1, bin2, bin3))
```
Put operations always return a key wrapped in a future.
In this specific case you would get a `Future[AerospikeKey[Int]]`.

#### get
A `get` operation for a given `key` requires you to specify a so-called `RecordReader`, a map that contains a way to convert values contained in your bins. So given:

You will need to specify a `AerospikeRecordReader` like this:

```scala
val recordReader = new AerospikeRecordReader(
    Map("x" -> AerospikeIntConverter,
        "y" -> AerospikeIntConverter,
        "z" -> AerospikeIntConverter))
```

A series of built-int implicit converters are defined in `AerospikeValue`. 
Now you can `get`:

```scala
client.get(key, recordReader)
```

In this case you would get a `Future[(AerospikeKey[_], AerospikeRecord)]`.
An instance of `AerospikeRecord` will then contain your bins.

```scala
val (k, r) = Await.result(client.get(key, recordReader), 5 millis)
//k: AerospikeKey[_] = AerospikeKey(test,[B@4d37d02c,Some(my-set),Some(0))
//r: AerospikeRecord = AerospikeRecord@5a04cc60 
```

```scala
r.getBins
//res0: Seq[AerospikeBin[_]] = List(AerospikeBin(x,1,AerospikeValue$AerospikeIntConverter$@58dd0316), AerospikeBin(y,2,AerospikeValue$AerospikeIntConverter$@58dd0316), AerospikeBin(z,3,AerospikeValue$AerospikeIntConverter$@58dd0316))
```

```scala
r.getAs[Int]("x")
//res0: Int: 3
```

## Sample usage

```scala

val writePolicyWithTTL = {
  val policy = new WritePolicy(aerospike.asyncClient.asyncWritePolicyDefault) // clone default policy
  policy.expiration = 60 * 60 * 24 * 30 // 30 days
  policy
}

val key = AerospikeKey("myNamespace", "persons", 123456)
val bins = Seq(
  AerospikeBin("name", "julien"),
  AerospikeBin("id", 123456L),
  AerospikeBin("counter", 0L)
)

val counterReader = new AerospikeRecordReader(
  Map("counter" -> AerospikeLongConverter)
)
  

val saveOperation = aerospike.put(key, bins)(writePolicyWithTTL)
val updateCounterOperation = aeropsike.add(key, Seq(AerospikeBin("counter", 0L))) // 0L is not used, just the name of the bin is required
val readCounterOperation = aerospike.get(key, counterReader)

val result: Future[Long] = for {
  _ <- saveOperation
  _ <- updateCounterOperation
  (key, record) <- readCounterOperation
} yield record.getAs[Long]("value")
  
```

## Authors
* Julien Lafont: <https://github.com/studiodev>

### Original version

* Andrea Peruffo: <https://github.com/andreaTP>
* Marco Firrincieli: <https://github.com/mfirry>

Thanks for assistance and contributions:

* Andrea Ferretti <http://github.com/andreaferretti>
* Gianluca Sabena: <http://github.com/gianluca-sabena>



