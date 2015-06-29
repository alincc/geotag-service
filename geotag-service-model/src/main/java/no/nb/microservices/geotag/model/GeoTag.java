package no.nb.microservices.geotag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mysema.query.annotations.QueryEntity;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.hateoas.ResourceSupport;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andreas Bjørnådal (andreasb) on 19.08.14.
 */
@XmlRootElement
@Document(collection = "GeoTag")
@JsonPropertyOrder({ "id" })
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties
@QueryEntity
public class GeoTag extends ResourceSupport {

    @Id
    private String id;

    @Indexed
    @NotEmpty
    @Length(max = 100)
    @Pattern(regexp = "URN:NBN:.*")
    private String urn;

    private Boolean sticky;
    
    private Boolean dirty;

    @NotNull
    private GeoPosition currentPosition;

    private List<GeoPosition> userPositions;

    public GeoTag() {

    }

    public GeoTag(String id, String urn, GeoPosition geoPosition) {
        this.id = id;
        this.urn = urn;

        if (currentPosition == null) {
            currentPosition = geoPosition;
            this.addUserPosition(geoPosition);
        }
    }

    @JsonProperty("id")
    public String getGeoId() {
        return id;
    }

    public void setGeoId(String id) {
        this.id = id;
    }

    public String getExpand() {
        return "userPositions";
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public Boolean isSticky() {
        return sticky;
    }

    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }

    public GeoPosition getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(GeoPosition currentPosition) {
        this.currentPosition = currentPosition;
    }

    public List<GeoPosition> getUserPositions() {
        if (this.userPositions == null) {
            this.userPositions = new ArrayList<>();
        }
        return userPositions;
    }

    public void setUserPositions(List<GeoPosition> userPositions) {
        this.userPositions = userPositions;
    }

    public void addUserPosition(GeoPosition geoPosition) {
        if (this.getUserPositions() == null) {
            this.setUserPositions(new ArrayList<GeoPosition>());
        }

        if (geoPosition != null) {
            this.userPositions.add(geoPosition);
        }
    }

    public Boolean isDirty() {
        return dirty;
    }

    public void setDirty(Boolean dirty) {
        this.dirty = dirty;
    }

    public void mask() {
        this.currentPosition.setUserEmail(null);
        this.userPositions = null;
        this.dirty = null;
    }
}