LLM-RAG-SPRING_AI

## Overview
This project is designed to process PDF files, extract text from them, split the extracted text into manageable chunks, and store those chunks in a vector store for further processing. The project integrates with OpenAI to process the extracted text and get responses. It also stores the resulting chunks in a vector store and provides easy integration with `JdbcTemplate` for database interactions.

## Features
- Extracts text from PDF files using PDFBox.
- Splits the extracted text into chunks using a custom `TokenTextSplitter`.
- Stores the resulting chunks in a vector store.
- Provides easy integration with `JdbcTemplate` for database interactions.

## Prerequisites
- Java 17 or higher
- Maven or Gradle for dependency management
- Required libraries:
  - Apache PDFBox for PDF text extraction
  - A custom `TokenTextSplitter` class
  - JDBC support for database interaction
  - OpenAI API integration (for processing and retrieving responses from OpenAI)
