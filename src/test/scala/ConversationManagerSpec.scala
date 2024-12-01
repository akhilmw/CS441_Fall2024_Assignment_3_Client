package services

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import org.scalatest.time.{Span, Seconds, Millis}
import org.scalatest.compatible.Assertion

class ConversationManagerSpec extends AsyncFlatSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("ConversationManagerTest")
  implicit val ec: ExecutionContext = system.dispatcher

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  val config = ConfigFactory.parseString("""
    ollama {
      host = "http://localhost:11434"
      model = "llama2"
      max-turns = 3
      request-timeout-seconds = 30
    }
  """)

  "ConversationManager" should "initialize conversation with correct state" in {
    val manager = new ConversationManager()
    val sessionId = "test-session"
    val initialQuery = "initial query"

    manager.initializeConversation(sessionId, initialQuery)
    val stateOpt = manager.getConversationHistory(sessionId)

    Future.successful {
      stateOpt shouldBe defined
      stateOpt.foreach { history =>
        history shouldBe empty
      }
      succeed
    }
  }


  it should "maintain conversation history" in {
    val manager = new ConversationManager()
    val sessionId = "test-session"
    val initialQuery = "test query"

    manager.initializeConversation(sessionId, initialQuery)
    val history = manager.getConversationHistory(sessionId)

    Future.successful {
      history shouldBe defined
      history.get shouldBe empty
      succeed
    }
  }

  it should "handle cleanup correctly" in {
    val manager = new ConversationManager()
    val sessionId = "test-session"

    manager.initializeConversation(sessionId, "test")
    manager.cleanup(sessionId)

    Future.successful {
      manager.getConversationHistory(sessionId) shouldBe None
      succeed
    }
  }

  // Clean up ActorSystem after all tests
  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}