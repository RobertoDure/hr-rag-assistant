package ie.com.rag.mapper;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.entity.Candidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CandidateMapperInterface {

    @Mapping(source = "id", target = "id", qualifiedByName = "stringToUuid")
    CandidateDTO toDTO(Candidate candidate);

    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    Candidate toEntity(CandidateDTO candidateDTO);

    @Named("stringToUuid")
    default UUID stringToUuid(final String id) {
        return id == null ? null : UUID.fromString(id);
    }

    @Named("uuidToString")
    default String uuidToString(final UUID id) {
        return id == null ? null : id.toString();
    }
}
