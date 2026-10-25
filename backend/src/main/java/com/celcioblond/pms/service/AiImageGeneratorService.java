package com.celcioblond.pms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AiImageGeneratorService {

    private static final OpenAiImageOptions IMAGE_OPTIONS = OpenAiImageOptions.builder()
            .N(1)
            .width(1024)
            .height(1024)
            .quality("medium")
            .model("gpt-image-2")
            .build();

    private final ImageModel imageModel;

    public byte[] generateImage(String imagePrompt) {
        ImageResponse response = imageModel.call(new ImagePrompt(imagePrompt, IMAGE_OPTIONS));
        // gpt-image models always returns the image as base64 (it has no "url" response format)
        return Base64.getDecoder().decode(response.getResult().getOutput().getB64Json());
    }
}
