package com.amazon.aws.am2.appmig.estimate;

import java.nio.file.Path;

/**
 * To be implemented by any class acting as a filter. Filter is applied on each
 * and every file in the project directory. The
 * {@code public boolean filter(Path path)} method decides whether to perform
 * analysis or not on the respective file passed as input argument to the filter
 * method
 * 
 * @author agoteti
 *
 */
public interface IFilter {

    public boolean filter(Path path);
}
