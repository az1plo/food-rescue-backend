package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.models.entities.Business;
import sk.posam.fsa.foodrescue.domain.models.entities.User;
import sk.posam.fsa.foodrescue.domain.services.BusinessFacade;
import sk.posam.fsa.foodrescue.mapper.BusinessMapper;
import sk.posam.fsa.foodrescue.rest.api.BusinessesApi;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.rest.dto.CreateBusinessRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.UpdateBusinessRequestDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.net.URI;
import java.util.List;

@RestController
public class BusinessController implements BusinessesApi {

    private final BusinessFacade businessFacade;
    private final BusinessMapper businessMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public BusinessController(BusinessFacade businessFacade,
                              BusinessMapper businessMapper,
                              CurrentUserDetailService currentUserDetailService) {
        this.businessFacade = businessFacade;
        this.businessMapper = businessMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<BusinessResponseDto>> getBusinesses() {
        User user = currentUserDetailService.getFullCurrentUser();
        List<BusinessResponseDto> businesses = businessFacade.getBusinesses(user).stream()
                .map(businessMapper::toDto)
                .toList();
        return ResponseEntity.ok(businesses);
    }

    @Override
    public ResponseEntity<Void> createBusiness(CreateBusinessRequestDto request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Business business = businessMapper.toEntity(request);
        Business createdBusiness = businessFacade.create(user, business);
        return ResponseEntity.created(URI.create("/businesses/" + createdBusiness.getId())).build();
    }

    @Override
    public ResponseEntity<BusinessResponseDto> getBusiness(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Business business = businessFacade.get(user, id);
        return ResponseEntity.ok(businessMapper.toDto(business));
    }

    @Override
    public ResponseEntity<BusinessResponseDto> updateBusiness(Long id, UpdateBusinessRequestDto request) {
        User user = currentUserDetailService.getFullCurrentUser();
        Business business = businessMapper.toEntity(request);
        Business updated = businessFacade.update(user, id, business);
        return ResponseEntity.ok(businessMapper.toDto(updated));
    }

    @Override
    public ResponseEntity<Void> deleteBusiness(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        businessFacade.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
