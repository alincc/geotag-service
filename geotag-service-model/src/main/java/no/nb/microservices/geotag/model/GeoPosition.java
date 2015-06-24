package no.nb.microservices.geotag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mysema.query.annotations.QueryEntity;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Created by Andreas Bjørnådal (andreasb) on 14.10.14.
 */
@Document
@CompoundIndexes(value = {@CompoundIndex(name = "position", def = "{'position' : '2dsphere'}", collection = "GeoTag")})
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties
@QueryEntity
public class GeoPosition {

    @Id
    private String posId;

    @NotNull
    @Valid
    private double[] position;

    @Indexed
    private Date date;

    @Indexed
    private String userId;
    
    private String userDisplayName;
    
    private String userEmail;

    @Length(max = 1000)
    private String userComment;
    
    public GeoPosition() { }

    public GeoPosition(String userId, double longitude, double latitude, Date date) {
        this.userId = userId;
        this.date = date;
        this.position = new double[] {longitude, latitude};
    }

    public String getPosId() {
        return posId;
    }

    public void setPosId(String posId) {
        this.posId = posId;
    }

    public double[] getPosition() {
        return position;
    }

    /**
     * Coordinates must contain longitude and latitude with longitude in first position
     * @param double[] position
     */
    public void setPosition(double[] position) {
        this.position = position;
    }

    @JsonIgnore
    public double getLongitude() {
        return (this.position != null && this.position.length  == 2) ? this.position[0] : 0;
    }

    public void setLongitude(double longitude) {
        if (this.position == null) {
            this.position = new double[2];
        }

        this.position[0] = longitude;
    }

    @JsonIgnore
    public double getLatitude() {
        return (this.position != null && this.position.length  == 2) ? this.position[1] : 0;
    }

    public void setLatitude(double latitude) {
        if (this.position == null) {
            this.position = new double[2];
        }

        this.position[1] = latitude;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String userDisplayName) {
		this.userDisplayName = userDisplayName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserComment() {
		return userComment;
	}

	public void setUserComment(String userComment) {
		this.userComment = userComment;
	} 
}
