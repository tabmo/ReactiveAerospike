/* Copyright 2014 UniCredit S.p.A.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package eu.unicredit.reactive_aerospike.model

import eu.unicredit.reactive_aerospike.data.{ AerospikeKey, AerospikeValue }

abstract class ModelObj[K](_digest: Array[Byte], _dao: Dao[K, _]) {

  val getDigest = _digest

  val dao: Dao[K, _] = _dao

}

abstract class OriginalKeyModelObj[K](_key: AerospikeValue[K], _dao: OriginalKeyDao[K, _]) extends ModelObj[K](AerospikeKey.computeDigest(_dao.setName, _key), _dao) {

  def getKey = AerospikeKey(dao.namespace, dao.setName, _key, _dao.keyConverter)

}