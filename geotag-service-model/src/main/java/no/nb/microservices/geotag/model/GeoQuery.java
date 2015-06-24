package no.nb.microservices.geotag.model;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Created by Andreas Bjørnådal (andreasb) on 28.10.14.
 */
public class GeoQuery {
	private boolean minify;
	private Boolean dirty;
	private Boolean sticky; 
    private String urn;
    private String user;
    private String email;
    private Double lon;
    private Double lat;
    private Double maxDistance;
    private Double secondLon;
    private Double secondLat;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedSince;

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public Double getSecondLon() {
        return secondLon;
    }

    public void setSecondLon(Double secondLon) {
        this.secondLon = secondLon;
    }

    public Double getSecondLat() {
        return secondLat;
    }

    public void setSecondLat(Double secondLat) {
        this.secondLat = secondLat;
    }

    public Date getUpdatedSince() {
        return updatedSince;
    }

    public void setUpdatedSince(Date updatedSince) {
        this.updatedSince = updatedSince;
    }

	public boolean isMinify() {
		return minify;
	}

	public void setMinify(boolean minify) {
		this.minify = minify;
	}

	public Boolean getDirty() {
		return dirty;
	}

	public void setDirty(Boolean dirty) {
		this.dirty = dirty;
	}

	public Boolean getSticky() {
		return sticky;
	}

	public void setSticky(Boolean sticky) {
		this.sticky = sticky;
	}
	
	
    
    
}
