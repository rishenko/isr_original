package com.astrodoorways.converter.mission.galileo;

import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.mission.MissionPlugin;

/** Galileo: label handling only; pixels are written as read. */
public class GalileoMission implements MissionPlugin {

    @Override
    public String name() {
        return "Galileo";
    }

    @Override
    public MetadataHandler newHandler() {
        return new GalileoHandler();
    }
}
