package com.example.symply_care.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.hibernate.annotations.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@Setter
@Getter
@ToString
public class User {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 45)
    private String email;

    @Column(nullable = false, length = 64)
    private String password;

    private String city;

    private String country;

    private String street;

    @Lob
    @Column(columnDefinition = "mediumblob")
    private byte[] imageData;

    @DateTimeFormat(pattern="dd/MM/yyyy")
    private String birthDay;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", street='" + street + '\'' +
                ", birthDay='" + birthDay + '\'' +
                '}';
    }


}
