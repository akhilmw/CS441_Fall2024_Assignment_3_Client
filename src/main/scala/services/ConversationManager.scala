package services

import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.OllamaResult
import io.github.ollama4j.utils.Options
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}

class ConversationManager(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  private val ollamaAPI = {
    val api = new OllamaAPI(config.getString("ollama.host"))
    api.setRequestTimeoutSeconds(config.getInt("ollama.request-timeout-seconds"))
    api
  }

  private var conversations = Map[String, ConversationState]()

  case class ConversationState(
                                sessionId: String,
                                turnCount: Int,
                                lastQuery: String,
                                lastResponse: String,
                                conversationHistory: List[(String, String)]
                              )

  def generateNextQuery(sessionId: String, currentResponse: String, retries: Int = 3): Future[Option[String]] = {
    try {
      logger.info(s"Generating next query for session $sessionId")

      val stateOpt = conversations.get(sessionId)
      if (stateOpt.isEmpty) {
        logger.error(s"Session $sessionId not found")
        return Future.successful(None)
      }

      val state = stateOpt.get

      val prompt = generatePrompt(state, currentResponse)
      logger.debug(s"Generated prompt: $prompt")

      val options = new Options(new java.util.HashMap[String, Object]())

      Future {
        val result = ollamaAPI.generate(
          config.getString("ollama.model"),
          prompt,
          false,
          options
        )

        val rawResponse = if (result != null) result.getResponse else ""
        logger.debug(s"Raw response from Ollama: '$rawResponse'")

        val responseText = rawResponse.trim
        val isNonEmpty = responseText.nonEmpty
        val containsTermination = responseText.toLowerCase.contains("conversation_end")

        logger.debug(s"Response text after trim: '$responseText'")
        logger.debug(s"isNonEmpty: $isNonEmpty, containsTermination: $containsTermination")

        val nextQuery = if (isNonEmpty && !containsTermination) {
          Some(responseText)
        } else {
          None
        }

        logger.debug(s"Next query: $nextQuery")

        nextQuery.foreach { query =>
          updateConversationState(sessionId, query, currentResponse)
        }

        nextQuery
      }.recoverWith {
        case ex: Exception if retries > 0 =>
          logger.warn(s"Failed to connect to Ollama, retrying... ($retries attempts left)")
          Thread.sleep(2000) // Wait 2 seconds before retry
          generateNextQuery(sessionId, currentResponse, retries - 1)
        case ex: Exception =>
          logger.error(s"Error calling Ollama API: ${ex.getMessage}", ex)
          Future.successful(None)
      }

    } catch {
      case ex: Exception =>
        logger.error(s"Unexpected error in generateNextQuery: ${ex.getMessage}", ex)
        Future.successful(None)
    }
  }



  def initializeConversation(sessionId: String, initialQuery: String): Unit = {
    logger.debug(s"Initializing conversation for session $sessionId with query: $initialQuery")
    synchronized {
      conversations += (sessionId -> ConversationState(
        sessionId = sessionId,
        turnCount = 1,
        lastQuery = initialQuery,
        lastResponse = "",
        conversationHistory = List.empty
      ))
    }
  }

  private def generatePrompt(state: ConversationState, currentResponse: String): String = {
    val historyContext = state.conversationHistory
      .takeRight(5)
      .map { case (q, r) =>
        s"""User: $q
           |Assistant: $r""".stripMargin
      }.mkString("\n\n")

    s"""Given this conversation history:
       |$historyContext
       |
       |Last response: $currentResponse
       |
       |Generate a natural follow-up question that continues this conversation.
       |The question should be direct and focused on the most interesting aspect of the last response.
       |Keep the question concise (1-2 sentences).
       |If the conversation seems complete, respond with 'CONVERSATION_END'.
       |
       |Follow-up question:""".stripMargin
  }

  private def shouldContinueConversation(state: ConversationState): Boolean = {
    val maxTurns = config.getInt("ollama.max-turns")
    state.turnCount < maxTurns
  }

  private def updateConversationState(sessionId: String, query: String, response: String): Unit = {
    synchronized {
      conversations.get(sessionId).foreach { state =>
        conversations += (sessionId -> state.copy(
          // Remove turnCount increment
          lastQuery = query,
          lastResponse = response,
          conversationHistory = state.conversationHistory :+ (query, response)
        ))
      }
    }
  }

  def getConversationHistory(sessionId: String): Option[List[(String, String)]] = {
    conversations.get(sessionId).map(_.conversationHistory)
  }

  def cleanup(sessionId: String): Unit = {
    synchronized {
      conversations -= sessionId
    }
  }
}