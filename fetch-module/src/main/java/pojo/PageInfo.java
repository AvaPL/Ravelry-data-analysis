package pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
public class PageInfo {
    private Integer pageNumber;
    private Integer pageLimit;
}