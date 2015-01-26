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

package eu.unicredit.reactive_aerospike.tests.model

import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.data.{AerospikeKey, AerospikeRecord, AerospikeBinProto}
import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.data.AerospikeValue
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.tests.crypt.AerospikeCryptValue._

case class Review(
		key: AerospikeKey[String],
		author: String,
        title: String,
        stars: Int,
        opinion: String)
        (implicit dao: Dao[String, Review])
        extends ModelObj[String](key.inner.digest, dao) {
  require {	stars >= 0 && stars <=5 }

  def this(keyS: String,
		  author: String,
		  title: String,
		  stars: Int,
		  opinion: String)
          (implicit dao: Dao[String, Review]) =
    this(AerospikeKey[String](dao.namespace, dao.setName, keyS),
        author,
        title,
        stars,
        opinion)
 
}

abstract class ReviewDao(client: AerospikeClient) 
				extends Dao[String, Review](client) {
  val namespace = "test"

  val setName = "reviews"
    
  def key(k: String) = AerospikeKey[String](namespace, setName, k)
  
  def sc: AerospikeRSAStringConverter
  
  def RsaBinProto(id: String, f: (Review) => String) = 
		AerospikeBinProto[Review, String](
		   id, 
		   (r: Review) => AerospikeString(f(r)), 
		   sc)
  
  val objWrite: Seq[AerospikeBinProto[Review, _]] =
	     Seq(("author", (r: Review) => r.author ),
	         ("title", (r: Review) => r.title ),
	         ("stars", (r: Review) => r.stars ),
	        RsaBinProto("opinion", (r: Review) => r.opinion ))
	        
  val objRead: (AerospikeKey[String], AerospikeRecord) => Review =
	   	(key: AerospikeKey[String], record: AerospikeRecord) =>
	   		Review(
	   			key,
	   			record.get("author"),
	   			record.get("title"),
	   			record.get("stars"),
	   			record.get("opinion"))
}

case class ReviewerReviewDao(publicKey: RSAPublicKey, 
					   		 client: AerospikeClient = new AerospikeClient("localhost", 3000)) 
							 extends ReviewDao(client) {
	def sc = AerospikeRSAStringConverter(publicKey)
}

case class AuthorReviewDao(privateKey: RSAPrivateKey, 
					   	   client: AerospikeClient = new AerospikeClient("localhost", 3000)) 
						   extends ReviewDao(client) {
	def sc = AerospikeRSAStringConverter(privateKey)
}

case class AuthorBossReviewDao(privateKey: RSAPrivateKey,
						 publicKey: RSAPublicKey,
					   	 client: AerospikeClient = new AerospikeClient("localhost", 3000)) 
						 extends ReviewDao(client) {
	def sc = AerospikeRSAStringConverter(privateKey,publicKey)
}