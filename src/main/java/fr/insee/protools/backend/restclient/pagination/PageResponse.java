package fr.insee.protools.backend.restclient.pagination;

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
public class PageResponse<T> {
    @Builder.Default
    private List<T> content = new ArrayList<>();
    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
    private Integer pageCount;

    public Boolean isLastPage(){
        if(currentPage==null || pageCount==null){
            return true;
        }
        else return (currentPage>=pageCount);
    }

}

