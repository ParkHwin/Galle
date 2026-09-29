package com.gallae.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fares",
    uniqueConstraints = @UniqueConstraint(columnNames = {"transport_type", "departure_id", "arrival_id", "class_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transport_type", nullable = false, length = 20)
    private String transportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id")
    private Station departure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_id")
    private Station arrival;

    @Column(name = "class_type", length = 10)
    private String classType; // standard, first

    @Column(nullable = false)
    private Long fare;
}
