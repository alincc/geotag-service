package no.nb.microservices.geotag.rest.controller;

import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.rest.assembler.GeoTagPage;
import no.nb.microservices.geotag.rest.assembler.GeoTagResourceAssembler;
import no.nb.microservices.geotag.service.IGeotagService;
import no.nb.microservices.geotag.service.NBUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.NoSuchElementException;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by Andreas Bjørnådal (andreasb) on 19.08.14.
 */
@RestController
@RequestMapping("/geotags")
public class GeoTagController {

    private final Logger LOG = LoggerFactory.getLogger(GeoTagController.class);

    private final NBUserService nbUserService;
    private final GeoTagResourceAssembler assembler;
    private final ApplicationSettings applicationSettings;
    private final IGeotagService geotagService;

    @Autowired
    public GeoTagController(NBUserService nbUserService, GeoTagResourceAssembler assembler, ApplicationSettings applicationSettings, IGeotagService geotagService) {
        this.nbUserService = nbUserService;
        this.assembler = assembler;
        this.applicationSettings = applicationSettings;
        this.geotagService = geotagService;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PagedResources<GeoTag>> getTags(GeoQuery query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String[] expand)
    {
        Page<GeoTag> pages = geotagService.query(query, page, size, expand);
        PagedResources<GeoTag> pagedResources = assembler.toResource(new GeoTagPage(pages, query, expand));

        return new ResponseEntity<PagedResources<GeoTag>>(pagedResources, HttpStatus.OK);
    }

    @RequestMapping(value = "/{geoTagID}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<GeoTag> getTag(@PathVariable String geoTagID,
                                         @RequestParam(required = false) String[] expand)
    {
        GeoTag geoTag = geotagService.findOne(geoTagID, expand);
        geoTag.add(linkTo(methodOn(GeoTagController.class).getTag(geoTagID, expand)).withSelfRel());

        return new ResponseEntity<GeoTag>(geoTag, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_TagsAdmin')")
    @RequestMapping(value = "/{geoTagID}/{posId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteTag(@PathVariable String geoTagID,
                                          @PathVariable String posId)
    {
        geotagService.delete(geoTagID, posId);

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<GeoTag> saveTag(@Valid @RequestBody GeoTag geoTag) {
        geotagService.save(geoTag);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new UriTemplate("/geotags/{geotagid}").expand(geoTag.getGeoId()));

        return new ResponseEntity<GeoTag>(geoTag, headers, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROLE_TagsAdmin')")
    @RequestMapping(value = "/{geoTagID}", method = RequestMethod.PUT)
    public ResponseEntity<GeoTag> updateTag(@PathVariable String geoTagID,
                                            @Valid @RequestBody GeoTag geoTag)
    {
        GeoTag geotag = geotagService.update(geoTagID, geoTag);
        return new ResponseEntity<GeoTag>(geoTag, HttpStatus.OK);
    }

    @RequestMapping(value = "/nearby", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PagedResources<GeoTag>> getNearbyTags(GeoQuery query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size)
    {
        Page<GeoTag> pages = geotagService.nearby(query, page, size);
        PagedResources<GeoTag> pagedResources = assembler.toResource(new GeoTagPage(pages, query));

        return new ResponseEntity<PagedResources<GeoTag>>(pagedResources, HttpStatus.OK);
    }

    @RequestMapping(value = "/within", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PagedResources<GeoTag>> getTagsWithin(GeoQuery query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size)
    {
        Page<GeoTag> pages = geotagService.within(query, page, size);
        PagedResources<GeoTag> pagedResources = assembler.toResource(new GeoTagPage(pages, query));

        return new ResponseEntity<PagedResources<GeoTag>>(pagedResources, HttpStatus.OK);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "No access")
    public void noAccessHandler(HttpServletRequest req, Exception e) {
        LOG.warn("User have no access", e);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The requested element is not found")
    public void noSuchElementHandler(HttpServletRequest req, Exception e) {
        LOG.warn("The requested element is not found", e);
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
