package com.playlistporter.adapter;

import com.playlistporter.entity.Platform;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AdapterRegistry {

    private final Map<Platform, MusicPlatformAdapter> adapters;

    // Spring injects all MusicPlatformAdapter implementations automatically
    public AdapterRegistry(List<MusicPlatformAdapter> adapterList) {
        this.adapters = adapterList.stream()
            .collect(Collectors.toMap(
                MusicPlatformAdapter::getPlatform,
                Function.identity()
            ));
    }

    public MusicPlatformAdapter getAdapter(Platform platform) {
        MusicPlatformAdapter adapter = adapters.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter found for platform: " + platform);
        }
        return adapter;
    }
}