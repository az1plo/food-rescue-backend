package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.mapper.BusinessMapper;
import sk.posam.fsa.foodrescue.rest.dto.BusinessResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

import java.util.List;

@RestController
@RequestMapping("/admin/businesses")
public class AdminBusinessController {

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

    @GetMapping("/pending")
    public ResponseEntity<List<BusinessResponseDto>> getPendingBusinesses() {
        User user = currentUserDetailService.getFullCurrentUser();
        List<BusinessResponseDto> businesses = businessFacade.getPendingBusinesses(user).stream()
                .map(businessMapper::toDto)
                .toList();
        return ResponseEntity.ok(businesses);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<BusinessResponseDto> approveBusiness(@PathVariable Long id) {
        User user = currentUserDetailService.getFullCurrentUser();
        Business approvedBusiness = businessFacade.approve(user, id);
        return ResponseEntity.ok(businessMapper.toDto(approvedBusiness));
    }
}

