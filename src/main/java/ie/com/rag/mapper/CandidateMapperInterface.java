package ie.com.rag.mapper;

import ie.com.rag.dto.CandidateDTO;
import ie.com.rag.entity.Candidate;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface CandidateMapperInterface {

    CandidateMapperInterface INSTANCE = Mappers.getMapper(CandidateMapperInterface.class);

    CandidateDTO toDTO(Candidate candidate);

    Candidate toEntity(CandidateDTO candidateDTO);
}
