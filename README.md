# ReactiveAerospike

 ReactiveAerospike is a pure functional and type safe Scalawrapper for the [Aerospike Java Client Library
](https://github.com/aerospike/aerospike-client-java).

 It makes use of the `async` client and returns values are wrapped into custom Future types wich can easily be mapped to your preferred Future implementation (though we also bundle Scala Standard Futures and [Twitter Futures](https://github.com/twitter/util#futures))

#### Installation Instructions

This step is no more needed thanks to version 3.0.34 of the java driver.
So, just clone and use Reactive Aerospike!

#### Usage

For usage examples, please refer to the tests in code.

You can use directly the client and have to manage Bins and Records manually or make use of the "model" api to have ORM functionalities.

#### API stability

The API described above should be considered unstable.
