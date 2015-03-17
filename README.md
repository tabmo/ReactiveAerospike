# ReactiveAerospike

 ReactiveAerospike is a pure functional and type safe Scalawrapper for the [Aerospike Java Client Library
](https://github.com/aerospike/aerospike-client-java).

 It makes use of the `async` client and return values are wrapped into Futures that can easily be mapped to your preferred Future implementation (we bundle with default standard Scala Futures but we also provide an example that you can use with [Twitter Futures](https://github.com/twitter/util#futures))

#### Usage
Usually you just need these imports:

```scala
import eu.unicredit.reactive_aerospike.client._

//for direct API
import eu.unicredit.reactive_aerospike.data._

//for ORM-like API
import eu.unicredit.reactive_aerospike.model._

//for built-in readers and converters
import eu.unicredit.reactive_aerospike.data._

//for conversion helpers to Scala Futures
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
```

A client can be easily instantiated by proving host and port for your running server

```scala
val client = new AerospikeClient("192.168.59.103", 3000)
```

ReactiveAerospike provides two levels of usage: [Direct](#direct) and [ORM-like](#orm-like).

#### Direct

Direct API lets you use the usual basic `put`, `get`, `delete` commands to interact with Aerospike.
You will always need to provide your `key` for any operation.

An `AerospikeKey` usually requires a `namespace`, the name of the `set` and its `value`.

```scala
val key = AerospikeKey("test", "my-set",  42)
//key: AerospikeKey[Int] = AerospikeKey(test,Some(my-set),Some(42))
```

**IMPORTANT note about Aerospike Keys**: Internally Aerospikes only cares about its [keys](https://github.com/aerospike/aerospike-client-java/blob/master/client/src/com/aerospike/client/Key.java) digests. By deafult the key value provided by the user is discarded. You're going to have to specifically define a `WritePolicy` with `sendKey = true` if you want Aerospike to store your key. 
You can go on using the key you have defined and pass it through your functions, but the original value would not be available, should you need it.

Also note that implicit conversions are used to support transformations from your types to Aerospike values.

If we now define some `bins`:

```scala
val bin1 = AerospikeBin("x", 1) //an Aerospike bin named x containing 1
val bin2 = AerospikeBin("y", 2)
val bin3 = AerospikeBin("z", 3)
```

we can then go on and persist them.

####### put
A `put` operation for a given `key` and a list of `bins` looks like this:
```scala
client.put(key, Seq(bin1, bin2, bin3))
```
Put operations always return a key wrapped in a future.
In this specific case you would get a `Future[AerospikeKey[Int]]`.

####### get
A `get` operation for a given `key` requires you to specify a so-called `RecordReader`, a map that contains a way to convert values contained in your bins. So given:

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
case class PersonDAO() extends Dao[String, Person] {

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

With that code you're just set and ready-to-go.

To persist your models on Aerospike you can write something like this:

```scala
    implicit val client = new AerospikeClient("localhost", 3000)(ScalaFactory)
    implicit val personDao = PersonDao()

    val bob = Person("rOBerT", "Bob", "Wood", 23)
    val tim = Person("TImoTHy", "Tim", "Forest", 32)

    //creating
    personDao.create(bob)
    personDao.create(tim)
```

That's it!

## Authors:
* Andrea Peruffo: <https://github.com/andreaTP>
* Marco Firrincieli: <https://github.com/mfirry>

Thanks for assistance and contributions:

* Andrea Ferretti <http://github.com/andreaferretti>
* Gianluca Sabena: <http://github.com/gianluca-sabena>



