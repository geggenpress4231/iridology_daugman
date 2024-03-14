package com.iris_segmentation.iris_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris_segmentation.iris_backend.services.Circle;
import com.iris_segmentation.iris_backend.services.image; // Import the image class

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class ImageProcessingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProcessingController.class);

    @PostMapping("/process")
    public ResponseEntity<?> processImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            LOGGER.error("Empty file received");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                LOGGER.error("Unsupported image format or corrupted file");
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Unsupported image format or corrupted file");
            }

            image imageProcessor = new image();
            imageProcessor.fromBufferedImage(bufferedImage);
            
            // Processing steps
            imageProcessor.grayscale();
            imageProcessor.histogram_normalization();
            imageProcessor.gaussian_filter();
            Circle initialPupilCircle = imageProcessor.approximate_pupil_detection();
            ArrayList<ArrayList<Point>> circleModels = imageProcessor.circle_models(1, Math.min(imageProcessor.w, imageProcessor.h)/2);
            Circle irisCircle = imageProcessor.segmentation_iris(initialPupilCircle, circleModels);
            Circle pupilCircle = imageProcessor.segmentation_pupil(irisCircle, circleModels);

            BufferedImage processedImage = imageProcessor.getBufferedImage();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "png", baos); // Using PNG for lossless format
            byte[] imageBytes = baos.toByteArray();

            return ResponseEntity.ok()
                                 .contentType(MediaType.IMAGE_PNG)
                                 .body(imageBytes);
        } catch (IOException e) {
            LOGGER.error("Error processing image: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image: " + e.getMessage());
        }
    }
}
