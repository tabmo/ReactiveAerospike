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
The ORM-like (model) API provides you with an higher-level toolbox that you can use to persist your custom Scala classes/objects into Aerospike.

Given your this example class:
```scala
case class Person(
  id: String,
  name: String,
  surname: String,
  age: Int)
```

you can define a `Dao` class that's going to handle persistence to and from Aerospike for you.
A custom `Dao` class is that a class that extends ReactiveAerospike `Dao[K, T]` (with `K` being your key type and `T` being the type you want to persist) and providing it with an instance of a client. So something like:
```scala
case class PersonDAO(client: AerospikeClient)
    extends Dao[String, Person](client)
```
You then need to set a `namespace` and `setName`:
```scala
val namespace = "test"
val setName = "people"
```
Lastly, you need to define a `getKeyDigest` method that defines how your key digest is computed and two more values `objWrite` and `objRead`: these values collect useful functions that are going to be used when playing with your classes to and from Aerospike.
In the end you should end with something like this:

```scala
case class PersonDAO(client: AerospikeClient) extends Dao[String, Person](client) {

  val namespace = "test"
  val setName = "people"

  def getKeyDigest(obj: Person)= AerospikeKey(namespace, setName, obj.id).digest

  val objWrite = Seq(
      ("name", (p: Person) => p.name),
      ("surname", (p: Person) => p.surname),
      ("age", (p: Person) => p.age))

  val objRead: (AerospikeKey[String], AerospikeRecord) => Person =
    (key: AerospikeKey[String], record: AerospikeRecord) =>
      Person(
        new String(key.digest),
        record.get("name"),
        record.get("surname"),
        record.get("age"))
}
```