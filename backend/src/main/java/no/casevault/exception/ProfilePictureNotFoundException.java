package no.casevault.exception;

public class ProfilePictureNotFoundException extends RuntimeException {
    public ProfilePictureNotFoundException() {
        super("This user has no profile picture set");
    }
}
