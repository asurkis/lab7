package net;

public class InvalidMailException extends Exception {
        private String invalidEmail;

        public InvalidMailException(String invalidEmail) {
            super("Email '" + invalidEmail + "' is invalid!");
            this.invalidEmail = invalidEmail;
        }

        @Override
        public String getMessage() {
            return super.getMessage();
        }

        public String getInvalidEmail() {
            return invalidEmail;
        }

}
