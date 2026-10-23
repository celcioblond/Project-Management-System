package com.celcioblond.pms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;

@Service
@RequiredArgsConstructor
public class AiImageGeneratorService {

    private static final OpenAiImageOptions IMAGE_OPTIONS = OpenAiImageOptions.builder()
            .N(1)
            .width(1024)
            .height(1024)
            .quality("standard")
            .responseFormat("url")
            .model("dall-e-3")
            .build();

    private final ImageModel imageModel;

    public byte[] generateImage(String imagePrompt) {
        ImageResponse response = imageModel.call(new ImagePrompt(imagePrompt, IMAGE_OPTIONS));
        String imageUrl = response.getResult().getOutput().getUrl();

        try (InputStream in = URI.create(imageUrl).toURL().openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not download generated image", e);
        }
    }
}
