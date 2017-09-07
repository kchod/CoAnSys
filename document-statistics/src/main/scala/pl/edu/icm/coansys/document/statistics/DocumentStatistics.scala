package pl.edu.icm.coansys.document.statistics
//import scala.collection.JavaConversions._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.hadoop.io.BytesWritable
import org.apache.spark.SparkConf
import pl.edu.icm.coansys.models.DocumentProtos
import pl.edu.icm.coansys.models.DocumentProtos._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import java.io.File
import java.io.FileWriter

object DocumentStatistics {
  val log = org.slf4j.LoggerFactory.getLogger(getClass().getName())
  
def determineCollection(x:String) = if (x.startsWith( "http://comac.icm.edu.pl/elements/medline")) {
                List("medline", "all")
        } else if (x.startsWith ( "http://comac.icm.edu.pl/elements/oai")) {
                List("oai", "all")
        } else if (x.startsWith( "pubmed:")) {
                List("pubmed", "all")
        } else if (x.startsWith( "cc-source:")) {
                List("cc-source", "all")
        } else {
                List("all")
        }
  
  def anyHasEmail(lista:Seq[Author]): Boolean = {
        for (x <- lista) {
          if (x.hasEmail()) {
            return true
          }
        }
        return false
      }

  case class Config(
    inputFile: String = "",
    outputFile: String = ""
  )
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("CoAnSys Deduplicate Documents") {
      head("Document statistics", "0.1")

      arg[String]("<input>").required.text("Input sequence file").action((f, c) => c.copy(inputFile = f))
      arg[String]("<output>").required.text("Output file").action((f, c) => c.copy(outputFile = f))
    }

    val cfg: Config = parser.parse(args, Config()) match {
      case Some(config) =>
        println(f"Got config:\n${config}")
        println(config);
        config
      case None =>
        // arguments are bad, error message will have been displayed
        println("No config.")
        return
    }

    println("Creating context...")

    val conf = new SparkConf()
      .setAppName("Document statistics")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", "pl.edu.icm.coansys.document.statistics.DocumentWrapperKryoRegistrator")

    val sc = new SparkContext(conf)

    println("Created context...")
    //    sc.getConf.getAll.foreach(x => println(x._1 + ": " + x._2))
    val inputDocuments = cfg.inputFile
    //  "/user/kura/curr-res-navigator/hbase-sf-out/DOCUMENT"
    //  "/user/kura/curr-res-navigator-no-blogs/hbase-sf-out/DOCUMENT"
    val outputDocuments = cfg.outputFile

    val results = collection.mutable.Map[String, Seq[Any]]()

    val rawbytes = sc.sequenceFile[String, BytesWritable](inputDocuments).mapValues(_.copyBytes)
    println("Loaded raw bytes.")

    val wrappers = rawbytes.mapValues(b => DocumentProtos.DocumentWrapper.parseFrom(b))
    //val metadata = wrappers.mapValues(x => x.getDocumentMetadata)
    val metadata = wrappers.mapValues(x => x.getDocumentMetadata)
      .flatMap(x => (determineCollection(x._1)++x._2.getCollectionList.asScala).distinct.map(coll => (coll, x._2)))
      .cache()

    val collections = metadata.keys
    val coll_array = collections.distinct.collect()

    def addResult(name: String, values: scala.collection.Map[String, Any]) {
      results(name) = coll_array.map(coll => values.getOrElse(coll, 0))
    }

    val docs_in_coll = collections.countByValue
    addResult("DocumentCount", docs_in_coll)
    
    //tu miejsce na kolejne operacje
    
    //zliczenie typów
  
    val types_coll = metadata.mapValues(x => x.getType)
    val types_coll_count = types_coll.keys.countByValue
    addResult("TypesCount", types_coll_count)
    
    //ilość abstraktów
    
    val abstracts_coll = metadata.mapValues(x => x.getDocumentAbstractCount()).filter(x => x._2 >0).map(x => x._1)
    val abstracts_coll_count = abstracts_coll.countByValue
    //results("DocumentsWith_Abstract_Count") = coll_array.map(coll => abstracts_coll_count.getOrElse(coll, 0))
    addResult("AbstractCount", abstracts_coll_count)
    
