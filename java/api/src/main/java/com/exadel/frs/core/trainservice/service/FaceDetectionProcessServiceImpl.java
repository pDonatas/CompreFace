package com.exadel.frs.core.trainservice.service;

import com.exadel.frs.commonservice.exception.BasicException;
import com.exadel.frs.commonservice.sdk.faces.FacesApiClient;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FindFacesResponse;
import com.exadel.frs.core.trainservice.dto.FacesDetectionResponseDto;
import com.exadel.frs.core.trainservice.dto.HashMapDetectionResponseDto;
import com.exadel.frs.core.trainservice.dto.MultipleFacesDetectionResponseDto;
import com.exadel.frs.core.trainservice.dto.ProcessImageParams;
import com.exadel.frs.core.trainservice.mapper.FacesMapper;
import com.exadel.frs.core.trainservice.validation.ImageExtensionValidator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service("detectionService")
@RequiredArgsConstructor
public class FaceDetectionProcessServiceImpl implements FaceProcessService {

    private final FacesApiClient facesApiClient;
    private final ImageExtensionValidator imageExtensionValidator;
    private final FacesMapper facesMapper;

    @Override
    public FacesDetectionResponseDto processImage(ProcessImageParams processImageParams) {
        Integer limit = processImageParams.getLimit();
        Double detProbThreshold = processImageParams.getDetProbThreshold();
        String facePlugins = processImageParams.getFacePlugins();

        FindFacesResponse findFacesResponse;
        if (processImageParams.getFile() != null) {
            MultipartFile file = (MultipartFile) processImageParams.getFile();
            imageExtensionValidator.validate(file);
            findFacesResponse = facesApiClient.findFaces(file, limit, detProbThreshold, facePlugins, true);
        } else {
            imageExtensionValidator.validateBase64(processImageParams.getImageBase64());
            findFacesResponse = facesApiClient.findFacesBase64(processImageParams.getImageBase64(), limit, detProbThreshold, facePlugins,true);
        }

        FacesDetectionResponseDto facesDetectionResponseDto = facesMapper.toFacesDetectionResponseDto(findFacesResponse);
        return facesDetectionResponseDto.prepareResponse(processImageParams);
    }

    public HashMapDetectionResponseDto processImages(final ProcessImageParams[] processImagesParams) {
        var responses = new MultipleFacesDetectionResponseDto[processImagesParams.length];
        int i = 0;
        for (ProcessImageParams processImageParams : processImagesParams) {
            MultipleFacesDetectionResponseDto response = MultipleFacesDetectionResponseDto.builder()
                                                                                          .build();
            MultipartFile file = (MultipartFile) processImageParams.getFile();
            response.setFileName(file.getOriginalFilename());
            try {
                response.setResult(processImage(processImageParams).getResult());
            } catch (BasicException e) {
                response.setResult(null);
            }

            responses[i] = response;
            i++;
        }

        return MultipleFacesDetectionResponseDto.buildResponse(responses);
    }

    public List<String> getImagesWithLimitedFaces(HashMapDetectionResponseDto responses, Integer limit) {
        return responses.getImagesWithLimitedFaces(limit);
    }

    public List<List<String>> getImagesWithLimitedAndNoFaces(final ProcessImageParams[] processImagesParams, Integer limit) {
        List<List<String>> imagesWithLimitedAndNoFaces = new ArrayList<>();

        HashMapDetectionResponseDto responses = processImages(processImagesParams);

        imagesWithLimitedAndNoFaces.add(responses.getImagesWithLimitedFaces(limit));
        imagesWithLimitedAndNoFaces.add(responses.getImagesWithNoFaces());

        return imagesWithLimitedAndNoFaces;
    }
}
