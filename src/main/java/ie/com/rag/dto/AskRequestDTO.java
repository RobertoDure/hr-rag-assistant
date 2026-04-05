package ie.com.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequestDTO(
        @NotBlank(message = "Question must not be blank")
        String question
) {
}
