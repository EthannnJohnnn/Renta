package models;

public class Property {
    private int id;
    private int landlordId;
    private String name;
    private String address;
    private String description;
    private String imageUrl;

    public Property() {}

    public Property(int id, int landlordId, String name, String address, String description) {
        this.id = id;
        this.landlordId = landlordId;
        this.name = name;
        this.address = address;
        this.description = description;
    }

    public Property(int landlordId, String name, String address, String description) {
        this.landlordId = landlordId;
        this.name = name;
        this.address = address;
        this.description = description;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLandlordId() { return landlordId; }
    public void setLandlordId(int landlordId) { this.landlordId = landlordId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}