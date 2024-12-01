import akka.actor.ActorSystem
import services.{ConversationManager, ConversationRecorder, AutoConversationHandler}
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory

object Main extends App {
  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem("ollama-system")
  implicit val executionContext: ExecutionContext = system.dispatcher

  try {
    val conversationManager = new ConversationManager()
    val recorder = new ConversationRecorder()

    // Get server URL from environment variable instead of config
    val bedrockServerUrl = sys.env.getOrElse("BEDROCK_SERVER_URL", "http://bedrock-server:8080")
    logger.info(s"Using Bedrock server URL: $bedrockServerUrl")  // Added logging

    val autoHandler = new AutoConversationHandler(
      bedrockServerUrl = bedrockServerUrl,
      conversationManager = conversationManager,
      recorder = recorder
    )

    // Get initial query from config
    val initialQuery = config.getString("client.initial-query")
    logger.info(s"Starting conversation with initial query: $initialQuery")

    // Start the conversation and wait for completion
    val conversationFuture = autoHandler.startAutonomousConversation(initialQuery)

    val sessionId = Await.result(conversationFuture, 10.minutes)
    logger.info(s"Conversation completed successfully. Session ID: $sessionId")

  } catch {
    case ex: Exception =>
      logger.error(s"Error during conversation: ${ex.getMessage}", ex)
  } finally {
    // Cleanup and shutdown
    system.terminate()
    logger.info("System terminated")
  }
}