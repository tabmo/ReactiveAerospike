//package eu.unicredit.hpasted
//
//object Test extends App {
//  
//  
//  /* from HPaste example*/
//  class WebTable extends AerospikeSet[WebTable, String, WebPageRecord]
//		  (setName = "pages", aerospikeKeyValueClass = classOf[String]) {
//    
//    //dopo
//    //def rowBuilder(result: DeserializedResult) = new WebPageRow(this, result)
//
//    val meta = record[String, String, Any]("meta")
//    val title = bin(meta, "title", classOf[String])
//    val lastCrawled = bin(meta, "lastCrawled", classOf[DateTime])
//
//    val content = record[String, String, Any]("text", compressed = true)
//    val article = bin(content, "article", classOf[String])
//    val attributes = bin(content, "attrs", classOf[Map[String, String]])
//
//    //val searchMetrics = family[String, DateMidnight, Long]("searchesByDay")
//
//
//  }
//
//  class WebPageRecord(table: WebTable, result: DeserializedResult) e
//  		xtends AerospikeRecord[WebTable, String](result, table)
//
//  		val WebTable = set(new WebTable)
//  }
//  
//
//}