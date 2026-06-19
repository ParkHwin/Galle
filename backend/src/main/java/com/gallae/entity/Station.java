package com.gallae.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String type; // ktx, srt, bus_terminal

    @Column(nullable = false, length = 30)
    private String city;

    @Column(length = 20)
    private String code;

    @Column(length = 500)
    private String bookingUrl;
}
