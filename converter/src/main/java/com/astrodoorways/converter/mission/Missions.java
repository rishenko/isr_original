package com.astrodoorways.converter.mission;

import com.astrodoorways.converter.metadata.handler.GenericMetadataHandler;
import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.mission.cassini.CassiniMission;
import com.astrodoorways.converter.mission.galileo.GalileoMission;
import com.astrodoorways.converter.mission.lro.LroMission;
import com.astrodoorways.converter.mission.rosetta.RosettaMission;
import com.astrodoorways.converter.mission.sdo.SdoMission;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The registered mission plugins. */
public final class Missions {

    /**
     * In the order their handlers are asked whether they recognise a file; the first that does is
     * used. Put more specific plugins first.
     */
    private static final List<MissionPlugin> ALL = Collections.unmodifiableList(Arrays.asList(
            new CassiniMission(),
            new GalileoMission(),
            new SdoMission(),
            new LroMission(),
            new RosettaMission()));

    /** Used when no plugin recognises a file: placeholder label values, no processing. */
    public static final MissionPlugin UNKNOWN = new MissionPlugin() {
        @Override
        public String name() {
            return "unknown";
        }

        @Override
        public MetadataHandler newHandler() {
            return new GenericMetadataHandler();
        }
    };

    private Missions() {
    }

    public static List<MissionPlugin> all() {
        return ALL;
    }
}
