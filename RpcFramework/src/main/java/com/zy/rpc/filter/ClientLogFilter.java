package com.zy.rpc.filter;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @package: com.zy.rpc.filter
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/4/27 17:33
 */
public class ClientLogFilter implements Filter{

    private Logger logger = LoggerFactory.getLogger(ClientLogFilter.class);

    @Override
    public void doFilter(FilterData filterData) {
        logger.info(filterData.toString());
    }
}
