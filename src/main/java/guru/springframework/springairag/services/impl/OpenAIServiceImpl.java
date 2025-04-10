package guru.springframework.springairag.services.impl;

import guru.springframework.springairag.model.Answer;
import guru.springframework.springairag.model.Question;
import guru.springframework.springairag.services.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAIServiceImpl implements OpenAIService {

    final ChatModel chatModel;
    final SimpleVectorStore vectorStore;

    @Value("classpath:templates/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    public OpenAIServiceImpl(ChatModel chatModel, SimpleVectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @Override
    public Answer getAnswer(Question question) {
        log.info("OpenAIServiceImpl.getAnswer(Question)");
        log.info("Question: {}", question.questionText());
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question.questionText())
                        .topK(5)
                        .build());
        List<String> contentList = documents.stream().map(Document::getContent).toList();
        // BeanOutputConverter<Answer> converter = new BeanOutputConverter<>(Answer.class);
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Prompt prompt = promptTemplate.create(Map.of("input", question.questionText(),
                "documents", String.join("\n", contentList)));

        contentList.forEach(s -> {
            System.out.println(s + "\n\n\n\n");
        });

        ChatResponse chatResponse = chatModel.call(prompt);
        return new Answer(chatResponse.getResult().getOutput().getText());
    }

}
