package ie.com.rag;

import ie.com.rag.service.RagDocumentService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class RagWiserApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagWiserApplication.class, args);
	}

	@Bean
	public List<ToolCallback> ragTools(RagDocumentService mcpService) {
		return List.of(ToolCallbacks.from(mcpService));
	}

}
