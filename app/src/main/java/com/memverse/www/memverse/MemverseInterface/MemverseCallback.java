package com.memverse.www.memverse.MemverseInterface;

/**
 * Created by Joshua Swaim on 12/23/17.
 * This is simple interface to use for callbacks that consume one input.
 */

public interface MemverseCallback<T> {
    void call(T input);
}
