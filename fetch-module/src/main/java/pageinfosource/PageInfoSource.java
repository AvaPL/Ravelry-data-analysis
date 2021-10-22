package pageinfosource;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import lombok.AllArgsConstructor;
import pojo.PageInfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor
public class PageInfoSource {

    private Integer numberOfRecords;
    private Integer pageLimit;

    public Source<PageInfo, NotUsed> getSource() {
        return Source.from(getPageInfos(numberOfRecords, pageLimit));
    }

    private List<PageInfo> getPageInfos(int numberOfRecords, int pageLimit) {
        if (numberOfRecords % pageLimit == 0)
            return IntStream.rangeClosed(1, numberOfRecords / pageLimit)
                    .mapToObj(x -> new PageInfo(x, pageLimit)).collect(Collectors.toList());
        else {
            int numberOfPages = (int) Math.ceil((double) numberOfRecords / pageLimit);
            return IntStream.rangeClosed(1, numberOfPages)
                    .mapToObj(x -> new PageInfo(x, x < numberOfPages ? pageLimit : numberOfRecords % pageLimit))
                    .collect(Collectors.toList());
        }
    }
}
