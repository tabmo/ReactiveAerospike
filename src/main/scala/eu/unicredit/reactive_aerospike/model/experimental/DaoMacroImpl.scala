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

package eu.unicredit.reactive_aerospike.model.experimental

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

import eu.unicredit.reactive_aerospike.data.AerospikeKey

object DaoMacroImpl {

  def keyCondition(c: Context) =
    (s: c.universe.Symbol) =>
      s.typeSignature.baseClasses.find(x => {
        x.name.decodedName.toString == "AerospikeKey"
      }).isDefined

  def materializeDigestGennerator[T: c.WeakTypeTag](c: Context): c.Expr[DigestGenerator[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val constructor = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get

    val fields = constructor.paramLists.head

    val key = fields.find(s => {
      keyCondition(c)(s)
    }).get

    c.Expr[DigestGenerator[T]] {
      q"""
      		import eu.unicredit.reactive_aerospike.data._
      		import eu.unicredit.reactive_aerospike.data.AerospikeValue._
     
    		new DigestGenerator[$tpe] {
    		
    			def get: ($tpe) => Array[Byte] = {
    				(obj: $tpe) => {
						obj.${key.name.toTermName}.digest
					}
    			}
    		
    		}
    		"""
    }
  }

  def materializeBinSeqGennerator[T: c.WeakTypeTag](c: Context): c.Expr[BinSeqGenerator[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val constructor = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get

    val fields = constructor.paramLists.head

    val binFields =
      fields.filterNot(s => {
        keyCondition(c)(s)
      })

    val binFieldsImpl =
      binFields.map(f => {
        val nameStr = f.name.decodedName.toString

        q"""
        	eu.unicredit.reactive_aerospike.data.fromTupleToABP[$tpe, ${f.typeSignature}](($nameStr, (o: $tpe) => o.${f.name.toTermName}))
        """
      })

    c.Expr[BinSeqGenerator[T]] {
      q"""
      		import eu.unicredit.reactive_aerospike.data._
      		import eu.unicredit.reactive_aerospike.data.AerospikeValue._
     
    		new BinSeqGenerator[$tpe] {
    		
    			def get: Seq[AerospikeBinProto[$tpe, _]] = {
    				${binFieldsImpl}.toSeq
    			}
    			
    		}
    		"""
    }
  }

  def materializeObjReadGennerator[T: c.WeakTypeTag](c: Context): c.Expr[ObjReadGenerator[T]] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val constructor = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get

    val fields = constructor.paramLists.head

    val key = fields.find(s => {
      keyCondition(c)(s)
    }).get

    val binFields =
      fields.filterNot(s => {
        keyCondition(c)(s)
      })

    val binFieldsImpl =
      binFields.map(f => {
        val nameStr = f.name.decodedName.toString

        q"""
        	record.get($nameStr)
        """
      })

    val keyType = key.typeSignature

    c.Expr[ObjReadGenerator[T]] {
      q"""
      		import eu.unicredit.reactive_aerospike.data._
      		import eu.unicredit.reactive_aerospike.data.AerospikeValue._
     
    		new ObjReadGenerator[$tpe] {
    			
    			def get[K]: (AerospikeKey[K], AerospikeRecord) => $tpe =
    				(key: AerospikeKey[K], record: AerospikeRecord) => {
    					new $tpe(
    						key.asInstanceOf[$keyType],
    						..$binFieldsImpl
    					)
    				}
    			
    		}
    		"""
    }
  }

}