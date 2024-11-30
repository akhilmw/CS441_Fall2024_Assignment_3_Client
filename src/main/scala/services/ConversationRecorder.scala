package services

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}
import scala.collection.concurrent.TrieMap

class ConversationRecorder {
  private val logger = LoggerFactory.getLogger(getClass)
  private val conversationsDir = "conversations"
  private val activeFiles = TrieMap[String, (Path, Int)]() // Path and turn count

  private def initializeFile(sessionId: String): (Path, Int) = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val filename = s"conversation_${sessionId}_$timestamp.txt"
    val dir = Paths.get(conversationsDir)

    if (!Files.exists(dir)) {
      Files.createDirectories(dir)
    }

    val filePath = dir.resolve(filename)
    if (!Files.exists(filePath)) {
      Files.createFile(filePath)
      // Write header
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

  private def getFilePathAndTurn(sessionId: String): (Path, Int) = {
    activeFiles.getOrElseUpdate(sessionId, initializeFile(sessionId))
  }

  def appendToConversation(sessionId: String, turn: (String, String)): Unit = {
    //    try {
    //
    //    } catch {
    //      case ex: Exception =>
    //        logger.error(s"Failed to append turn: ${ex.getMessage}", ex)
    //    }
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

  def cleanup(sessionId: String): Unit = {
    activeFiles.remove(sessionId)
  }
}