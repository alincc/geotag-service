package no.nb.microservices.geotag.model;

import java.lang.Boolean;import java.lang.String;import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mysema.query.annotations.QueryEntity;

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
    @Pattern(regexp = "URN:NBN:no-nb_.*")
    private String urn;

    @NotEmpty
    @Length(min = 32, max = 32)
    private String sesamId;

    @Length(max = 160)
    private String title;

    private Boolean sticky;
    
    private Boolean dirty;

    @NotNull
    private GeoPosition currentPosition;

    private List<GeoPosition> positionHistory;

    public GeoTag() {}

    public GeoTag(String id, String userID, String urn, String sesamId, double longitude, double latitude, Date date, String title) {
        this.id = id;
        this.urn = urn;
        this.title = title;
        this.sesamId = sesamId;

        if (currentPosition == null) {
            currentPosition = new GeoPosition(userID, longitude, latitude, date);
        }
    }

    @JsonProperty("id")
    public String getGeoId() {
        return id;
    }

    public void setGeoId(String id) {
        this.id = id;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSesamId() {
        return sesamId;
    }

    public void setSesamId(String sesamID) {
        this.sesamId = sesamID;
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

    public List<GeoPosition> getPositionHistory() {
        return positionHistory;
    }

    public void setPositionHistory(List<GeoPosition> positionHistory) {
        this.positionHistory = positionHistory;
    }

    public void addPositionHistory(GeoPosition geoPosition) {
        if (this.getPositionHistory() == null) {
            this.setPositionHistory(new ArrayList<GeoPosition>());
        }

        if (geoPosition != null) {
            this.positionHistory.add(geoPosition);
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
        this.positionHistory = null;
        this.dirty = null;
    }
}