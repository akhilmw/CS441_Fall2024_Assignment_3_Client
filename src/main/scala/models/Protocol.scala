package models


case class QueryRequest(query: String)

case class QueryResponse(
                          response: String
                        ) {
  // Ensure response is never null
  def this() = this("")

  // Clean the response string
  def cleanResponse: String = {
    Option(response)
      .map(_.replaceAll("\u0000", "").trim)
      .getOrElse("")
  }
}

// Bedrock specific models
case class TextGenerationConfig(
                                 maxTokenCount: Int,
                                 temperature: Double,
                                 topP: Double,
                                 stopSequences: Seq[String]
                               )

case class TextGenerationResult(
                                 outputText: String
                               )

case class TextGenerationResponse(
                                   results: Seq[TextGenerationResult]
                                 )