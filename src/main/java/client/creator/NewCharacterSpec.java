package client.creator;

import client.SkinColor;
import lombok.Builder;

@Builder
public record NewCharacterSpec(
        String name,
        JobType type,
        int face,
        int hair,
        int hairColor,
        SkinColor skin,
        int topItemId,
        int bottomItemId,
        int shoesItemId,
        int weaponItemId,
        byte gender
) {
}
