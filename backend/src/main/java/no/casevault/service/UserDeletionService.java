package no.casevault.service;

import no.casevault.exception.CannotDeleteUserException;
import no.casevault.model.User;
import no.casevault.repository.ActivityLogRepository;
import no.casevault.repository.CaseRepository;
import no.casevault.repository.CaseShareRepository;
import no.casevault.repository.ConversationRepository;
import no.casevault.repository.DocumentRepository;
import no.casevault.repository.MessageRepository;
import no.casevault.repository.PasswordResetCodeRepository;
import no.casevault.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserDeletionService {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final DocumentRepository documentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CaseShareRepository caseShareRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;

    public UserDeletionService(UserRepository userRepository, CaseRepository caseRepository,
                                DocumentRepository documentRepository, ActivityLogRepository activityLogRepository,
                                CaseShareRepository caseShareRepository, MessageRepository messageRepository,
                                ConversationRepository conversationRepository,
                                PasswordResetCodeRepository passwordResetCodeRepository) {
        this.userRepository = userRepository;
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.activityLogRepository = activityLogRepository;
        this.caseShareRepository = caseShareRepository;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
    }

    public void deleteUser(User user) {
        if (hasHistory(user)) {
            throw new CannotDeleteUserException();
        }

        passwordResetCodeRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    private boolean hasHistory(User user) {
        return caseRepository.existsByAssignedOfficer(user)
            || documentRepository.existsByAuthor(user)
            || activityLogRepository.existsByActor(user)
            || caseShareRepository.existsBySharedByOrSharedWithUser(user, user)
            || messageRepository.existsBySenderOrRecipient(user, user)
            || conversationRepository.existsByUserAOrUserB(user, user);
    }
}
