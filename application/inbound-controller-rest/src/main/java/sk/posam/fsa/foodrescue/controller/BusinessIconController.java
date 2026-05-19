package sk.posam.fsa.foodrescue.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.BusinessIconMapper;
import sk.posam.fsa.foodrescue.mapper.BusinessMapper;
import sk.posam.fsa.foodrescue.rest.api.BusinessIconsApi;
import sk.posam.fsa.foodrescue.rest.dto.BusinessIconUploadRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

@RestController
public class BusinessIconController implements BusinessIconsApi {

    private final BusinessFacade businessFacade;
    private final BusinessIconMapper businessIconMapper;
    private final BusinessMapper businessMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public BusinessIconController(BusinessFacade businessFacade,
                                  BusinessIconMapper businessIconMapper,
                                  BusinessMapper businessMapper,
                                  CurrentUserDetailService currentUserDetailService) {
        this.businessFacade = businessFacade;
        this.businessIconMapper = businessIconMapper;
        this.businessMapper = businessMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<BusinessResponseDto> uploadBusinessIcon(BusinessIconUploadRequestDto businessIconUploadRequestDto) {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(businessMapper.toDto(businessFacade.uploadIcon(
                user,
                businessIconUploadRequestDto.getBusinessId(),
                businessIconMapper.toUpload(businessIconUploadRequestDto)
        )));
    }

    @Override
    public ResponseEntity<Resource> getBusinessIcon(String id) {
        StoredBusinessIconContent image = businessFacade.getIcon(id);
        return ResponseEntity.ok()
                .contentType(businessIconMapper.toMediaType(image))
                .body(businessIconMapper.toResource(image));
    }
}
