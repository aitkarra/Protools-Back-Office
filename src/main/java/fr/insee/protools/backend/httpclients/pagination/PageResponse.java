package fr.insee.protools.backend.httpclients.pagination;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown=true)
public class PageResponse <T> {
    private int totalElements;
    private int totalPages;
    private int size;
    @ToString.Exclude
    private List<T> content;
    private long number;
    private int numberOfElements;
    private boolean first;
    private boolean last;
    private boolean empty;
}