package sk.posam.fsa.foodrescue.domain.user;

import sk.posam.fsa.foodrescue.domain.shared.ValidationException;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }

    public boolean isDeleted() {
        return status == UserStatus.DELETED;
    }

    public void activate() {
        status = UserStatus.ACTIVE;
    }

    public void block() {
        status = UserStatus.BLOCKED;
    }

    public void markDeleted() {
        status = UserStatus.DELETED;
    }

    public void prepareForCreation() {
        require(firstName != null && !firstName.isBlank(), "First name is required");
        require(lastName != null && !lastName.isBlank(), "Last name is required");
        require(email != null && !email.isBlank(), "Email is required");
        require(password != null && !password.isBlank(), "Password is required");
        require(role != null, "Role is required");

        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim();
        password = password.trim();

        if (status == null) {
            status = UserStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(message);
        }
    }
}


