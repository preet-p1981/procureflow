package com.procureflow.identity.domain;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class User {

    private final UserId id;
    private final EmailAddress email;
    private final String fullName;
    private final Set<Role> roles = new HashSet<>();
    private final long version;

    private String passwordHash;
    private UserStatus status;
    private int failedAttempts;
    private Instant lockedUntil;

    private User(UserId id, EmailAddress email, String passwordHash, String fullName,
                 UserStatus status, int failedAttempts, Instant lockedUntil,
                 Set<Role> roles, long version) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = requireText(passwordHash, "Password hash");
        this.fullName = requireText(fullName, "Full name");
        if (this.fullName.length() > 150) {
            throw new DomainException(CommonError.VALIDATION_FAILED, "Full name is too long");
        }
        this.status = Objects.requireNonNull(status);
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.roles.addAll(roles == null ? Set.of() : roles);
        this.version = version;
    }

    /** Creates a brand-new user. The caller has already hashed the password. */
    public static User register(EmailAddress email, String passwordHash,
                                String fullName, Set<Role> roles) {
        return new User(UserId.newId(), email, passwordHash, fullName,
                UserStatus.ACTIVE, 0, null, roles, 0L);
    }

    /** Rebuilds a user loaded from storage. Used only by the persistence adapter. */
    public static User reconstitute(UserId id, EmailAddress email, String passwordHash,
                                    String fullName, UserStatus status, int failedAttempts,
                                    Instant lockedUntil, Set<Role> roles, long version) {
        return new User(id, email, passwordHash, fullName, status,
                failedAttempts, lockedUntil, roles, version);
    }

    // ---- behaviour ----

    public boolean isLocked(Instant now) {
        return status == UserStatus.LOCKED && (lockedUntil == null || now.isBefore(lockedUntil));
    }

    /** Throws if this user must not log in right now. Does not change any state. */
    public void checkCanAuthenticate(Instant now) {
        if (status == UserStatus.DISABLED) {
            throw new DomainException(IdentityError.ACCOUNT_DISABLED, "Account is disabled");
        }
        if (isLocked(now)) {
            throw new DomainException(IdentityError.ACCOUNT_LOCKED, "Account is temporarily locked");
        }
    }

    public void recordFailedLogin(Instant now, LockoutPolicy policy) {
        if (status == UserStatus.DISABLED) {
            return;
        }
        if (status == UserStatus.LOCKED && !isLocked(now)) {
            resetLock();                       // the old lock has expired
        }
        if (isLocked(now)) {
            return;                            // already locked, nothing more to count
        }
        failedAttempts++;
        if (failedAttempts >= policy.maxFailedAttempts()) {
            status = UserStatus.LOCKED;
            lockedUntil = now.plus(policy.lockDuration());
        }
    }

    public void recordSuccessfulLogin() {
        if (status != UserStatus.DISABLED) {
            resetLock();
        }
    }

    public void disable() {
        status = UserStatus.DISABLED;
    }

    public void enable() {
        resetLock();
    }

    public void changePasswordHash(String newHash) {
        this.passwordHash = requireText(newHash, "Password hash");
    }

    public void assignRole(Role role) {
        roles.add(Objects.requireNonNull(role));
    }

    public void removeRole(String roleCode) {
        roles.removeIf(r -> r.code().equals(roleCode));
    }

    /** Role names (prefixed ROLE_) plus every permission those roles grant. */
    public Set<String> authorities() {
        Set<String> result = new HashSet<>();
        for (Role role : roles) {
            result.add("ROLE_" + role.code());
            result.addAll(role.permission());
        }
        return Set.copyOf(result);
    }

    // ---- read access (no setters on purpose) ----

    public UserId id() { return id; }
    public EmailAddress email() { return email; }
    public String fullName() { return fullName; }
    public String passwordHash() { return passwordHash; }
    public UserStatus status() { return status; }
    public int failedAttempts() { return failedAttempts; }
    public Instant lockedUntil() { return lockedUntil; }
    public Set<Role> roles() { return Set.copyOf(roles); }
    public long version() { return version; }

    // ---- helpers ----

    private void resetLock() {
        status = UserStatus.ACTIVE;
        failedAttempts = 0;
        lockedUntil = null;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CommonError.VALIDATION_FAILED, name + " must not be blank");
        }
        return value.trim();
    }
}