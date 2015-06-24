package no.nb.microservices.geotag.rest.controller;

import com.mysema.query.types.expr.BooleanExpression;
import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.model.QGeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.microservices.geotag.rest.assembler.GeoTagPage;
import no.nb.microservices.geotag.rest.assembler.GeoTagResourceAssembler;
import no.nb.microservices.geotag.service.NBUserService;
import no.nb.nbsecurity.NBUserDetails;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by Andreas Bjørnådal (andreasb) on 19.08.14.
 */
@RestController
@RequestMapping("/geotags")
public class GeoTagController {

    private final static String ADMIN_ROLE = "ROLE_TagsAdmin";

    private final Logger LOG = LoggerFactory.getLogger(GeoTagController.class);

    private final NBUserService nbUserService;
    private final GeoTagRepository geoTagRepository;
    private final GeoTagResourceAssembler assembler;
    private final ApplicationSettings applicationSettings;

    @Autowired
    public GeoTagController(NBUserService nbUserService, GeoTagRepository geoTagRepository, GeoTagResourceAssembler assembler, ApplicationSettings applicationSettings) {
        this.nbUserService = nbUserService;
        this.geoTagRepository = geoTagRepository;
        this.assembler = assembler;
        this.applicationSettings = applicationSettings;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PagedResources<GeoTag>> getTags(GeoQuery query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size)
    {
        QGeoTag t = QGeoTag.geoTag;
        BooleanExpression expression = t.urn.isNotNull();

        if (StringUtils.isNotBlank(query.getUrn())) {
        	expression = expression.and(t.urn.eq(query.getUrn()));
        }
        if (StringUtils.isNotBlank(query.getEmail())) {
            expression = expression.and(t.currentPosition.userEmail.eq(query.getEmail())).or(t.positionHistory.any().userEmail.eq(query.getEmail()));
        }
        if (StringUtils.isNotBlank(query.getUser())) {
        	expression = expression.and(t.currentPosition.userId.eq(query.getUser()));
        }
        if (query.getUpdatedSince() != null) {
        	expression = expression.and(t.currentPosition.date.after(query.getUpdatedSince()));
        }
        if (query.getDirty() != null) {
        	expression = expression.and(t.dirty.eq(query.getDirty()));
        }
        if (query.getSticky() != null) {
        	expression = expression.and(t.sticky.eq(query.getSticky()));
        }

        PageRequest pageRequest = new PageRequest(page, size, new Sort(Sort.Direction.DESC, "date"));
        Page<GeoTag> pages = geoTagRepository.findAll(expression, pageRequest);

        // If not admin then remove some fields.
        if (pages != null && (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(ADMIN_ROLE)))) {
            for (GeoTag geoTag : pages.getContent()) {
                geoTag.mask();
            }
        }

        PagedResources<GeoTag> pagedResources = assembler.toResource(new GeoTagPage(pages, query));
        return new ResponseEntity<PagedResources<GeoTag>>(pagedResources, HttpStatus.OK);
    }

    @RequestMapping(value = "/{geoTagID}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<GeoTag> getTag(@PathVariable String geoTagID) {
        GeoTag geoTag = geoTagRepository.findOne(geoTagID);

        if (geoTag == null) {
            return new ResponseEntity<GeoTag>(HttpStatus.NOT_FOUND);
        }

        // If not admin then remove some fields
        if (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(ADMIN_ROLE))) {
            geoTag.mask();
        }

