package com.spendwisebackend.spendwisebackend.user.repositories.Projection;

import java.util.UUID;

public interface UserLoginProjection {
    UUID getId();

    String getPassword();

    String getName();

    String getEmail();

    String getRole();

    Boolean getActive();
}
