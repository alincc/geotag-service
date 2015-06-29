package no.nb.microservices.geotag.rest.controller;

import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.service.IGeoTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by andreasb on 29.06.15.
 */
@RestController
public class PositionController {

    private final IGeoTagService geotagService;

    @Autowired
    public PositionController(IGeoTagService geotagService) {
        this.geotagService = geotagService;
    }

    @RequestMapping(value = "/geotags/{geoTagID}/positions", method = RequestMethod.GET)
    public ResponseEntity<List<GeoPosition>> getPositions(@PathVariable String geoTagID)
    {
        GeoTag geoTag = geotagService.findOne(geoTagID, new String[]{"positions"});
        geoTag.add(linkTo(methodOn(PositionController.class).getPositions(geoTagID)).withSelfRel());
        return new ResponseEntity<List<GeoPosition>>(geoTag.getUserPositions(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/geotags/{geoTagID}/positions", method = RequestMethod.POST)
    public ResponseEntity<GeoPosition> createPosition(@PathVariable String geoTagID,
                                                      @Valid @RequestBody GeoPosition geoPosition)
    {
        GeoPosition savedGeoPosition = geotagService.savePosition(geoTagID, geoPosition);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new UriTemplate("/geotags/{geotagid}/positions/{posId}").expand(geoTagID, geoPosition.getPosId()));

        return new ResponseEntity<GeoPosition>(savedGeoPosition, headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/geotags/{geoTagID}/positions/{posId}", method = RequestMethod.GET)
    public ResponseEntity<GeoPosition> getPosition(@PathVariable String geoTagID,
                                                   @PathVariable String posId)
    {
        GeoPosition position = geotagService.findOnePosition(geoTagID, posId);
        return new ResponseEntity<GeoPosition>(position, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_TagsAdmin')")
    @RequestMapping(value = "/geotags/{geoTagID}/positions/{posId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deletePosition(@PathVariable String geoTagID,
                                               @PathVariable String posId)
    {
        geotagService.deletePosition(geoTagID, posId);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}
