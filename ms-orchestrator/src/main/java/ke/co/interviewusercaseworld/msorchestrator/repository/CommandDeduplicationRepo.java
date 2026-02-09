package ke.co.interviewusercaseworld.msorchestrator.repository;

import ke.co.interviewusercaseworld.msorchestrator.model.entities.CommandDeDuplication;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface CommandDeduplicationRepo extends ReactiveCrudRepository<CommandDeDuplication, UUID> {
}
