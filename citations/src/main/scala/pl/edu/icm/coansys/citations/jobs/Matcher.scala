/*
 * (C) 2010-2012 ICM UW. All rights reserved.
 */

package pl.edu.icm.coansys.citations.jobs

import com.nicta.scoobi.application.ScoobiApp
import com.nicta.scoobi.core.DList
import com.nicta.scoobi.Persist._
import com.nicta.scoobi.InputsOutputs._
import pl.edu.icm.coansys.importers.models.PICProtos
import pl.edu.icm.coansys.citations.util.AugmentedDList.augmentDList
import pl.edu.icm.coansys.citations.indices.{EntityIndex, AuthorIndex}
import pl.edu.icm.coansys.citations.util.{misc, BytesConverter}
import pl.edu.icm.coansys.citations.util.misc.readCitationsFromDocumentsFromSeqFiles
import pl.edu.icm.coansys.citations.data._
import feature_calculators._
import pl.edu.icm.cermine.tools.classification.features.FeatureVectorBuilder
import scala.Some
import collection.JavaConversions._

/**
 * @author Mateusz Fedoryszak (m.fedoryszak@icm.edu.pl)
 */
object Matcher extends ScoobiApp {
  override def upload = false

  private implicit val picOutConverter =
    new BytesConverter[PICProtos.PicOut](_.toByteArray, PICProtos.PicOut.parseFrom(_))

  /**
   * A heuristics to retrieve documents that are most probable to be associated with a given citation.
   *
   * Algorithm:
   * 1.	Retrieve all documents which contain at least one non-exactly matching author.
   * 2.	Let M=maximum number of matching authors.
   * 3.	Filter out documents containing less than M-1 matching authors.
   *
   * @param citation a citation to process
   * @param index an index to be used for document retrieval
   * @return matching documents
   */
  def approximatelyMatchingDocuments(citation: CitationEntity, index: AuthorIndex): Iterable[EntityId] = {
    val documentsWithMatchNo =
      citation.normalisedAuthorTokens
        .flatMap(tok => index.getDocumentsByAuthor(tok).filterNot(_ == citation.entityId))
        .groupBy(identity)
        .map {
        case (doc, iterable) => (doc, iterable.size)
      }

    val maxMatchNo =
      if (!documentsWithMatchNo.isEmpty)
        documentsWithMatchNo.values.max
      else
        0
    val minMatchNo = math.max(maxMatchNo - 1, citation.normalisedAuthorTokens.size.toDouble * 2 / 3)
    documentsWithMatchNo.filter {
      case (doc, matchNo) => matchNo >= minMatchNo
    }.keys
  }

  def approximatelyMatchingDocumentsStats(citation: CitationEntity, index: AuthorIndex) = {
    val documentsWithMatchNo =
      citation.normalisedAuthorTokens
        .flatMap(tok => index.getDocumentsByAuthor(tok).filterNot(_ == citation.entityId))
        .groupBy(identity)
        .map {
        case (doc, iterable) => (doc, iterable.size)
      }

    val maxMatchNo =
      if (!documentsWithMatchNo.isEmpty)
        documentsWithMatchNo.values.max
      else
        0
    val minMatchNo = math.max(maxMatchNo - 1, citation.normalisedAuthorTokens.size.toDouble * 2 / 3)
    val docs = documentsWithMatchNo.filter {
      case (doc, matchNo) => matchNo >= minMatchNo
    }.keys

    ('"' + citation.rawText.filterNot(_ == '"') + '"', citation.normalisedAuthorTokens.mkString(" "), docs.size, maxMatchNo)
  }

  type EntityId = String

  def addHeuristic(citations: DList[CitationEntity], indexUri: String): DList[(CitationEntity, EntityId)] =
    citations
      .flatMapWithResource(new AuthorIndex(indexUri)) {
      case (index, cit) => Stream.continually(cit) zip approximatelyMatchingDocuments(cit, index)
    }.groupByKey[CitationEntity, EntityId].flatMap {
      case (cit, ents) => Stream.continually(cit) zip ents
    }

  def extractGoodMatches(citationsWithEntityIds: DList[(CitationEntity, EntityId)], entityIndexUri: String): DList[(CitationEntity, (EntityId, Double))] =
    citationsWithEntityIds.flatMapWithResource(new ScalaObject {
      val index = new EntityIndex(entityIndexUri)
      val similarityMeasurer = new SimilarityMeasurer

      def close() {
        index.close()
      }
    }) {
      case (res, (cit, entityId)) =>
        val minimalSimilarity = 0.5
        val entity = res.index.getEntityById(entityId) // DocumentMetadata.parseFrom(index.get(docId).copyBytes)
        val similarity = res.similarityMeasurer.similarity(cit, entity)
        if (similarity >= minimalSimilarity)
          Some(cit, (entityId, similarity))
        else
          None
    }

