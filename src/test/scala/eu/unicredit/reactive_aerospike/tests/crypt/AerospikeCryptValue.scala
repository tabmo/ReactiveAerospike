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

package eu.unicredit.reactive_aerospike.tests.crypt

import eu.unicredit.reactive_aerospike.data._
import javax.crypto.{KeyGenerator, SecretKey, Cipher}
import javax.crypto.spec.SecretKeySpec
import com.aerospike.client.Value.StringValue
import eu.unicredit.reactive_aerospike.data.AerospikeValue.AerospikeValueConverter
import sun.misc.{BASE64Encoder, BASE64Decoder}
import com.aerospike.client.Value

object AerospikeCryptValue {

    final val AES = "AES"
    
    private def encrypt(data: String, key: String, algo: String): String = {
    	val skey = new SecretKeySpec(key.getBytes(), algo)
    	val c = Cipher.getInstance(algo)
    	c.init(Cipher.ENCRYPT_MODE, skey)
		val encVal = c.doFinal(data.getBytes())
        new BASE64Encoder().encode(encVal)
    }
    
    private def decrypt(data: String, key: String, algo: String): String = {
    	val skey = new SecretKeySpec(key.getBytes(), algo)
    	val c = Cipher.getInstance(algo)
    	c.init(Cipher.DECRYPT_MODE, skey)
    	val decodedValue = new BASE64Decoder().decodeBuffer(data)
    	new String(c.doFinal(decodedValue))
    }

  	case class AESKey(key: String) {}

  	case class AerospikeAESString(s: String, k: AESKey)
  	  	extends AerospikeValue[String] {
    	override val inner = new StringValue(encrypt(s, k.key, AES))
    	override val base = s
    	override def toString() =
    		s.toString
    }
  
  	case class AerospikeAESStringReader(k: AESKey) extends AerospikeValueConverter[String] {
  		def toAsV(s: String): AerospikeAESString = AerospikeAESString(s,k)
  		def fromValue(vs: Value): AerospikeAESString =
  			AerospikeAESString(decrypt(vs.toString, k.key, AES), k) 
  	}

}