package pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class PageInfo {
    private Integer pageNumber;
    private Integer pageLimit;
}