  def extractBestMatches(matchesCandidates: DList[(CitationEntity, (EntityId, Double))]) =
    matchesCandidates.groupByKey[CitationEntity, (EntityId, Double)].map {
      case (cit, matches) =>
        val best = matches.maxBy(_._2)._1
        (cit, best)
    }

  def extractBestMatches(citationsWithEntityIds: DList[(CitationEntity, EntityId)], entityIndexUri: String) =
    citationsWithEntityIds
      .groupByKey[CitationEntity, EntityId]
      .flatMapWithResource(new EntityIndex(entityIndexUri)) {
      case (index, (cit, entityIds)) =>
        val minimalSimilarity = 0.5
        val aboveThreshold =
          entityIds
            .map {
            entityId =>
              val entity = index.getEntityById(entityId) // DocumentMetadata.parseFrom(index.get(docId).copyBytes)
              (entity, cit.similarityTo(entity))
          }
            .filter(_._2 >= minimalSimilarity)
        if (!aboveThreshold.isEmpty) {
          val target = aboveThreshold.maxBy(_._2)._1
          val sourceUuid = cit.sourceDocKey
          val position = cit.position
          val targetExtId = target.asInstanceOf[DocumentEntity].extId
          Some(sourceUuid, (position, targetExtId))
        }
        else
          None
    }

  def reformatToPicOut(matches: DList[(String, (Int, String))], keyIndexUri: String) =
    matches.groupByKey[String, (Int, String)]
      .mapWithResource(new EntityIndex(keyIndexUri)) {
      case (index, (sourceUuid, refs)) =>
        val sourceEntity = index.getEntityById("doc-" + sourceUuid).asInstanceOf[DocumentEntity]

        val outBuilder = PICProtos.PicOut.newBuilder()
        outBuilder.setDocId(sourceEntity.extId)
        for ((position, targetExtId) <- refs) {
          outBuilder.addRefs(PICProtos.Reference.newBuilder().setDocId(targetExtId).setRefNum(position))
        }

        (sourceUuid, outBuilder.build())
    }

  def matches(citations: DList[CitationEntity], keyIndexUri: String, authorIndexUri: String) = {
    reformatToPicOut(extractBestMatches(addHeuristic(citations, authorIndexUri), keyIndexUri), keyIndexUri)
  }

  def matchesDebug(citations: DList[CitationEntity], keyIndexUri: String, authorIndexUri: String) = {
    extractGoodMatches(addHeuristic(citations, authorIndexUri), keyIndexUri).mapWithResource(new EntityIndex(keyIndexUri)) {
      case (index, (citation, (entityId, similarity))) =>
        val entity = index.getEntityById(entityId)
        val featureVectorBuilder = new FeatureVectorBuilder[Entity, Entity]
        featureVectorBuilder.setFeatureCalculators(List(
          AuthorTrigramMatchFactor,
          AuthorTokenMatchFactor,
          PagesMatchFactor,
          SourceMatchFactor,
          TitleMatchFactor,
          YearMatchFactor))
        val fv = featureVectorBuilder.getFeatureVector(citation, entity)
        (citation.toReferenceString + " : " + entity.toReferenceString + " : " + similarity + " : " + fv.dump() + "\n")
    }
  }

  def heuristicStats(citations: DList[CitationEntity], keyIndexUri: String, authorIndexUri: String) = {
    citations.mapWithResource(new AuthorIndex(authorIndexUri)) {
      case (index, cit) =>
        approximatelyMatchingDocumentsStats(cit, index)
    }
  }

  def run() {
    configuration.set("mapred.max.split.size", 500000)
//    configuration.set("mapred.task.timeout", 4 * 60 * 60 * 1000)
    configuration.setMinReducers(4)

    val parserModelUri = args(0)
    val keyIndexUri = args(1)
    val authorIndexUri = args(2)
    val documentsUri = args(3)
    val outUri = args(4)

    val myMatchesDebug = matchesDebug(readCitationsFromDocumentsFromSeqFiles(List(documentsUri), parserModelUri), keyIndexUri, authorIndexUri)

    implicit val stringConverter = new BytesConverter[String](misc.uuidEncode, misc.uuidDecode)
    implicit val picOutConverter = new BytesConverter[PICProtos.PicOut](_.toByteString.toByteArray, PICProtos.PicOut.parseFrom(_))
    persist(toTextFile(myMatchesDebug, outUri, overwrite = true))
//    persist(toTextFile(heuristicStats(readCitationsFromDocumentsFromSeqFiles(List(documentsUri), parserModelUri), keyIndexUri, authorIndexUri), outUri, overwrite = true))
  }
}