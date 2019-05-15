package net;

public class InvalidMailException extends Exception {
        private String invalidEmail;

        public InvalidMailException(String invalidEmail) {
            super("Email '" + invalidEmail + "' is invalid!");
            this.invalidEmail = invalidEmail;
        }

        public String getInvalidEmail() {
            return invalidEmail;
        }

}
