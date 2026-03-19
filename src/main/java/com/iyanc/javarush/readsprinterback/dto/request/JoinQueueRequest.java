package com.iyanc.javarush.readsprinterback.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(
        use     = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "exerciseType",
        visible  = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SchulteQueueRequest.class,   name = "schulte-table"),
        @JsonSubTypes.Type(value = NumbersQueueRequest.class,   name = "numbers"),
        @JsonSubTypes.Type(value = WordPairsQueueRequest.class, name = "word-pairs"),
        @JsonSubTypes.Type(value = RsvpQueueRequest.class,      name = "rsvp"),
})
@Getter
@Setter
public abstract class JoinQueueRequest {

    @NotBlank
    private String exerciseType;
}
