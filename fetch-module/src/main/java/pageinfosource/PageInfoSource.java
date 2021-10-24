package pageinfosource;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import lombok.AllArgsConstructor;
import lombok.val;
import pojo.PageInfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AllArgsConstructor
public class PageInfoSource {

    private Integer numberOfRecords;
    private Integer pageLimit;

    public Source<PageInfo, NotUsed> create() {
        return Source.from(getPageInfos());
    }

    private List<PageInfo> getPageInfos() {
        val mostPageInfos = IntStream.rangeClosed(1, numberOfRecords / pageLimit)
                .mapToObj(x -> new PageInfo(x, pageLimit)).collect(Collectors.toList());
        val lastPageInfo = Stream.of(new PageInfo(numberOfRecords / pageLimit + 1, numberOfRecords % pageLimit))
                .filter(pi -> pi.getPageLimit() != 0).collect(Collectors.toList());
        return Stream.concat(mostPageInfos.stream(), lastPageInfo.stream()).collect(Collectors.toList());
    }
}
