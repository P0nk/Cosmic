package model;

public record CharacterIdentity(String name, int id) {
    public CharacterIdentity {
        if (name == null) {
            throw new IllegalArgumentException("Character name must not be null");
        }
    }
}
