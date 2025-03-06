package com.authdemo.auth.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.*;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.sql.Timestamp;
import java.util.UUID;

;import static java.lang.String.valueOf;

@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "id")})
@Data
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(insertable = false)
    private Long id;

    @Column
    private String first_name;

    @Column
    private String last_name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String pass;

    @Column(unique = true, nullable = true)
    private String phoneNumber;

    @Column
    @ColumnDefault("0")
    private int verifyTries = 0;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'GUEST'")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Role role = Role.GUEST;

    @CreationTimestamp
    @Column
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column
    private Timestamp updatedAt;

    public User() {}
}
