package ishaan_rag.service;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

    @Bean
    VectorStore vectorStore(EmbeddingClient ec,
                            JdbcTemplate t) {
        return new PgVectorStore(t, ec);
    }

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    static void init(VectorStore vectorStore, JdbcTemplate template, Resource pdfResource) throws Exception {
        // Delete existing records in vector store
        template.update("DELETE FROM vector_store");

        // Load the PDF document using PDFBox
        try (PDDocument document = PDDocument.load(pdfResource.getInputStream())) {
            // Initialize PDFTextStripper to extract text
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // Set up page range to extract text from
            pdfStripper.setStartPage(1);  // Start from the first page
            pdfStripper.setEndPage(document.getNumberOfPages());  // Extract all pages

            // Extract the text from the PDF
            String extractedText = pdfStripper.getText(document);

            // Check if extracted text is not empty
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new Exception("No text extracted from the PDF.");
            }

            // Create Document objects from the extracted text
            List<Document> documents = new ArrayList<>();
            documents.add(new Document(extractedText));  // Assuming you have one document here

            // Initialize the text splitter
            var textSplitter = new TokenTextSplitter();

            // Split the documents using the TokenTextSplitter
            List<Document> splitDocuments = textSplitter.apply(documents);

            // Optionally, log the resulting split documents to check the chunks
            for (Document doc : splitDocuments) {
                System.out.println(doc.getContent());
            }

            // Store the split documents in the vector store
            vectorStore.accept(splitDocuments);

        } catch (IOException e) {
            throw new Exception("Error processing PDF", e);
        }
    }

    @Bean
    ApplicationRunner applicationRunner(
            Chatbot chatbot,
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            @Value("file://${HOME}/Downloads/RAG-LLM-Spring-AI/service/Dentist FAQs (Final 12-12-2019).pdf") Resource resource) {
        return args -> {
            init(vectorStore, jdbcTemplate, resource);
            var response = chatbot.chat("what should I know about the transition to consumer direct care network washington?");
            System.out.println(Map.of("response", response));
        };
    }

}


@Component
class Chatbot {


    private final String template = """
                        
            You're assisting with questions about services offered by Carina.
            Carina is a two-sided healthcare marketplace focusing on home care aides (caregivers)
            and their Medicaid in-home care clients (adults and children with developmental disabilities and low income elderly population).
            Carina's mission is to build online tools to bring good jobs to care workers, so care workers can provide the
            best possible care for those who need it.
                    
            Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
            If unsure, simply state that you don't know.
                    
            DOCUMENTS:
            {documents}
                        
            """;
    private final ChatClient aiClient;
    private final VectorStore vectorStore;

    Chatbot(ChatClient aiClient, VectorStore vectorStore) {
        this.aiClient = aiClient;
        this.vectorStore = vectorStore;
    }

    public String chat(String message) {
        var listOfSimilarDocuments = this.vectorStore.similaritySearch(message);
        var documents = listOfSimilarDocuments
                .stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
        var systemMessage = new SystemPromptTemplate(this.template)
                .createMessage(Map.of("documents", documents));
        var userMessage = new UserMessage(message);
        var prompt = new Prompt(List.of(systemMessage, userMessage));
        var aiResponse = aiClient.call(prompt);
        return aiResponse.getResult().getOutput().getContent();
    }
} // ...












