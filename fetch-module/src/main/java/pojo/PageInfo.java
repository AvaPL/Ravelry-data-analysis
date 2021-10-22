package pojo;

import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class PageInfo {
    private Integer pageNumber;
    private Integer pageLimit;
}