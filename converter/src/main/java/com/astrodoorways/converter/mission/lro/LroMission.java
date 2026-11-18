package com.astrodoorways.converter.mission.lro;

import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.mission.MissionPlugin;

/** LRO: label handling only; pixels are written as read. */
public class LroMission implements MissionPlugin {

    @Override
    public String name() {
        return "LRO";
    }

    @Override
    public MetadataHandler newHandler() {
        return new LROHandler();
    }
}
