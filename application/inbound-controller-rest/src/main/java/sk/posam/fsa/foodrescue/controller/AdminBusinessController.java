package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.BusinessMapper;
import sk.posam.fsa.foodrescue.rest.api.AdminApi;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.util.List;

@RestController
public class AdminBusinessController implements AdminApi {

    private final BusinessFacade businessFacade;
    private final BusinessMapper businessMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public AdminBusinessController(BusinessFacade businessFacade,
                                   BusinessMapper businessMapper,
                                   CurrentUserDetailService currentUserDetailService) {
        this.businessFacade = businessFacade;
        this.businessMapper = businessMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<List<BusinessResponseDto>> getPendingBusinessesForApproval() {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(businessMapper.toDtos(businessFacade.getPendingBusinesses(user)));
    }

    @Override
    public ResponseEntity<BusinessResponseDto> approveBusiness(Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        return ResponseEntity.ok(businessMapper.toDto(businessFacade.approve(user, id)));
    }
}
