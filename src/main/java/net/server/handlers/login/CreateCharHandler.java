/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.handlers.login;

import client.Client;
import client.SkinColor;
import client.creator.CharacterCreator;
import client.creator.JobType;
import client.creator.NewCharacterSpec;
import client.creator.novice.BeginnerCreator;
import client.creator.novice.LegendCreator;
import client.creator.novice.NoblesseCreator;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import tools.PacketCreator;

import java.util.Optional;

public final class CreateCharHandler extends AbstractPacketHandler {
    private final CharacterCreator characterCreator;

    public CreateCharHandler(CharacterCreator characterCreator) {
        this.characterCreator = characterCreator;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        NewCharacterSpec spec = NewCharacterSpec.builder()
                .name(p.readString())
                .type(parseJobType(p.readInt()))
                .face(p.readInt())
                .hair(p.readInt())
                .hairColor(p.readInt())
                .skin(parseSkinColor(p.readInt()))
                .topItemId(p.readInt())
                .bottomItemId(p.readInt())
                .shoesItemId(p.readInt())
                .weaponItemId(p.readInt())
                .gender(p.readByte())
                .build();

        characterCreator.createNew(spec, c.getAccID(), c.getWorld());

        int status = switch (spec.type()) {
            case KNIGHT_OF_CYGNUS -> NoblesseCreator.createCharacter(c, spec.name(), spec.face(), spec.hair() + spec.hairColor(),
                    spec.skin().getId(), spec.topItemId(), spec.bottomItemId(), spec.shoesItemId(), spec.weaponItemId(),
                    spec.gender());
            case ADVENTURER -> BeginnerCreator.createCharacter(c, spec.name(), spec.face(), spec.hair() + spec.hairColor(),
                    spec.skin().getId(), spec.topItemId(), spec.bottomItemId(), spec.shoesItemId(), spec.weaponItemId(),
                    spec.gender());
            case ARAN -> LegendCreator.createCharacter(c, spec.name(), spec.face(), spec.hair() + spec.hairColor(),
                    spec.skin().getId(), spec.topItemId(), spec.bottomItemId(), spec.shoesItemId(), spec.weaponItemId(),
                    spec.gender());
        };

        if (status == -2) {
            c.sendPacket(PacketCreator.deleteCharResponse(0, 9));
        }
    }

    private static JobType parseJobType(int value) {
        return switch (value) {
            case 0 -> JobType.KNIGHT_OF_CYGNUS;
            case 1 -> JobType.ADVENTURER;
            case 2 -> JobType.ARAN;
            default -> throw new IllegalArgumentException("Invalid job type: " + value);
        };
    }

    private static SkinColor parseSkinColor(int value) {
        return Optional.ofNullable(SkinColor.getById(value))
                .orElseThrow(() -> new IllegalArgumentException("Invalid skin color: " + value));
    }
}
