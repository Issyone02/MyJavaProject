package controller;

/** Authentication contract for the login screen. Implemented by utils.AuthManager. */
public interface AuthController {
    boolean validate(int userId, String rawPassword);
    String  getFullName(int userId);
    void    logFailedAttempt(String attemptedId);
}
