package sk.posam.fsa.foodrescue.transactional;

import org.springframework.transaction.annotation.Transactional;
import sk.posam.fsa.foodrescue.domain.business.Business;
import sk.posam.fsa.foodrescue.domain.business.BusinessFacade;
import sk.posam.fsa.foodrescue.domain.business.BusinessIconUpload;
import sk.posam.fsa.foodrescue.domain.business.BusinessService;
import sk.posam.fsa.foodrescue.domain.business.StoredBusinessIconContent;
import sk.posam.fsa.foodrescue.domain.user.User;

import java.util.List;

public class TransactionalBusinessFacade implements BusinessFacade {

    private final BusinessService businessService;

    public TransactionalBusinessFacade(BusinessService businessService) {
        this.businessService = businessService;
    }

    @Override
    @Transactional
    public Business create(User currentUser, Business business) {
        return businessService.create(currentUser, business);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Business> getBusinesses(User currentUser) {
        return businessService.getBusinesses(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Business> getPendingBusinesses(User currentUser) {
        return businessService.getPendingBusinesses(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Business get(User currentUser, Long id) {
        return businessService.get(currentUser, id);
    }

    @Override
    @Transactional
    public Business approve(User currentUser, Long id) {
        return businessService.approve(currentUser, id);
    }

    @Override
    @Transactional
    public Business update(User currentUser, Long id, Business business) {
        return businessService.update(currentUser, id, business);
    }

    @Override
    @Transactional
    public Business uploadIcon(User currentUser, Long businessId, BusinessIconUpload upload) {
        return businessService.uploadIcon(currentUser, businessId, upload);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredBusinessIconContent getIcon(String iconId) {
        return businessService.getIcon(iconId);
    }

    @Override
    @Transactional
    public void delete(User currentUser, Long id) {
        businessService.delete(currentUser, id);
    }
}
