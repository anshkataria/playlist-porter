package com.playlistporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.playlistporter.config.SpotifyConfig;
import com.playlistporter.config.YouTubeConfig;

@EnableConfigurationProperties({SpotifyConfig.class,YouTubeConfig.class})
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
