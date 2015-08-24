# ReactiveAerospike

ReactiveAerospike is a pure functional and type safe Scalawrapper for the [Aerospike Java Client Library
](https://github.com/aerospike/aerospike-client-java).

This is a simplified and fuller version of [ReactiveAerospike](https://github.com/unicredit/ReactiveAerospike) made by Andrea Ferretti and Gianluca Sabena.

It makes use of the `async` client and return values are wrapped into scala Futures.

## Installation

Add in your build.sbt

```scala
resolvers += "Tabmo Bintray" at "https://dl.bintray.com/tabmo/maven"

libraryDependencies += "io.tabmo" %% "reactive-aerospike" % "1.0.7"
```

## Usage

Usually you just need these imports:

```scala
import io.tabmo.aerospike.client._
import io.tabmo.aerospike.data._
import io.tabmo.aerospike.converter.key._
```

A client can be easily instantiated by proving host and port for your running server

```scala
import io.tabmo.aerospike.client.ReactiveAerospikeClient

// Connect to only one instance
val client = ReactiveAerospikeClient.connect("192.168.59.103", 3000)

// Connect to many instances
val client = ReactiveAerospikeClient("server1:3000", "server2:3000", "server3:3000")
```

Don't forget to close the connection when your app shutdown.

```scala
client.close()
```

Direct API lets you use the usual basic `put`, `get`, `delete` commands to interact with Aerospike.
You will always need to provide your `key` for any operation.

An `AerospikeKey` usually requires a `namespace`, the name of the `set` (Optional) and its `value`. A key can only be a Long or a String.

```scala
val key = AerospikeKey("test", "my-set",  42)
```

**IMPORTANT note about Aerospike Keys**: Internally Aerospikes only cares about its [keys](https://github.com/aerospike/aerospike-client-java/blob/master/client/src/com/aerospike/client/Key.java) digests.
By default the key value provided by the user is discarded. You're going to have to specifically define a `WritePolicy` with `sendKey = true` if you want Aerospike to store your key. 
You can go on using the key you have defined and pass it through your functions, but the original value would not be available, if you need it.

Also note that implicit conversions are used to support transformations from your types to Aerospike values.

If we now define some `bins`:

```scala
val binLong = Bin("x", 1L)
val binString = Bin("z", "hello")

// By convenience, you can also define a bin with an Int, but it will be converted to a Long bin.
val binLongToo = Bin("y", 2) 

```

We can then go on and persist them.

#### put
A `put` operation for a given `key` and a list of `bins` looks like this:

```scala
client.put(key, Seq(bin1, bin2, bin3))
```

Put operations always return a key wrapped in a future.
In this specific case you would get a `Future[AerospikeKey[K]]`.

#### get
A `get` operation for a given `key`. You can define the list of bins you want, or use `Seq.empty` to return them all.

```scala
client.get(key, Seq("bin1", "bin2")) // Return only the bins bin1 and bin2
client.get(key) // Return all bins
```

In this case you would get a `Future[AerospikeRecord]`.
An instance of `AerospikeRecord` will then contain your bins.

```scala
for {
  record <- client.get(key)
} yield {

  // Underlying bins can only be a Long or a String
  val l: Long = record.getLong("binLong")
  val s: String = record.getString("binString")
  
  // You can try to transform these values to another type, but it's an unsafe operation
  val i: Option[Int] = record.asInt("binLong")
  val d: Option[Double] = record.asDouble("binString")
  
  // You can also retrieve the expiration of the record (in s)
  val exp: Long = record.expiration
  
  // You can check if a bin exists:
  val exists: Boolean = record.exists("binName")
  
  // You can get all bins
  val bins: Map[String, AnyRef] = record.bins
  
  // You can get the number of bins defined
  val count: Int = record.sizes
  
}
```

See [`AerospikeRecord`](https://github.com/tabmo/ReactiveAerospike/blob/master/src/main/scala/io/tabmo/aerospike/data/AerospikeRecord.scala) to know safe and unsafe operations.


### Other operations

You can use all operations available on the Java driver: `touch`, `header`, `add`, `prepend`, `append`, `exists`, `delete`.

See [BasicUsage](https://github.com/tabmo/ReactiveAerospike/blob/master/src/test/scala/BasicUsage.scala) for sample code.

## Sample usage

```scala
val writePolicyWithTTL = {
  val policy = new WritePolicy(aerospike.asyncClient.asyncWritePolicyDefault) // clone default policy
  policy.expiration = 60 * 60 * 24 * 30 // 30 days
  policy
}

val key = AerospikeKey("myNS", "mySet", 123456)
val bins = Seq(
  Bin("name", "julien"),
  Bin("id", 123456L),
  Bin("counter", 0)
)

val saveOperation = aerospike.put(key, bins)(writePolicyWithTTL)
val updateCounterOperation = aeropsike.add(key, Seq(Bin("counter", 1)))
val readCounterOperation = aerospike.get(key, Seq("counter"))

val result: Future[Long] = for {
  _ <- saveOperation
  _ <- updateCounterOperation
  record <- readCounterOperation
} yield {
  record.getLong("counter")
}
```

## Advanced usage

### Operate

```scala
val result: Future[AerospikeRecord] = 
  client.operate(key,
    put("foo", "bar"),
    put("long", 1000),
    add("v", 1),
    append("string", "world"),
    getAll)()
```

See [OperateUsage](https://github.com/tabmo/ReactiveAerospike/blob/master/src/test/scala/OperateUsage.scala) for sample code.


### Query

```scala
val result: Future[Map[AerospikeKey[Long], AerospikeRecord]] = 
  client.queryEqual[Long, String](ns, set, Seq("id", "name"), "name", "julien")

val result: Future[Map[AerospikeKey[Long], AerospikeRecord]] = 
  client.queryEqual[Long, Long](ns, set, Seq("id", "name"), "id", 1000)
  
val result: Future[Map[AerospikeKey[Long], AerospikeRecord]] = 
  client.queryRange[Long](ns, set, Seq("id", "name"), "id", 1000, 2000)
```

See [QueryUsage](https://github.com/tabmo/ReactiveAerospike/blob/master/src/test/scala/QueryUsage.scala) for sample code.

### UDF

```scala

import io.tabmo.aerospike.converter.value._

val result: Seq[AerospikeRecord] = 
  client.queryEqualAggregate(ns, set,
  "name", "thomas", // On what the filter is made?
  this.getClass.getClassLoader, "persons.lua", // where the UDF is
  "persons", "filterByAge", Seq(19)) // What method/args call on the UDF?
```

See [UdfUsage](https://github.com/tabmo/ReactiveAerospike/blob/master/src/test/scala/UdfUsage.scala) for sample code.

## Authors

* Julien Lafont: <https://github.com/studiodev>

### Original version

* Andrea Peruffo: <https://github.com/andreaTP>
* Marco Firrincieli: <https://github.com/mfirry>

Thanks for assistance and contributions:

* Andrea Ferretti <http://github.com/andreaferretti>
* Gianluca Sabena: <http://github.com/gianluca-sabena>
