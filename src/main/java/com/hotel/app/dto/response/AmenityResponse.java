package com.hotel.app.dto.response;

public class AmenityResponse {
    private Long id;
    private String name;
    private String icon;

    public AmenityResponse() {}

    public AmenityResponse(Long id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public static AmenityResponseBuilder builder() {
        return new AmenityResponseBuilder();
    }

    public static class AmenityResponseBuilder {
        private Long id;
        private String name;
        private String icon;

        public AmenityResponseBuilder id(Long id) { this.id = id; return this; }
        public AmenityResponseBuilder name(String name) { this.name = name; return this; }
        public AmenityResponseBuilder icon(String icon) { this.icon = icon; return this; }
        public AmenityResponse build() {
            return new AmenityResponse(id, name, icon);
        }
    }
}
