package seucxxy.csd.backend.dto;

public class MenuItemDto {
    private String name;
    private String path;
    private String icon;

    public MenuItemDto() {}

    public MenuItemDto(String name, String path, String icon) {
        this.name = name;
        this.path = path;
        this.icon = icon;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}