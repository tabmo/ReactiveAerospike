
 
 This is a pure Scala FUNCTIONAL and TYPE SAFE wrapper of the "aerospike java client".
 No more explicit cast and "asInstanceOf" in client code are needed!
 
 It makes use of the async client and returns are wrapped into custom Future types wich can easily be mapped to your preferred Future implementation (bundled with Scala Standard Futures but if you prefer Twitter ones just copy and paste the code at the end of Future.scala file in your project and explicitly pass the Factory to the AerospikeClient).
 
 Under the "model" package you can see a proposal for an handy "ORM-like" pattern.

#### Installation Instructions

This step is no more needed thanks to version 3.0.34 of the java driver.
So, just clone and use Reactive Aerospike!

#### Usage

For usage examples, please refer to the tests in code.

You can use directly the client and have to manage Bins and Records manually or make use of the "model" api to have ORM functionalities.

#### API stability

The API described above should be considered unstable.
