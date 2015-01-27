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

package eu.unicredit.reactive_aerospike.crypt

import eu.unicredit.reactive_aerospike.data._
import javax.crypto.{ KeyGenerator, SecretKey, Cipher }
import javax.crypto.spec.SecretKeySpec
import com.aerospike.client.Value.StringValue
import eu.unicredit.reactive_aerospike.data.AerospikeValue.AerospikeValueConverter
import sun.misc.{ BASE64Encoder, BASE64Decoder }
import com.aerospike.client.Value
import java.security.{ PublicKey, PrivateKey, KeyPairGenerator }
import eu.unicredit.reactive_aerospike.data.AerospikeValue.AerospikeString

object AerospikeCryptValue {

  final val AES = "AES"

  final val RSA = "RSA"
  final val RSAxform = "RSA/ECB/PKCS1Padding"

  case class AESKey(key: String)

  case class RSAKey(lenght: Int = 2048) {
    val inner = generateAsyncKey(lenght)
    val privateK = RSAPrivateKey(inner._1)
    val publicK = RSAPublicKey(inner._2)
  }
  case class RSAPrivateKey(key: PrivateKey)
  case class RSAPublicKey(key: PublicKey)

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

  private def generateAsyncKey(lenght: Int) = {
    val keyPairGenerator = KeyPairGenerator.getInstance(RSA)
    keyPairGenerator.initialize(lenght)
    val keyPair = keyPairGenerator.generateKeyPair
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic

    (privateKey, publicKey)
  }

  private def asyncEncrypt(data: String, publicKey: PublicKey, xform: String) = {
    val cipher = Cipher.getInstance(xform)
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    new BASE64Encoder().encode(cipher.doFinal(data.getBytes))
  }

  private def asyncDecrypt(data: String, publicKey: PrivateKey, xform: String) = {
    val cipher = Cipher.getInstance(xform)
    cipher.init(Cipher.DECRYPT_MODE, publicKey)
    val decodedValue = new BASE64Decoder().decodeBuffer(data)
    new String(cipher.doFinal(decodedValue))
  }

  case class AerospikeAESString(s: String, k: AESKey)
      extends AerospikeValue[String] {
    override val inner = new StringValue(encrypt(s, k.key, AES))
    override val base = s
    override def toString() =
      s.toString
  }

  case class AerospikeAESStringConverter(k: AESKey) extends AerospikeValueConverter[String] {
    def toAsV(s: String): AerospikeAESString = AerospikeAESString(s, k)
    def fromValue(vs: Value): AerospikeAESString =
      AerospikeAESString(decrypt(vs.toString, k.key, AES), k)
  }

  object AerospikeRSAStringConverter {
    def apply(k: RSAPrivateKey): AerospikeRSAStringConverter =
      new AerospikeRSAStringConverter(Some(k), None)

    def apply(k: RSAPublicKey): AerospikeRSAStringConverter =
      new AerospikeRSAStringConverter(None, Some(k))

    def apply(privateK: RSAPrivateKey, publicK: RSAPublicKey): AerospikeRSAStringConverter =
      new AerospikeRSAStringConverter(Some(privateK), Some(publicK))
  }

  case class AerospikeRSAStringConverter(privateK: Option[RSAPrivateKey],
      publicK: Option[RSAPublicKey]) extends AerospikeValueConverter[String] {
    def toAsV(s: String): AerospikeString =
      if (publicK.isDefined)
        AerospikeString(asyncEncrypt(s, publicK.get.key, RSAxform))
      else
        throw new Exception("Cannot write data without public key!")
    def fromValue(vs: Value): AerospikeString =
      if (privateK.isDefined)
        AerospikeString(asyncDecrypt(vs.toString, privateK.get.key, RSAxform))
      else
        throw new Exception("Cannot read data without private key!")
  }

}