package sk.posam.fsa.foodrescue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sk.posam.fsa.foodrescue.domain.support.SupportChatRequest;
import sk.posam.fsa.foodrescue.domain.support.SupportChatResponse;
import sk.posam.fsa.foodrescue.domain.support.SupportFacade;
import sk.posam.fsa.foodrescue.domain.user.User;
import sk.posam.fsa.foodrescue.mapper.SupportChatMapper;
import sk.posam.fsa.foodrescue.rest.api.SupportApi;
import sk.posam.fsa.foodrescue.rest.dto.SupportChatRequestDto;
import sk.posam.fsa.foodrescue.rest.dto.SupportChatResponseDto;
import sk.posam.fsa.foodrescue.security.CurrentUserDetailService;

@RestController
public class SupportController implements SupportApi {

    private final SupportFacade supportFacade;
    private final SupportChatMapper supportChatMapper;
    private final CurrentUserDetailService currentUserDetailService;

    public SupportController(SupportFacade supportFacade,
                             SupportChatMapper supportChatMapper,
                             CurrentUserDetailService currentUserDetailService) {
        this.supportFacade = supportFacade;
        this.supportChatMapper = supportChatMapper;
        this.currentUserDetailService = currentUserDetailService;
    }

    @Override
    public ResponseEntity<SupportChatResponseDto> sendSupportChatMessage(SupportChatRequestDto supportChatRequestDto) {
        User currentUser = currentUserDetailService.getOptionalCurrentUser();
        SupportChatRequest request = supportChatMapper.toDomain(supportChatRequestDto);
        SupportChatResponse response = supportFacade.reply(currentUser, request);
        return ResponseEntity.ok(supportChatMapper.toDto(response));
    }
}