        geoTag.add(linkTo(methodOn(GeoTagController.class).getTag(geoTagID)).withSelfRel());
        return new ResponseEntity<GeoTag>(geoTag, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_TagsAdmin')")
    @RequestMapping(value = "/{geoTagID}/{posId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteTag(@PathVariable String geoTagID, @PathVariable String posId) {
        GeoTag geoTag = geoTagRepository.findOne(geoTagID);

        if (geoTag == null) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }

        for (Iterator<GeoPosition> iterator = geoTag.getPositionHistory().iterator(); iterator.hasNext(); ) {
            GeoPosition geoPosition = iterator.next();
            if (geoPosition.getPosId() != null && geoPosition.getPosId().equals(posId)) {
                iterator.remove();
            }
        }

        if (geoTag.getCurrentPosition().getPosId().equals(posId) && geoTag.getPositionHistory().size() - 1 >= 0) {
            geoTag.setCurrentPosition(geoTag.getPositionHistory().get(geoTag.getPositionHistory().size() - 1));
            geoTag.getPositionHistory().remove(geoTag.getPositionHistory().size() - 1);
        }

        geoTagRepository.save(geoTag);

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<GeoTag> saveTag(@Valid @RequestBody GeoTag geoTag) {
        Page<GeoTag> geoTags = geoTagRepository.findByUrn(geoTag.getUrn(), new PageRequest(0, 1));
        GeoTag oldGeoTag = (geoTags.hasContent()) ? geoTags.getContent().get(0) : null;

        NBUserDetails user = nbUserService.getNBUser();
        String currentUser = user.getUserId().toString();
        geoTag.getCurrentPosition().setPosId(UUID.randomUUID().toString());
        geoTag.getCurrentPosition().setUserId(currentUser);
        geoTag.getCurrentPosition().setUserDisplayName(user.getDisplayName());
        geoTag.getCurrentPosition().setUserEmail(user.getEmail());
        geoTag.getCurrentPosition().setDate(Calendar.getInstance().getTime());

        if (oldGeoTag != null) {
            if (oldGeoTag.isSticky() != null && oldGeoTag.isSticky()) {
                return new ResponseEntity<GeoTag>(HttpStatus.FORBIDDEN);
            }

            oldGeoTag.setDirty(true);
            oldGeoTag.addPositionHistory(oldGeoTag.getCurrentPosition());
            oldGeoTag.setCurrentPosition(geoTag.getCurrentPosition());

            // Sletter tidligere tagger brukeren har på denne taggen slik at det blir kun 1 tag per bruker og urn.
            for (Iterator<GeoPosition> iterator = oldGeoTag.getPositionHistory().iterator(); iterator.hasNext(); ) {
                GeoPosition geoPosition = iterator.next();
                if (geoPosition.getUserId().equals(currentUser)) {
                    iterator.remove();
                }
            }

            geoTagRepository.save(oldGeoTag);
        }
        else {
            geoTag.setSticky(false);
            geoTag.setDirty(true);
            geoTag.setPositionHistory(null);
            geoTag.getLinks().clear();
            geoTag.setGeoId(UUID.randomUUID().toString());
            geoTagRepository.save(geoTag);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new UriTemplate("/geotags/{geotagid}").expand(geoTag.getGeoId()));

        return new ResponseEntity<GeoTag>(geoTag, headers, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/{geoTagID}", method = RequestMethod.PATCH)
    public ResponseEntity<GeoTag> patchTag(@PathVariable String geoTagID, @RequestBody GeoTag geoTag) {
    	NBUserDetails user = nbUserService.getNBUser();
        String currentUser = user.getUserId().toString();
        GeoTag oldGeoTag = geoTagRepository.findOne(geoTagID);

        geoTag.getLinks().clear();

        // This checks that only admin can change isSticky
        if (nbUserService.getNBUser() != null && nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(ADMIN_ROLE))) {
            oldGeoTag.setSticky((geoTag.isSticky() != null) ? geoTag.isSticky() : null);
            oldGeoTag.setDirty((geoTag.isDirty() != null) ? geoTag.isDirty() : true);
        }
        else {
            if (oldGeoTag.isSticky()) {
                return new ResponseEntity<GeoTag>(HttpStatus.FORBIDDEN);
            }
            oldGeoTag.setDirty(true);
        }

        if (geoTag.getCurrentPosition() != null) {
            geoTag.getCurrentPosition().setUserId(currentUser);
            geoTag.getCurrentPosition().setPosId(UUID.randomUUID().toString());
            geoTag.getCurrentPosition().setDate(Calendar.getInstance().getTime());
            oldGeoTag.addPositionHistory(oldGeoTag.getCurrentPosition());
            geoTag.getCurrentPosition().setUserDisplayName(user.getDisplayName());
            geoTag.getCurrentPosition().setUserEmail(user.getEmail());
            oldGeoTag.setCurrentPosition(geoTag.getCurrentPosition());

            // Sletter tidligere tagger brukeren har på denne taggen slik at det blir kun 1 tag per bruker og urn.
            for (Iterator<GeoPosition> iterator = oldGeoTag.getPositionHistory().iterator(); iterator.hasNext(); ) {
                GeoPosition geoPosition = iterator.next();
                if (geoPosition.getUserId().equals(currentUser)) {
                    iterator.remove();
                }
            }
        }

        geoTagRepository.save(oldGeoTag);
        return new ResponseEntity<GeoTag>(oldGeoTag, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_TagsAdmin')")
    @RequestMapping(value = "/{geoTagID}", method = RequestMethod.PUT)
    public ResponseEntity<GeoTag> updateTag(@PathVariable String geoTagID, @Valid @RequestBody GeoTag geoTag) {
        geoTag.getLinks().clear();
        geoTag.setGeoId(geoTagID);
        geoTagRepository.save(geoTag);
        return new ResponseEntity<GeoTag>(geoTag, HttpStatus.OK);
    }

    @RequestMapping(value = "/nearby", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PagedResources<GeoTag>> getNearbyTags(GeoQuery query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size)
    {
        Point position = new Point(query.getLon(), query.getLat());
        Distance distance = new Distance(query.getMaxDistance(), Metrics.KILOMETERS);
        Page<GeoTag> pages = geoTagRepository.findByCurrentPositionPositionNear(position, distance, new PageRequest(page, size));

        // If not admin then remove some fields
        if (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(ADMIN_ROLE))) {
            for (GeoTag geoTag : pages.getContent()) {
                geoTag.mask();
            }
        }

        PagedResources<GeoTag> pagedResources = assembler.toResource(new GeoTagPage(pages, query));

        return new ResponseEntity<PagedResources<GeoTag>>(pagedResources, HttpStatus.OK);
    }

    @RequestMapping(value = "/within", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PagedResources<GeoTag>> getTagsWithin(GeoQuery query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size)
    {
        Box box = new Box(new Point(query.getLon(), query.getLat()), new Point(query.getSecondLon(), query.getSecondLat()));
        Page<GeoTag> pages = geoTagRepository.findByCurrentPositionPositionWithin(box, new PageRequest(page, size));

        // If not admin then remove some fields
        if (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(ADMIN_ROLE))) {
            for (GeoTag geoTag : pages.getContent()) {
                geoTag.mask();
            }
        }

        PagedResources<GeoTag> pagedResources = assembler.toResource(new GeoTagPage(pages, query));

        return new ResponseEntity<PagedResources<GeoTag>>(pagedResources, HttpStatus.OK);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "It looks like we have a internal error in our application. The error have been logged and will be looked at by our development team.")
    public void defaultHandler(HttpServletRequest req, Exception e) {

        // Build Header string
        StringBuilder headers = new StringBuilder();
        for (String headerKey : Collections.list(req.getHeaderNames())) {
            String headerValue = req.getHeader(headerKey);
            headers.append(headerKey + ": " + headerValue + ", ");
        }

        LOG.error("" +
                "Got an unexcepted exception.\n" +
                "Context Path: " + req.getContextPath() + "\n" +
                "Request URI: " + req.getRequestURI() + "\n" +
                "Query String: " + req.getQueryString() + "\n" +
                "Method: " + req.getMethod() + "\n" +
                "Headers: " + headers + "\n" +
                "Auth Type: " + req.getAuthType() + "\n" +
                "Remote User: " + req.getRemoteUser() + "\n" +
                "Username: " + ((req.getUserPrincipal() != null) ? req.getUserPrincipal().getName() : "Anonymous") + "\n"
                , e);
    }
}
