# CS441 - Fall 2024 - Assignment 3 Client: Ollama Conversational Agent
### Author : Akhil S Nair
### Email : anair56@uic.edu

## Project Description
This project implements the client component of a distributed conversational system, utilizing Ollama for local LLM processing and interacting with a cloud-based Bedrock server. The client maintains autonomous conversations by generating follow-up queries based on server responses.

## Demo Video
[Project Demo and Deployment Walkthrough Video](https://youtu.be/PKA09SNOq60?si=z3r6hO1_Hq6M23sv)

## Architecture
- **Conversation Management**: Handles conversation flow and state
- **Ollama Integration**: Local LLM for query generation
- **Server Communication**: HTTP client for Bedrock server interaction
- **Conversation Storage**: Local storage for conversation logs

## Prerequisites
- Java 11 or higher
- SBT
- Docker (for containerized deployment)
- Ollama installed locally

### Installation

#### Local Setup

1. Clone the git repository
```bash
git clone git@github.com:akhilmw2/CS441_Fall2024_Assignment_3_Client.git
cd CS441_Fall2024_Assignment_3_Client
```
2. Build and Run using SBT
```
ollama serve (Make sure ollama is installed locally)
sbt clean compile
sbt run
```

### Local Docker Deployment
Visit the Serve ReadME file

###Built With
Scala 2.13
Akka HTTP Client
Ollama4j
SBT

###Conclusion
The client component effectively manages automated conversations between local Ollama and cloud-based Bedrock services, demonstrating practical implementation of a hybrid LLM system with robust conversation management and storage capabilities.

## Acknowledgments
I would like to thank Professor Mark Grechanik for his guidance and instruction in the CS 441 course, which has been invaluable in developing and refining this project. Special thanks to TA Vasu Garg for support and insights throughout the process.

