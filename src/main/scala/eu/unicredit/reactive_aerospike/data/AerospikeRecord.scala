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

package eu.unicredit.reactive_aerospike.data

import scala.util.control.NonFatal

import com.aerospike.client.Record
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import org.slf4j.LoggerFactory

import AerospikeValue._

class AerospikeRecord(
    bins: Seq[AerospikeBin[_]],
    generation: Int,
    expiration: Int) {

  def getBins = bins

  def get[X](binName: String): AerospikeValue[X] = {
    getOpt[X](binName).getOrElse(
      throw new Exception(s"Bin name $binName not found"))
  }

  def getOpt[X](binName: String): Option[AerospikeValue[X]] = {
    type C = AerospikeValue[X]
    bins
      .find(_.name == binName)
      .flatMap { bin =>
        bin.value match {
          case av: AerospikeValue[_] =>
            try {
              val manif = scala.reflect.ClassManifestFactory.classType[X](bin.value.base.getClass)
              if (manif.runtimeClass.isInstance(bin.value.base))
                Some(bin.value.asInstanceOf[AerospikeValue[X]])
              else None
            } catch {
              case NonFatal(err) =>
                AerospikeRecord.logger.error("Cannot call getOpt on AerospikeRecord", err)
                None
            }
          case _ => None
        }
    }
  }

  def toRecordBins: Seq[(String, Object)] = bins.map(_.toRecordValue)

  val inner = new Record(toRecordBins.toMap.asJava, generation, expiration)
}

class AerospikeRecordReader(val stub: Map[String, AerospikeValueConverter[_]]) {

  def getStub = stub

  def extractor: Record => AerospikeRecord = (record: Record) => {
    val generation = Option(record).map(_.generation).getOrElse(0)
    val expiration = Option(record).map(_.expiration).getOrElse(0)

    new AerospikeRecord(stub.map(bin =>
      AerospikeBin((bin._1, record.bins.get(bin._1)), bin._2)).toSeq,
      generation,
      expiration
    )
  }
}

object AerospikeRecordReader {
  def apply(bins: Seq[AerospikeBin[_]]): AerospikeRecordReader =
    new AerospikeRecordReader(bins.map(bin => bin.name -> bin.converter).toMap)
}

object AerospikeRecord {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(record: Record)(implicit recordReader: AerospikeRecordReader): AerospikeRecord =
    recordReader.extractor(record)
}
