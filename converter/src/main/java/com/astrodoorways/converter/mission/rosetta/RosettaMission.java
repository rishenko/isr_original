package com.astrodoorways.converter.mission.rosetta;

import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.mission.MissionPlugin;

/** Rosetta: label handling only; pixels are written as read. */
public class RosettaMission implements MissionPlugin {

    @Override
    public String name() {
        return "Rosetta";
    }

    @Override
    public MetadataHandler newHandler() {
        return new RosettaHandler();
    }
}
