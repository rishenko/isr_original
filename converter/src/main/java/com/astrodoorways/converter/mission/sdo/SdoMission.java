package com.astrodoorways.converter.mission.sdo;

import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.mission.MissionPlugin;

/** SDO: label handling only; pixels are written as read. */
public class SdoMission implements MissionPlugin {

    @Override
    public String name() {
        return "SDO";
    }

    @Override
    public MetadataHandler newHandler() {
        return new SDOHandler();
    }
}
