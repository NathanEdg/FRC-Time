package activities;

import helpers.InputValidation;

public record MemberRegistration(String firstName, String lastName, String email) {
  public MemberRegistration {
    if (!InputValidation.isValidMember(firstName, lastName, email)) {
      throw new IllegalArgumentException("Enter your first name, last name, and a valid email.");
    }
    firstName = firstName.trim();
    lastName = lastName.trim();
    email = email.trim();
  }
}