    //odsetek abstraktów
    
    val docs_with_abstracts = metadata.mapValues(x => x.getDocumentAbstractCount())
    val docs_with_abstracts2 = docs_with_abstracts.mapValues(x => (if (x>0) { 1 } else { 0 }, 1))
    val docs_with_abstracts3 = docs_with_abstracts2.reduceByKey((x, y) => (x._1+y._1, x._2+y._2))
    val docs_with_abstracts4 = docs_with_abstracts3.mapValues(x => x._1.toFloat/x._2)
    val docs_with_abstracts_perc = docs_with_abstracts4.collectAsMap()
    addResult("AbstractPercent", docs_with_abstracts_perc)
    
    //średnia długość abstraktów (znaki)
    
    val abstracts_list = metadata.flatMapValues(x => x.getDocumentAbstractList().asScala)
    val abstracts_list1 = abstracts_list.map(x => (x._1, x._2.getText()))
     
    val abstracts_len = abstracts_list1.map(x => (x._1, x._2.length().toFloat))
      .map(x => (x._1, (x._2, 1)))
    val abstract_len_sum = abstracts_len.reduceByKey((x,y)=>(x._1+y._1, x._2+y._2))
    val abstract_avg_count = abstract_len_sum.mapValues(x => x._1/x._2)
    val abstract_avg_lenght = abstract_avg_count.collectAsMap()
    addResult("AbstractLenghtAvg", abstract_avg_lenght)
    
    //referencje ilość
    
    val reference_coll = metadata.mapValues(x => x.getReferenceCount()).filter(x => x._2 >0)
    val reference_coll_count = reference_coll.keys.countByValue
    addResult("ReferenceCount", reference_coll_count)

    //referencje odsetek
    
   val docs_with_refs_coll = metadata.mapValues(x => x.getReferenceCount())
   val docs_with_refs2_coll  = docs_with_refs_coll.mapValues(x => (if (x>0) { 1 } else { 0 }, 1))
   val docs_with_refs3_coll  = docs_with_refs2_coll.reduceByKey((x, y) => (x._1+y._1, x._2+y._2))
   val docs_with_refs4_coll  = docs_with_refs3_coll.mapValues(x => x._1.toFloat/x._2)
   val docs_with_refs_perc = docs_with_refs4_coll.collectAsMap()
   addResult("ReferencePercent", docs_with_refs_perc)
   
   //średnia ilość referencji w dokumencie posiadającym referencje
   
   val reference_list = metadata.mapValues(x => x.getReferenceList().asScala)
   val reference_len = reference_list.mapValues(x => x.length.toFloat).filter(x => x._2 > 0).mapValues(x => (x, 1))
   val reference_len_sum = reference_len.reduceByKey((x,y)=>(x._1+y._1, x._2+y._2))
   val reference_avg = reference_len_sum.mapValues(x => x._1/x._2)
   val reference_avg_count = reference_avg.collectAsMap()
   addResult("ReferenceCountAvg", reference_avg_count)
     
   //niepuste ref ID ilość
   
   val docs_with_extId = metadata.mapValues(x => x.getReferenceList.asScala.map(x => x.getExtIdCount()))
   val docs_with_extId_count = docs_with_extId.keys.countByValue
   addResult("DocsExtIdCount", docs_with_extId_count)
    
   //niepuste ref ID odsetek
   
   val refs_with_extId = metadata.flatMapValues(x => x.getReferenceList.asScala.map(x => x.getExtIdCount()))
   val refs_with_extId2 = refs_with_extId.mapValues(x => (if (x >0) { 1 } else { 0 }, 1))  
   val refs_with_extId3  = refs_with_extId2.reduceByKey((x, y) => (x._1+y._1, x._2+y._2))
   val refs_with_extId4 = refs_with_extId3.mapValues(x => x._1.toFloat/x._2)
   val refs_with_extId_perc = refs_with_extId4.collectAsMap()
   addResult("ReferenceExtIdPercent", refs_with_extId_perc)

