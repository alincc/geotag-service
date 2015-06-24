package no.nb.microservices.geotag.service;

import no.nb.nbsecurity.NBUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


/**
 * Created by Andreas Bjørnådal (andreasb) on 19.08.14.
 */
@Service
public class NBUserService {

    private static final Logger LOG = LoggerFactory.getLogger(NBUserService.class);

    public NBUserDetails getNBUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof NBUserDetails) {
                return (NBUserDetails)principal;
            }
            else {
                LOG.error("Principal is not instance of " + NBUserDetails.class.getName() + " was - " + principal.getClass() + "( " + principal + " )");
            }
        }
        return null;
    }
}


