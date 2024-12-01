package services

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.after
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import com.typesafe.config.ConfigFactory

class AutoConversationHandler(
                               bedrockServerUrl: String,
                               conversationManager: ConversationManager,
                               recorder: ConversationRecorder
                             )(implicit ec: ExecutionContext, system: ActorSystem) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  private val MaxTurns = config.getInt("ollama.max-turns")

  // Models for JSON communication
  case class QueryRequest(query: String)
  case class QueryResponse(response: String)

  def startAutonomousConversation(initialQuery: String): Future[String] = {
    val sessionId = java.util.UUID.randomUUID().toString
    logger.info(s"Starting autonomous conversation $sessionId with query: $initialQuery")

    conversationManager.initializeConversation(sessionId, initialQuery)
    processTurns(sessionId, initialQuery, 1)
  }

  private def processTurns(sessionId: String, query: String, turnCount: Int): Future[String] = {
    if (turnCount > MaxTurns) {
      logger.info(s"Reached max turns ($MaxTurns) for session $sessionId")
      saveAndFinish(sessionId)
    } else {
      logger.info(s"Processing turn $turnCount for session $sessionId")

      processQuery(query).flatMap { response =>
        recorder.appendToConversation(sessionId, (query, response.response))

        // Introduce a non-blocking delay
        after(2.seconds, system.scheduler)(Future.unit).flatMap { _ =>
          conversationManager.generateNextQuery(sessionId, response.response).flatMap {
            case Some(nextQuery) =>
              logger.info(s"Generated next query for turn ${turnCount + 1}")
              processTurns(sessionId, nextQuery, turnCount + 1)
            case None =>
              logger.info(s"No more queries to process for session $sessionId")
              saveAndFinish(sessionId)
          }
        }
      }.recoverWith { case ex =>
        logger.error(s"Error in process turn: ${ex.getMessage}", ex)
        saveAndFinish(sessionId)
      }
    }
  }

  private def processQuery(query: String): Future[QueryResponse] = {
    def attemptQuery(retries: Int): Future[QueryResponse] = {
      logger.info(s"Attempting to connect to server URL: ${bedrockServerUrl}/query (attempt ${5 - retries + 1})")

      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = s"$bedrockServerUrl/query",
        entity = HttpEntity(
          ContentTypes.`application/json`,
          QueryRequest(query).asJson.noSpaces
        )
      )

      Http().singleRequest(request).flatMap { response =>
        response.status match {
          case StatusCodes.OK =>
            Unmarshal(response.entity).to[String].map { jsonStr =>
              decode[QueryResponse](jsonStr) match {
                case Right(queryResponse) => queryResponse
                case Left(error) =>
                  throw new RuntimeException(s"Failed to decode response: ${error.getMessage}")
              }
            }
          case _ =>
            Unmarshal(response.entity).to[String].flatMap { errorBody =>
              Future.failed(new RuntimeException(s"Request failed with status ${response.status}: $errorBody"))
            }
        }
      }.recoverWith {
        case ex if retries > 0 =>
          logger.warn(s"Connection failed, retrying in 2 seconds... (${retries - 1} retries left)")
          after(2.seconds, system.scheduler)(attemptQuery(retries - 1))
        case ex =>
          Future.failed(ex)
      }
    }

    attemptQuery(5) // Start with 5 retries
  }

  private def saveAndFinish(sessionId: String): Future[String] = Future {
    try {
      conversationManager.getConversationHistory(sessionId).foreach { history =>
        recorder.saveConversation(sessionId, history)
      }
      sessionId
    } catch {
      case ex: Exception =>
        logger.error(s"Error saving conversation: ${ex.getMessage}")
        sessionId
    }
  }
}