   //dokumenty bez autorów ilość
   
   val docs_no_authors_coll = metadata.mapValues(x => x.getBasicMetadata.getAuthorCount()).filter(x => x._2 == 0)
   val docs_no_authors_count = docs_no_authors_coll.countByKey
   addResult("DocsNoAuthors", docs_no_authors_count)
 //??
    
   //średnia ilość autorów
   
   val authors = metadata.mapValues(x => x.getBasicMetadata.getAuthorCount().toFloat)
   val authors_len = authors.filter(x => x._2 > 0).mapValues(x => (x, 1))
   val authors_len_sum = authors_len.reduceByKey((x,y)=>(x._1+y._1, x._2+y._2))
   val authors_avg = authors_len_sum.mapValues(x => x._1/x._2)
   val authors_avg_count = authors_avg.collectAsMap()
   addResult("AuthorsCountAvg", docs_no_authors_count)
   
   //maksymalna ilość autorów
   
   val authors_max_count = authors.reduceByKey((x,y) => x max y).collectAsMap()
   addResult("AuthorsMaxCount", authors_max_count)  
  
   //dokumenty, w których choć jeden autor ma email, ilość
  
   val emails = metadata.mapValues(x => anyHasEmail(x.getBasicMetadata.getAuthorList.asScala))
   val docs_with_emails_count = emails.keys.countByValue
   addResult("DocsWithAuthorsEmails", docs_with_emails_count)
    
   //dokumenty, w których choć jeden autor ma email, odsetek
   
   val real_emails = emails.mapValues(x => (if (x == true) { 1 } else { 0 }, 1))
   val real_emails2 = real_emails.reduceByKey((x, y) => (x._1+y._1, x._2+y._2))
   val real_emails3  = real_emails2.mapValues(x => x._1.toFloat/x._2)
   val docs_authors_with_emails_perc = real_emails3.collectAsMap()
   addResult("DocsAuthorsEmailsPercent", docs_authors_with_emails_perc)

    //autorzy, którzy mają extID, ilość
   
   val authors_extId = metadata.flatMapValues(x => (x.getBasicMetadata.getAuthorList.asScala.map(x => x.getExtIdCount())))

   val authors_extId_count = authors_extId.keys.countByValue

//autorzy, którzy mają extId, odsetek
   
   val authors_with_extId2  = authors_extId.mapValues(x => (if (x>0) { 1 } else { 0 }, 1))
   val authors_with_extId3  = authors_with_extId2.reduceByKey((x, y) => (x._1+y._1, x._2+y._2))
   val authors_with_extId4  = authors_with_extId3.mapValues(x => x._1.toFloat/x._2)
   val authors_with_extId_perc = authors_with_extId4.collectAsMap()
   addResult("AuthorsExtIdPercent", authors_with_extId_perc)

   //afiliacje autorów, ilość
   
   val affiliations = metadata.mapValues(x => x.getAffiliationsCount()).filter(x => x._2 >0).map(x => x._1)
   val affiliations_count = affiliations.countByValue
   addResult("AffiliationCount", affiliations_count)
    
   //afiliacje autorów, odsetek
   
   val affs = metadata.mapValues(x => x.getAffiliationsCount())
   val docs_with_aff  = affs.mapValues(x => (if (x>0) { 1 } else { 0 }, 1))
   val docs_with_aff2  = docs_with_aff.reduceByKey((x, y) => (x._1+y._1, x._2+y._2))
   val docs_with_aff3  = docs_with_aff2.mapValues(x => x._1.toFloat/x._2)
   val docs_with_aff3_perc = docs_with_aff3.collectAsMap()
   addResult("AffiliationPercent", docs_with_aff3_perc)
   
     
   
    val output_file = new File(outputDocuments)
    val writer = new FileWriter(output_file)
    writer.write(coll_array.mkString("Variable, ", ", ", "\n"))
    for ((variable, result) <- results) {
      writer.write(result.mkString(variable+", " , ", ", "\n"))
    }
    writer.close()
  }
}
