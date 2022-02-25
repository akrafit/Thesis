package main.model;

import lombok.Data;

@Data
public class UserShort {
    private Long id;
    String name;
    public UserShort(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
