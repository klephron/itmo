package ru.itmo.zavar.devopslabs.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotEmpty
    private String driveName;

    @NotNull
    @Min(0)
    private Integer channelsUsed;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "network_id")
    private NetworkEntity network;
}
