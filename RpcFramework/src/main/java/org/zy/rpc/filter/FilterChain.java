package org.zy.rpc.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * @package: com.zy.rpc.filter
 * @author: zyakmd
 * @description: 拦截器链
 * @date: 2024/4/30 15:13
 */
public class FilterChain {

    private List<Filter> filters = new ArrayList<>();

    public void addFilter(Filter filter){ filters.add(filter);}

    public void addFilter(List<Object> filters){
        for (Object filter:filters){
            addFilter((Filter) filter);
        }
    }

    public void  doFilter(FilterData data){
        for (Filter filter : filters){
            filter.doFilter(data);
        }
    }
}
