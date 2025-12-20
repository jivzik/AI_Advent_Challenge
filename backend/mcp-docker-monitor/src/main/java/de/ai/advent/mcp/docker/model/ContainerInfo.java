package de.ai.advent.mcp.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docker Container Information
 */
public class ContainerInfo {

    @JsonProperty("container_id")
    private String containerId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("image")
    private String image;

    @JsonProperty("status")
    private String status;

    @JsonProperty("state")
    private String state;

    @JsonProperty("ports")
    private String ports;

    @JsonProperty("created")
    private String created;

    public ContainerInfo() {}

    public ContainerInfo(String containerId, String name, String image, String status, String state, String ports, String created) {
        this.containerId = containerId;
        this.name = name;
        this.image = image;
        this.status = status;
        this.state = state;
        this.ports = ports;
        this.created = created;
    }

    // Getters and Setters
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "ContainerInfo{" +
                "containerId='" + containerId + '\'' +
                ", name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", status='" + status + '\'' +
                ", state='" + state + '\'' +
                ", ports='" + ports + '\'' +
                ", created='" + created + '\'' +
                '}';
    }
}

