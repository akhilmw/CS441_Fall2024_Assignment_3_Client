package services

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}
import scala.collection.concurrent.TrieMap

/** ConversationRecorder.scala
 * Service responsible for persisting conversation logs to the filesystem.
 * Manages conversation file creation, appending turns, and final conversation summaries.
 */

class ConversationRecorder {
  private val logger = LoggerFactory.getLogger(getClass)
  // Directory for storing conversation logs, configurable via environment
  private val conversationsDir = sys.env.getOrElse("CONVERSATIONS_DIR", "/app/conversations")
  // Thread-safe map to track active conversation files and turn counts
  private val activeFiles = TrieMap[String, (Path, Int)]() // Path and turn count

  /** Initialize a new conversation file with headers
   * @param sessionId Unique identifier for the conversation
   * @return Tuple of (file path, initial turn count)
   */

  private def initializeFile(sessionId: String): (Path, Int) = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val filename = s"conversation_${sessionId}_$timestamp.txt"
    val dir = Paths.get(conversationsDir)

    // Ensure conversations directory exists
    if (!Files.exists(dir)) {
      Files.createDirectories(dir)
    }

    val filePath = dir.resolve(filename)
    if (!Files.exists(filePath)) {
      Files.createFile(filePath)
      // Write header
      // Write conversation header with metadata
      val header = s"""Conversation ID: $sessionId
                      |Started at: $timestamp
                      |==================================================
                      |
                      |Conversation Log:
                      |
                      |""".stripMargin
      Files.write(filePath, header.getBytes, StandardOpenOption.WRITE)
    }

    (filePath, 0)
  }

  /** Get or create file path and turn count for a session
   * @param sessionId Unique conversation identifier
   * @return Tuple of (file path, current turn count)
   */

  private def getFilePathAndTurn(sessionId: String): (Path, Int) = {
    activeFiles.getOrElseUpdate(sessionId, initializeFile(sessionId))
  }

  /** Append a conversation turn to the log file
   * @param sessionId Conversation identifier
   * @param turn Tuple of (question, response)
   */
  def appendToConversation(sessionId: String, turn: (String, String)): Unit = {
    val (filePath, currentTurn) = getFilePathAndTurn(sessionId)
    val nextTurn = currentTurn + 1

    val content = s"""Turn $nextTurn
                     |Question: ${turn._1.trim}
                     |Response: ${turn._2.trim}
                     |${"-" * 80}
                     |
                     |""".stripMargin

    Files.write(
      filePath,
      content.getBytes,
      StandardOpenOption.APPEND
    )

    activeFiles.put(sessionId, (filePath, nextTurn))
    logger.debug(s"Appended turn $nextTurn to conversation $sessionId")
  }

  /** Save final conversation summary and metadata
   * @param sessionId Conversation identifier
   * @param conversation Complete list of conversation turns
   */

  def saveConversation(sessionId: String, conversation: List[(String, String)]): Unit = {
    try {
      val (filePath, _) = getFilePathAndTurn(sessionId)
      val summary = s"""
                       |==================================================
                       |Conversation Summary:
                       |Total Turns: ${conversation.length}
                       |Completed at: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
                       |==================================================
                       |""".stripMargin

      Files.write(
        filePath,
        summary.getBytes,
        StandardOpenOption.APPEND
      )

      logger.info(s"Saved conversation summary for session $sessionId")
    } catch {
      case ex: Exception =>
        logger.error(s"Failed to save conversation: ${ex.getMessage}", ex)
    }
  }

  /** Clean up resources for completed conversation
   * @param sessionId Conversation to clean up
   */

  def cleanup(sessionId: String): Unit = {
    activeFiles.remove(sessionId)
  }
}