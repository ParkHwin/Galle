package com.gallae.dto;

import com.gallae.entity.Favorite;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoriteDto {
    private Long id;
    private String from;
    private String to;
    private String label;

    public static FavoriteDto from(Favorite favorite) {
        return FavoriteDto.builder()
                .id(favorite.getId())
                .from(favorite.getDepartureCity())
                .to(favorite.getArrivalCity())
                .label(favorite.getLabel())
                .build();
    }
}
